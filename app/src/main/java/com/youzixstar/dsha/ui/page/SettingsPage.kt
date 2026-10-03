package com.youzixstar.dsha.ui.page

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.youzixstar.dsha.setup.DshaController
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference

@Composable
fun SettingsPage(controller: DshaController) {
    var autoStart by remember { mutableStateOf(true) }
    var keepScreenOn by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
    ) {
        Spacer(Modifier.height(12.dp))

        SmallTitle("运行环境")
        Card {
            ArrowPreference(
                title = "DSH Web UI 地址",
                summary = DSH_WEB_URL,
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
                title = "重新运行安装引导",
                summary = "重新检测 Termux、权限与服务状态",
                onClick = { controller.reopenSetup() },
            )
        }

        Spacer(Modifier.height(12.dp))

        SmallTitle("使用")
        Card {
            SwitchPreference(
                checked = autoStart,
                onCheckedChange = { autoStart = it },
                title = "进入应用时自动启动服务",
                summary = "打开 DSHA 时自动在 Termux 中拉起 DSH",
            )
            SwitchPreference(
                checked = keepScreenOn,
                onCheckedChange = { keepScreenOn = it },
                title = "保持屏幕常亮",
                summary = "长时间使用 Web UI 时避免息屏",
            )
        }

        Spacer(Modifier.height(12.dp))

        SmallTitle("关于")
        Card {
            ArrowPreference(
                title = "版本",
                summary = "0.1.0",
                enabled = false,
            )
            ArrowPreference(
                title = "开源许可",
                summary = "GPL-3.0",
                enabled = false,
            )
        }

        Spacer(Modifier.height(24.dp))
    }
}
