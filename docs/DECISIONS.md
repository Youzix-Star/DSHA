# 架构决策记录

## ADR-0001：通过 Termux 授权驱动，而非内置 Linux 环境

- **状态**：已采纳
- **决策**：DSHA 作为**独立包名的伴生应用**，通过 Termux 官方公开的 `RUN_COMMAND`
  接口驱动用户已安装的 Termux；**不**在应用内内置 Termux，也不内置任何 Linux 容器环境。

本文件记录该决策的依据与实测证据，避免后续重复调研。

---

### 背景

产品目标是"装上就能用、零外部依赖"。因此认真评估过三条"把环境内置进 APK"的路线，
结论是都不可取或代价过高，最终采纳外部 Termux 授权方案。

---

### 备选 A：改包名打包 Termux（内置 Termux 本体）——**不可行**

Termux 的 `$PREFIX` 是 `/data/data/com.termux/files/usr`。注意其中的 `com.termux`
**不是名字，而是 Android 数据目录的一部分**（`/data/data/<包名>/`），由 UID + SELinux
保证只有该包能写。因此"改成我们的包名"等价于"改掉整套二进制的运行路径"。

实测证据（在已安装 Termux 的设备上取得）：

| 绑定形式 | 实测结果 |
| :-- | :-- |
| ELF `RUNPATH`（动态库搜索路径） | `$PREFIX/bin` 下 **665 个 ELF 二进制，665 个** 均为 `/data/data/com.termux/files/usr/lib` |
| shebang | `$PREFIX/bin/pkg` → `#!/data/data/com.termux/files/usr/bin/bash`；`dsh` 同理 |
| dpkg/apt 元数据 | `$PREFIX/var/lib/dpkg/info/*.list` 记录绝对路径；apt 仓库的 `.deb` 亦按该前缀打包 |
| 安装器硬断言 | `TermuxInstaller.java:92` 要求应用数据目录等于 `/data/data/com.termux/files` |

关键点：`bash` 依赖的 `libandroid-support.so`、`libreadline.so.8`、`libiconv.so`
**只存在于 `$PREFIX/lib`，系统 `/system/lib64` 中不存在**，而定位它们唯一依靠 `RUNPATH`。
故前缀一旦变化，连 `bash` 都无法启动——不是功能缺失，是无法执行。

Termux 官方 README 亦明确：改包名需重建 bootstrap **以及其它软件包**（即整棵依赖树 + 自建 apt 源）。
这不是工程任务，而是重新制作一个发行版。

**结论**：只有 applicationId 保持 `com.termux` 才能复用官方 bootstrap 与 apt 仓库；
而那样就无法与用户已安装的 Termux 共存（同包名不同签名，Android 拒绝安装）。

---

### 备选 B：内置 proot + 发行版 rootfs（容器，独立包名）——**可行但已否决**

有成熟先例：**AstrBot Android App** 经 **Code LFA**（BSD-3-Clause）实现，
机制为 proot + proot-distro + Ubuntu 24.04 rootfs，native 二进制改名 `lib*.so`
放入 `jniLibs`（借 Android 解包机制获得可执行权限），包名为独立的 `com.nightmare.code`。

实测（本机 MEIZU 18 / Android 16，API 36）**proot 完全可用**：
`proot -r / -b /dev -b /proc -b /sys -0 id` 返回 `uid=0(root)`，
说明该机型的 SELinux 未拦截 ptrace。

> 勘误：本节早先误记为“华为 Mate 60 / HarmonyOS 4.2”，那是
> `deepseek-harness-android` README 里作者的另一台测试机，并非本机。
> 结论不受影响，但机型信息应以上述实测环境为准。

实测开销：

| 负载 | 原生 | proot 内 | 倍率 | 绝对增量 |
| :-- | --: | --: | --: | --: |
| CPU 密集 | 614 ms | 655 ms | 1.07x | +7% |
| HTTP 请求（DSH 的负载形态） | 0.62 ms/次 | 0.73 ms/次 | 1.18x | **+0.11 ms/次** |
| 进程创建 | 8.9 ms/次 | 33.4 ms/次 | 3.77x | +24.5 ms |
| 文件建+删 | 0.066 ms/个 | 0.35 ms/个 | 5.30x | +0.28 ms |
| proot 自身内存 | — | 4 MB RSS | — | 可忽略 |

运行期影响几乎不可感知，但**否决理由**是：引入容器层与约 62 MB 的 rootfs，
安装期系统调用开销放大 4~5 倍，且相比既有的 Termux 授权方案显著增加维护面与分发面。

---

### 备选 B′：不用 proot，直接以 glibc loader 运行——**已否决**

`$ROOTFS/lib/ld-linux-aarch64.so.1 --library-path ...` 可做到零 ptrace 开销，
但凡写死 `/usr` 的组件（npm 的 prefix、`#!/usr/bin/env` shebang、基于 `/proc/self/exe`
的解析）全部错乱，需要持续打补丁，`npm` 基本无法正常工作。

---

### 采纳方案 C：外部 Termux + `RUN_COMMAND` 授权

#### 正面

- **运行期零额外开销**：命令在 Termux 中由原生二进制直接执行，无 ptrace 层。
- **分发面最小**：APK 中不分发 Ubuntu rootfs，也不分发 proot/bash/busybox 等 GPL 二进制。
- **复用既有成果**：Termux 与 `deepseek-harness-android` 已趟平 Android/bionic 兼容问题
  （`node-pty` 的 `android_ndk_path`、`koffi` 的 `statx`、`link()`→`rename()`、
  `sharp` wasm 回退、`--expose-internals` 等）。

#### 负面：必须由安装引导消化

| 前提 | 为何无法由应用代劳 |
| :-- | :-- |
| 用户需自行安装 Termux（F-Droid / GitHub 版） | 应用无法静默安装其它 APK |
| 授予 `com.termux.permission.RUN_COMMAND` | 该权限由 Termux 声明为 `dangerous`，必须运行时授权 |
| 开启 `allow-external-apps = true` | 该配置位于 Termux 私有目录，其它应用无权写入，只能在 Termux 内执行一次命令 |

以上三项即安装引导的前三步，详见 `README.md`。

---

### 复现证据的命令

```bash
P=/data/data/com.termux/files/usr
# 1. RUNPATH 全部指向死路径
readelf -d $P/bin/bash | grep -i runpath
# 2. 依赖库只在 Termux 前缀里，系统目录没有
ls $P/lib/libandroid-support.so /system/lib64/libandroid-support.so
# 3. shebang 是绝对路径
head -1 $P/bin/pkg
# 4. proot 在本机可用（ptrace 未被 SELinux 拦截）
$P/bin/proot -r / -b /dev -b /proc -b /sys -0 $P/bin/id
```
