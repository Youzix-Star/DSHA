package com.youzixstar.dsha.ui.page

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/** DSHA 的默认 Web UI 地址（DSH 服务监听本机回环）。 */
const val DSH_WEB_URL: String = "http://127.0.0.1:3080"

@Composable
fun WebPage(url: String = DSH_WEB_URL) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = WebViewClient()
                loadUrl(url)
            }
        },
    )
}
