package com.lamuier.cursorT.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.lamuier.cursorT.MainActivity
import com.lamuier.cursorT.R
import com.lamuier.cursorT.data.CursorRepository
import com.lamuier.cursorT.data.NotificationPreferences
import com.lamuier.cursorT.data.PercentDisplayMode
import com.lamuier.cursorT.model.CursorTOverview
import com.lamuier.cursorT.model.TotalFormat
import com.lamuier.cursorT.util.UsageCalculations
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * 常驻「用量监控」Live Update 通知 + 用量阈值提醒的总协调。
 *
 * Live Updates 设计对齐 ScheduleTimeline 真机验证结论：
 *  - channel 必须 IMPORTANCE_HIGH（小米焦点通知只依附 HIGH 渠道）；
 *    渠道 ID 带 `_v2` 后缀，规避 Android「渠道创建后 importance 被系统锁定」的陷阱；
 *  - Android 16 上用 framework Notification.ProgressStyle + promoted ongoing 请求；
 *    Android 17+ 升级为 MetricStyle 三指标模板（用量 % · 重置倒计时 · Credits）
 *    并叠加 Live Update 语义颜色（<80% INFO / ≥80% CAUTION / ≥100% DANGER）；
 *    HyperOS 3（API 36.0）公开方法 `setRequestPromotedOngoing` 尚不存在，直接写入
 *    "android.requestPromotedOngoing" extra（与 NotificationCompat 内部行为一致，且 36.1+
 *    同样识别该 extra）；
 *  - HyperOS 3 的超级岛由 promoted ongoing 通道驱动，miui.focus.* payload 作为
 *    旧版 OS3 表面的兼容层保留（[XiaomiHyperIslandAdapter]）。
 *
 * 刷新由现有刷新链路驱动（应用内用量刷新成功后调用 [refresh]），另挂一个 15 分钟
 * 周期任务（[NotificationRefreshScheduler]）保证无小组件 / 后台时通知不过期。
 */
