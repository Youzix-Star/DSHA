package com.youzixstar.dsha.data

import android.content.Context

/**
 * 轻量偏好存储。
 *
 * 目前只有三个键，用不上 DataStore，SharedPreferences 足够且无额外依赖。
 */
class AppPrefs(context: Context) {

    private val sp = context.getSharedPreferences("dsha", Context.MODE_PRIVATE)

    var autoStart: Boolean
        get() = sp.getBoolean(KEY_AUTO_START, true)
        set(value) = sp.edit().putBoolean(KEY_AUTO_START, value).apply()

    var keepScreenOn: Boolean
        get() = sp.getBoolean(KEY_KEEP_SCREEN_ON, false)
        set(value) = sp.edit().putBoolean(KEY_KEEP_SCREEN_ON, value).apply()

    private companion object {
        const val KEY_AUTO_START = "auto_start"
        const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
    }
}
