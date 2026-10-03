/*
 * Copyright 2026, Youzix-Star
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.youzixstar.dsha.ui.miuix.guide

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

/** 在 Termux 里执行一次即可开启外部应用调用。本应用无权写 Termux 的私有目录。 */
private const val ALLOW_EXTERNAL_APPS_CMD =
    "echo 'allow-external-apps = true' >> ~/.termux/termux.properties && termux-reload-settings"

@Composable
fun GuideScreen(
    controller: DshaController,
    contentPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
    onNotify: (String) -> Unit,
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { controller.refresh() }

    var elapsed by remember { mutableIntStateOf(0) }
    LaunchedEffect(controller.busy) {
        elapsed = 0
        while (controller.busy) {
            delay(1000)
            elapsed++
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .overScrollVertical(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "intro") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                insideMargin = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
            ) {
                Text(
                    text = "DSHA 通过 Termux 在本机安装并运行 DSH。下面几项确认完成后即可使用完整功能。",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurface,
                )
            }
        }

        item(key = "step-1") {
            StepCard(
                index = 1,
                title = "安装 Termux",
                done = controller.termuxInstalled,
                body = if (controller.termuxInstalled) {
                    "已检测到 Termux。"
                } else {
                    "未检测到 Termux。请安装 F-Droid 版或 GitHub Releases 版；" +
                        "Google Play 版已停止更新，不要使用。"
                },
            ) {
                if (!controller.termuxInstalled) {
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
        }

        item(key = "step-2") {
            StepCard(
                index = 2,
                title = "授予 RUN_COMMAND 权限",
                done = controller.permissionGranted,
                body = "DSHA 需要调用 Termux 的 RUN_COMMAND 接口来安装与启动服务，" +
                    "该权限由 Termux 声明，需要手动授予。",
            ) {
                if (!controller.permissionGranted) {
                    Button(
                        onClick = {
                            permissionLauncher.launch(TermuxBridge.PERMISSION_RUN_COMMAND)
                        },
                    ) {
                        Text("请求权限")
                    }
                }
            }
        }

        item(key = "step-3") {
            StepCard(
                index = 3,
                title = "开启 Termux 外部调用",
                done = controller.bridgeOk,
                body = if (controller.blockedByAllowExternalApps) {
                    "Termux 拒绝了外部调用：需要在其配置里打开 allow-external-apps。" +
                        "请在 Termux 中粘贴执行下面这行命令。"
                } else if (controller.bridgeOk) {
                    "已连通，可以正常在 Termux 中执行命令。"
                } else {
                    "完成前两项后会自动检测；若被拒绝，这里会给出开启方法。"
                },
            ) {
                if (controller.blockedByAllowExternalApps) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        insideMargin = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    ) {
                        Text(
                            text = ALLOW_EXTERNAL_APPS_CMD,
                            fontFamily = FontFamily.Monospace,
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                copyToClipboard(context, ALLOW_EXTERNAL_APPS_CMD)
                                onNotify("命令已复制")
                            },
                        ) {
                            Text("复制命令")
                        }
                        Button(onClick = { openTermux(context) }) {
                            Text("打开 Termux")
                        }
                    }
                }
                Button(
                    onClick = { controller.refresh() },
                    enabled = !controller.checking && !controller.busy,
                ) {
                    Text(if (controller.checking) "检测中…" else "重新检测")
                }
            }
        }

        item(key = "step-4") {
            StepCard(
                index = 4,
                title = "安装 DSHA 运行环境",
                done = controller.dshBinAvailable,
                body = if (controller.dshBinAvailable) {
                    "dsh 已就绪（版本 ${controller.dshVersion}，Node ${controller.nodeVersion}）。" +
                        "如需重新安装或升级，可再次执行。"
                } else {
                    "将克隆安装脚本仓库并执行 setup.sh：安装构建依赖、下载 dsh 并完成 Android " +
                        "兼容修复。首次需要 5~15 分钟，请保持应用在前台。"
                },
            ) {
                Button(
                    onClick = { controller.installRuntime() },
                    enabled = controller.bridgeOk && !controller.busy,
                ) {
                    Text(if (controller.dshBinAvailable) "重新安装 / 升级" else "开始安装")
                }
            }
        }

        item(key = "step-5") {
            StepCard(
                index = 5,
                title = "启动 DSH 服务",
                done = controller.serverRunning,
                body = if (controller.serverRunning) {
                    "服务正在运行，监听 http://127.0.0.1:3080。可以切到「网页」页签使用。"
                } else {
                    "启动 DSH 的 Web 服务；随后即可在「网页」页签中使用。"
                },
            ) {
                Button(
                    onClick = { controller.startServer() },
                    enabled = controller.dshBinAvailable &&
                        !controller.busy &&
                        !controller.serverRunning,
                ) {
                    Text("启动服务")
                }
            }
        }

        if (controller.busy || controller.setupLog.isNotEmpty()) {
            item(key = "output") {
                Column {
                    SmallTitle(text = "执行输出")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    ) {
                        if (controller.busy) {
                            Text(
                                text = "${controller.busyLabel} … 已用 ${elapsed}s",
                                style = MiuixTheme.textStyles.footnote1,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        Text(
                            text = controller.setupLog.joinToString("\n").ifBlank { "(等待输出)" },
                            fontFamily = FontFamily.Monospace,
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 引导里的单步卡片。
 *
 * 卡内是裸 Column/Text，miuix 的 Card 自身内边距为 0，所以这里必须显式给 insideMargin。
 */
@Composable
private fun StepCard(
    index: Int,
    title: String,
    done: Boolean,
    body: String,
    actions: @Composable () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row {
            Text(
                text = if (done) "✓" else "$index",
                style = MiuixTheme.textStyles.title4,
                fontWeight = FontWeight.Bold,
                color = if (done) {
                    MiuixTheme.colorScheme.primary
                } else {
                    MiuixTheme.colorScheme.onSurfaceVariantSummary
                },
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = body,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                actions()
            }
        }
    }
}
