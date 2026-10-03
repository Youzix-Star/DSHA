/*
 * Copyright 2026, Youzix-Star
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.youzixstar.dsha.ui

import android.content.Context
import android.os.Build
import android.security.NetworkSecurityPolicy
import android.webkit.WebSettings
import android.webkit.WebView
import com.youzixstar.dsha.BuildConfig
import com.youzixstar.dsha.DSH_WEB_URL
import com.youzixstar.dsha.setup.DshaController

/**
 * 调试信息快照，从关于页连点彩蛋后可调出。
 *
 * 「网页」页签用的到底是什么：不是自研引擎，也不是 Chrome Custom Tabs，
 * 而是 **Android 系统自带的 WebView** —— 即系统里那个 Chromium 内核，由
 * `com.google.android.webview` 或 ROM 内置的 `com.android.webview` 提供。
 * 页面地址是本机回环上的 DSH Web UI。这里把引擎包名与版本一并列出来，便于排查渲染问题。
 */
object DebugInfo {

    fun build(context: Context, controller: DshaController): String = buildString {
        appendLine("## 应用")
        appendLine("版本: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        appendLine("包名: ${context.packageName}")
        appendLine()
        appendLine("## 设备")
        appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
        appendLine("设备: ${Build.MANUFACTURER} ${Build.MODEL}")
        appendLine("ABI: ${Build.SUPPORTED_ABIS.joinToString()}")
        appendLine()
        appendLine("## 网页渲染")
        appendLine("引擎: ${webViewEngine()}")
        appendLine("明文放行(127.0.0.1): ${if (cleartextPermitted("127.0.0.1")) "是" else "否 ← 会被拦"}")
        appendLine("地址: $DSH_WEB_URL")
        appendLine("UA: ${defaultUserAgent(context)}")
        appendLine()
        appendLine("## 运行状态")
        controller.statusSnapshot().forEach { (key, value) -> appendLine("$key: $value") }
    }

    /**
     * 本应用是否允许对 [host] 走明文 HTTP。
     *
     * 直接问系统的网络安全策略，而不是从清单推断——这一项能一句话区分
     * 「被明文策略拦下」和「其它原因加载失败」。
     */
    fun cleartextPermitted(host: String): Boolean = runCatching {
        NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted(host)
    }.getOrDefault(false)

    /** 供失败卡片用的简短引擎名。 */
    fun webViewEngineName(): String = webViewEngine()

    /** 系统 WebView 的包名与版本；取不到时给出原因而不是静默留空。 */
    private fun webViewEngine(): String = runCatching {
        WebView.getCurrentWebViewPackage()?.let { "${it.packageName} ${it.versionName}" }
    }.getOrElse { "读取失败: ${it.javaClass.simpleName}" } ?: "系统未提供 WebView"

    private fun defaultUserAgent(context: Context): String =
        runCatching { WebSettings.getDefaultUserAgent(context) }.getOrDefault("读取失败")
}
