package com.youzixstar.dsha.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController

/**
 * 主题模式：0 = 跟随系统，1 = 浅色，2 = 深色。
 * 颜色体系完全交给 Miuix（与 InstallerX Revived 同一套 UI 库）。
 */
@Composable
fun AppTheme(
    colorMode: Int = 0,
    content: @Composable () -> Unit,
) {
    val controller = remember(colorMode) {
        when (colorMode) {
            1 -> ThemeController(ColorSchemeMode.Light)
            2 -> ThemeController(ColorSchemeMode.Dark)
            else -> ThemeController(ColorSchemeMode.System)
        }
    }
    MiuixTheme(controller = controller, content = content)
}
