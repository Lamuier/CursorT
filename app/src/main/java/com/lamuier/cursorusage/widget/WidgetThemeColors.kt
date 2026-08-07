package com.lamuier.cursorusage.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import androidx.core.graphics.toColorInt
import com.lamuier.cursorusage.data.ThemePreferences
import com.lamuier.cursorusage.data.ThemeSettings
import com.lamuier.cursorusage.ui.theme.ColorPalette
import com.lamuier.cursorusage.ui.theme.ThemeMode

data class WidgetThemeColors(
    val surface: Int,
    val surfaceContainer: Int,
    val surfaceVariant: Int,
    val onSurface: Int,
    val onSurfaceVariant: Int,
    val primary: Int,
    val primaryContainer: Int,
    val onPrimaryContainer: Int,
    val secondary: Int,
    val tertiary: Int,
    val progressTrack: Int,
    val outline: Int,
) {
    companion object {
        fun resolve(context: Context, settings: ThemeSettings = ThemePreferences.get(context).read()): WidgetThemeColors {
            val dark = when (settings.themeMode) {
                ThemeMode.System -> {
                    (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                        Configuration.UI_MODE_NIGHT_YES
                }
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }
            if (settings.colorPalette == ColorPalette.System &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
            ) {
                return systemDynamic(context, dark)
            }
            return staticPalette(settings.colorPalette, dark)
        }

        private fun systemDynamic(context: Context, dark: Boolean): WidgetThemeColors {
            val res = context.resources
            return if (dark) {
                WidgetThemeColors(
                    surface = systemColor(res, android.R.color.system_neutral1_900, "#13171E"),
                    surfaceContainer = systemColor(res, android.R.color.system_neutral1_800, "#1A1F28"),
                    surfaceVariant = systemColor(res, android.R.color.system_neutral2_700, "#242B36"),
                    onSurface = systemColor(res, android.R.color.system_neutral1_100, "#E8ECF2"),
                    onSurfaceVariant = systemColor(res, android.R.color.system_neutral2_200, "#9AA5B5"),
                    primary = systemColor(res, android.R.color.system_accent1_200, "#60A5FA"),
                    primaryContainer = systemColor(res, android.R.color.system_accent1_700, "#1E3A5F"),
                    onPrimaryContainer = systemColor(res, android.R.color.system_accent1_100, "#DCEBFF"),
                    secondary = systemColor(res, android.R.color.system_accent2_200, "#94A3B8"),
                    tertiary = systemColor(res, android.R.color.system_accent3_200, "#22D3EE"),
                    progressTrack = systemColor(res, android.R.color.system_neutral2_700, "#41464F"),
                    outline = systemColor(res, android.R.color.system_neutral2_600, "#3A4454"),
                )
            } else {
                WidgetThemeColors(
                    surface = systemColor(res, android.R.color.system_neutral1_50, "#FFFFFF"),
                    surfaceContainer = systemColor(res, android.R.color.system_neutral1_100, "#EEF1F5"),
                    surfaceVariant = systemColor(res, android.R.color.system_neutral2_100, "#E4E9F0"),
                    onSurface = systemColor(res, android.R.color.system_neutral1_900, "#11151A"),
                    onSurfaceVariant = systemColor(res, android.R.color.system_neutral2_700, "#5C6775"),
                    primary = systemColor(res, android.R.color.system_accent1_600, "#2563EB"),
                    primaryContainer = systemColor(res, android.R.color.system_accent1_100, "#DCEBFF"),
                    onPrimaryContainer = systemColor(res, android.R.color.system_accent1_900, "#0A2A5C"),
                    secondary = systemColor(res, android.R.color.system_accent2_600, "#475569"),
                    tertiary = systemColor(res, android.R.color.system_accent3_600, "#0E7490"),
                    progressTrack = systemColor(res, android.R.color.system_neutral2_200, "#D9DFEA"),
                    outline = systemColor(res, android.R.color.system_neutral2_400, "#C5CDD8"),
                )
            }
        }

        private fun systemColor(
            res: android.content.res.Resources,
            id: Int,
            fallback: String,
        ): Int = runCatching { res.getColor(id, null) }.getOrElse { fallback.toColorInt() }

        private fun staticPalette(palette: ColorPalette, dark: Boolean): WidgetThemeColors {
            val key = when (palette) {
                ColorPalette.Aurora -> if (dark) AuroraDark else AuroraLight
                ColorPalette.Ember -> if (dark) EmberDark else EmberLight
                ColorPalette.Violet -> if (dark) VioletDark else VioletLight
                ColorPalette.Pulse, ColorPalette.System -> if (dark) PulseDark else PulseLight
            }
            return key
        }

        private val PulseLight = WidgetThemeColors(
            surface = "#FFFFFF".toColorInt(),
            surfaceContainer = "#EEF1F5".toColorInt(),
            surfaceVariant = "#E4E9F0".toColorInt(),
            onSurface = "#11151A".toColorInt(),
            onSurfaceVariant = "#5C6775".toColorInt(),
            primary = "#2563EB".toColorInt(),
            primaryContainer = "#DCEBFF".toColorInt(),
            onPrimaryContainer = "#0A2A5C".toColorInt(),
            secondary = "#475569".toColorInt(),
            tertiary = "#0E7490".toColorInt(),
            progressTrack = "#D9DFEA".toColorInt(),
            outline = "#C5CDD8".toColorInt(),
        )

        private val PulseDark = WidgetThemeColors(
            surface = "#13171E".toColorInt(),
            surfaceContainer = "#1A1F28".toColorInt(),
            surfaceVariant = "#242B36".toColorInt(),
            onSurface = "#E8ECF2".toColorInt(),
            onSurfaceVariant = "#9AA5B5".toColorInt(),
            primary = "#60A5FA".toColorInt(),
            primaryContainer = "#1E3A5F".toColorInt(),
            onPrimaryContainer = "#DCEBFF".toColorInt(),
            secondary = "#94A3B8".toColorInt(),
            tertiary = "#22D3EE".toColorInt(),
            progressTrack = "#41464F".toColorInt(),
            outline = "#3A4454".toColorInt(),
        )

        private val AuroraLight = WidgetThemeColors(
            surface = "#FFFFFF".toColorInt(),
            surfaceContainer = "#EEF1F5".toColorInt(),
            surfaceVariant = "#E4E9F0".toColorInt(),
            onSurface = "#11151A".toColorInt(),
            onSurfaceVariant = "#5C6775".toColorInt(),
            primary = "#0F766E".toColorInt(),
            primaryContainer = "#CCFBF1".toColorInt(),
            onPrimaryContainer = "#134E4A".toColorInt(),
            secondary = "#54716C".toColorInt(),
            tertiary = "#0369A1".toColorInt(),
            progressTrack = "#D9DFEA".toColorInt(),
            outline = "#C5CDD8".toColorInt(),
        )

        private val AuroraDark = WidgetThemeColors(
            surface = "#13171E".toColorInt(),
            surfaceContainer = "#1A1F28".toColorInt(),
            surfaceVariant = "#242B36".toColorInt(),
            onSurface = "#E8ECF2".toColorInt(),
            onSurfaceVariant = "#9AA5B5".toColorInt(),
            primary = "#2DD4BF".toColorInt(),
            primaryContainer = "#115E59".toColorInt(),
            onPrimaryContainer = "#CCFBF1".toColorInt(),
            secondary = "#A7C1BB".toColorInt(),
            tertiary = "#38BDF8".toColorInt(),
            progressTrack = "#41464F".toColorInt(),
            outline = "#3A4454".toColorInt(),
        )

        private val EmberLight = WidgetThemeColors(
            surface = "#FFFFFF".toColorInt(),
            surfaceContainer = "#EEF1F5".toColorInt(),
            surfaceVariant = "#E4E9F0".toColorInt(),
            onSurface = "#11151A".toColorInt(),
            onSurfaceVariant = "#5C6775".toColorInt(),
            primary = "#B45309".toColorInt(),
            primaryContainer = "#FEF3C7".toColorInt(),
            onPrimaryContainer = "#78350F".toColorInt(),
            secondary = "#6B5E4E".toColorInt(),
            tertiary = "#C2410C".toColorInt(),
            progressTrack = "#D9DFEA".toColorInt(),
            outline = "#C5CDD8".toColorInt(),
        )

        private val EmberDark = WidgetThemeColors(
            surface = "#13171E".toColorInt(),
            surfaceContainer = "#1A1F28".toColorInt(),
            surfaceVariant = "#242B36".toColorInt(),
            onSurface = "#E8ECF2".toColorInt(),
            onSurfaceVariant = "#9AA5B5".toColorInt(),
            primary = "#FBBF24".toColorInt(),
            primaryContainer = "#92400E".toColorInt(),
            onPrimaryContainer = "#FEF3C7".toColorInt(),
            secondary = "#D4C4B0".toColorInt(),
            tertiary = "#FB923C".toColorInt(),
            progressTrack = "#41464F".toColorInt(),
            outline = "#3A4454".toColorInt(),
        )

        private val VioletLight = WidgetThemeColors(
            surface = "#FFFFFF".toColorInt(),
            surfaceContainer = "#EEF1F5".toColorInt(),
            surfaceVariant = "#E4E9F0".toColorInt(),
            onSurface = "#11151A".toColorInt(),
            onSurfaceVariant = "#5C6775".toColorInt(),
            primary = "#4F46E5".toColorInt(),
            primaryContainer = "#E0E7FF".toColorInt(),
            onPrimaryContainer = "#312E81".toColorInt(),
            secondary = "#5B5F7A".toColorInt(),
            tertiary = "#7C3AED".toColorInt(),
            progressTrack = "#D9DFEA".toColorInt(),
            outline = "#C5CDD8".toColorInt(),
        )

        private val VioletDark = WidgetThemeColors(
            surface = "#13171E".toColorInt(),
            surfaceContainer = "#1A1F28".toColorInt(),
            surfaceVariant = "#242B36".toColorInt(),
            onSurface = "#E8ECF2".toColorInt(),
            onSurfaceVariant = "#9AA5B5".toColorInt(),
            primary = "#A5B4FC".toColorInt(),
            primaryContainer = "#3730A3".toColorInt(),
            onPrimaryContainer = "#E0E7FF".toColorInt(),
            secondary = "#C5C8DC".toColorInt(),
            tertiary = "#C4B5FD".toColorInt(),
            progressTrack = "#41464F".toColorInt(),
            outline = "#3A4454".toColorInt(),
        )
    }
}

/** Alpha-blend [color] onto opaque black/white for tinted chip backgrounds. */
fun WidgetThemeColors.tintedSurface(fraction: Float = 0.18f): Int {
    val r = Color.red(primary)
    val g = Color.green(primary)
    val b = Color.blue(primary)
    val br = Color.red(surface)
    val bg = Color.green(surface)
    val bb = Color.blue(surface)
    return Color.rgb(
        (br + (r - br) * fraction).toInt().coerceIn(0, 255),
        (bg + (g - bg) * fraction).toInt().coerceIn(0, 255),
        (bb + (b - bb) * fraction).toInt().coerceIn(0, 255),
    )
}
