/*
 * Copyright 2026, Youzix-Star
 * SPDX-License-Identifier: AGPL-3.0-only
 *
 * 应用外壳：大标题顶栏 + 液态玻璃悬浮底栏 + 可滑动页签。
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

/**
 * 二级页面。
 *
 * 只有这些页面响应返回手势：页签之间是平级切换，平台返回动画没有意义。
 */
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

    // 回到前台时重新检测：用户可能在 Termux 里改了配置或刚授予权限
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        controller.refresh()
    }

    val surfaceColor = MiuixTheme.colorScheme.surface
    val backdrop = rememberLayerBackdrop {
        drawRect(surfaceColor)
        drawContent()
    }

    // 一级返回：从任意页签回到首页。二级页面打开时由它自己接管。
    BackHandler(enabled = subPage == null && pagerState.currentPage != TAB_HOME) {
        scope.launch { pagerState.animateScrollToPage(TAB_HOME) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val page = subPage
        if (page == null) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = titles[pagerState.currentPage],
                        largeTitle = titles[pagerState.currentPage],
                        scrollBehavior = scrollBehavior,
                    )
                },
                bottomBar = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        FloatingBottomBar(
                            items = navigationItems,
                            selectedIndex = pagerState.currentPage,
                            onItemClick = { index ->
                                scope.launch { pagerState.animateScrollToPage(index) }
                            },
                            backdrop = backdrop,
                            isBlurActive = controller.useLiquidGlass,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(
                                    bottom = 12.dp +
                                        WindowInsets.navigationBars
                                            .asPaddingValues()
                                            .calculateBottomPadding(),
                                ),
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

                Box(
                    modifier = Modifier
                        .fillMaxSize()
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
            // 二级页面自带滚动行为：交给页面本身，避免大标题无处收起的滚动吞噬问题。
            val pageScrollBehavior = MiuixScrollBehavior()
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = page.title,
                        scrollBehavior = pageScrollBehavior,
                        navigationIcon = {
                            IconButton(onClick = { subPage = null }) {
                                Icon(
                                    imageVector = AppIcons.Back,
                                    contentDescription = "返回",
                                )
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

        // 宿主放在根节点，二级页面打开时提示也浮在最上层
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
