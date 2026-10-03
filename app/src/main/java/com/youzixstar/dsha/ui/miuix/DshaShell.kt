/*
 * Copyright 2026, Youzix-Star
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * 应用外壳：大标题顶栏 + 悬浮底栏 + 可滑动页签。
 * 结构与 NekoPlus 的 MiuixApp / MiaoShell 一致，便于两个项目之间同步改动。
 */

package com.youzixstar.dsha.ui.miuix

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.youzixstar.dsha.setup.DshaController
import com.youzixstar.dsha.ui.AppIcons
import com.youzixstar.dsha.ui.miuix.console.ConsoleScreen
import com.youzixstar.dsha.ui.miuix.guide.GuideScreen
import com.youzixstar.dsha.ui.miuix.home.HomeScreen
import com.youzixstar.dsha.ui.miuix.liquid.FloatingBottomBar
import com.youzixstar.dsha.ui.miuix.settings.SettingsScreen
import com.youzixstar.dsha.ui.miuix.web.WebScreen
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.FloatingNavigationBar
import top.yukonga.miuix.kmp.basic.FloatingNavigationBarItem
import top.yukonga.miuix.kmp.basic.FloatingToolbarDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.BlurDefaults
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.blur.highlight.Highlight
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val TAB_HOME = 0
private const val TAB_WEB = 1
private const val TAB_CONSOLE = 2
private const val TAB_SETTINGS = 3

/**
 * 底栏样式档位。
 *
 * 外观与开销的取舍，序号与 [com.youzixstar.dsha.data.AppPrefs.barStyle] 一致：
 * - [Standard]：miuix 的 `textureBlur` 单 pass 纹理模糊，只有一个半径参数，开销低
 * - [LiquidGlass]：NekoPlus 的液态玻璃底栏，blur + lens 折射 + vibrancy + 高光四次叠加，
 *   最漂亮也最重，老机器上会明显掉帧
 * - [None]：不做模糊，底栏用不透明容器色
 */
enum class BarStyle(val label: String) {
    Standard("标准"),
    LiquidGlass("液态玻璃"),
    None("关闭模糊"),
}

/** 二级页面：只有这些页面响应返回手势，页签之间是平级切换。 */
enum class DshaSubPage(val title: String) {
    Guide("环境配置引导"),
}

@Composable
fun DshaApp(controller: DshaController) {
    val modeIndex = controller.themeModeIndex.coerceIn(0, ThemeModeOptions.lastIndex)
    val colorSchemeMode = ThemeModeOptions[modeIndex].first

    MiuixAppTheme(colorSchemeMode = colorSchemeMode) {
        DshaShell(controller = controller)
    }
}

