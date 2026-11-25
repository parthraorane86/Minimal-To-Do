package com.personal.minimaltodo.data

import android.content.Context

/**
 * Lightweight synchronous cache for theme & font size so MainActivity renders
 * the user's exact chosen theme on frame 1 (preventing any dark/light startup flash).
 */
object ThemePrefs {
    private const val PREFS_NAME = "minimal_todo_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_FONT_SIZE = "font_size"

    fun getThemeMode(context: Context): ThemeMode {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_THEME_MODE, null)
        return raw?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_THEME_MODE, mode.name)
            .apply()
    }

    fun getFontSize(context: Context): FontSizeOption {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_FONT_SIZE, null)
        return raw?.let { runCatching { FontSizeOption.valueOf(it) }.getOrNull() }
            ?: FontSizeOption.MEDIUM
    }

    fun setFontSize(context: Context, option: FontSizeOption) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FONT_SIZE, option.name)
            .apply()
    }
}
