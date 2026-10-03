# DSHA

> DeepSeek Harness 的 Android 图形化安装器 · 基于 Termux

在 Android 手机上安装、启动并使用 [DeepSeek Harness](https://github.com/deepseek-ai/deepseek-harness)（`@deepseek-ai/dsh`）。
UI 使用 [Miuix](https://github.com/compose-miuix-ui/miuix)，与 [InstallerX Revived](https://github.com/wxxsfxyzm/InstallerX-Revived) 同一套设计体系。

## 当前进度

已完成可编译的应用骨架与 Termux 桥接：

- **悬浮底栏**（Miuix `FloatingNavigationBar`）承载三个页面：网页 / 终端 / 设置
- **安装引导**：环境未就绪时作为门禁展示，逐步引导
- **Termux 桥接**：走 Termux 公开的 `RUN_COMMAND` 接口执行命令并回收结果
- **终端页**：命令输入 + 输出回显 + 服务启停 / 日志查看
- **网页页**：WebView 承载 DSH Web UI（`http://127.0.0.1:3080`）

## 安装引导的五个步骤

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

## 与 Termux 的通信方式

使用 Termux 官方公开的 [RUN_COMMAND 接口](https://github.com/termux/termux-app/wiki/RUN_COMMAND-Intent)：
把命令交给 `com.termux.app.RunCommandService`，通过 `PendingIntent` 回传 stdout / stderr / 退出码。
常量取值参照 termux-app 源码 `TermuxConstants.java` 与 `ResultSender.java`，不依赖任何私有接口。

命令以 `setsid nohup` 方式启动 DSH 服务，使其脱离 RUN_COMMAND 的 app-shell 会话常驻。

## 终端页的说明

终端页是「Termux 命令 + 输出回显」的控制台，**不是完整的交互式 PTY**。
真正的 PTY 终端需要内置 Termux 的终端模拟器原生库（`terminal-emulator` / `terminal-view`），
属于后续版本的工作，当前版本先保证命令执行与输出可见这条主链路可用。

## 构建

**只通过 GitHub Actions 构建**，不在本地构建。推送后前往
[Actions](https://github.com/Youzix-Star/DSHA/actions) 下载 `dsha-debug-apk` 产物。

- 工具链：AGP 9.4.1 / Kotlin 2.4.20 / Compose BOM 2026.09.00 / JDK 25
- `compileSdk 37`，`minSdk 26`，`targetSdk 37`
- Miuix 0.9.4 来自 Maven Central，无需任何私有仓库鉴权

## 许可

GPL-3.0。详见 [LICENSE](LICENSE) 与 [NOTICE](NOTICE)。