@Composable
private fun DshaShell(controller: DshaController) {
    var subPage by remember { mutableStateOf<DshaSubPage?>(null) }

    val navigationItems = remember {
        listOf(
            NavigationItem(label = "首页", icon = AppIcons.Home),
            NavigationItem(label = "网页", icon = AppIcons.Web),
            NavigationItem(label = "终端", icon = AppIcons.Terminal),
            NavigationItem(label = "设置", icon = AppIcons.Settings),
        )
    }
    val titles = remember { listOf("DSHA", "网页", "终端", "设置") }

    val pagerState = rememberPagerState(pageCount = { navigationItems.size })
    val scrollBehavior = MiuixScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val notify: (String) -> Unit = { message ->
        scope.launch { snackbarHostState.showSnackbar(message) }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        controller.refresh()
    }

    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    val currentTab = pagerState.currentPage
    // 「网页」页签里是独立的原生 WebView。把它放进被逐帧重录的 backdrop 图层，
    // 既可能让互操作 View 画不出来，也会因为每帧重录整页而严重掉帧；
    // 因此该页签既不捕获背景层，也不启用底栏模糊。
    val onWebTab = currentTab == TAB_WEB
    val barStyle = BarStyle.entries.getOrElse(controller.barStyle) { BarStyle.Standard }
    val blurActive = barStyle != BarStyle.None && !onWebTab

    // 一级返回：从任意页签回到首页
    BackHandler(enabled = subPage == null && currentTab != TAB_HOME) {
        scope.launch { pagerState.animateScrollToPage(TAB_HOME) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val page = subPage
        if (page == null) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = titles[currentTab],
                        largeTitle = titles[currentTab],
                        scrollBehavior = scrollBehavior,
                    )
                },
                bottomBar = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val barModifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(
                                bottom = 12.dp +
                                    WindowInsets.navigationBars
                                        .asPaddingValues()
                                        .calculateBottomPadding(),
                            )

                        if (barStyle == BarStyle.LiquidGlass && blurActive) {
                            FloatingBottomBar(
                                items = navigationItems,
                                selectedIndex = currentTab,
                                onItemClick = { index ->
                                    scope.launch { pagerState.animateScrollToPage(index) }
                                },
                                backdrop = backdrop,
                                isBlurActive = true,
                                modifier = barModifier,
                            )
                        } else {
                            // 标准档：miuix 官方 example 的默认做法——单 pass 纹理模糊，
                            // 半径由 barBlurRadius 参数控制。
                            val glassModifier = if (blurActive) {
                                Modifier.textureBlur(
                                    backdrop = backdrop,
                                    shape = RoundedCornerShape(FloatingToolbarDefaults.CornerRadius),
                                    blurRadius = controller.barBlurRadius.toFloat(),
                                    colors = BlurDefaults.blurColors(
                                        blendColors = listOf(
                                            BlendColorEntry(
                                                color = MiuixTheme.colorScheme.surfaceContainer
                                                    .copy(alpha = 0.6f),
                                            ),
                                        ),
                                    ),
                                    highlight = if (isInDarkTheme()) {
                                        Highlight.GlassStrokeMiddleDark
                                    } else {
                                        Highlight.GlassStrokeMiddleLight
                                    },
                                )
                            } else {
                                Modifier
                            }
                            FloatingNavigationBar(
                                modifier = barModifier.then(glassModifier),
                                color = if (blurActive) {
                                    Color.Transparent
                                } else {
                                    MiuixTheme.colorScheme.surfaceContainer
                                },
                            ) {
                                navigationItems.forEachIndexed { index, item ->
                                    FloatingNavigationBarItem(
                                        selected = currentTab == index,
                                        onClick = {
                                            scope.launch { pagerState.animateScrollToPage(index) }
                                        },
                                        icon = item.icon,
                                        label = item.label,
                                    )
                                }
                            }
                        }
                    }
                },
            ) { innerPadding ->
                val layoutDirection = LocalLayoutDirection.current
                val pagePadding = PaddingValues(
                    start = innerPadding.calculateStartPadding(layoutDirection) + 12.dp,
                    top = innerPadding.calculateTopPadding() + 12.dp,
                    end = innerPadding.calculateEndPadding(layoutDirection) + 12.dp,
                    bottom = innerPadding.calculateBottomPadding() + 12.dp,
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (onWebTab) Modifier else Modifier.layerBackdrop(backdrop))
                        .imePadding(),
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = true,
                    ) { index ->
                        when (index) {
                            TAB_HOME -> HomeScreen(
                                controller = controller,
                                contentPadding = pagePadding,
                                scrollBehavior = scrollBehavior,
                                onNotify = notify,
                                onOpenGuide = { subPage = DshaSubPage.Guide },
                            )

                            TAB_WEB -> WebScreen(contentPadding = pagePadding)

                            TAB_CONSOLE -> ConsoleScreen(
                                controller = controller,
                                contentPadding = pagePadding,
                                scrollBehavior = scrollBehavior,
                                onNotify = notify,
                            )

                            else -> SettingsScreen(
                                controller = controller,
                                contentPadding = pagePadding,
                                scrollBehavior = scrollBehavior,
                                onOpenGuide = { subPage = DshaSubPage.Guide },
                                onNotify = notify,
                            )
                        }
                    }
                }
            }
        } else {
            val pageScrollBehavior = MiuixScrollBehavior()
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = page.title,
                        scrollBehavior = pageScrollBehavior,
                        navigationIcon = {
                            IconButton(onClick = { subPage = null }) {
                                Icon(imageVector = AppIcons.Back, contentDescription = "返回")
                            }
                        },
                    )
                },
            ) { innerPadding ->
                val layoutDirection = LocalLayoutDirection.current
                val subPadding = PaddingValues(
                    start = innerPadding.calculateStartPadding(layoutDirection) + 12.dp,
                    top = innerPadding.calculateTopPadding() + 12.dp,
                    end = innerPadding.calculateEndPadding(layoutDirection) + 12.dp,
                    bottom = innerPadding.calculateBottomPadding() + 24.dp,
                )
                when (page) {
                    DshaSubPage.Guide -> GuideScreen(
                        controller = controller,
                        contentPadding = subPadding,
                        scrollBehavior = pageScrollBehavior,
                        onNotify = notify,
                    )
                }
            }
        }

        SnackbarHost(
            state = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    bottom = 84.dp +
                        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
                ),
        )
    }
}
