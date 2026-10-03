/*
 * Copyright 2026, Youzix-Star
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.youzixstar.dsha.ui.miuix.home

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.youzixstar.dsha.BuildConfig
import com.youzixstar.dsha.setup.DshaController
import com.youzixstar.dsha.ui.AppIcons
import top.yukonga.miuix.kmp.basic.BasicComponent
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType
import top.yukonga.miuix.kmp.utils.overScrollVertical

@Composable
fun HomeScreen(
    controller: DshaController,
    contentPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
    onNotify: (String) -> Unit,
    onOpenGuide: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .overScrollVertical(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 主状态块：这一台设备上 DSHA 现在到底在做什么，以及点一下能做什么。
        item(key = "status-service") {
            val active = controller.serverRunning
            StatusCard(
                active = active,
                icon = if (active) AppIcons.Play else AppIcons.Update,
                title = when {
                    !controller.bridgeOk -> "需要先配置 Termux"
                    !controller.dshBinAvailable -> "运行环境尚未安装"
                    controller.serverRunning -> "DSH 正在运行"
                    else -> "DSH 未运行"
                },
                description = when {
                    !controller.bridgeOk -> "尚未连通 Termux"
                    !controller.dshBinAvailable -> "缺少 dsh 命令"
                    controller.serverRunning -> "已在 127.0.0.1:3080 提供服务"
                    else -> "当前已停止"
                },
                hint = when {
                    !controller.bridgeOk -> "点击查看引导"
                    !controller.dshBinAvailable -> "点击开始安装"
                    controller.serverRunning -> "点击停止"
                    else -> "点击启动"
                },
                onClick = {
                    if (controller.busy) return@StatusCard
                    when {
                        !controller.bridgeOk -> onOpenGuide()
                        !controller.dshBinAvailable -> {
                            controller.installRuntime()
                            onNotify("开始安装运行环境")
                        }
                        controller.serverRunning -> {
                            controller.stopServer()
                            onNotify("正在停止 DSH")
                        }
                        else -> {
                            controller.startServer()
                            onNotify("正在启动 DSH")
                        }
                    }
                },
            )
        }

        item(key = "status-bridge") {
            CompactStatusCard(
                active = controller.bridgeOk,
                icon = AppIcons.Grant,
                title = if (controller.bridgeOk) "Termux 已连通" else "Termux 未连通",
                summary = if (controller.bridgeOk) {
                    "可在本机执行命令"
                } else {
                    controller.bridgeError ?: "需要安装 Termux 并授予权限"
                },
                hint = if (controller.bridgeOk) "已连接" else "去修复",
                onClick = {
                    if (controller.bridgeOk) {
                        controller.refresh()
                        onNotify("已重新检测")
                    } else {
                        onOpenGuide()
                    }
                },
            )
        }

        item(key = "status-runtime") {
            CompactStatusCard(
                active = controller.dshBinAvailable,
                icon = AppIcons.Tune,
                title = if (controller.dshBinAvailable) "运行环境已就绪" else "运行环境未安装",
                summary = if (controller.dshBinAvailable) {
                    "dsh ${controller.dshVersion} · Node ${controller.nodeVersion}"
                } else {
                    "需要安装 dsh 与 Node"
                },
                hint = if (controller.dshBinAvailable) "已就绪" else "去安装",
                onClick = {
                    if (controller.dshBinAvailable) {
                        controller.refresh()
                        onNotify("已重新检测")
                    } else {
                        controller.installRuntime()
                        onNotify("开始安装运行环境")
                    }
                },
            )
        }

        item(key = "stats") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatisticCard(
                    title = "服务",
                    value = if (controller.serverRunning) "运行中" else "已停止",
                    modifier = Modifier.weight(1f),
                )
                StatisticCard(
                    title = "桥接",
                    value = if (controller.bridgeOk) "已连通" else "未连通",
                    modifier = Modifier.weight(1f),
                )
                StatisticCard(
                    title = "环境",
                    value = if (controller.dshBinAvailable) "已就绪" else "未安装",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        item(key = "overview") {
            Column {
                SmallTitle(text = "概览")
                // 卡内是 BasicComponent：它自带 16dp 内边距，外层再给就会双重留白，
                // 所以这里不设 insideMargin。反之若卡内是裸 Text/Row，则必须自己补。
                Card(modifier = Modifier.fillMaxWidth()) {
                    BasicComponent(
                        title = "应用版本",
                        summary = BuildConfig.VERSION_NAME,
                        startAction = {
                            Icon(
                                imageVector = AppIcons.About,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                    )
                    BasicComponent(
                        title = "系统版本",
                        summary = "Android ${Build.VERSION.RELEASE}",
                        startAction = {
                            Icon(
                                imageVector = AppIcons.Phones,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                    )
                    BasicComponent(
                        title = "Web 地址",
                        summary = "http://127.0.0.1:3080",
                        startAction = {
                            Icon(
                                imageVector = AppIcons.Web,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp),
                            )
                        },
                    )
                }
            }
        }
    }
}

/**
 * 首页顶部的主状态块：一块按状态着色的可点区域，直接说清现在在做什么。
 * 与 NekoPlus 首页的状态卡同一套语言（primaryContainer / errorContainer 双色）。
 */
