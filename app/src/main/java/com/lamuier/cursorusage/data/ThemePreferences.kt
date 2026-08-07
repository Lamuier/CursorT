package com.lamuier.cursorusage.data

import android.content.Context
import com.lamuier.cursorusage.ui.theme.ColorPalette
import com.lamuier.cursorusage.ui.theme.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ThemeSettings(
    val themeMode: ThemeMode = ThemeMode.System,
    val colorPalette: ColorPalette = ColorPalette.Pulse,
)

class ThemePreferences(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<ThemeSettings> = _settings.asStateFlow()

    fun read(): ThemeSettings = ThemeSettings(
        themeMode = ThemeMode.fromStorage(preferences.getString(KEY_THEME_MODE, null)),
        colorPalette = ColorPalette.fromStorage(preferences.getString(KEY_COLOR_PALETTE, null)),
    )

    fun setThemeMode(mode: ThemeMode) {
        preferences.edit().putString(KEY_THEME_MODE, mode.storageKey).apply()
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun setColorPalette(palette: ColorPalette) {
        preferences.edit().putString(KEY_COLOR_PALETTE, palette.storageKey).apply()
        _settings.value = _settings.value.copy(colorPalette = palette)
    }

    fun update(settings: ThemeSettings) {
        preferences.edit()
            .putString(KEY_THEME_MODE, settings.themeMode.storageKey)
            .putString(KEY_COLOR_PALETTE, settings.colorPalette.storageKey)
            .apply()
        _settings.value = settings
    }

    companion object {
        private const val PREFERENCES_NAME = "cursor_pulse_theme_v1"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_COLOR_PALETTE = "color_palette"

        @Volatile
        private var instance: ThemePreferences? = null

        fun get(context: Context): ThemePreferences {
            return instance ?: synchronized(this) {
                instance ?: ThemePreferences(context).also { instance = it }
            }
        }
    }
}
