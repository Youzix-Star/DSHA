/*
 * Copyright 2026, Youzix-Star
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.youzixstar.dsha.ui.miuix.settings

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
import com.youzixstar.dsha.DSH_WEB_URL
import com.youzixstar.dsha.setup.DshaController
import com.youzixstar.dsha.ui.AppIcons
import com.youzixstar.dsha.ui.miuix.DebugInfoDialog
import com.youzixstar.dsha.ui.miuix.ThemeModeOptions
import com.youzixstar.dsha.ui.openUrl
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical

private const val REPOSITORY_URL = "https://github.com/Youzix-Star/DSHA"
private const val DEVELOPER_URL = "https://github.com/Youzix-Star"
private const val TERMUX_URL = "https://github.com/termux/termux-app"
private const val DSH_URL = "https://github.com/deepseek-ai/deepseek-harness"

/** 连点彩蛋判定：1 秒内累计 3 次。 */
private const val TAP_WINDOW_MS = 1000L
private const val TAPS_REQUIRED = 3

/**
 * 设置页（已并入原「关于」页）。
 *
 * 卡内放 miuix 偏好组件时不设 `insideMargin`：这些组件自带 16dp 内边距，
 * 外层再给会双重留白。只有卡内是裸 Text/Row 时才需要自己补。
 *
 * 选项类设置一律用 [WindowSpinnerPreference] 一行点开选择器，不铺开成一行一项。
 */
@Composable
fun SettingsScreen(
    controller: DshaController,
    contentPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
    onOpenGuide: () -> Unit,
    onNotify: (String) -> Unit,
) {
    val context = LocalContext.current
    val themeItems = remember { ThemeModeOptions.map { DropdownItem(text = it.second) } }

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
        item(key = "appearance") {
            Column {
                SmallTitle(text = "外观")
                Card(modifier = Modifier.fillMaxWidth()) {
                    WindowSpinnerPreference(
                        title = "主题模式",
                        items = themeItems,
                        selectedIndex = controller.themeModeIndex.coerceIn(0, themeItems.lastIndex),
                        onSelectedIndexChange = { controller.updateThemeModeIndex(it) },
                    )
                }
            }
        }

        item(key = "usage") {
            Column {
                SmallTitle(text = "使用")
                Card(modifier = Modifier.fillMaxWidth()) {
                    SwitchPreference(
                        title = "进入应用时自动启动服务",
                        summary = "打开 DSHA 且服务未运行时自动拉起 DSH",
                        checked = controller.autoStart,
                        onCheckedChange = { controller.updateAutoStart(it) },
                    )
                    SwitchPreference(
                        title = "保持屏幕常亮",
                        summary = "长时间使用 Web UI 时避免息屏",
                        checked = controller.keepScreenOn,
                        onCheckedChange = { controller.updateKeepScreenOn(it) },
                    )
                }
            }
        }

        item(key = "runtime") {
            Column {
                SmallTitle(text = "运行环境")
                Card(modifier = Modifier.fillMaxWidth()) {
                    ArrowPreference(
                        title = "环境配置引导",
                        summary = "检测 Termux、权限与服务状态",
                        startAction = {
                            Icon(
                                imageVector = AppIcons.Grant,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                        onClick = onOpenGuide,
                    )
                    ArrowPreference(
                        title = "dsh 版本",
                        summary = controller.dshVersion,
                        enabled = false,
                    )
                    ArrowPreference(
                        title = "Node 版本",
                        summary = controller.nodeVersion,
                        enabled = false,
                    )
                    ArrowPreference(
                        title = "Web UI 地址",
                        summary = DSH_WEB_URL,
                        enabled = false,
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
                        SwitchPreference(
                            title = "开发者模式",
                            summary = "在下方连点彩蛋三次可切换",
                            checked = true,
                            onCheckedChange = { controller.updateDeveloperMode(it) },
                        )
                    }
                }
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

                // 应用标记用文本画，而不是加载自适应图标位图
                Spacer(modifier = Modifier.height(20.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = ">_",
                        fontSize = 40.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MiuixTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "DSHA v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
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
