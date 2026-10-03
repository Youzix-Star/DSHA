package com.youzixstar.dsha.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.youzixstar.dsha.ui.page.ConsolePage
import com.youzixstar.dsha.ui.page.SettingsPage
import com.youzixstar.dsha.ui.page.WebPage
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Link
import top.yukonga.miuix.kmp.icon.extended.ScreenCapture
import top.yukonga.miuix.kmp.icon.extended.Settings

private enum class DshaTab(val label: String, val icon: ImageVector) {
    Web("网页", MiuixIcons.Link),
    Console("终端", MiuixIcons.ScreenCapture),
    Settings("设置", MiuixIcons.Settings),
}

@Composable
fun DshaRoot() {
    var current by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            FloatingNavigationBar {
                DshaTab.entries.forEachIndexed { index, item ->
                    FloatingNavigationBarItem(
                        selected = current == index,
                        onClick = { current = index },
                        icon = item.icon,
                        label = item.label,
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (DshaTab.entries[current]) {
                DshaTab.Web -> WebPage()
                DshaTab.Console -> ConsolePage()
                DshaTab.Settings -> SettingsPage()
            }
        }
    }
}
