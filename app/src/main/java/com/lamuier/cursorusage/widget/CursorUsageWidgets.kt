package com.lamuier.cursorusage.widget

import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.PersistableBundle
import android.os.SystemClock
import android.widget.RemoteViews
import com.lamuier.cursorusage.MainActivity
import com.lamuier.cursorusage.R
import com.lamuier.cursorusage.data.AccountRevisionChangedException
import com.lamuier.cursorusage.data.CursorRepository
import com.lamuier.cursorusage.model.CursorAccount
import com.lamuier.cursorusage.model.CursorUsageOverview
import com.lamuier.cursorusage.network.ApiException
import com.lamuier.cursorusage.util.UsageCalculations
import java.io.IOException
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val ACTION_MANUAL_REFRESH = "com.lamuier.cursorusage.action.MANUAL_WIDGET_REFRESH"
private const val EXTRA_FORCE_REFRESH = "force_refresh"
private const val WIDGET_REFRESH_JOB_ID = 0x43505731
private const val CACHE_FRESH_SECONDS = 15 * 60
private const val MANUAL_REFRESH_COOLDOWN_MS = 10_000L

private enum class WidgetKind(val layoutId: Int, val requestBase: Int) {
    Mini(R.layout.widget_cursor_mini, 15_000),
    Tall(R.layout.widget_cursor_tall, 25_000),
}

private data class ProviderSpec(
    val providerClass: Class<out AppWidgetProvider>,
    val kind: WidgetKind,
)

private data class WidgetSnapshot(
    val account: CursorAccount?,
    val usage: CursorUsageOverview?,
    val status: String,
    val accountRevision: Long? = null,
)

private data class WidgetLoadResult(
    val snapshot: WidgetSnapshot,
    val shouldRetry: Boolean = false,
)

abstract class BaseCursorWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        CursorUsageWidgetUpdater.updateFromCache(context.applicationContext, goAsync())
        CursorUsageWidgetUpdater.scheduleRefresh(context.applicationContext, force = false)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: android.os.Bundle,
    ) {
        CursorUsageWidgetUpdater.updateFromCache(context.applicationContext, goAsync())
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_MANUAL_REFRESH -> {
                val appWidgetId = intent.getIntExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID,
                )
                val ownedIds = AppWidgetManager.getInstance(context).getAppWidgetIds(
                    ComponentName(context, javaClass),
                )
                if (appWidgetId in ownedIds) {
                    val accepted = CursorUsageWidgetUpdater.scheduleManualRefresh(context.applicationContext)
                    CursorUsageWidgetUpdater.updateFromCache(
                        context.applicationContext,
                        goAsync(),
                        statusOverride = if (accepted) "正在刷新…" else "请稍后再刷新",
                    )
                }
            }
            else -> super.onReceive(context, intent)
        }
    }

    override fun onEnabled(context: Context) {
        CursorUsageWidgetUpdater.scheduleRefresh(context.applicationContext, force = false)
    }

    override fun onDisabled(context: Context) {
        CursorUsageWidgetUpdater.cancelIfNoWidgets(context.applicationContext)
    }
}

class MiniCursorWidgetProvider : BaseCursorWidgetProvider()

class TallCursorWidgetProvider : BaseCursorWidgetProvider()

object CursorUsageWidgetUpdater {
    private val shortScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val refreshMutex = Mutex()
    private val lastManualRefreshAt = AtomicLong(0L)
    private val cacheRenderGeneration = AtomicLong(0L)
    private val scheduleLock = Any()
    private val renderLock = Any()
    private var jobRunning = false
    private var forceQueuedWhileRunning = false

    private val providerSpecs = listOf(
        ProviderSpec(MiniCursorWidgetProvider::class.java, WidgetKind.Mini),
        ProviderSpec(TallCursorWidgetProvider::class.java, WidgetKind.Tall),
    )

    fun requestUpdate(context: Context) {
        val appContext = context.applicationContext
        if (!hasWidgets(appContext)) return
        val generation = cacheRenderGeneration.incrementAndGet()
        shortScope.launch {
            val snapshot = withContext(Dispatchers.IO) { WidgetLoader.readCached(appContext) }
            renderAll(appContext, snapshot, generation)
        }
        scheduleRefresh(appContext, force = false)
    }

