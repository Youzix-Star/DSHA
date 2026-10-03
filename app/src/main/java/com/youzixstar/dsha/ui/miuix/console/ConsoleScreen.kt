/*
 * Copyright 2026, Youzix-Star
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.youzixstar.dsha.ui.miuix.console

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.youzixstar.dsha.setup.DshaController
import com.youzixstar.dsha.ui.miuix.dshaTextFieldColors
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 终端页：命令交给 Termux 执行，输出汇总在这里。
 *
 * 这是「命令 + 输出回显」的控制台，不是完整的交互式 PTY；
 * 真 PTY 需要内置终端模拟器原生库，属于后续工作。
 */
@Composable
fun ConsoleScreen(
    controller: DshaController,
    contentPadding: PaddingValues,
    scrollBehavior: ScrollBehavior,
    onNotify: (String) -> Unit,
) {
    var input by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(controller.consoleLog.size) {
        if (controller.consoleLog.isNotEmpty()) {
            listState.animateScrollToItem(controller.consoleLog.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            insideMargin = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Text(
                text = "服务控制",
                style = MiuixTheme.textStyles.title4,
                color = MiuixTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { controller.startServer() },
                    enabled = controller.dshBinAvailable &&
                        !controller.busy &&
                        !controller.serverRunning,
                ) {
                    Text("启动")
                }
                Button(
                    onClick = { controller.stopServer() },
                    enabled = controller.serverRunning && !controller.busy,
                ) {
                    Text("停止")
                }
                Button(
                    onClick = { controller.readServerLog() },
                    enabled = controller.bridgeOk && !controller.busy,
                ) {
                    Text("日志")
                }
                Button(
                    onClick = {
                        controller.clearConsole()
                        onNotify("已清空")
                    },
                    enabled = controller.consoleLog.isNotEmpty() && !controller.busy,
                ) {
                    Text("清空")
                }
            }
            if (controller.busy) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "${controller.busyLabel} …",
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            insideMargin = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            if (controller.consoleLog.isEmpty()) {
                Text(
                    text = "在下面输入命令，会在本机 Termux 中执行，输出显示在这里。" +
                        "例如：pkg list-installed | head",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    items(controller.consoleLog) { line ->
                        Text(
                            text = line,
                            fontFamily = FontFamily.Monospace,
                            style = MiuixTheme.textStyles.footnote1,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                label = "输入命令",
                colors = dshaTextFieldColors(),
                enabled = controller.bridgeOk,
                singleLine = true,
            )
            Button(
                onClick = {
                    controller.sendCommand(input)
                    input = ""
                },
                enabled = controller.bridgeOk && !controller.busy && input.isNotBlank(),
            ) {
                Text("执行")
            }
        }
    }
}
