package com.youzixstar.dsha.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.youzixstar.dsha.termux.TermuxBridge

/** Termux 的官方获取渠道（Google Play 版已停止更新，不予推荐） */
const val TERMUX_FDROID_URL = "https://f-droid.org/packages/com.termux/"
const val TERMUX_RELEASE_URL = "https://github.com/termux/termux-app/releases"

fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}

/** 打开 Termux；未安装则退回下载页 */
fun openTermux(context: Context) {
    val intent = context.packageManager.getLaunchIntentForPackage(TermuxBridge.TERMUX_PACKAGE)
    if (intent == null) {
        openUrl(context, TERMUX_FDROID_URL)
        return
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

fun copyToClipboard(context: Context, text: String) {
    val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    manager.setPrimaryClip(ClipData.newPlainText("DSHA", text))
}
