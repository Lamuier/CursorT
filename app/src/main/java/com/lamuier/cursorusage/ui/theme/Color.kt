package com.lamuier.cursorusage.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

enum class ThemeMode(val storageKey: String) {
    System("system"),
    Light("light"),
    Dark("dark"),
    ;

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.firstOrNull { it.storageKey == value } ?: System
    }
}

enum class ColorPalette(
    val storageKey: String,
    val displayName: String,
    val previewSwatch: Color,
) {
    Pulse("pulse", "冰蓝", Color(0xFF3B82F6)),
    Aurora("aurora", "青绿", Color(0xFF0D9488)),
    Ember("ember", "琥珀", Color(0xFFD97706)),
    Violet("violet", "靛紫", Color(0xFF6366F1)),
    System("system", "系统", Color(0xFF78909C)),
    ;

    companion object {
        fun fromStorage(value: String?): ColorPalette =
            entries.firstOrNull { it.storageKey == value } ?: Pulse
    }
}

@Immutable
data class PulseChartColors(
    val healthy: Color,
    val warning: Color,
    val critical: Color,
    val chart1: Color,
    val chart2: Color,
    val chart3: Color,
)

object PulseSemantic {
    val Healthy = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    val Critical = Color(0xFFEF4444)
}

private object Surfaces {
    val LightBg = Color(0xFFF4F6F8)
    val LightSurface = Color(0xFFFFFFFF)
    val LightSurfaceLow = Color(0xFFEEF1F5)
    val LightSurfaceHigh = Color(0xFFE4E9F0)
    val LightOn = Color(0xFF11151A)
    val LightOnVariant = Color(0xFF5C6775)
    val LightOutline = Color(0xFFC5CDD8)
    val LightOutlineVariant = Color(0xFFDDE3EA)

    val DarkBg = Color(0xFF0B0E12)
    val DarkSurface = Color(0xFF13171E)
    val DarkSurfaceLow = Color(0xFF1A1F28)
    val DarkSurfaceHigh = Color(0xFF242B36)
    val DarkOn = Color(0xFFE8ECF2)
    val DarkOnVariant = Color(0xFF9AA5B5)
    val DarkOutline = Color(0xFF3A4454)
    val DarkOutlineVariant = Color(0xFF2A3342)
}

fun pulseLightScheme(): ColorScheme = lightColorScheme(
    primary = Color(0xFF2563EB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEBFF),
    onPrimaryContainer = Color(0xFF0A2A5C),
    secondary = Color(0xFF475569),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E8F0),
    onSecondaryContainer = Color(0xFF1E293B),
    tertiary = Color(0xFF0E7490),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCFFAFE),
    onTertiaryContainer = Color(0xFF164E63),
    background = Surfaces.LightBg,
    onBackground = Surfaces.LightOn,
    surface = Surfaces.LightSurface,
    onSurface = Surfaces.LightOn,
    surfaceVariant = Surfaces.LightSurfaceHigh,
    onSurfaceVariant = Surfaces.LightOnVariant,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Surfaces.LightSurfaceLow,
    surfaceContainer = Color(0xFFE8ECF2),
    surfaceContainerHigh = Surfaces.LightSurfaceHigh,
    surfaceContainerHighest = Color(0xFFD8DFE8),
    outline = Surfaces.LightOutline,
    outlineVariant = Surfaces.LightOutlineVariant,
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
)

fun pulseDarkScheme(): ColorScheme = darkColorScheme(
    primary = Color(0xFF60A5FA),
    onPrimary = Color(0xFF0A1F3D),
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = Color(0xFFDCEBFF),
    secondary = Color(0xFF94A3B8),
    onSecondary = Color(0xFF1E293B),
    secondaryContainer = Color(0xFF334155),
    onSecondaryContainer = Color(0xFFE2E8F0),
    tertiary = Color(0xFF22D3EE),
    onTertiary = Color(0xFF083344),
    tertiaryContainer = Color(0xFF155E75),
    onTertiaryContainer = Color(0xFFCFFAFE),
    background = Surfaces.DarkBg,
    onBackground = Surfaces.DarkOn,
    surface = Surfaces.DarkSurface,
    onSurface = Surfaces.DarkOn,
    surfaceVariant = Surfaces.DarkSurfaceHigh,
    onSurfaceVariant = Surfaces.DarkOnVariant,
    surfaceContainerLowest = Color(0xFF080A0E),
    surfaceContainerLow = Surfaces.DarkSurfaceLow,
    surfaceContainer = Color(0xFF1E2430),
    surfaceContainerHigh = Surfaces.DarkSurfaceHigh,
    surfaceContainerHighest = Color(0xFF2E3644),
    outline = Surfaces.DarkOutline,
    outlineVariant = Surfaces.DarkOutlineVariant,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
)

