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

    /**
     * 底栏样式档位，见 [com.youzixstar.dsha.ui.miuix.BarStyle]。
     *
     * 默认「标准」：走 miuix 的 textureBlur 单 pass 纹理模糊，只有 blurRadius
     * 一个半径参数，开销远低于液态玻璃的多 pass 折射。
     */
    var barStyle: Int
        get() = sp.getInt(KEY_BAR_STYLE, BAR_STYLE_STANDARD)
        set(value) = sp.edit().putInt(KEY_BAR_STYLE, value).apply()

    /** 底栏模糊半径（dp）。仅「标准」档生效，对应 miuix textureBlur 的 blurRadius。 */
    var barBlurRadius: Int
        get() = sp.getInt(KEY_BAR_BLUR_RADIUS, 25)
        set(value) = sp.edit().putInt(KEY_BAR_BLUR_RADIUS, value).apply()

    /** 开发者模式：在关于页连点三次彩蛋开启。 */
    var developerMode: Boolean
        get() = sp.getBoolean(KEY_DEVELOPER_MODE, false)
        set(value) = sp.edit().putBoolean(KEY_DEVELOPER_MODE, value).apply()

    private companion object {
        const val KEY_AUTO_START = "auto_start"
        const val KEY_KEEP_SCREEN_ON = "keep_screen_on"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_BAR_STYLE = "bar_style"
        const val KEY_BAR_BLUR_RADIUS = "bar_blur_radius"

        /** 与 [com.youzixstar.dsha.ui.miuix.BarStyle] 的序号一致，避免 data 层依赖 ui 层。 */
        const val BAR_STYLE_STANDARD = 0
        const val KEY_DEVELOPER_MODE = "developer_mode"
    }
}
