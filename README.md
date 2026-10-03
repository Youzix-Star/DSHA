# DSHA

> DeepSeek Harness 的 Android 图形化安装器 · 基于 Termux

在 Android 手机上安装、启动并使用 [DeepSeek Harness](https://github.com/deepseek-ai/deepseek-harness)（`@deepseek-ai/dsh`）。
UI 使用 [Miuix](https://github.com/compose-miuix-ui/miuix)，与 [InstallerX Revived](https://github.com/wxxsfxyzm/InstallerX-Revived) 同一套设计体系。

## 状态

早期开发中。当前已具备可编译的应用骨架：悬浮底栏 + 三个页面（网页 / 终端 / 设置）。

## 计划

| 页面 | 说明 |
| :-- | :-- |
| 网页 | 内嵌 WebView，承载 DSH 的 Web UI（默认 `http://127.0.0.1:3080`） |
| 终端 | 通过 Termux 执行命令并实时显示输出 |
| 设置 | Miuix 风格设置项，管理服务地址、自动启动等 |

安装引导流程将覆盖：Termux 检测 → 开启 `allow-external-apps` → 安装脚本执行 → 启动 DSH。

## 构建

**只通过 GitHub Actions 构建**，不在本地构建。推送后前往
[Actions](https://github.com/Youzix-Star/DSHA/actions) 下载 `dsha-debug-apk` 产物。

## 许可

GPL-3.0。详见 [LICENSE](LICENSE) 与 [NOTICE](NOTICE)。