class CursorTNotificationCoordinator(
    context: Context,
    private val preferences: NotificationPreferences,
) {
    private val appContext = context.applicationContext
    private val notificationManager =
        appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * 以最新用量快照刷新常驻通知与阈值提醒。usage 为 null（无账号 / 无数据）时
     * 撤下常驻通知。每次调用都会按开关状态维护周期刷新任务。
     */
    fun refresh(usage: CursorTOverview?) {
        ensureChannels()
        val settings = preferences.read()
        if (!settings.liveUpdatesEnabled || !canPostNotifications()) {
            notificationManager.cancel(LIVE_NOTIFICATION_ID)
            NotificationRefreshScheduler.cancel(appContext)
        } else {
            NotificationRefreshScheduler.schedule(appContext)
            if (usage == null || usage.accountId <= 0) {
                notificationManager.cancel(LIVE_NOTIFICATION_ID)
            } else {
                postLiveUpdate(usage)
            }
        }
        if (settings.thresholdRemindersEnabled && canPostNotifications() && usage != null) {
            maybePostThresholdReminder(usage)
        }
    }

    /** 开机 / 应用更新后：从本地缓存恢复通知并确保周期任务在跑。 */
    fun refreshFromCache() {
        val settings = preferences.read()
        if (!settings.liveUpdatesEnabled || !canPostNotifications()) {
            notificationManager.cancel(LIVE_NOTIFICATION_ID)
            NotificationRefreshScheduler.cancel(appContext)
            return
        }
        NotificationRefreshScheduler.schedule(appContext)
        val usage = runCatching {
            val repository = CursorRepository(appContext)
            val accountId = repository.selectedAccountId()
                ?: repository.listAccounts().firstOrNull()?.id
            accountId?.let { repository.cachedUsage(it) }
        }.getOrNull()
        refresh(usage)
    }

    private fun postLiveUpdate(usage: CursorTOverview) {
        val nowMillis = System.currentTimeMillis()
        val percent = UsageCalculations.usagePercent(usage)
        val percentInt = percent.roundToInt().coerceIn(0, 999)
        val billing = UsageCalculations.billingProgress(
            usage.billingCycle.start,
            usage.billingCycle.end,
            nowMillis,
        )
        val cycleEndMillis = parseDate(usage.billingCycle.end)

        val title = appContext.getString(R.string.notification_live_title)
        val settings = preferences.read()
        val mode = settings.percentDisplayMode
        val usedPercentInt = percentInt
        // 主百分比按用户选择的口径翻转：剩余/可用 = 100 − 已用（下限 0）。
        val displayPercentInt = when (mode) {
            PercentDisplayMode.Remaining -> (100 - usedPercentInt).coerceAtLeast(0)
            else -> usedPercentInt
        }
        val usagePart = if (usage.usage.totalFormat == TotalFormat.Dollars &&
            usage.usage.limitDollars > 0
        ) {
            if (mode == PercentDisplayMode.Remaining) {
                appContext.getString(
                    R.string.notification_live_usage_remaining_dollars,
                    displayPercentInt,
                    money(usage.usage.remainingDollars),
                    money(usage.usage.limitDollars),
                )
            } else {
                appContext.getString(
                    R.string.notification_live_usage_dollars,
                    usedPercentInt,
                    money(usage.usage.totalSpendDollars),
                    money(usage.usage.limitDollars),
                )
            }
        } else {
            if (mode == PercentDisplayMode.Remaining) {
                appContext.getString(
                    R.string.notification_live_usage_remaining_percent,
                    displayPercentInt,
                )
            } else {
                appContext.getString(R.string.notification_live_usage_percent, usedPercentInt)
            }
        }
        val resetSuffix = billing?.let {
            appContext.getString(
                R.string.notification_live_body_suffix,
                UsageCalculations.formatRemaining(it.remainingMillis),
            )
        }.orEmpty()
        val text = usagePart + resetSuffix
        // Android 17+：正文用量部分套语义颜色（蓝 / 橙 / 红），低版本退化为纯文本。
        val semanticStyle = semanticStyleFor(percent)
        val displayText: CharSequence = if (Build.VERSION.SDK_INT >= 37) {
            SpannableStringBuilder().apply {
                append(
                    usagePart,
                    Notification.createSemanticStyleAnnotation(semanticStyle),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
                append(resetSuffix)
            }
        } else {
            text
        }

        val contentIntent = PendingIntent.getActivity(
            appContext,
            CONTENT_REQUEST_CODE,
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = Notification.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(Icon.createWithResource(appContext, R.drawable.ic_island))
            .setContentTitle(title)
            .setContentText(displayText)
            .setStyle(Notification.BigTextStyle().bigText(displayText))
            .setContentIntent(contentIntent)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(PROGRESS_MAX, displayPercentInt.coerceAtMost(PROGRESS_MAX), false)

        if (cycleEndMillis != null && cycleEndMillis > nowMillis) {
            builder
                .setShowWhen(true)
                .setWhen(cycleEndMillis)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setTimeoutAfter(max(1L, cycleEndMillis - nowMillis))
        } else {
            builder.setShowWhen(false)
        }

        val displayPercentFloat = when (mode) {
            PercentDisplayMode.Remaining -> (100.0 - percent).coerceAtLeast(0.0)
            else -> percent
        }.toFloat()
        when {
            Build.VERSION.SDK_INT >= 37 -> applyAndroid17LiveUpdate(
                builder,
                displayPercentFloat,
                semanticStyle,
                cycleEndMillis?.takeIf { it > nowMillis },
                usage.credits.totalDollars,
            )
            Build.VERSION.SDK_INT >= 36 -> applyAndroid16LiveUpdate(builder, displayPercentInt)
        }
        if (Build.VERSION.SDK_INT >= 36) {
            requestPromotedOngoing(builder)
        }

        val notification = builder.build()
        // 超级岛 payload 仅在小米的非 1/2 协议机型注入（适配器内部二次判定）。
        // 不再要求 cycleEndMillis 非空：套餐未返回计费周期结束时间时，仍展示胶囊
        // （倒计时可选，缺失则只显示图标 + 用量）。适配器内对 timer 做空值保护。
        XiaomiHyperIslandAdapter.applyIfSupported(
            context = appContext,
            notification = notification,
            title = title,
            content = text,
            islandTitle = appContext.getString(
                R.string.notification_island_title,
                displayPercentInt,
            ),
            islandContent = islandRemainingText(usage),
            timerSuffix = appContext.getString(R.string.notification_island_suffix_reset),
            cycleEndMillis = cycleEndMillis?.takeIf { it > nowMillis },
        )
        runCatching { notificationManager.notify(LIVE_NOTIFICATION_ID, notification) }
    }

    @RequiresApi(36)
    private fun applyAndroid16LiveUpdate(builder: Notification.Builder, percentInt: Int) {
        val progressStyle = Notification.ProgressStyle()
            .setProgress(percentInt.coerceIn(0, PROGRESS_MAX))
            .addProgressSegment(Notification.ProgressStyle.Segment(PROGRESS_MAX))
        builder.setStyle(progressStyle)
        builder.setShortCriticalText(
            appContext.getString(R.string.notification_short_percent, percentInt),
        )
    }

    /**
     * Android 17+：Live Update 换用 MetricStyle 模板，AOD / 锁屏 / 状态栏同时
     * 展示「用量 %（关键指标）· 重置倒计时 · Credits」。用量指标套语义颜色，
     * 倒计时用 TimeDifference（FORMAT_ADAPTIVE）随系统自动走动。
     */
    @RequiresApi(37)
    private fun applyAndroid17LiveUpdate(
        builder: Notification.Builder,
        displayPercentFloat: Float,
        semanticStyle: Int,
        cycleEndMillis: Long?,
        creditsDollars: Double,
    ) {
        val metricStyle = Notification.MetricStyle()
            .addMetric(
                Notification.Metric(
                    Notification.Metric.FixedFloat(displayPercentFloat, "%", 2, 2),
                    appContext.getString(R.string.notification_metric_usage),
                    semanticStyle,
                ),
            )
        cycleEndMillis?.let { end ->
            metricStyle.addMetric(
                Notification.Metric(
                    Notification.Metric.TimeDifference.forTimer(
                        Instant.ofEpochMilli(end),
                        Notification.Metric.TimeDifference.FORMAT_ADAPTIVE,
                    ),
                    appContext.getString(R.string.notification_metric_reset),
                ),
            )
        }
        if (creditsDollars > 0.0) {
            metricStyle.addMetric(
                Notification.Metric(
                    Notification.Metric.FixedFloat(creditsDollars.toFloat(), "$", 2, 2),
                    appContext.getString(R.string.notification_metric_credits),
                ),
            )
        }
        metricStyle.setCriticalMetric(0)
        builder.setStyle(metricStyle)
        builder.setShortCriticalText(
            appContext.getString(
                R.string.notification_short_percent,
                displayPercentFloat.toInt(),
            ),
        )
    }

    /** 用量档位 → 语义颜色：<80% INFO（蓝）、≥80% CAUTION（橙）、≥100% DANGER（红）。 */
    private fun semanticStyleFor(usedPercent: Double): Int = when {
        usedPercent >= 100.0 -> Notification.SEMANTIC_STYLE_DANGER
        usedPercent >= 80.0 -> Notification.SEMANTIC_STYLE_CAUTION
        else -> Notification.SEMANTIC_STYLE_INFO
    }

    // Promotion (Live Updates / HyperOS island) works on every Android 16 build,
    // not just QPR2: verified on HyperOS 3 (API 36.0). The public
    // setRequestPromotedOngoing() setter only exists on 36.1+, so on 36.0 we write
    // the same extra NotificationCompat emits. Writing the extra directly is honored
    // on both 36.0 and 36.1+, so we do it unconditionally here. Colorized is never
    // combined with a promotion request (that makes the notification ineligible).
    private fun requestPromotedOngoing(builder: Notification.Builder) {
        builder.addExtras(
            Bundle().apply {
                putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true)
            },
        )
    }

    /**
     * 用量达到 80% / 100% 时各提醒一次，按计费周期去重（周期切换自动重置）。
     */
    private fun maybePostThresholdReminder(usage: CursorTOverview) {
        val percent = UsageCalculations.usagePercent(usage)
        val cycleKey = usage.billingCycle.start ?: usage.fetchedAt
        val reminded = preferences.remindedThresholds(cycleKey)
        val threshold = THRESHOLDS.firstOrNull { percent >= it && it !in reminded } ?: return

        val title = appContext.getString(R.string.notification_reminder_title)
        val text = if (threshold >= 100) {
            appContext.getString(R.string.notification_reminder_body_full)
        } else {
            appContext.getString(
                R.string.notification_reminder_body_threshold,
                threshold,
                percent.roundToInt(),
            )
        }
        val contentIntent = PendingIntent.getActivity(
            appContext,
            REMINDER_CONTENT_REQUEST_CODE,
            Intent(appContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val reminderBuilder = Notification.Builder(appContext, REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
        if (Build.VERSION.SDK_INT >= 37) {
            // Android 17+：80% 提醒套 CAUTION（橙）、100% 提醒套 DANGER（红）。
            val style = if (threshold >= 100) {
                Notification.SEMANTIC_STYLE_DANGER
            } else {
                Notification.SEMANTIC_STYLE_CAUTION
            }
            val spannable = SpannableString(text)
            spannable.setSpan(
                Notification.createSemanticStyleAnnotation(style),
                0,
                spannable.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            reminderBuilder
                .setContentText(spannable)
                .setStyle(Notification.BigTextStyle().bigText(spannable))
        }
        val notification = reminderBuilder.build()
        runCatching {
            notificationManager.notify(REMINDER_NOTIFICATION_ID_BASE + threshold, notification)
        }
        // 越过的所有低档位一并标记，避免 100% 提醒之后又补发一条 80% 提醒。
        THRESHOLDS.filter { percent >= it }.forEach {
            preferences.markThresholdReminded(cycleKey, it)
        }
    }

    private fun islandRemainingText(usage: CursorTOverview): String {
        if (usage.usage.totalFormat != TotalFormat.Dollars ||
            usage.usage.remainingDollars <= 0
        ) {
            return ""
        }
        return appContext.getString(
            R.string.notification_island_remaining,
            money(usage.usage.remainingDollars),
        )
    }

    private fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                appContext.getString(R.string.notification_live_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description =
                    appContext.getString(R.string.notification_live_channel_description)
                setShowBadge(true)
            },
        )
        notificationManager.createNotificationChannel(
            NotificationChannel(
                REMINDER_CHANNEL_ID,
                appContext.getString(R.string.notification_reminder_channel_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description =
                    appContext.getString(R.string.notification_reminder_channel_description)
                setShowBadge(true)
            },
        )
    }

    private fun canPostNotifications(): Boolean =
        hasNotificationPermission() && notificationManager.areNotificationsEnabled()

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    private fun parseDate(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        val normalized = value.replace(' ', 'T')
        return try {
            LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (_: DateTimeParseException) {
            null
        }
    }

    private fun money(value: Double): String =
        "$" + String.format(Locale.US, "%.2f", value)

    companion object {
        const val LIVE_NOTIFICATION_ID = 4201
        private const val CHANNEL_ID = "usage_live_updates_v2"
        private const val REMINDER_CHANNEL_ID = "usage_reminders_v2"
        private const val CONTENT_REQUEST_CODE = 4202
        private const val REMINDER_CONTENT_REQUEST_CODE = 4203
        private const val REMINDER_NOTIFICATION_ID_BASE = 4300
        private const val PROGRESS_MAX = 100
        private const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing"
        private val THRESHOLDS = listOf(100, 80)

        @Volatile
        private var instance: CursorTNotificationCoordinator? = null

        fun get(context: Context): CursorTNotificationCoordinator {
            return instance ?: synchronized(this) {
                instance ?: CursorTNotificationCoordinator(
                    context.applicationContext,
                    NotificationPreferences.get(context),
                ).also { instance = it }
            }
        }
    }
}
