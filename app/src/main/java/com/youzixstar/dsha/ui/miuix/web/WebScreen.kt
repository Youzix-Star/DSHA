/*
 * Copyright 2026, Youzix-Star
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.youzixstar.dsha.ui.miuix.web

import android.graphics.Bitmap
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.youzixstar.dsha.DSH_WEB_URL
import com.youzixstar.dsha.ui.DebugInfo
import com.youzixstar.dsha.ui.copyToClipboard
import com.youzixstar.dsha.ui.openUrl
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private sealed interface WebStatus {
    data object Loading : WebStatus
    data object Loaded : WebStatus
    data class Failed(val message: String) : WebStatus
}

/**
 * 内嵌 DSH 的 Web UI。
 *
 * 用的是 **Android 系统自带 WebView**（系统里的 Chromium 内核），不是自研引擎。
 * 页面加载失败时不再是一片空白：这里会把 WebView 报出的错误码、HTTP 状态码
 * 以及网页控制台输出直接显示出来，便于定位。
 */
@Composable
fun WebScreen(contentPadding: PaddingValues) {
    val context = LocalContext.current
    var status by remember { mutableStateOf<WebStatus>(WebStatus.Loading) }
    val console = remember { mutableStateListOf<String>() }
    var reloadKey by remember { mutableIntStateOf(0) }
    var showConsole by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        key(reloadKey) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(
                                view: WebView?,
                                url: String?,
                                favicon: Bitmap?,
                            ) {
                                // 失败之后不再被后续回调覆盖回「加载中」
                                if (status !is WebStatus.Failed) status = WebStatus.Loading
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                if (status !is WebStatus.Failed) status = WebStatus.Loaded
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?,
                            ) {
                                if (request?.isForMainFrame == true) {
                                    status = WebStatus.Failed(
                                        "主文档加载失败：${error?.errorCode} ${error?.description}",
                                    )
                                }
                            }

                            override fun onReceivedHttpError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                errorResponse: WebResourceResponse?,
                            ) {
                                if (request?.isForMainFrame == true) {
                                    status = WebStatus.Failed(
                                        "主文档返回 HTTP ${errorResponse?.statusCode} " +
                                            "${errorResponse?.reasonPhrase.orEmpty()}",
                                    )
                                }
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(msg: ConsoleMessage): Boolean {
                                console.add(
                                    "[${msg.messageLevel()}] ${msg.message()}" +
                                        "  @${msg.sourceId()}:${msg.lineNumber()}",
                                )
                                while (console.size > 200) console.removeAt(0)
                                return true
                            }
                        }

                        loadUrl(DSH_WEB_URL)
                    }
                },
            )
        }

        if (status !is WebStatus.Loaded) {
            val failed = status as? WebStatus.Failed
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(12.dp),
                insideMargin = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
            ) {
                Text(
                    text = if (failed == null) "正在加载网页…" else "网页没能加载出来",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = failed?.message ?: DSH_WEB_URL,
                    style = MiuixTheme.textStyles.footnote1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                if (failed != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "本机 WebView：" +
                            "${DebugInfo.webViewEngineName()} · 明文放行：" +
                            if (DebugInfo.cleartextPermitted("127.0.0.1")) "是" else "否",
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }

                if (failed != null && console.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (showConsole) {
                            console.takeLast(12).joinToString("\n")
                        } else {
                            "网页控制台有 ${console.size} 条输出"
                        },
                        style = MiuixTheme.textStyles.footnote2,
                        fontFamily = FontFamily.Monospace,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = if (showConsole) {
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .verticalScroll(rememberScrollState())
                        } else {
                            Modifier
                        },
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = { showConsole = !showConsole },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (showConsole) "收起控制台输出" else "查看控制台输出")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            status = WebStatus.Loading
                            reloadKey++
                        },
                    ) {
                        Text("重新加载")
                    }
                    Button(onClick = { openUrl(context, DSH_WEB_URL) }) {
                        Text("用浏览器打开")
                    }
                }
                if (failed != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            copyToClipboard(
                                context,
                                buildString {
                                    appendLine("地址: $DSH_WEB_URL")
                                    appendLine("错误: ${failed.message}")
                                    appendLine(
                                        "明文放行: " +
                                            if (DebugInfo.cleartextPermitted("127.0.0.1")) "是" else "否",
                                    )
                                    appendLine("控制台:")
                                    console.takeLast(50).forEach { appendLine(it) }
                                },
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("复制诊断信息")
                    }
                }
            }
        }
    }
}