    internal fun updateFromCache(
        context: Context,
        pendingResult: android.content.BroadcastReceiver.PendingResult,
        statusOverride: String? = null,
    ) {
        val generation = cacheRenderGeneration.incrementAndGet()
        shortScope.launch {
            try {
                val snapshot = withContext(Dispatchers.IO) { WidgetLoader.readCached(context) }
                renderAll(
                    context,
                    statusOverride?.let { snapshot.copy(status = it) } ?: snapshot,
                    generation,
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    internal fun scheduleManualRefresh(context: Context): Boolean {
        val now = SystemClock.elapsedRealtime()
        while (true) {
            val previous = lastManualRefreshAt.get()
            if (previous != 0L && now - previous < MANUAL_REFRESH_COOLDOWN_MS) return false
            if (lastManualRefreshAt.compareAndSet(previous, now)) break
        }
        val scheduled = scheduleRefresh(context, force = true)
        if (!scheduled) lastManualRefreshAt.compareAndSet(now, 0L)
        return scheduled
    }

    internal fun scheduleRefresh(context: Context, force: Boolean): Boolean {
        if (!hasWidgets(context)) return false
        return synchronized(scheduleLock) {
            if (jobRunning) {
                forceQueuedWhileRunning = forceQueuedWhileRunning || force
                return@synchronized true
            }
            val scheduler = context.getSystemService(JobScheduler::class.java)
            val pendingJob = scheduler.getPendingJob(WIDGET_REFRESH_JOB_ID)
            val pendingForce = pendingJob
                ?.extras
                ?.getBoolean(EXTRA_FORCE_REFRESH, false)
                ?: false
            if (pendingJob != null && (!force || pendingForce)) return@synchronized true

            val extras = PersistableBundle().apply {
                putBoolean(EXTRA_FORCE_REFRESH, force || pendingForce)
            }
            val job = JobInfo.Builder(
                WIDGET_REFRESH_JOB_ID,
                ComponentName(context, CursorUsageWidgetJobService::class.java),
            )
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setMinimumLatency(0L)
                .setBackoffCriteria(30_000L, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                .setExtras(extras)
                .build()
            scheduler.schedule(job) == JobScheduler.RESULT_SUCCESS
        }
    }

    internal fun onJobStarted(force: Boolean): Boolean = synchronized(scheduleLock) {
        jobRunning = true
        val effectiveForce = force || forceQueuedWhileRunning
        forceQueuedWhileRunning = false
        effectiveForce
    }

    internal fun takeQueuedForce(): Boolean = synchronized(scheduleLock) {
        val queued = forceQueuedWhileRunning
        forceQueuedWhileRunning = false
        queued
    }

    internal fun onJobFinished(context: Context) {
        val scheduleForce = synchronized(scheduleLock) {
            jobRunning = false
            val queued = forceQueuedWhileRunning
            forceQueuedWhileRunning = false
            queued
        }
        if (scheduleForce) scheduleRefresh(context, force = true)
    }

    internal fun onJobStopped(context: Context) {
        onJobFinished(context)
    }

    internal suspend fun performScheduledRefresh(context: Context, force: Boolean): Boolean =
        refreshMutex.withLock {
            val result = withContext(Dispatchers.IO) { WidgetLoader.load(context, force) }
            val generation = cacheRenderGeneration.incrementAndGet()
            renderAll(context, result.snapshot, generation)
            result.shouldRetry
        }

    internal fun cancelIfNoWidgets(context: Context) {
        if (!hasWidgets(context)) {
            context.getSystemService(JobScheduler::class.java).cancel(WIDGET_REFRESH_JOB_ID)
        }
    }

    private fun hasWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        return providerSpecs.any { spec ->
            manager.getAppWidgetIds(ComponentName(context, spec.providerClass)).isNotEmpty()
        }
    }

    private fun renderAll(
        context: Context,
        snapshot: WidgetSnapshot,
        expectedGeneration: Long,
    ) {
        synchronized(renderLock) {
            if (expectedGeneration != cacheRenderGeneration.get()) return
            if (!WidgetLoader.isCurrent(context, snapshot)) return
            val manager = AppWidgetManager.getInstance(context)
            providerSpecs.forEach { spec ->
                manager.getAppWidgetIds(ComponentName(context, spec.providerClass)).forEach { widgetId ->
                    val views = render(context, spec, widgetId, snapshot)
                    manager.updateAppWidget(widgetId, views)
                }
            }
        }
    }

    private fun render(
        context: Context,
        spec: ProviderSpec,
        widgetId: Int,
        snapshot: WidgetSnapshot,
    ): RemoteViews {
        // Each provider renders its own declared layout.
        // HyperOS (and many launchers) report [OPTION_APPWIDGET_MAX_HEIGHT] values
        // that are 3–4× smaller than reality, making runtime-height-based layout
        // selection unreliable. Future work: wire [onAppWidgetOptionsChanged]
        // to handle genuine drag-resize events.
        val effectiveKind = spec.kind
        val views = RemoteViews(context.packageName, effectiveKind.layoutId)
        val usage = snapshot.usage
        val account = snapshot.account
        val totalPercent = usage?.let(WidgetCalculations::totalPercent)
        val brand = context.getString(R.string.widget_brand)
        val accountLabel = account?.alias?.takeIf { it.isNotBlank() }
        val colors = WidgetThemeColors.resolve(context)

        views.setTextViewText(R.id.widget_brand, brand)
        if (accountLabel != null) {
            views.setTextViewText(R.id.widget_title, accountLabel)
            views.setViewVisibility(R.id.widget_title, android.view.View.VISIBLE)
        } else {
            views.setViewVisibility(R.id.widget_title, android.view.View.GONE)
        }
        views.setTextViewText(R.id.widget_value, usage?.let(WidgetCalculations::totalLabel) ?: "—")
        views.setTextViewText(R.id.widget_status, snapshot.status)
        views.setOnClickPendingIntent(
            R.id.widget_root,
            openAppPendingIntent(context, spec, widgetId),
        )
        applyWidgetTheme(context, views, effectiveKind, widgetId, colors, totalPercent)

        if (effectiveKind == WidgetKind.Tall) {
            views.setOnClickPendingIntent(
                R.id.widget_refresh,
                if (account == null || account.tokenExpired) {
                    openAppPendingIntent(context, spec, widgetId)
                } else {
                    refreshPendingIntent(context, spec, widgetId)
                },
            )
        }

        if (effectiveKind == WidgetKind.Tall) {
            val progress = WidgetCalculations.progress(totalPercent)
            views.setProgressBar(R.id.widget_progress, 100, progress, false)
            views.setContentDescription(
                R.id.widget_progress,
                "Cursor 总用量 ${totalPercent?.let(WidgetCalculations::percent) ?: "暂无数据"}",
            )
            views.setTextViewText(
                R.id.widget_plan,
                usage?.plan?.name?.takeIf { it.isNotBlank() } ?: "套餐 —",
            )
            bindModeProgress(
                views,
                R.id.widget_auto_value,
                R.id.widget_auto_progress,
                "Cursor 模型",
                usage?.usage?.autoPercentUsed,
            )
            bindModeProgress(
                views,
                R.id.widget_api_value,
                R.id.widget_api_progress,
                "其他模型",
                usage?.usage?.apiPercentUsed,
            )
            views.setTextViewText(
                R.id.widget_credits,
                when {
                    usage == null -> "Credits —"
                    usage.partialData -> "Credits —"
                    else -> "Credits ${WidgetCalculations.money(usage.credits.totalDollars)}"
                },
            )
            views.setTextViewText(
                R.id.widget_billing,
                usage?.let(::billingLabel) ?: "周期 —",
            )
        }
        return views
    }

    private fun applyWidgetTheme(
        context: Context,
        views: RemoteViews,
        kind: WidgetKind,
        widgetId: Int,
        colors: WidgetThemeColors,
        totalPercent: Double?,
    ) {
        val (fallbackW, fallbackH) = when (kind) {
            WidgetKind.Mini -> 110 to 40
            WidgetKind.Tall -> 250 to 180
        }
        // Root must stay transparent — a solid color here fills the bitmap's
        // transparent corners and makes the widget look completely square.
        views.setInt(R.id.widget_root, "setBackgroundColor", android.graphics.Color.TRANSPARENT)
        views.setImageViewBitmap(
            R.id.widget_surface,
            WidgetVisuals.surfaceBitmap(
                context,
                widgetId,
                WidgetVisuals.glassSurface(colors.surface),
                fallbackW,
                fallbackH,
            ),
        )
        views.setTextColor(R.id.widget_brand, colors.onSurfaceVariant)
        views.setTextColor(R.id.widget_title, colors.onSurfaceVariant)
        views.setTextColor(R.id.widget_status, colors.onSurfaceVariant)
        views.setTextColor(R.id.widget_value, colors.onSurface)

        when (kind) {
            WidgetKind.Mini -> {
                views.setImageViewBitmap(
                    R.id.widget_donut,
                    WidgetVisuals.donutBitmap(
                        context = context,
                        sizeDp = 28,
                        progress = WidgetCalculations.progress(totalPercent),
                        progressColor = colors.primary,
                        trackColor = colors.progressTrack,
                        strokeDp = 4f,
                    ),
                )
            }
            WidgetKind.Tall -> {
                tintRefreshButton(views, colors)
                views.setImageViewBitmap(
                    R.id.widget_refresh,
                    WidgetVisuals.refreshIconBitmap(context, colors.onSurfaceVariant),
                )
                views.setImageViewBitmap(
                    R.id.widget_donut,
                    WidgetVisuals.donutBitmap(
                        context = context,
                        sizeDp = 72,
                        progress = WidgetCalculations.progress(totalPercent),
                        progressColor = colors.primary,
                        trackColor = colors.progressTrack,
                    ),
                )
                views.setImageViewBitmap(
                    R.id.widget_auto_dot,
                    WidgetVisuals.chipBitmap(context, colors.primary, 8, 8),
                )
                views.setImageViewBitmap(
                    R.id.widget_api_dot,
                    WidgetVisuals.chipBitmap(context, colors.tertiary, 8, 8),
                )
                tintChipBackground(views, R.id.widget_plan, colors.surfaceContainer)
                tintChipBackground(views, R.id.widget_credits, colors.surfaceContainer)
                tintChipBackground(views, R.id.widget_billing, colors.surfaceContainer)
                views.setTextColor(R.id.widget_plan, colors.onSurfaceVariant)
                views.setTextColor(R.id.widget_auto_value, colors.onSurface)
                views.setTextColor(R.id.widget_api_value, colors.onSurface)
                views.setTextColor(R.id.widget_credits, colors.onSurface)
                views.setTextColor(R.id.widget_billing, colors.onSurfaceVariant)
                tintProgress(views, R.id.widget_progress, colors)
                tintProgress(views, R.id.widget_auto_progress, colors)
                tintProgress(views, R.id.widget_api_progress, colors, secondary = true)
            }
        }
    }

    private fun tintRefreshButton(views: RemoteViews, colors: WidgetThemeColors) {
        views.setInt(R.id.widget_refresh, "setBackgroundResource", R.drawable.widget_refresh_background)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            views.setColorStateList(
                R.id.widget_refresh,
                "setBackgroundTintList",
                ColorStateList.valueOf(colors.surfaceVariant),
            )
        }
    }

    private fun tintChipBackground(views: RemoteViews, viewId: Int, color: Int) {
        views.setInt(viewId, "setBackgroundResource", R.drawable.widget_metric_background)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            views.setColorStateList(
                viewId,
                "setBackgroundTintList",
                ColorStateList.valueOf(color),
            )
        }
    }

    private fun tintProgress(
        views: RemoteViews,
        progressId: Int,
        colors: WidgetThemeColors,
        secondary: Boolean = false,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val tint = if (secondary) colors.secondary else colors.primary
        views.setColorStateList(
            progressId,
            "setProgressTintList",
            android.content.res.ColorStateList.valueOf(tint),
        )
        views.setColorStateList(
            progressId,
            "setProgressBackgroundTintList",
            android.content.res.ColorStateList.valueOf(colors.progressTrack),
        )
    }

    private fun bindModeProgress(
        views: RemoteViews,
        labelId: Int,
        progressId: Int,
        label: String,
        value: Double?,
    ) {
        views.setTextViewText(labelId, WidgetCalculations.modeLabel(label, value))
        views.setProgressBar(progressId, 100, WidgetCalculations.progress(value), false)
        views.setContentDescription(
            progressId,
            "$label 用量池 ${value?.let(WidgetCalculations::percent) ?: "暂无数据"}",
        )
    }

    private fun billingLabel(usage: CursorUsageOverview): String {
        val billing = UsageCalculations.billingProgress(
            usage.billingCycle.start,
            usage.billingCycle.end,
        )
        return when {
            billing != null -> "周期剩 ${billing.remainingDays} 天"
            !usage.plan.billingCycleEnd.isNullOrBlank() -> "截至 ${usage.plan.billingCycleEnd.take(10)}"
            else -> "周期 —"
        }
    }

    private fun openAppPendingIntent(
        context: Context,
        spec: ProviderSpec,
        widgetId: Int,
    ): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            action = "com.lamuier.cursorusage.action.OPEN_FROM_WIDGET"
            data = widgetUri("open", spec, widgetId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            spec.kind.requestBase + widgetId * 2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun refreshPendingIntent(
        context: Context,
        spec: ProviderSpec,
        widgetId: Int,
    ): PendingIntent {
        val intent = Intent(context, spec.providerClass).apply {
            action = ACTION_MANUAL_REFRESH
            data = widgetUri("refresh", spec, widgetId)
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        }
        return PendingIntent.getBroadcast(
            context,
            spec.kind.requestBase + widgetId * 2 + 1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun widgetUri(action: String, spec: ProviderSpec, widgetId: Int): Uri =
        Uri.Builder()
            .scheme("cursormeter")
            .authority("widget")
            .appendPath(action)
            .appendPath(spec.kind.name.lowercase())
            .appendPath(widgetId.toString())
            .build()
}

class CursorUsageWidgetJobService : JobService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeJob: Job? = null

    override fun onStartJob(params: JobParameters): Boolean {
        val initialForce = CursorUsageWidgetUpdater.onJobStarted(
            params.extras.getBoolean(EXTRA_FORCE_REFRESH, false),
        )
        activeJob?.cancel()
        activeJob = serviceScope.launch {
            var force = initialForce
            var retry: Boolean
            while (true) {
                retry = try {
                    CursorUsageWidgetUpdater.performScheduledRefresh(applicationContext, force)
                } catch (_: CancellationException) {
                    return@launch
                } catch (_: Exception) {
                    true
                }
                if (!CursorUsageWidgetUpdater.takeQueuedForce()) break
                force = true
            }
            withContext(Dispatchers.Main) {
                jobFinished(params, retry)
                CursorUsageWidgetUpdater.onJobFinished(applicationContext)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        activeJob?.cancel()
        activeJob = null
        CursorUsageWidgetUpdater.onJobStopped(applicationContext)
        return true
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}

private object WidgetLoader {
    fun readCached(context: Context): WidgetSnapshot {
        return try {
            val repository = CursorRepository(context)
            repeat(3) {
                val account = selectedAccount(repository)
                if (account == null) {
                    if (selectedAccount(repository) == null) {
                        return WidgetSnapshot(null, null, "点击添加账号")
                    }
                    return@repeat
                }
                val revision = runCatching { repository.accountRevision(account.id) }.getOrNull()
                    ?: return@repeat
                val usage = repository.cachedUsage(account.id, allowInvalidCredential = true)
                val stableAccount = accountAtRevision(repository, account.id, revision)
                    ?: return@repeat
                val status = if (stableAccount.tokenExpired) {
                    "Token 已过期 · 点击更新"
                } else {
                    usage?.let(::cachedStatus) ?: "等待首次刷新"
                }
                return WidgetSnapshot(stableAccount, usage, status, revision)
            }
            WidgetSnapshot(null, null, "账号已变化 · 正在更新")
        } catch (_: Exception) {
            WidgetSnapshot(null, null, "打开应用检查账号")
        }
    }

    suspend fun load(context: Context, force: Boolean): WidgetLoadResult {
        val repository = CursorRepository(context)
        val account = try {
            selectedAccount(repository)
        } catch (_: Exception) {
            return WidgetLoadResult(WidgetSnapshot(null, null, "打开应用检查账号"))
        } ?: return WidgetLoadResult(WidgetSnapshot(null, null, "点击添加账号"))

        val revision = runCatching { repository.accountRevision(account.id) }.getOrElse {
            return WidgetLoadResult(readCached(context))
        }
        val cached = runCatching {
            repository.cachedUsage(account.id, allowInvalidCredential = true)
        }.getOrNull()
        val stableAccount = accountAtRevision(repository, account.id, revision)
            ?: return WidgetLoadResult(readCached(context))
        if (stableAccount.tokenExpired) {
            return WidgetLoadResult(WidgetSnapshot(stableAccount, cached, "Token 已过期 · 点击更新", revision))
        }
        if (!force && cached != null && cached.cacheAgeSeconds < CACHE_FRESH_SECONDS) {
            return WidgetLoadResult(WidgetSnapshot(stableAccount, cached, cachedStatus(cached), revision))
        }

        return try {
            val live = repository.fetchUsage(stableAccount.id, forceRefresh = force)
            val latestAccount = accountAtRevision(repository, stableAccount.id, revision)
                ?: return WidgetLoadResult(readCached(context))
            WidgetLoadResult(WidgetSnapshot(latestAccount, live, liveStatus(live), revision))
        } catch (error: ApiException) {
            val latestAccount = accountAtRevision(repository, stableAccount.id, revision)
                ?: return WidgetLoadResult(readCached(context))
            if (error.statusCode == 401 || error.statusCode == 403) {
                WidgetLoadResult(WidgetSnapshot(latestAccount, cached, "Token 已过期 · 点击更新", revision))
            } else {
                WidgetLoadResult(
                    WidgetSnapshot(
                        latestAccount,
                        cached,
                        if (cached != null) "缓存 · 刷新失败" else "刷新失败",
                        revision,
                    ),
                    shouldRetry = error.statusCode == 429 || error.statusCode in 500..599,
                )
            }
        } catch (_: AccountRevisionChangedException) {
            WidgetLoadResult(readCached(context))
        } catch (_: IOException) {
            val latestAccount = accountAtRevision(repository, stableAccount.id, revision)
                ?: return WidgetLoadResult(readCached(context))
            WidgetLoadResult(
                WidgetSnapshot(
                    latestAccount,
                    cached,
                    if (cached != null) "缓存 · 网络不可用" else "网络不可用",
                    revision,
                ),
                shouldRetry = true,
            )
        } catch (_: Exception) {
            val latestAccount = accountAtRevision(repository, stableAccount.id, revision)
                ?: return WidgetLoadResult(readCached(context))
            WidgetLoadResult(
                WidgetSnapshot(
                    latestAccount,
                    cached,
                    if (cached != null) "缓存 · 刷新失败" else "刷新失败",
                    revision,
                ),
            )
        }
    }

    fun isCurrent(context: Context, snapshot: WidgetSnapshot): Boolean {
        return try {
            val repository = CursorRepository(context)
            val account = snapshot.account
            if (account == null) {
                selectedAccount(repository) == null
            } else {
                val revision = snapshot.accountRevision ?: return false
                accountAtRevision(repository, account.id, revision) != null
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun selectedAccount(repository: CursorRepository): CursorAccount? {
        val accounts = repository.listAccounts()
        val selected = repository.selectedAccountId()
        return accounts.firstOrNull { it.id == selected } ?: accounts.firstOrNull()
    }

    private fun accountAtRevision(
        repository: CursorRepository,
        accountId: Int,
        revision: Long,
    ): CursorAccount? {
        val current = runCatching { selectedAccount(repository) }.getOrNull() ?: return null
        if (current.id != accountId) return null
        val currentRevision = runCatching { repository.accountRevision(accountId) }.getOrNull()
        return current.takeIf { currentRevision == revision }
    }

    private fun cachedStatus(usage: CursorUsageOverview): String = when {
        usage.partialData -> "缓存 · 部分数据"
        else -> "缓存 · ${timeLabel(usage.fetchedAt)}"
    }

    private fun liveStatus(usage: CursorUsageOverview): String = when {
        usage.partialData -> "已更新 · 部分数据"
        else -> "更新 ${timeLabel(usage.fetchedAt)}"
    }

    private fun timeLabel(value: String): String = value
        .takeIf { it.length >= 16 }
        ?.substring(11, 16)
        ?: "完成"
}
