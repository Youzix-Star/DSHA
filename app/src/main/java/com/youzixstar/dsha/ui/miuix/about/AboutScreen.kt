/*
 * Copyright 2026, Youzix-Star
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.youzixstar.dsha.ui.miuix.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.youzixstar.dsha.BuildConfig
import com.youzixstar.dsha.setup.DshaController
import com.youzixstar.dsha.ui.AppIcons
import com.youzixstar.dsha.ui.miuix.DebugInfoDialog
import com.youzixstar.dsha.ui.openUrl
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

private const val REPOSITORY_URL = "https://github.com/Youzix-Star/DSHA"
private const val DEVELOPER_URL = "https://github.com/Youzix-Star"
private const val TERMUX_URL = "https://github.com/termux/termux-app"
private const val DSH_URL = "https://github.com/deepseek-ai/deepseek-harness"

/** 连点判定：1 秒内累计 3 次为一次连击。 */
private const val TAP_WINDOW_MS = 1000L
private const val TAPS_REQUIRED = 3

@Composable
fun AboutScreen(
    controller: DshaController,
    contentPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
    onNotify: (String) -> Unit,
) {
    val context = LocalContext.current
    var showDebug by remember { mutableStateOf(false) }
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapAt by remember { mutableLongStateOf(0L) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .overScrollVertical(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // 与应用图标同源的标记（箭头 + 下划线），用文本画而不是加载自适应图标位图。
                Text(
                    text = ">_",
                    fontSize = 52.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MiuixTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "DSHA",
                    style = MiuixTheme.textStyles.title1,
                    color = MiuixTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "DeepSeek Harness 的 Android 安装器",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Ciallo～(∠・ω c)⌒★",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable {
                        val now = System.currentTimeMillis()
                        if (now - lastTapAt > TAP_WINDOW_MS) tapCount = 0
                        lastTapAt = now
                        tapCount++
                        if (tapCount >= TAPS_REQUIRED) {
                            tapCount = 0
                            val enabled = !controller.developerMode
                            controller.updateDeveloperMode(enabled)
                            onNotify(if (enabled) "开发者模式已开启" else "开发者模式已关闭")
                            if (enabled) showDebug = true
                        }
                    },
                )
            }
        }

        item(key = "about") {
            Column {
                SmallTitle(text = "关于")
                Card(modifier = Modifier.fillMaxWidth()) {
                    ArrowPreference(
                        title = "获取源代码",
                        summary = "GitHub 上的源码",
                        startAction = {
                            Icon(
                                imageVector = AppIcons.SourceCode,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        onClick = { openUrl(context, REPOSITORY_URL) },
                    )
                    ArrowPreference(
                        title = "开源许可",
                        summary = "AGPL-3.0",
                        startAction = {
                            Icon(
                                imageVector = AppIcons.License,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        onClick = { openUrl(context, "$REPOSITORY_URL/blob/main/NOTICE") },
                    )
                    ArrowPreference(
                        title = "开发者",
                        summary = "Youzix_Star",
                        startAction = {
                            Icon(
                                imageVector = AppIcons.Developer,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        onClick = { openUrl(context, DEVELOPER_URL) },
                    )
                }
            }
        }

        item(key = "dependencies") {
            Column {
                SmallTitle(text = "依赖")
                Card(modifier = Modifier.fillMaxWidth()) {
                    ArrowPreference(
                        title = "Termux",
                        summary = "运行宿主环境",
                        onClick = { openUrl(context, TERMUX_URL) },
                    )
                    ArrowPreference(
                        title = "DeepSeek Harness",
                        summary = "本安装器托管的目标程序",
                        onClick = { openUrl(context, DSH_URL) },
                    )
                }
            }
        }

        if (controller.developerMode) {
            item(key = "debug") {
                Column {
                    SmallTitle(text = "调试")
                    Card(modifier = Modifier.fillMaxWidth()) {
                        ArrowPreference(
                            title = "调试信息",
                            summary = "系统、WebView 引擎与运行状态快照",
                            startAction = {
                                Icon(
                                    imageVector = AppIcons.Rule,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp),
                                )
                            },
                            onClick = { showDebug = true },
                        )
                        ArrowPreference(
                            title = "关闭开发者模式",
                            summary = "藏起这一区，也可再次连点彩蛋切换",
                            onClick = {
                                controller.updateDeveloperMode(false)
                                onNotify("开发者模式已关闭")
                            },
                        )
                    }
                }
            }
        }
    }

    if (showDebug) {
        DebugInfoDialog(
            controller = controller,
            onDismiss = { showDebug = false },
            onNotify = onNotify,
        )
    }
}
