# DSHA

> DeepSeek Harness 的 Android 图形化安装器 · 基于 Termux

在 Android 手机上安装、启动并使用 [DeepSeek Harness](https://github.com/deepseek-ai/deepseek-harness)（`@deepseek-ai/dsh`）。

界面与 [NekoPlus](https://github.com/Youzix-Star/NekoPlus) 同一套框架：Miuix 组件库 + 液态玻璃悬浮底栏 + 大标题顶栏。

## 下载安装

最新版本可从 [Releases](https://github.com/Youzix-Star/DSHA/releases/latest) 下载，
也可以在每个提交的 [Actions](https://github.com/Youzix-Star/DSHA/actions) 运行页里取 `dsha-debug-apk` 产物。

产物是 **debug 签名**的 APK，可直接安装试用（同一版本覆盖安装即可升级）。

## 界面结构

四个页签，可左右滑动切换：

| 页签 | 内容 |
| :-- | :-- |
| 首页 | 主状态卡（DSH 服务）+ Termux 桥接、运行环境两张紧凑卡 + 三项统计 + 概览 |
| 网页 | 内嵌 WebView，承载 DSH 的 Web UI（默认 `http://127.0.0.1:3080`） |
| 终端 | 命令输入与输出回显，可启停服务、查看日志 |
| 设置 | 主题、自动启动、屏幕常亮、液态玻璃，以及运行环境信息 |

一级返回从任意页签回到首页；「环境配置引导」是二级页面，带独立返回。

## 内边距约定（重要）

**miuix 的 `Card` 默认内边距是 0**（`CardDefaults.InsideMargin = PaddingValues(0.dp)`），
所以卡内直接放裸 `Text` / `Row` 时文字会紧贴卡片边缘。本项目遵循两条规则：

| 卡内内容 | 做法 |
| :-- | :-- |
| miuix 偏好组件（`SwitchPreference`、`ArrowPreference`、`BasicComponent` 等） | **不要**给 Card 设 `insideMargin`——这些组件自带 16dp，外层再给会双重留白 |
| 裸 `Text` / `Row` / `Column` | **必须**显式给 `insideMargin`（首页主状态卡 20dp、紧凑卡与统计卡 16~18dp、引导步骤卡 20/18dp） |

页面级留白不手写：外壳把 `Scaffold` 的 `innerPadding` 加上 12dp 后作为
`contentPadding` 交给每个页面，页面用 `LazyColumn(contentPadding = ...)` 消费。

## 安装引导的五个步骤

环境未就绪时，首页的状态卡会直接指出缺哪一项，点一下即执行对应的修复动作；
「设置 → 环境配置引导」里有完整的逐步说明。

| 步骤 | 检测方式 | 需要用户做什么 |
| :-- | :-- | :-- |
| 1. 安装 Termux | `PackageManager` 查询 `com.termux` | 安装 F-Droid 版（**不要**用 Google Play 版） |
| 2. 授予 RUN_COMMAND 权限 | `checkSelfPermission` | 点「请求权限」授权 |
| 3. 开启外部调用 | 探针命令的返回错误里是否含 `allow-external-apps` | 在 Termux 里粘贴执行一行命令 |
| 4. 安装运行环境 | 状态脚本回报 `dsh_bin=yes` | 点「开始安装」，等 5~15 分钟 |
| 5. 启动服务 | 探测 `127.0.0.1:3080` | 点「启动服务」 |

第 2、3 步无法由本应用代劳：`RUN_COMMAND` 是 Termux 声明的 dangerous 权限，需要运行时授权；
而 `allow-external-apps` 位于 Termux 的私有目录，其它应用无权写入，因此引导用户在 Termux 内执行：

```bash
echo 'allow-external-apps = true' >> ~/.termux/termux.properties && termux-reload-settings
```

## 架构决策

本项目**有意选择**通过 Termux 授权驱动，而非在 APK 内内置 Termux 或 Linux 容器。
这一决策的实测依据（含 `RUNPATH`、proot 性能数据与备选方案排除理由）记录在
[`docs/DECISIONS.md`](docs/DECISIONS.md)，避免后续重复调研。

## 与 Termux 的通信方式

使用 Termux 官方公开的 [RUN_COMMAND 接口](https://github.com/termux/termux-app/wiki/RUN_COMMAND-Intent)：
把命令交给 `com.termux.app.RunCommandService`，通过 `PendingIntent` 回传 stdout / stderr / 退出码。
常量取值参照 termux-app 源码 `TermuxConstants.java` 与 `ResultSender.java`，不依赖任何私有接口。

命令以 `setsid nohup` 方式启动 DSH 服务，使其脱离 RUN_COMMAND 的 app-shell 会话常驻。

## 终端页的说明

终端页是「Termux 命令 + 输出回显」的控制台，**不是完整的交互式 PTY**。
真正的 PTY 终端需要内置终端模拟器原生库，属于后续版本的工作。

## 构建

**只通过 GitHub Actions 构建**，不在本地构建。推送后前往
[Actions](https://github.com/Youzix-Star/DSHA/actions) 下载 `dsha-debug-apk` 产物。

- 工具链：AGP 9.4.1 / Kotlin 2.4.20 / Compose BOM 2026.09.00 / JDK 25
- `compileSdk 37`，`minSdk 33`，`targetSdk 37`
  （minSdk 33 是对齐 NekoPlus：液态玻璃底栏依赖 `miuix-blur`，其 AAR 声明 minSdk 33）
- Miuix 0.9.4 与 material-icons-extended 1.7.8 分别来自 Maven Central 与 Google Maven，均无需鉴权

## 许可

AGPL-3.0。详见 [LICENSE](LICENSE) 与 [NOTICE](NOTICE)。
