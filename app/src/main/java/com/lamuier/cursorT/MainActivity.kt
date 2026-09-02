package com.lamuier.cursorT

import android.Manifest
import android.content.Context
import android.content.Intent
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lamuier.cursorT.data.DashboardPreferences
import com.lamuier.cursorT.data.NotificationPreferences
import com.lamuier.cursorT.data.PercentDisplayMode
import com.lamuier.cursorT.data.ThemePreferences
import com.lamuier.cursorT.model.ShortcutAction
import com.lamuier.cursorT.notification.CursorTNotificationCoordinator
import com.lamuier.cursorT.ui.CursorTApp
import com.lamuier.cursorT.ui.CursorTViewModel
import com.lamuier.cursorT.ui.theme.CursorTTheme
import com.lamuier.cursorT.util.AppLanguage
import com.lamuier.cursorT.util.AppLocale
import com.lamuier.cursorT.widget.CursorTWidgetUpdater

class MainActivity : FragmentActivity() {
    /** 长按图标 Shortcut 带来的待执行动作，由 UI 层消费后清空。 */
    private var pendingShortcutAction by mutableStateOf<ShortcutAction?>(null)

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        preferWideColorGamut()
        pendingShortcutAction = ShortcutAction.fromIntentAction(intent?.action)
        val viewModel = ViewModelProvider(
            this,
            CursorTViewModel.Factory(applicationContext),
        )[CursorTViewModel::class.java]
        val themePreferences = ThemePreferences.get(applicationContext)
        val notificationPreferences = NotificationPreferences.get(applicationContext)
        val dashboardPreferences = DashboardPreferences.get(applicationContext)

        setContent {
            val themeSettings by themePreferences.settings.collectAsStateWithLifecycle()
            val notificationSettings by notificationPreferences.settings.collectAsStateWithLifecycle()
            val tabOrder by dashboardPreferences.order.collectAsStateWithLifecycle()
            val timeZoneId by dashboardPreferences.timeZoneId.collectAsStateWithLifecycle()
            val language by dashboardPreferences.language.collectAsStateWithLifecycle()
            CursorTTheme(settings = themeSettings) {
                CursorTApp(
                    viewModel = viewModel,
                    themeSettings = themeSettings,
                    notificationSettings = notificationSettings,
                    pendingShortcutAction = pendingShortcutAction,
                    onShortcutActionConsumed = { pendingShortcutAction = null },
                    onThemeModeChange = { mode ->
                        themePreferences.setThemeMode(mode)
                        CursorTWidgetUpdater.requestUpdate(applicationContext)
                    },
                    onPaletteChange = { palette ->
                        themePreferences.setColorPalette(palette)
                        CursorTWidgetUpdater.requestUpdate(applicationContext)
                    },
                    timeZoneId = timeZoneId,
                    onTimeZoneChange = { id ->
                        dashboardPreferences.setTimeZoneId(id)
                        CursorTWidgetUpdater.requestUpdate(applicationContext)
                    },
                    language = language,
                    onLanguageChange = { selected ->
                        if (selected != dashboardPreferences.readLanguage()) {
                            dashboardPreferences.setLanguage(selected)
                            CursorTWidgetUpdater.requestUpdate(applicationContext)
                            recreate()
                        }
                    },
                    onLiveUpdatesToggle = { enabled ->
                    notificationPreferences.setLiveUpdatesEnabled(enabled)
                    if (enabled) {
                        ensureNotificationPermissionAndRefresh()
                    } else {
                        runCatching {
                            CursorTNotificationCoordinator
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
                        CursorTNotificationCoordinator
                            .get(applicationContext)
                            .refreshFromCache()
                    }
                },
                tabOrder = tabOrder,
                onTabOrderChange = dashboardPreferences::setOrder,
                onTabOrderReset = dashboardPreferences::resetOrder,
            )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingShortcutAction = ShortcutAction.fromIntentAction(intent.action)
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
                CursorTNotificationCoordinator.get(applicationContext).refreshFromCache()
            }
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        // 无论授权与否都按当前偏好刷新一次（未授权则 coordinator 内部撤下通知）。
        runCatching {
            CursorTNotificationCoordinator.get(applicationContext).refreshFromCache()
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
        CursorTWidgetUpdater.requestUpdate(applicationContext)
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
