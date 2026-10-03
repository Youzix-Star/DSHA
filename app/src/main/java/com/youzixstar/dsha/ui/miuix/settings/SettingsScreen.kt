/*
 * Copyright 2026, Youzix-Star
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.youzixstar.dsha.ui.miuix.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.youzixstar.dsha.BuildConfig
import com.youzixstar.dsha.DSH_WEB_URL
import com.youzixstar.dsha.setup.DshaController
import com.youzixstar.dsha.ui.miuix.ThemeModeOptions
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.utils.overScrollVertical

/**
 * 设置页。
 *
 * 卡内放 miuix 偏好组件时不设 `insideMargin`：这些组件自带 16dp 内边距，
 * 外层再给会双重留白。只有卡内是裸 Text/Row 时才需要自己补。
 */
@Composable
fun SettingsScreen(
    controller: DshaController,
    contentPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
    onOpenGuide: () -> Unit,
    onNotify: (String) -> Unit,
) {
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
                    ThemeModeOptions.forEachIndexed { index, (_, label) ->
                        RadioButtonPreference(
                            title = label,
                            selected = controller.themeModeIndex == index,
                            onClick = {
                                controller.updateThemeModeIndex(index)
                                onNotify("主题已切换为$label")
                            },
                        )
                    }
                }
            }
        }

        item(key = "usage") {
            Column {
                SmallTitle(text = "使用")
                Card(modifier = Modifier.fillMaxWidth()) {
                    SwitchPreference(
                        checked = controller.autoStart,
                        onCheckedChange = { controller.updateAutoStart(it) },
                        title = "进入应用时自动启动服务",
                        summary = "打开 DSHA 且服务未运行时自动拉起 DSH",
                    )
                    SwitchPreference(
                        checked = controller.keepScreenOn,
                        onCheckedChange = { controller.updateKeepScreenOn(it) },
                        title = "保持屏幕常亮",
                        summary = "长时间使用 Web UI 时避免息屏",
                    )
                    SwitchPreference(
                        checked = controller.useLiquidGlass,
                        onCheckedChange = { controller.updateUseLiquidGlass(it) },
                        title = "液态玻璃",
                        summary = "底栏与内容层使用背景模糊",
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

        item(key = "about") {
            Column {
                SmallTitle(text = "关于")
                Card(modifier = Modifier.fillMaxWidth()) {
                    ArrowPreference(
                        title = "版本",
                        summary = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        enabled = false,
                    )
                    ArrowPreference(
                        title = "开源许可",
                        summary = "AGPL-3.0",
                        enabled = false,
                    )
                }
            }
        }
    }
}
