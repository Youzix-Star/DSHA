package com.youzixstar.dsha.data

import android.content.Context

/**
 * 轻量偏好存储。
 *
 * 目前只有几个键，用不上 DataStore，SharedPreferences 足够且无额外依赖。
 */
class AppPrefs(context: Context) {

    private val sp = context.getSharedPreferences("dsha", Context.MODE_PRIVATE)

    var autoStart: Boolean
        get() = sp.getBoolean(KEY_AUTO_START, true)
        set(value) = sp.edit().putBoolean(KEY_AUTO_START, value).apply()

    var keepScreenOn: Boolean
        get() = sp.getBoolean(KEY_KEEP_SCREEN_ON, false)
        set(value) = sp.edit().putBoolean(KEY_KEEP_SCREEN_ON, value).apply()

    /** 主题模式在 [com.youzixstar.dsha.ui.miuix.ThemeModeOptions] 中的下标。 */
    var themeModeIndex: Int
        get() = sp.getInt(KEY_THEME_MODE, 0)
        set(value) = sp.edit().putInt(KEY_THEME_MODE, value).apply()

    /** 开发者模式：在关于页连点三次彩蛋开启。 */
    var developerMode: Boolean
        get() = sp.getBoolean(KEY_DEVELOPER_MODE, false)
        set(value) = sp.edit().putBoolean(KEY_DEVELOPER_MODE, value).apply()

    private companion object {
        const val KEY_AUTO_START = "auto_start"
        const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_DEVELOPER_MODE = "developer_mode"
    }
}
