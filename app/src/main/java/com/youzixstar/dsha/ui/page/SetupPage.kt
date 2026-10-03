package com.youzixstar.dsha.ui.page

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.youzixstar.dsha.setup.DshaController
import com.youzixstar.dsha.termux.TermuxBridge
import com.youzixstar.dsha.ui.TERMUX_FDROID_URL
import com.youzixstar.dsha.ui.TERMUX_RELEASE_URL
import com.youzixstar.dsha.ui.copyToClipboard
import com.youzixstar.dsha.ui.openTermux
import com.youzixstar.dsha.ui.openUrl
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text

/** 在 Termux 里执行一次即可开启外部应用调用（本应用无法代改 Termux 私有目录） */
private const val ALLOW_EXTERNAL_APPS_CMD =
    "echo 'allow-external-apps = true' >> ~/.termux/termux.properties && termux-reload-settings"

@Composable
fun SetupPage(
    controller: DshaController,
    onOpenWeb: () -> Unit,
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { controller.refresh() }

    var elapsed by remember { mutableStateOf(0) }
    LaunchedEffect(controller.busy) {
        elapsed = 0
        while (controller.busy) {
            delay(1000)
            elapsed++
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
    ) {
        Spacer(Modifier.height(12.dp))

        Card {
            Text(
                text = "欢迎使用 DSHA",
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text("DSHA 是 DeepSeek Harness 的 Android 图形化安装器。它通过 Termux 在本机安装并运行 DSH，再用内置 WebView 承载 DSH 的网页界面。下面几项确认完成后即可使用。")
        }

        Spacer(Modifier.height(12.dp))

        // ---------- 1. Termux ----------
        Card {
            StepTitle(1, "安装 Termux", controller.termuxInstalled)
            Text(
                if (controller.termuxInstalled) {
                    "已检测到 Termux。"
                } else {
                    "未检测到 Termux。请安装 F-Droid 版或 GitHub Releases 版；Google Play 版已停止更新，不要使用。"
                },
            )
            if (!controller.termuxInstalled) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { openUrl(context, TERMUX_FDROID_URL) }) {
                        Text("F-Droid 下载")
                    }
                    Button(onClick = { openUrl(context, TERMUX_RELEASE_URL) }) {
                        Text("GitHub 下载")
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---------- 2. RUN_COMMAND 权限 ----------
        Card {
            StepTitle(2, "授予 RUN_COMMAND 权限", controller.permissionGranted)
            Text("DSHA 需要调用 Termux 的 RUN_COMMAND 接口来安装与启动服务，该权限由 Termux 声明，需要手动授予。")
            if (!controller.permissionGranted) {
                Spacer(Modifier.height(8.dp))
                Button(onClick = { permissionLauncher.launch(TermuxBridge.PERMISSION_RUN_COMMAND) }) {
                    Text("请求权限")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---------- 3. allow-external-apps ----------
        Card {
            StepTitle(3, "开启 Termux 外部调用", controller.bridgeOk)
            if (controller.blockedByAllowExternalApps) {
                Text("Termux 拒绝了外部调用：需要在其配置里打开 allow-external-apps。请在 Termux 中粘贴执行下面这行命令（或手动编辑 ~/.termux/termux.properties，取消该行的注释）：")
                Spacer(Modifier.height(8.dp))
                Text(
                    text = ALLOW_EXTERNAL_APPS_CMD,
                    fontFamily = FontFamily.Monospace,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { copyToClipboard(context, ALLOW_EXTERNAL_APPS_CMD) }) {
                        Text("复制命令")
                    }
                    Button(onClick = { openTermux(context) }) {
                        Text("打开 Termux")
                    }
                    Button(onClick = { controller.refresh() }) {
                        Text("重新检测")
                    }
                }
            } else if (controller.bridgeOk) {
                Text("已连通，可以正常在 Termux 中执行命令。")
            } else {
                Text("这一步在完成前两项后自动检测。若提示被拒绝，这里会给出开启方法。")
                Spacer(Modifier.height(8.dp))
                Button(onClick = { controller.refresh() }, enabled = !controller.checking) {
                    Text(if (controller.checking) "检测中…" else "重新检测")
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---------- 4. 安装运行环境 ----------
        Card {
            StepTitle(4, "安装 DSHA 运行环境", controller.dshBinAvailable)
            Text(
                if (controller.dshBinAvailable) {
                    "dsh 已就绪（版本 ${controller.dshVersion}，Node ${controller.nodeVersion}）。如需重新安装或升级，可再次执行。"
                } else {
                    "将克隆安装脚本仓库并执行 setup.sh：安装构建依赖、下载 dsh 并完成 Android 兼容修复。首次需要 5~15 分钟，请保持应用在前台。"
                },
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { controller.installRuntime() },
                enabled = controller.bridgeOk && !controller.busy,
            ) {
                Text(if (controller.dshBinAvailable) "重新安装 / 升级" else "开始安装")
            }
        }

        Spacer(Modifier.height(12.dp))

        // ---------- 5. 启动服务 ----------
        Card {
            StepTitle(5, "启动 DSH 服务", controller.serverRunning)
            Text(
                if (controller.serverRunning) {
                    "服务正在运行，监听 http://127.0.0.1:3080。"
                } else {
                    "启动 DSH 的 Web 服务；随后即可在「网页」标签中使用。"
                },
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { controller.startServer() },
                    enabled = controller.dshBinAvailable && !controller.busy && !controller.serverRunning,
                ) {
                    Text("启动服务")
                }
                Button(
                    onClick = onOpenWeb,
                    enabled = controller.serverRunning,
                ) {
                    Text("打开网页")
                }
            }
        }

        // ---------- 执行状态 ----------
        if (controller.busy || controller.setupLog.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            SmallTitle("执行输出")
            Card {
                if (controller.busy) {
                    Text("${controller.busyLabel} … 已用 ${elapsed}s")
                    Spacer(Modifier.height(6.dp))
                }
                Text(
                    text = controller.setupLog.joinToString("\n").ifBlank { "(等待输出)" },
                    fontFamily = FontFamily.Monospace,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = { controller.refresh() },
                enabled = !controller.checking && !controller.busy,
            ) {
                Text(if (controller.checking) "检测中…" else "全部重新检测")
            }
            Button(onClick = { controller.dismissSetup() }) {
                Text("跳过，先看看")
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StepTitle(index: Int, title: String, done: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = if (done) "✓" else "○",
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$index. $title",
            fontWeight = FontWeight.SemiBold,
        )
    }
    Spacer(Modifier.height(4.dp))
}
