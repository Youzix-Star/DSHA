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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.NavigationItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SnackbarHost
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme

private const val TAB_HOME = 0
private const val TAB_WEB = 1
private const val TAB_CONSOLE = 2
private const val TAB_SETTINGS = 3

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
    // 底栏始终是液态玻璃，且所有页签都参与背景层捕获，保证外观一致。
    // WebView 是独立的原生视图，被放在 pager 之外、这个捕获图层之外，
    // 因此既不会因为逐帧重录而掉帧，也不会被图层影响绘制。
    val onWebTab = currentTab == TAB_WEB

    // 首次进入「网页」页签后让 WebView 常驻：切页签不再重新加载那几十个插件模块
    var webOpened by remember { mutableStateOf(false) }
    LaunchedEffect(currentTab) {
        if (currentTab == TAB_WEB) webOpened = true
    }

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

                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            // 所有页签都捕获背景层：否则「网页」页签上底栏会采到上一次
                            // 录下的旧内容，看起来就是「变色」。
                            .layerBackdrop(backdrop)
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

                                // 该页签的内容由外层的常驻 WebView 承载
                                TAB_WEB -> Unit

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

                    // 常驻 WebView：不在 pager 内、不在 backdrop 图层内，
                    // 并用 pagePadding 让出顶栏与底栏的空间，避免原生视图盖住悬浮底栏。
                    if (webOpened) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(pagePadding),
                        ) {
                            WebScreen(controller = controller, visible = onWebTab)
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
