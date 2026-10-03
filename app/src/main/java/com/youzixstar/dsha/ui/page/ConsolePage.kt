package com.youzixstar.dsha.ui.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.youzixstar.dsha.setup.DshaController
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField

/**
 * 终端页：把命令交给 Termux 执行，并把输出汇总在这里。
 *
 * 说明：这里不是完整的本地 PTY 终端，而是「Termux 命令 + 输出回显」的控制台。
 * 完整的交互式终端需要内置 Termux 的终端模拟器原生库，留待后续版本。
 */
@Composable
fun ConsolePage(controller: DshaController) {
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
            .padding(horizontal = 12.dp),
    ) {
        Spacer(Modifier.height(12.dp))

        SmallTitle("服务状态")
        Card {
            StatusRow("Termux", if (controller.termuxInstalled) "已安装" else "未安装")
            StatusRow("命令桥接", if (controller.bridgeOk) "已连通" else "未连通")
            StatusRow("dsh 命令", if (controller.dshBinAvailable) controller.dshVersion else "未就绪")
            StatusRow("Node", controller.nodeVersion)
            StatusRow("Web 服务", if (controller.serverRunning) "运行中 · 127.0.0.1:3080" else "已停止")

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { controller.startServer() },
                    enabled = controller.dshBinAvailable && !controller.busy && !controller.serverRunning,
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
                    onClick = { controller.clearConsole() },
                    enabled = controller.consoleLog.isNotEmpty() && !controller.busy,
                ) {
                    Text("清空")
                }
            }
            if (controller.busy) {
                Spacer(Modifier.height(6.dp))
                Text("${controller.busyLabel} …", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(12.dp))

        SmallTitle("控制台")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            if (controller.consoleLog.isEmpty()) {
                Text("在下面输入命令，会在本机 Termux 中执行，输出显示在这里。例如：pkg list-installed | head")
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
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                label = "输入命令",
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

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}