fun auroraLightScheme(): ColorScheme = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF134E4A),
    secondary = Color(0xFF54716C),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5E5E1),
    onSecondaryContainer = Color(0xFF1A2F2C),
    tertiary = Color(0xFF0369A1),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE0F2FE),
    onTertiaryContainer = Color(0xFF0C4A6E),
    background = Surfaces.LightBg,
    onBackground = Surfaces.LightOn,
    surface = Surfaces.LightSurface,
    onSurface = Surfaces.LightOn,
    surfaceVariant = Surfaces.LightSurfaceHigh,
    onSurfaceVariant = Surfaces.LightOnVariant,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Surfaces.LightSurfaceLow,
    surfaceContainer = Color(0xFFE8ECF2),
    surfaceContainerHigh = Surfaces.LightSurfaceHigh,
    surfaceContainerHighest = Color(0xFFD8DFE8),
    outline = Surfaces.LightOutline,
    outlineVariant = Surfaces.LightOutlineVariant,
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
)

fun auroraDarkScheme(): ColorScheme = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF115E59),
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Color(0xFFA7C1BB),
    onSecondary = Color(0xFF1A2F2C),
    secondaryContainer = Color(0xFF2F4440),
    onSecondaryContainer = Color(0xFFD5E5E1),
    tertiary = Color(0xFF38BDF8),
    onTertiary = Color(0xFF0C4A6E),
    tertiaryContainer = Color(0xFF075985),
    onTertiaryContainer = Color(0xFFE0F2FE),
    background = Surfaces.DarkBg,
    onBackground = Surfaces.DarkOn,
    surface = Surfaces.DarkSurface,
    onSurface = Surfaces.DarkOn,
    surfaceVariant = Surfaces.DarkSurfaceHigh,
    onSurfaceVariant = Surfaces.DarkOnVariant,
    surfaceContainerLowest = Color(0xFF080A0E),
    surfaceContainerLow = Surfaces.DarkSurfaceLow,
    surfaceContainer = Color(0xFF1E2430),
    surfaceContainerHigh = Surfaces.DarkSurfaceHigh,
    surfaceContainerHighest = Color(0xFF2E3644),
    outline = Surfaces.DarkOutline,
    outlineVariant = Surfaces.DarkOutlineVariant,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
)

fun emberLightScheme(): ColorScheme = lightColorScheme(
    primary = Color(0xFFB45309),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),
    secondary = Color(0xFF6B5E4E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0E6D8),
    onSecondaryContainer = Color(0xFF2A2218),
    tertiary = Color(0xFFC2410C),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFEDD5),
    onTertiaryContainer = Color(0xFF7C2D12),
    background = Surfaces.LightBg,
    onBackground = Surfaces.LightOn,
    surface = Surfaces.LightSurface,
    onSurface = Surfaces.LightOn,
    surfaceVariant = Surfaces.LightSurfaceHigh,
    onSurfaceVariant = Surfaces.LightOnVariant,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Surfaces.LightSurfaceLow,
    surfaceContainer = Color(0xFFE8ECF2),
    surfaceContainerHigh = Surfaces.LightSurfaceHigh,
    surfaceContainerHighest = Color(0xFFD8DFE8),
    outline = Surfaces.LightOutline,
    outlineVariant = Surfaces.LightOutlineVariant,
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
)

fun emberDarkScheme(): ColorScheme = darkColorScheme(
    primary = Color(0xFFFBBF24),
    onPrimary = Color(0xFF451A03),
    primaryContainer = Color(0xFF92400E),
    onPrimaryContainer = Color(0xFFFEF3C7),
    secondary = Color(0xFFD4C4B0),
    onSecondary = Color(0xFF2A2218),
    secondaryContainer = Color(0xFF4A4034),
    onSecondaryContainer = Color(0xFFF0E6D8),
    tertiary = Color(0xFFFB923C),
    onTertiary = Color(0xFF7C2D12),
    tertiaryContainer = Color(0xFF9A3412),
    onTertiaryContainer = Color(0xFFFFEDD5),
    background = Surfaces.DarkBg,
    onBackground = Surfaces.DarkOn,
    surface = Surfaces.DarkSurface,
    onSurface = Surfaces.DarkOn,
    surfaceVariant = Surfaces.DarkSurfaceHigh,
    onSurfaceVariant = Surfaces.DarkOnVariant,
    surfaceContainerLowest = Color(0xFF080A0E),
    surfaceContainerLow = Surfaces.DarkSurfaceLow,
    surfaceContainer = Color(0xFF1E2430),
    surfaceContainerHigh = Surfaces.DarkSurfaceHigh,
    surfaceContainerHighest = Color(0xFF2E3644),
    outline = Surfaces.DarkOutline,
    outlineVariant = Surfaces.DarkOutlineVariant,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
)