@Composable
private fun StatusCard(
    active: Boolean,
    icon: ImageVector,
    title: String,
    description: String,
    hint: String,
    onClick: () -> Unit,
) {
    val containerColor = if (active) {
        MiuixTheme.colorScheme.primaryContainer
    } else {
        MiuixTheme.colorScheme.errorContainer
    }
    val contentColor = if (active) {
        MiuixTheme.colorScheme.onPrimaryContainer
    } else {
        MiuixTheme.colorScheme.onErrorContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        colors = CardDefaults.defaultColors(color = containerColor, contentColor = contentColor),
        onClick = onClick,
        showIndication = true,
        pressFeedbackType = PressFeedbackType.Tilt,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(88.dp)
                    .alpha(0.16f),
                tint = contentColor,
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = title,
                    color = contentColor,
                    style = MiuixTheme.textStyles.title4,
                )
                Text(
                    text = description,
                    color = contentColor,
                    style = MiuixTheme.textStyles.body2,
                )
                Text(
                    text = hint,
                    color = contentColor,
                    style = MiuixTheme.textStyles.footnote1,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .alpha(0.75f),
                )
            }
        }
    }
}

@Composable
private fun StatisticCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.footnote2,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MiuixTheme.textStyles.title3,
            color = MiuixTheme.colorScheme.onSurface,
        )
    }
}

/**
 * [StatusCard] 的紧凑版：一行说清某个开关是否就绪，以及点一下做什么。
 * 两块满高的状态卡叠在一起会显得像一堵墙，所以第二、三项用紧凑版。
 */
@Composable
private fun CompactStatusCard(
    active: Boolean,
    icon: ImageVector,
    title: String,
    summary: String,
    hint: String,
    onClick: () -> Unit,
) {
    val containerColor = if (active) {
        MiuixTheme.colorScheme.primaryContainer
    } else {
        MiuixTheme.colorScheme.errorContainer
    }
    val contentColor = if (active) {
        MiuixTheme.colorScheme.onPrimaryContainer
    } else {
        MiuixTheme.colorScheme.onErrorContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        colors = CardDefaults.defaultColors(color = containerColor, contentColor = contentColor),
        onClick = onClick,
        showIndication = true,
        pressFeedbackType = PressFeedbackType.Tilt,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
            ) {
                Text(
                    text = title,
                    color = contentColor,
                    style = MiuixTheme.textStyles.body2,
                )
                Text(
                    text = summary,
                    color = contentColor,
                    style = MiuixTheme.textStyles.footnote1,
                    modifier = Modifier.alpha(0.75f),
                )
            }
            Text(
                text = hint,
                color = contentColor,
                style = MiuixTheme.textStyles.footnote1,
                modifier = Modifier.alpha(0.75f),
            )
        }
    }
}
