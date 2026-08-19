package com.lamuier.cursorT.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.lamuier.cursorT.data.ThemeSettings

val LocalPulseChartColors = staticCompositionLocalOf {
    PulseChartColors(
        healthy = PulseSemantic.Healthy,
        warning = PulseSemantic.Warning,
        critical = PulseSemantic.Critical,
        chart1 = Color(0xFF2563EB),
        chart2 = Color(0xFF0E7490),
        chart3 = Color(0xFF475569),
    )
}

@Composable
fun CursorTTheme(
    settings: ThemeSettings = ThemeSettings(),
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (settings.themeMode) {
        ThemeMode.System -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val context = LocalContext.current
    val view = LocalView.current
    val useDynamic = settings.colorPalette == ColorPalette.System &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    val colorScheme = when {
        useDynamic && darkTheme -> dynamicDarkColorScheme(context)
        useDynamic -> dynamicLightColorScheme(context)
        else -> staticScheme(settings.colorPalette, darkTheme)
    }
    val chartColors = chartColorsFor(settings.colorPalette, darkTheme, colorScheme)

    val animatedBackground by animateColorAsState(
        targetValue = colorScheme.background,
        animationSpec = tween(280),
        label = "theme background",
    )

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalPulseChartColors provides chartColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CursorTTypography,
            shapes = CursorTShapes,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(animatedBackground),
            ) {
                content()
            }
        }
    }
}
