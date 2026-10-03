/*
 * Copyright 2026, Youzix-Star
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.youzixstar.dsha.ui.miuix

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.youzixstar.dsha.setup.DshaController
import com.youzixstar.dsha.ui.DebugInfo
import com.youzixstar.dsha.ui.copyToClipboard
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** 关于页与设置页共用的调试信息面板。 */
@Composable
fun DebugInfoDialog(
    controller: DshaController,
    onDismiss: () -> Unit,
    onNotify: (String) -> Unit,
) {
    val context = LocalContext.current
    val text = remember { DebugInfo.build(context, controller) }

    OverlayDialog(
        show = true,
        title = "调试信息",
        summary = "在关于页连点彩蛋三次可再次唤出",
        onDismissRequest = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = text,
                    style = MiuixTheme.textStyles.footnote2,
                    fontFamily = FontFamily.Monospace,
                )
            }
            Button(
                onClick = {
                    copyToClipboard(context, text)
                    onNotify("已复制调试信息")
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("复制")
            }
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("关闭")
            }
        }
    }
}
