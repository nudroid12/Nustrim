package app.nudroidlabs.nustrim.tv.player

import android.content.Context

internal class TvSubtitleStyleStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    var fontSizeSp: Int
        get() = preferences.getInt(KEY_FONT_SIZE, DEFAULT_FONT_SIZE).coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)
        set(value) = preferences.edit().putInt(KEY_FONT_SIZE, value.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE)).apply()

    var bold: Boolean
        get() = preferences.getBoolean(KEY_BOLD, false)
        set(value) = preferences.edit().putBoolean(KEY_BOLD, value).apply()

    companion object {
        const val MIN_FONT_SIZE = 12
        const val MAX_FONT_SIZE = 32
        const val FONT_SIZE_STEP = 2

        private const val PREFERENCES_NAME = "nustrim_tv_layout"
        private const val KEY_FONT_SIZE = "subtitle_font_size"
        private const val KEY_BOLD = "subtitle_bold"
        private const val DEFAULT_FONT_SIZE = 18
    }
}