fun violetLightScheme(): ColorScheme = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF312E81),
    secondary = Color(0xFF5B5F7A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E4F0),
    onSecondaryContainer = Color(0xFF1E2030),
    tertiary = Color(0xFF7C3AED),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE9FE),
    onTertiaryContainer = Color(0xFF4C1D95),
    background = Surfaces.LightBg,
    onBackground = Surfaces.LightOn,
    surface = Surfaces.LightSurface,
    onSurface = Surfaces.LightOn,
    surfaceVariant = Surfaces.LightSurfaceHigh,
    onSurfaceVariant = Surfaces.LightOnVariant,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Surfaces.LightSurfaceLow,
    surfaceContainer = Color(0xFFE8ECF2),
    surfaceContainerHigh = Surfaces.LightSurfaceHigh,
    surfaceContainerHighest = Color(0xFFD8DFE8),
    outline = Surfaces.LightOutline,
    outlineVariant = Surfaces.LightOutlineVariant,
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
)

fun violetDarkScheme(): ColorScheme = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFFC5C8DC),
    onSecondary = Color(0xFF1E2030),
    secondaryContainer = Color(0xFF3A3E55),
    onSecondaryContainer = Color(0xFFE2E4F0),
    tertiary = Color(0xFFC4B5FD),
    onTertiary = Color(0xFF4C1D95),
    tertiaryContainer = Color(0xFF6D28D9),
    onTertiaryContainer = Color(0xFFEDE9FE),
    background = Surfaces.DarkBg,
    onBackground = Surfaces.DarkOn,
    surface = Surfaces.DarkSurface,
    onSurface = Surfaces.DarkOn,
    surfaceVariant = Surfaces.DarkSurfaceHigh,
    onSurfaceVariant = Surfaces.DarkOnVariant,
    surfaceContainerLowest = Color(0xFF080A0E),
    surfaceContainerLow = Surfaces.DarkSurfaceLow,
    surfaceContainer = Color(0xFF1E2430),
    surfaceContainerHigh = Surfaces.DarkSurfaceHigh,
    surfaceContainerHighest = Color(0xFF2E3644),
    outline = Surfaces.DarkOutline,
    outlineVariant = Surfaces.DarkOutlineVariant,
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
)

fun chartColorsFor(palette: ColorPalette, dark: Boolean, scheme: ColorScheme): PulseChartColors {
    val chart1 = scheme.primary
    val chart2 = scheme.tertiary
    val chart3 = if (dark) scheme.secondary else scheme.secondary
    return when (palette) {
        ColorPalette.Pulse, ColorPalette.System -> PulseChartColors(
            healthy = PulseSemantic.Healthy,
            warning = PulseSemantic.Warning,
            critical = PulseSemantic.Critical,
            chart1 = chart1,
            chart2 = chart2,
            chart3 = chart3,
        )
        ColorPalette.Aurora -> PulseChartColors(
            healthy = Color(0xFF14B8A6),
            warning = PulseSemantic.Warning,
            critical = PulseSemantic.Critical,
            chart1 = chart1,
            chart2 = chart2,
            chart3 = chart3,
        )
        ColorPalette.Ember -> PulseChartColors(
            healthy = Color(0xFF22C55E),
            warning = Color(0xFFFBBF24),
            critical = PulseSemantic.Critical,
            chart1 = chart1,
            chart2 = chart2,
            chart3 = chart3,
        )
        ColorPalette.Violet -> PulseChartColors(
            healthy = PulseSemantic.Healthy,
            warning = PulseSemantic.Warning,
            critical = PulseSemantic.Critical,
            chart1 = chart1,
            chart2 = chart2,
            chart3 = chart3,
        )
    }
}

fun staticScheme(palette: ColorPalette, dark: Boolean): ColorScheme = when (palette) {
    ColorPalette.Pulse, ColorPalette.System -> if (dark) pulseDarkScheme() else pulseLightScheme()
    ColorPalette.Aurora -> if (dark) auroraDarkScheme() else auroraLightScheme()
    ColorPalette.Ember -> if (dark) emberDarkScheme() else emberLightScheme()
    ColorPalette.Violet -> if (dark) violetDarkScheme() else violetLightScheme()
}
