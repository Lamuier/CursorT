package com.lamuier.cursorusage

import android.Manifest
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.View
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lamuier.cursorusage.data.NotificationPreferences
import com.lamuier.cursorusage.data.PercentDisplayMode
import com.lamuier.cursorusage.data.ThemePreferences
import com.lamuier.cursorusage.notification.CursorUsageNotificationCoordinator
import com.lamuier.cursorusage.ui.CursorUsageApp
import com.lamuier.cursorusage.ui.CursorUsageViewModel
import com.lamuier.cursorusage.ui.theme.CursorUsageTheme
import com.lamuier.cursorusage.widget.CursorUsageWidgetUpdater

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        preferWideColorGamut()
        val viewModel = ViewModelProvider(
            this,
            CursorUsageViewModel.Factory(applicationContext),
        )[CursorUsageViewModel::class.java]
        val themePreferences = ThemePreferences.get(applicationContext)
        val notificationPreferences = NotificationPreferences.get(applicationContext)

        setContent {
            val themeSettings by themePreferences.settings.collectAsStateWithLifecycle()
            val notificationSettings by notificationPreferences.settings.collectAsStateWithLifecycle()
            CursorUsageTheme(settings = themeSettings) {
                CursorUsageApp(
                    viewModel = viewModel,
                    themeSettings = themeSettings,
                    notificationSettings = notificationSettings,
                    onThemeModeChange = { mode ->
                        themePreferences.setThemeMode(mode)
                        CursorUsageWidgetUpdater.requestUpdate(applicationContext)
                    },
                    onPaletteChange = { palette ->
                        themePreferences.setColorPalette(palette)
                        CursorUsageWidgetUpdater.requestUpdate(applicationContext)
                    },
                onLiveUpdatesToggle = { enabled ->
                    notificationPreferences.setLiveUpdatesEnabled(enabled)
                    if (enabled) {
                        ensureNotificationPermissionAndRefresh()
                    } else {
                        runCatching {
                            CursorUsageNotificationCoordinator
                                .get(applicationContext)
                                .refreshFromCache()
                        }
                    }
                },
                onThresholdRemindersToggle = { enabled ->
                    notificationPreferences.setThresholdRemindersEnabled(enabled)
                    if (enabled) ensureNotificationPermissionAndRefresh()
                },
                percentDisplayMode = notificationSettings.percentDisplayMode,
                onPercentDisplayModeChange = { mode ->
                    notificationPreferences.setPercentDisplayMode(mode)
                    runCatching {
                        CursorUsageNotificationCoordinator
                            .get(applicationContext)
                            .refreshFromCache()
                    }
                },
            )
            }
        }
    }

    /**
     * 在 Android 13+ 上先申请 POST_NOTIFICATIONS，授权（或本就无需授权）后
     * 用最近一次缓存刷新通知并启动周期任务；未授权时 coordinator 内部会撤下通知。
     */
    private fun ensureNotificationPermissionAndRefresh() {
        val needsRequest = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        if (needsRequest) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            runCatching {
                CursorUsageNotificationCoordinator.get(applicationContext).refreshFromCache()
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        // 无论授权与否都按当前偏好刷新一次（未授权则 coordinator 内部撤下通知）。
        runCatching {
            CursorUsageNotificationCoordinator.get(applicationContext).refreshFromCache()
        }
    }

    override fun onResume() {
        super.onResume()
        preferHighRefreshRate()
        preferWideColorGamut()
    }

    override fun onPause() {
        clearRefreshRatePreference()
        super.onPause()
    }

    override fun onStop() {
        super.onStop()
        CursorUsageWidgetUpdater.requestUpdate(applicationContext)
    }

    private fun preferWideColorGamut() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        // Wide color (Display P3) improves accent/chart fidelity on capable panels.
        // Full HDR window mode is intentionally skipped: this UI is SDR charts/text.
        window.colorMode = ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT
    }

    @Suppress("DEPRECATION")
    private fun preferHighRefreshRate() {
        if (getSystemService(PowerManager::class.java).isPowerSaveMode) {
            clearRefreshRatePreference()
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.decorView.setRequestedFrameRate(View.REQUESTED_FRAME_RATE_CATEGORY_HIGH)
        }
        val targetDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            windowManager.defaultDisplay
        } ?: return
        val currentMode = targetDisplay.mode
        val preferredMode = targetDisplay.supportedModes
            .asSequence()
            .filter { mode ->
                mode.physicalWidth == currentMode.physicalWidth &&
                    mode.physicalHeight == currentMode.physicalHeight &&
                    mode.refreshRate <= HIGH_REFRESH_RATE_MAX
            }
            .maxByOrNull { mode -> mode.refreshRate }
            ?: return
        window.attributes = window.attributes.apply {
            preferredDisplayModeId = preferredMode.modeId
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                preferredRefreshRate = preferredMode.refreshRate
            }
        }
    }

    private fun clearRefreshRatePreference() {
        window.attributes = window.attributes.apply {
            preferredDisplayModeId = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                preferredRefreshRate = 0f
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            window.decorView.setRequestedFrameRate(View.REQUESTED_FRAME_RATE_CATEGORY_NO_PREFERENCE)
        }
    }

    private companion object {
        const val HIGH_REFRESH_RATE_MAX = 120.5f
    }
}
