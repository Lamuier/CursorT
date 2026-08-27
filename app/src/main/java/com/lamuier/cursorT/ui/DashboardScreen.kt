package com.lamuier.cursorT.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Input
import androidx.compose.material.icons.outlined.KeyOff
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Output
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lamuier.cursorT.model.AppUiState
import com.lamuier.cursorT.model.CursorAccount
import com.lamuier.cursorT.model.CursorTOverview
import com.lamuier.cursorT.model.DashboardTab
import com.lamuier.cursorT.model.ModelTokenUsage
import com.lamuier.cursorT.model.TokenUsageBreakdown
import com.lamuier.cursorT.model.UsageWindow
import com.lamuier.cursorT.ui.theme.LocalPulseChartColors
import com.lamuier.cursorT.util.BillingProgress
import com.lamuier.cursorT.util.UsageCalculations
import com.lamuier.cursorT.util.UsageHistoryWindows
import com.lamuier.cursorT.util.UsageLevel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import java.util.Locale
import kotlinx.coroutines.delay
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.launch

/** 周期倒计时/百分比的本地走动间隔（仅重算时间，不触发网络请求）。 */
private const val BILLING_TICK_MS = 5_000L

private val DashboardTab.icon: ImageVector
    get() = when (this) {
        DashboardTab.Overview -> Icons.Outlined.SpaceDashboard
        DashboardTab.Usage -> Icons.Outlined.DataUsage
        DashboardTab.Billing -> Icons.AutoMirrored.Outlined.ReceiptLong
        DashboardTab.Tasks -> Icons.Outlined.SmartToy
        DashboardTab.Status -> Icons.Outlined.HealthAndSafety
    }

@Immutable
private data class ChartSegment(
    val label: String,
    val value: Double,
    val color: Color,
)

@Immutable
private data class MetricTile(
    val label: String,
    val value: String,
    val icon: ImageVector,
    val accent: Color,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DashboardScreen(
    state: AppUiState,
    snackbarHostState: SnackbarHostState,
    tabOrder: List<DashboardTab>,
    onLoadHistoryMonth: (String) -> Unit,
    onRefresh: () -> Unit,
    onManageAccount: () -> Unit,
    onShowSettings: () -> Unit,
    onShowTokenHelp: () -> Unit,
) {
    val uiScope = rememberCoroutineScope()
    val account = remember(state.accounts, state.selectedAccountId) {
        state.accounts.firstOrNull { it.id == state.selectedAccountId }
            ?: state.accounts.firstOrNull()
    }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "Cursor助手",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        AccountStatusRow(account = account, loading = state.loadingAccounts)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(
                        modifier = Modifier.semantics {
                            val busy = state.refreshing || state.refreshingStatus || state.refreshingTasks
                            contentDescription = if (busy) "正在刷新数据" else "刷新数据"
                            if (busy) liveRegion = LiveRegionMode.Polite
                        },
                        enabled = !state.loadingAccounts && !state.refreshing &&
                            !state.refreshingStatus && !state.refreshingTasks && !state.submitting,
                        onClick = onRefresh,
                    ) {
                        if (state.refreshing || state.refreshingStatus || state.refreshingTasks) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(Icons.Outlined.Refresh, contentDescription = null)
                        }
                    }
                    IconButton(
                        enabled = !state.loadingAccounts && !state.submitting,
                        onClick = onShowSettings,
                    ) {
                        Icon(Icons.Outlined.Settings, contentDescription = "设置")
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(contentPadding),
        ) {
            val contentKey = if (state.loadingAccounts) "loading_accounts" else "dashboard"
            AnimatedContent(
                targetState = contentKey,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = {
                    (
                        fadeIn(tween(240, easing = FastOutSlowInEasing)) +
                            scaleIn(tween(280, easing = FastOutSlowInEasing), initialScale = 0.97f)
                        ).togetherWith(
                        fadeOut(tween(140)) + scaleOut(tween(160), targetScale = 1.01f),
                    )
                },
                label = "dashboard content",
            ) { key ->
                if (key == "loading_accounts") {
                    DashboardState(
                        icon = null,
                        title = "正在读取本机账号",
                        description = "请稍候…",
                        loading = true,
                    )
                } else {
                    val tabs = remember(tabOrder) { DashboardTab.resolveOrder(tabOrder.map { it.id }) }
                    val pagerState = rememberPagerState(
                        pageCount = { tabs.size },
                    )
                    val pagerScope = rememberCoroutineScope()
                    var selectedTab by remember { mutableStateOf(tabs.first()) }
                    LaunchedEffect(pagerState.settledPage, tabs) {
                        tabs.getOrNull(pagerState.settledPage)?.let { selectedTab = it }
                    }
                    LaunchedEffect(tabs) {
                        val index = tabs.indexOf(selectedTab).takeIf { it >= 0 } ?: 0
                        if (pagerState.currentPage != index) {
                            pagerState.scrollToPage(index)
                        }
                    }
                    val selectedIndex = tabs.indexOf(selectedTab).takeIf { it >= 0 } ?: 0
                    val refreshing = state.refreshing || state.refreshingStatus || state.refreshingTasks

                    Column(modifier = Modifier.fillMaxSize()) {
                        DashboardTabPills(
                            tabs = tabs,
                            selectedIndex = selectedIndex,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            onSelect = { index ->
                                tabs.getOrNull(index)?.let { selectedTab = it }
                                pagerScope.launch { pagerState.animateScrollToPage(index) }
                            },
                        )
                        PullToRefreshBox(
                            isRefreshing = refreshing,
                            onRefresh = {
                                if (!state.submitting && !refreshing) onRefresh()
                            },
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                beyondViewportPageCount = 1,
                            ) { page ->
                                when (tabs.getOrNull(page) ?: DashboardTab.Overview) {
                                    DashboardTab.Overview -> UsageDependentTab(
                                        state = state,
                                        onRefresh = onRefresh,
                                        onManageAccount = onManageAccount,
                                        onShowTokenHelp = onShowTokenHelp,
                                    ) { OverviewTab(it) }
                                    DashboardTab.Usage -> UsageDependentTab(
                                        state = state,
                                        onRefresh = onRefresh,
                                        onManageAccount = onManageAccount,
                                        onShowTokenHelp = onShowTokenHelp,
                                    ) {
                                        UsageTab(
                                            usage = it,
                                            extraMonths = state.extraMonthHistory,
                                            loadingMonthKey = state.loadingHistoryMonth,
                                            onLoadHistoryMonth = onLoadHistoryMonth,
                                        )
                                    }
                                    DashboardTab.Billing -> UsageDependentTab(
                                        state = state,
                                        onRefresh = onRefresh,
                                        onManageAccount = onManageAccount,
                                        onShowTokenHelp = onShowTokenHelp,
                                    ) { BillingTab(it) }
                                    DashboardTab.Tasks -> AccountOnlyTab(
                                        state = state,
                                        onManageAccount = onManageAccount,
                                        onShowTokenHelp = onShowTokenHelp,
                                    ) {
                                        TasksTab(
                                            tasks = state.tasks,
                                            loading = state.loadingTasks,
                                            refreshing = state.refreshingTasks,
                                            error = state.tasksError,
                                            onRetry = onRefresh,
                                            onOpenFailed = { message ->
                                                uiScope.launch {
                                                    snackbarHostState.showSnackbar(message)
                                                }
                                            },
                                        )
                                    }
                                    DashboardTab.Status -> StatusTab(
                                        status = state.serviceStatus,
                                        loading = state.loadingStatus,
                                        refreshing = state.refreshingStatus,
                                        error = state.statusError,
                                        onRetry = onRefresh,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardTabPills(
    tabs: List<DashboardTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val compactTabs = LocalConfiguration.current.screenWidthDp < 380 ||
        LocalDensity.current.fontScale > 1.12f
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            tabs.forEachIndexed { index, tab ->
                val active = index == selectedIndex
                val container by animateColorAsState(
                    targetValue = if (active) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        Color.Transparent
                    },
                    animationSpec = tween(240, easing = FastOutSlowInEasing),
                    label = "${tab.label} container",
                )
                val content by animateColorAsState(
                    targetValue = if (active) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(240, easing = FastOutSlowInEasing),
                    label = "${tab.label} content",
                )
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .semantics {
                            role = Role.Tab
                            selected = active
                        }
                        .clip(CircleShape)
                        .clickable { onSelect(index) },
                    shape = CircleShape,
                    color = container,
                    contentColor = content,
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 9.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (!compactTabs) {
                            Icon(tab.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            tab.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UsageDependentTab(
    state: AppUiState,
    onRefresh: () -> Unit,
    onManageAccount: () -> Unit,
    onShowTokenHelp: () -> Unit,
    content: @Composable (CursorTOverview) -> Unit,
) {
    val account = state.accounts.firstOrNull { it.id == state.selectedAccountId }
        ?: state.accounts.firstOrNull()
    when {
        account == null -> DashboardState(
            icon = Icons.Outlined.PersonAdd,
            title = "添加 Cursor 账号",
            description = "录入 Access Token 后即可在本机查看套餐与用量。也可先查看「状态」页。",
            primaryActionLabel = "添加账号",
            onPrimaryAction = onManageAccount,
            secondaryActionLabel = "如何获取 Token",
            onSecondaryAction = onShowTokenHelp,
        )
        account.tokenExpired -> DashboardState(
            icon = Icons.Outlined.KeyOff,
            title = "Access Token 已过期",
            description = "更新 Token 后即可继续获取 Cursor 用量。",
            primaryActionLabel = "更新 Token",
            onPrimaryAction = onManageAccount,
            secondaryActionLabel = "Token 帮助",
            onSecondaryAction = onShowTokenHelp,
        )
        state.loadingUsage && state.usage == null -> DashboardState(
            icon = null,
            title = "正在获取 Cursor 用量",
            description = "正在连接 Cursor 官方接口…",
            loading = true,
        )
        state.usage == null -> DashboardState(
            icon = Icons.Outlined.CloudOff,
            title = "暂时无法显示用量",
            description = "请检查网络后重试；若持续失败，可重新录入 Token。",
            primaryActionLabel = "重新加载",
            onPrimaryAction = onRefresh,
            secondaryActionLabel = "管理账号",
            onSecondaryAction = onManageAccount,
        )
        else -> content(state.usage!!)
    }
}

@Composable
private fun AccountOnlyTab(
    state: AppUiState,
    onManageAccount: () -> Unit,
    onShowTokenHelp: () -> Unit,
    content: @Composable () -> Unit,
) {
    val account = state.accounts.firstOrNull { it.id == state.selectedAccountId }
        ?: state.accounts.firstOrNull()
    when {
        account == null -> DashboardState(
            icon = Icons.Outlined.PersonAdd,
            title = "添加 Cursor 账号",
            description = "录入 Access Token 后即可查看云端任务。也可先查看「状态」页。",
            primaryActionLabel = "添加账号",
            onPrimaryAction = onManageAccount,
            secondaryActionLabel = "如何获取 Token",
            onSecondaryAction = onShowTokenHelp,
        )
        account.tokenExpired -> DashboardState(
            icon = Icons.Outlined.KeyOff,
            title = "Access Token 已过期",
            description = "更新 Token 后即可继续获取云端任务。",
            primaryActionLabel = "更新 Token",
            onPrimaryAction = onManageAccount,
            secondaryActionLabel = "Token 帮助",
            onSecondaryAction = onShowTokenHelp,
        )
        else -> content()
    }
}

@Composable
private fun AccountStatusRow(account: CursorAccount?, loading: Boolean) {
    when {
        loading -> Text(
            "正在加载账号",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        account == null -> Text(
            "尚未添加账号",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        account.tokenExpired -> Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                account.alias,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            StatusChip(label = "Token 过期", error = true)
        }
        else -> Text(
            account.alias,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun StatusChip(label: String, error: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = if (error) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        contentColor = if (error) {
            MaterialTheme.colorScheme.onErrorContainer
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        },
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
internal fun DashboardState(
    icon: ImageVector?,
    title: String,
    description: String,
    loading: Boolean = false,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when {
                loading -> CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp,
                )
                icon != null -> {
                    val badgeBrush = Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f),
                        ),
                    )
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(badgeBrush),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            icon,
                            contentDescription = null,
                            modifier = Modifier.size(30.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
                else -> Icon(
                    Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (primaryActionLabel != null && onPrimaryAction != null) {
                Spacer(Modifier.height(4.dp))
                Button(
                    onClick = onPrimaryAction,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(primaryActionLabel)
                }
            }
            if (secondaryActionLabel != null && onSecondaryAction != null) {
                TextButton(onClick = onSecondaryAction) {
                    Text(secondaryActionLabel)
                }
            }
        }
    }
}

@Composable
private fun OverviewTab(usage: CursorTOverview) {
    AdaptiveTabContent { compact ->
        val chartColors = LocalPulseChartColors.current
        val chartSize = if (compact) 150.dp else 178.dp
        val limit = remember(usage) { effectiveLimit(usage) }
        val percent = remember(usage, limit) {
            if (usage.isTeam && limit > 0.0) {
                usage.usage.includedSpendDollars.safeNonNegative() / limit * 100.0
            } else {
                UsageCalculations.usagePercent(usage)
            }
        }
        val level = remember(percent) { UsageCalculations.level(percent) }
        val percentKnown = !usage.isTeam || limit > 0.0
        val nowMillis = rememberNowMillis()
        val billing = remember(usage, nowMillis) {
            UsageCalculations.billingProgress(usage.billingCycle.start, usage.billingCycle.end, nowMillis)
        }
        val quotaTiles = remember(usage, limit, chartColors) {
            val pools = UsageCalculations.poolSpend(usage.tokenUsage)
            listOf(
                MetricTile(
                    label = "套餐额度",
                    value = limit.takeIf { it > 0.0 }?.let(::money) ?: "—",
                    icon = Icons.Outlined.Savings,
                    accent = chartColors.chart1,
                ),
                MetricTile(
                    label = "自有池消费",
                    value = pools?.let { money(it.ownPoolDollars) } ?: "—",
                    icon = Icons.Outlined.Layers,
                    accent = chartColors.chart2,
                ),
                MetricTile(
                    label = "三方池费用",
                    value = pools?.let { money(it.thirdPartyDollars) } ?: "—",
                    icon = Icons.Outlined.Hub,
                    accent = chartColors.chart3,
                ),
            )
        }
        val tokenTiles = remember(usage, chartColors) {
            val tokenUsage = usage.tokenUsage
            listOf(
                MetricTile(
                    label = "输入 Token",
                    value = tokenUsage
                        ?.let { UsageCalculations.formatTokens(it.totalInputTokens) }
                        ?: "—",
                    icon = Icons.Outlined.Input,
                    accent = chartColors.chart2,
                ),
                MetricTile(
                    label = "输出 Token",
                    value = tokenUsage
                        ?.let { UsageCalculations.formatTokens(it.totalOutputTokens) }
                        ?: "—",
                    icon = Icons.Outlined.Output,
                    accent = chartColors.chart3,
                ),
                MetricTile(
                    label = "缓存 Token",
                    value = tokenUsage
                        ?.let {
                            UsageCalculations.formatTokens(
                                it.totalCacheWriteTokens + it.totalCacheReadTokens,
                            )
                        }
                        ?: "—",
                    icon = Icons.Outlined.Cached,
                    accent = chartColors.healthy,
                ),
            )
        }
        val heroBrush = Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.92f),
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f),
            ),
        )
        val usageColor = when (level) {
            UsageLevel.Healthy -> chartColors.healthy
            UsageLevel.Warning -> chartColors.warning
            UsageLevel.Critical -> chartColors.critical
            UsageLevel.Exhausted -> chartColors.critical
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shadowElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier
                    .background(heroBrush)
                    .padding(if (compact) 16.dp else 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            "本周期",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            usage.plan.name ?: "Cursor 套餐",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    usage.plan.price?.takeIf { it.isNotBlank() }?.let { price ->
                        StatusChip(label = price)
                    }
                }
                UsageRing(
                    percent = percent,
                    size = chartSize,
                    progressColor = usageColor,
                    centerValue = if (usage.isTeam) money(usage.usage.totalUsed) else formatPercent(percent),
                    caption = if (percentKnown) level.label() else "额度未知",
                    description = buildString {
                        if (usage.isTeam) {
                            append("团队总消费 ${money(usage.usage.totalUsed)}，")
                            append(
                                if (percentKnown) {
                                    "套餐额度使用 ${formatPercent(percent)}"
                                } else {
                                    "套餐额度未知"
                                },
                            )
                        } else {
                            append("总用量 ${formatPercent(percent)}，${level.label()}")
                        }
                        billing?.let {
                            append("，计费周期剩余 ${UsageCalculations.formatRemaining(it.remainingMillis)}")
                        }
                    },
                    showProgress = percentKnown,
                    cyclePercent = billing?.percent,
                    cycleColor = MaterialTheme.colorScheme.primary,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DotLabel(
                        color = usageColor,
                        text = if (percentKnown) "用量 ${formatPercent(percent)}" else "用量 —",
                    )
                    DotLabel(
                        color = MaterialTheme.colorScheme.primary,
                        text = billing?.let { "周期 ${formatPercent(it.percent.toDouble())}" } ?: "周期 —",
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                OverviewCycleRow(billing = billing, planCycleEnd = usage.plan.billingCycleEnd)
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
        ) {
            MetricRowCard(tiles = quotaTiles, compact = compact)
            MetricRowCard(tiles = tokenTiles, compact = compact)
        }
        usage.grokBot?.let { grok ->
            val grokProgress = remember(grok, nowMillis) {
                UsageCalculations.billingProgress(grok.periodStart, grok.resetsAt, nowMillis)
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shadowElevation = 1.dp,
            ) {
                Column(
                    modifier = Modifier.padding(if (compact) 14.dp else 18.dp),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
                ) {
                    HorizontalUsageChart(
                        label = "Grok Bot 本周",
                        percent = grok.percentUsed,
                        color = chartColors.chart3,
                        caption = grokProgress?.let {
                            "每周独立额度，${UsageCalculations.formatRemaining(it.remainingMillis)} 后重置"
                        } ?: "每周独立额度，不计入月度用量池",
                    )
                }
            }
        }
        FreshnessRow(usage)
        if (usage.partialData) {
            Text(
                "当前仅返回部分数据",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun OverviewCycleRow(billing: BillingProgress?, planCycleEnd: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            Icons.Outlined.CalendarMonth,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                "计费周期",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                when {
                    billing != null -> "${billing.startLabel} — ${billing.endLabel}"
                    !planCycleEnd.isNullOrBlank() -> "结束于 ${planCycleEnd.take(10)}"
                    else -> "暂未返回周期信息"
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        billing?.let {
            StatusChip(label = "剩余 ${UsageCalculations.formatRemaining(it.remainingMillis)}")
        }
    }
}

@Composable
private fun DotLabel(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FreshnessRow(usage: CursorTOverview) {
    val stamp = usage.fetchedAt.takeIf { it.length >= 16 }?.substring(11, 16)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            if (usage.fromCache) Icons.Outlined.Cached else Icons.Outlined.CloudDone,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            buildString {
                append(if (usage.fromCache) "缓存数据" else "已更新")
                stamp?.let { append(" · $it") }
                append(" · 下拉可刷新")
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private enum class HistoryQueryMode { CalendarMonth, PreviousCycle }

@Composable
private fun UsageTab(
    usage: CursorTOverview,
    extraMonths: Map<String, UsageWindow>,
    loadingMonthKey: String?,
    onLoadHistoryMonth: (String) -> Unit,
) {
    AdaptiveTabContent { compact ->
        val chartColors = LocalPulseChartColors.current
        val segments = remember(usage, chartColors) {
            listOf(
                ChartSegment("包含用量", usage.usage.includedSpendDollars.safeNonNegative(), chartColors.chart1),
                ChartSegment("Bonus", usage.usage.bonusSpendDollars.safeNonNegative(), chartColors.chart2),
                ChartSegment("剩余额度", usage.usage.remainingDollars.safeNonNegative(), chartColors.chart3),
            )
        }

        SectionHeading(
            icon = Icons.Outlined.Layers,
            title = "用量池",
            supporting = "两个独立用量池，均在月度计费周期重置",
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shadowElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(if (compact) 14.dp else 18.dp),
                verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
            ) {
                HorizontalUsageChart(
                    label = "Cursor 模型",
                    percent = usage.usage.autoPercentUsed,
                    color = chartColors.chart1,
                    caption = "Cursor Grok 4.5 与 Composer 2.5，包含用量显著更多",
                )
                HorizontalUsageChart(
                    label = "其他模型",
                    percent = usage.usage.apiPercentUsed,
                    color = chartColors.chart2,
                    caption = "第三方模型，按模型的 API 价格计费",
                )
            }
        }

        usage.grokBot?.let { grok ->
            val nowMillis = rememberNowMillis()
            val grokProgress = remember(grok, nowMillis) {
                UsageCalculations.billingProgress(grok.periodStart, grok.resetsAt, nowMillis)
            }
            SectionHeading(
                icon = Icons.Outlined.SmartToy,
                title = "Grok Bot",
                supporting = grokProgress?.let {
                    "每周独立额度，${UsageCalculations.formatRemaining(it.remainingMillis)} 后重置"
                } ?: "每周独立额度，不计入上方月度用量池",
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shadowElevation = 1.dp,
            ) {
                Column(
                    modifier = Modifier.padding(if (compact) 14.dp else 18.dp),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
                ) {
                    HorizontalUsageChart(
                        label = "本周用量",
                        percent = grok.percentUsed,
                        color = chartColors.chart3,
                        caption = "超额后走已有的 On-demand 用量",
                    )
                }
            }
        }

        SectionHeading(
            icon = Icons.Outlined.PieChart,
            title = "额度构成",
            supporting = "包含用量、Bonus 与剩余额度",
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shadowElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(if (compact) 14.dp else 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StackedBar(
                    segments = segments,
                    description = buildSegmentDescription("额度构成", segments),
                    height = if (compact) 14.dp else 16.dp,
                )
                SegmentLegend(segments)
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "总消费",
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AnimatedValueText(
                        value = money(usage.usage.totalSpendDollars),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        TokenUsageSection(usage = usage, compact = compact)
        HistoryUsageSection(
            usage = usage,
            extraMonths = extraMonths,
            loadingMonthKey = loadingMonthKey,
            onLoadHistoryMonth = onLoadHistoryMonth,
            compact = compact,
        )
    }
}

@Composable
private fun TokenUsageSection(usage: CursorTOverview, compact: Boolean) {
    val tokenUsage = usage.tokenUsage
    SectionHeading(
        icon = Icons.AutoMirrored.Outlined.TrendingUp,
        title = "Token 用量",
        supporting = "本计费周期按模型汇总的输入 / 输出 Token",
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 14.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 12.dp),
        ) {
            when {
                tokenUsage == null -> {
                    Text(
                        text = "Token 明细暂时不可用，花费与用量百分比仍可正常查看。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                tokenUsage.models.isEmpty() -> {
                    Text(
                        text = "本周期暂无按模型 Token 记录。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    tokenUsage.models.forEachIndexed { index, model ->
                        ModelTokenRow(model = model, compact = compact)
                        if (index < tokenUsage.models.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryUsageSection(
    usage: CursorTOverview,
    extraMonths: Map<String, UsageWindow>,
    loadingMonthKey: String?,
    onLoadHistoryMonth: (String) -> Unit,
    compact: Boolean,
) {
    val zone = remember { ZoneId.systemDefault() }
    val nowMonth = remember { YearMonth.now(zone) }
    var mode by remember { mutableStateOf(HistoryQueryMode.CalendarMonth) }
    var selectedMonth by remember { mutableStateOf(nowMonth) }
    val selectedKey = UsageHistoryWindows.yearMonthKey(selectedMonth)
    LaunchedEffect(selectedKey, mode, usage.accountId) {
        if (mode != HistoryQueryMode.CalendarMonth) return@LaunchedEffect
        val prefetched = usage.history?.calendarMonth?.yearMonth
        if (selectedKey != prefetched && extraMonths[selectedKey] == null) {
            onLoadHistoryMonth(selectedKey)
        }
    }
    val window = when (mode) {
        HistoryQueryMode.PreviousCycle -> usage.history?.previousCycle
        HistoryQueryMode.CalendarMonth -> {
            if (usage.history?.calendarMonth?.yearMonth == selectedKey) {
                usage.history?.calendarMonth
            } else {
                extraMonths[selectedKey]
            }
        }
    }
    val loading = mode == HistoryQueryMode.CalendarMonth &&
        loadingMonthKey == selectedKey &&
        window == null
    val earliest = nowMonth.minusMonths(12)
    SectionHeading(
        icon = Icons.Outlined.History,
        title = "历史用量",
        supporting = "按模型 Token 汇总，不含套餐百分比",
    )
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 14.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 12.dp),
        ) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(
                    HistoryQueryMode.CalendarMonth to "自然月",
                    HistoryQueryMode.PreviousCycle to "上个计费周期",
                )
                options.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = mode == value,
                        onClick = { mode = value },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    ) {
                        Text(label)
                    }
                }
            }
            if (mode == HistoryQueryMode.CalendarMonth) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { selectedMonth = selectedMonth.minusMonths(1) },
                        enabled = selectedMonth.isAfter(earliest),
                    ) {
                        Icon(Icons.Outlined.ChevronLeft, contentDescription = "上一个月")
                    }
                    Text(
                        "${selectedMonth.year}年${selectedMonth.monthValue}月",
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(
                        onClick = { selectedMonth = selectedMonth.plusMonths(1) },
                        enabled = selectedMonth.isBefore(nowMonth),
                    ) {
                        Icon(Icons.Outlined.ChevronRight, contentDescription = "下一个月")
                    }
                }
            } else {
                Text(
                    text = listOfNotNull(window?.start, window?.end).joinToString(" → ")
                        .ifBlank { "上一个计费周期" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            when {
                loading -> {
                    Text(
                        "正在加载该窗口的 Token 汇总…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                window?.tokenUsage == null -> {
                    Text(
                        "该窗口暂无 Token 明细。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                window.tokenUsage!!.models.isEmpty() -> {
                    Text(
                        "该窗口暂无按模型 Token 记录。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    val tokenUsage = window.tokenUsage!!
                    Text(
                        buildString {
                            append("入 ${UsageCalculations.formatTokens(tokenUsage.totalInputTokens)}")
                            append(" · 出 ${UsageCalculations.formatTokens(tokenUsage.totalOutputTokens)}")
                            val cached = tokenUsage.totalCacheWriteTokens + tokenUsage.totalCacheReadTokens
                            if (cached > 0L) {
                                append(" · 缓存 ${UsageCalculations.formatTokens(cached)}")
                            }
                            append(" · ${money(tokenUsage.totalCostDollars)}")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    tokenUsage.models.forEachIndexed { index, model ->
                        ModelTokenRow(model = model, compact = compact)
                        if (index < tokenUsage.models.lastIndex) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelTokenRow(
    model: ModelTokenUsage,
    compact: Boolean,
) {
    val cached = model.cacheWriteTokens + model.cacheReadTokens
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append(model.modelIntent)
                    append("，输入 ${model.inputTokens}，输出 ${model.outputTokens}")
                    if (cached > 0L) append("，缓存 $cached")
                    append("，费用 ${money(model.costDollars)}")
                }
            },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = model.modelIntent,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = money(model.costDollars),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = buildString {
                append("入 ${UsageCalculations.formatTokens(model.inputTokens)}")
                append(" · 出 ${UsageCalculations.formatTokens(model.outputTokens)}")
                if (cached > 0L) {
                    append(" · 缓存 ${UsageCalculations.formatTokens(cached)}")
                }
            },
            style = if (compact) {
                MaterialTheme.typography.bodySmall
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BillingTab(usage: CursorTOverview) {
    AdaptiveTabContent { compact ->
        val chartColors = LocalPulseChartColors.current
        val segments = remember(usage, chartColors) {
            val unclassified = (
                usage.credits.totalDollars -
                    usage.credits.grantTotalDollars -
                    usage.credits.stripeBalanceDollars
                ).coerceAtLeast(0.0)
            listOf(
                ChartSegment("Grant", usage.credits.grantTotalDollars, chartColors.chart1),
                ChartSegment("Stripe", usage.credits.stripeBalanceDollars, chartColors.chart2),
                ChartSegment("其他", unclassified, chartColors.chart3),
            )
        }
        val visibleSegments = remember(segments, usage.partialData) {
            if (usage.partialData) {
                segments.filter { it.value.safeNonNegative() > 0.0 }
            } else {
                segments
            }
        }

        BillingCycleChart(usage, compact)

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shadowElevation = 1.dp,
        ) {
            Column(
                modifier = Modifier.padding(if (compact) 14.dp else 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SectionHeading(
                    icon = Icons.Outlined.CardGiftcard,
                    title = "Credits 构成",
                    supporting = "Grant 与 Stripe 余额",
                )
                if (usage.partialData) {
                    Text(
                        "部分来源未返回，以下仅展示已获取数据",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (visibleSegments.isEmpty()) {
                    Text("Credits 数据暂不可用", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    StackedBar(
                        segments = visibleSegments,
                        description = buildSegmentDescription(
                            if (usage.partialData) "已返回 Credits" else "Credits 构成",
                            visibleSegments,
                        ),
                        height = if (compact) 14.dp else 16.dp,
                    )
                    SegmentLegend(visibleSegments)
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (usage.partialData) "已返回合计" else "Credits 合计",
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AnimatedValueText(
                        value = if (usage.partialData) {
                            visibleSegments.sumOf { it.value.safeNonNegative() }.takeIf { it > 0.0 }
                                ?.let(::money) ?: "—"
                        } else {
                            money(usage.credits.totalDollars)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        BillingStatusCard(usage, compact)
    }
}

@Composable
internal fun AdaptiveTabContent(content: @Composable ColumnScope.(compact: Boolean) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val fontScale = LocalDensity.current.fontScale
        val compact = maxHeight < 620.dp || maxWidth < 360.dp || fontScale > 1.15f
        val scrollState = rememberScrollState()
        var entered by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { entered = true }
        val enterAlpha by animateFloatAsState(
            targetValue = if (entered) 1f else 0f,
            animationSpec = tween(320, easing = FastOutSlowInEasing),
            label = "tab enter alpha",
        )
        val enterOffset by animateFloatAsState(
            targetValue = if (entered) 0f else 18f,
            animationSpec = tween(360, easing = FastOutSlowInEasing),
            label = "tab enter offset",
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = enterAlpha
                    translationY = enterOffset
                }
                .verticalScroll(scrollState)
                .padding(
                    horizontal = if (compact) 12.dp else 16.dp,
                    vertical = if (compact) 10.dp else 14.dp,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compact) 12.dp else 16.dp),
        ) {
            content(compact)
        }
    }
}

@Composable
internal fun SectionHeading(
    icon: ImageVector,
    title: String,
    supporting: String? = null,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                title,
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            supporting?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
private fun AnimatedValueText(
    value: String,
    style: TextStyle,
    fontWeight: FontWeight? = null,
) {
    AnimatedContent(
        targetState = value,
        transitionSpec = {
            (
                fadeIn(tween(180)) +
                    slideInVertically(tween(220, easing = FastOutSlowInEasing)) { it / 4 }
                ).togetherWith(
                fadeOut(tween(100)) + slideOutVertically(tween(140)) { -it / 5 },
            )
        },
        label = "metric value",
    ) { text ->
        Text(
            text,
            style = style,
            fontWeight = fontWeight,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun UsageRing(
    percent: Double,
    size: Dp,
    progressColor: Color,
    centerValue: String,
    caption: String,
    description: String,
    showProgress: Boolean,
    cyclePercent: Float?,
    cycleColor: Color,
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val targetSweep = if (showProgress) percent.visualPercent().toFloat() / 100f * 360f else 0f
    val sweep by animateFloatAsState(
        targetValue = targetSweep,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "usage ring",
    )
    val cycleSweep by animateFloatAsState(
        targetValue = (cyclePercent ?: 0f).coerceIn(0f, 100f) / 100f * 360f,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "cycle ring",
    )
    val progressBrush = Brush.linearGradient(
        listOf(progressColor.copy(alpha = 0.6f), progressColor),
    )
    Box(
        modifier = Modifier
            .size(size)
            .clearAndSetSemantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val outerStroke = size.toPx() * 0.09f
            val innerStroke = size.toPx() * 0.04f
            val outerInset = outerStroke / 2f
            val outerArc = Size(this.size.width - outerStroke, this.size.height - outerStroke)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(outerInset, outerInset),
                size = outerArc,
                style = Stroke(outerStroke, cap = StrokeCap.Round),
            )
            if (sweep > 0f) {
                drawArc(
                    color = progressColor,
                    alpha = 0.18f,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(outerInset, outerInset),
                    size = outerArc,
                    style = Stroke(outerStroke * 1.85f, cap = StrokeCap.Round),
                )
                drawArc(
                    brush = progressBrush,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(outerInset, outerInset),
                    size = outerArc,
                    style = Stroke(outerStroke, cap = StrokeCap.Round),
                )
            }
            val innerInset = outerStroke + innerStroke + size.toPx() * 0.035f
            val innerArc = Size(
                this.size.width - innerInset * 2f,
                this.size.height - innerInset * 2f,
            )
            drawArc(
                color = trackColor.copy(alpha = 0.55f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(innerInset, innerInset),
                size = innerArc,
                style = Stroke(innerStroke, cap = StrokeCap.Round),
            )
            if (cycleSweep > 0f) {
                drawArc(
                    color = cycleColor,
                    startAngle = -90f,
                    sweepAngle = cycleSweep,
                    useCenter = false,
                    topLeft = Offset(innerInset, innerInset),
                    size = innerArc,
                    style = Stroke(innerStroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            AnimatedValueText(
                value = centerValue,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                caption,
                style = MaterialTheme.typography.labelMedium,
                color = progressColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun MetricRowCard(tiles: List<MetricTile>, compact: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(if (compact) 12.dp else 14.dp),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 12.dp),
        ) {
            tiles.forEach { tile ->
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(tile.accent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                tile.icon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = tile.accent,
                            )
                        }
                        Text(
                            tile.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    AnimatedValueText(
                        value = tile.value,
                        style = if (compact) {
                            MaterialTheme.typography.titleMedium
                        } else {
                            MaterialTheme.typography.titleLarge
                        },
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun HorizontalUsageChart(
    label: String,
    percent: Double?,
    color: Color,
    caption: String? = null,
) {
    val value = percent?.safeNonNegative()
    val animatedProgress by animateFloatAsState(
        targetValue = value?.toProgress() ?: 0f,
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "$label progress",
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
            AnimatedValueText(
                value = value?.let(::formatPercent) ?: "—",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
        caption?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        ProgressTrack(
            progress = animatedProgress,
            color = color,
            height = 12.dp,
            trackColor = trackColor,
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = "$label 用量池 ${value?.let(::formatPercent) ?: "暂无数据"}"
            },
        )
    }
}

@Composable
private fun ProgressTrack(
    progress: Float,
    color: Color,
    height: Dp,
    modifier: Modifier = Modifier,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant,
) {
    val fill = Brush.horizontalGradient(listOf(color.copy(alpha = 0.62f), color))
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
    ) {
        val radius = CornerRadius(size.height / 2f, size.height / 2f)
        drawRoundRect(color = trackColor, cornerRadius = radius)
        val width = size.width * progress.coerceIn(0f, 1f)
        if (width > 0f) {
            drawRoundRect(
                brush = fill,
                size = Size(width, size.height),
                cornerRadius = radius,
            )
        }
    }
}

@Composable
private fun StackedBar(segments: List<ChartSegment>, description: String, height: Dp) {
    val positiveSegments = segments.map { it.copy(value = it.value.safeNonNegative()) }
    val total = positiveSegments.sumOf { it.value }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(positiveSegments) {
        reveal.snapTo(0f)
        reveal.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
    }
    val gapPx = with(LocalDensity.current) { 2.dp.toPx() }
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(999.dp))
            .background(trackColor)
            .semantics { contentDescription = description },
    ) {
        if (total <= 0.0) return@Canvas
        val active = positiveSegments.filter { it.value > 0.0 }
        val gaps = ((active.size - 1).coerceAtLeast(0) * gapPx)
        val usable = (size.width - gaps).coerceAtLeast(0f)
        var startX = 0f
        active.forEachIndexed { index, segment ->
            val segmentWidth = (segment.value / total * usable * reveal.value).toFloat()
            drawRoundRect(
                color = segment.color,
                topLeft = Offset(startX, 0f),
                size = Size(segmentWidth, size.height),
                cornerRadius = CornerRadius(size.height / 2f, size.height / 2f),
            )
            startX += segmentWidth
            if (index < active.lastIndex) startX += gapPx
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SegmentLegend(segments: List<ChartSegment>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        segments.forEach { segment ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(segment.color),
                )
                Text(segment.label, style = MaterialTheme.typography.bodySmall)
                AnimatedValueText(
                    value = money(segment.value),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

/**
 * 页面可见期间每 [BILLING_TICK_MS] 返回一次最新时间，驱动周期百分比/倒计时本地走动；
 * 后台自动暂停，不触发网络请求。
 */
@Composable
private fun rememberNowMillis(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(BILLING_TICK_MS)
                now = System.currentTimeMillis()
            }
        }
    }
    return now
}

@Composable
private fun BillingCycleChart(usage: CursorTOverview, compact: Boolean) {
    val nowMillis = rememberNowMillis()
    val billing = UsageCalculations.billingProgress(usage.billingCycle.start, usage.billingCycle.end, nowMillis)
    val animatedProgress by animateFloatAsState(
        targetValue = billing?.let { (it.percent / 100f).coerceIn(0f, 1f) } ?: 0f,
        animationSpec = tween(560, easing = FastOutSlowInEasing),
        label = "billing cycle",
    )
    val progressColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 14.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionHeading(icon = Icons.Outlined.CalendarMonth, title = "计费周期")
            if (billing == null) {
                Text(
                    usage.plan.billingCycleEnd?.let { "周期结束：$it" } ?: "暂未返回周期信息",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${billing.startLabel} — ${billing.endLabel}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "剩余 ${UsageCalculations.formatRemaining(billing.remainingMillis)}",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                    )
                }
                ProgressTrack(
                    progress = animatedProgress,
                    color = progressColor,
                    height = if (compact) 10.dp else 12.dp,
                    trackColor = trackColor,
                    modifier = Modifier.clearAndSetSemantics {
                        contentDescription =
                            "计费周期已进行 ${formatPercent(billing.percent.toDouble())}，" +
                                "剩余 ${UsageCalculations.formatRemaining(billing.remainingMillis)}"
                    },
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "已过 ${billing.elapsedDays} / ${billing.totalDays} 天",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        formatPercent(billing.percent.toDouble()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun BillingStatusCard(usage: CursorTOverview, compact: Boolean) {
    val onDemand = usage.onDemand
    val membership = usage.subscription.membershipType?.takeIf { it.isNotBlank() }
    val status = usage.subscription.status?.takeIf { it.isNotBlank() }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 14.dp else 18.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
        ) {
            SectionHeading(icon = Icons.AutoMirrored.Outlined.ReceiptLong, title = "账单状态")
            if (onDemand != null) {
                BillingRow("On-demand", money(onDemand.totalSpendDollars))
                if (onDemand.individualLimitDollars > 0.0) {
                    BudgetProgress(
                        label = "个人额度",
                        used = onDemand.individualUsedDollars,
                        limit = onDemand.individualLimitDollars,
                    )
                }
                if (onDemand.pooledLimitDollars > 0.0) {
                    BudgetProgress(
                        label = "共享额度",
                        used = onDemand.pooledUsedDollars,
                        limit = onDemand.pooledLimitDollars,
                    )
                }
            }
            if (membership != null || status != null) {
                BillingRow("订阅", listOfNotNull(membership, status).joinToString(" · "))
            }
            if (onDemand == null && membership == null && status == null) {
                Text("暂未返回额外账单信息", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BudgetProgress(label: String, used: Double, limit: Double) {
    val safeUsed = used.safeNonNegative()
    val safeLimit = limit.safeNonNegative()
    val percent = if (safeLimit > 0.0) safeUsed / safeLimit * 100.0 else 0.0
    val animatedProgress by animateFloatAsState(
        targetValue = percent.toProgress(),
        animationSpec = tween(520, easing = FastOutSlowInEasing),
        label = "$label budget",
    )
    val progressColor = MaterialTheme.colorScheme.tertiary
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${money(safeUsed)} / ${money(safeLimit)}", fontWeight = FontWeight.Medium)
        }
        ProgressTrack(
            progress = animatedProgress,
            color = progressColor,
            height = 8.dp,
            trackColor = trackColor,
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = "$label 已使用 ${formatPercent(percent)}"
            },
        )
    }
}

@Composable
private fun BillingRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(value, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private fun effectiveLimit(usage: CursorTOverview): Double = when {
    usage.usage.limitDollars > 0.0 -> usage.usage.limitDollars
    usage.plan.includedAmountDollars > 0.0 -> usage.plan.includedAmountDollars
    else -> 0.0
}

private fun buildSegmentDescription(prefix: String, segments: List<ChartSegment>): String =
    buildString {
        append(prefix)
        segments.forEach { segment ->
            append("，${segment.label} ${money(segment.value)}")
        }
    }

private fun UsageLevel.label(): String = when (this) {
    UsageLevel.Healthy -> "用量正常"
    UsageLevel.Warning -> "请关注用量"
    UsageLevel.Critical -> "即将用尽"
    UsageLevel.Exhausted -> "已用尽"
}

private fun Double.safeNonNegative(): Double = takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0

private fun Double.visualPercent(): Double = safeNonNegative().coerceAtMost(100.0)

private fun Double.toProgress(): Float = (visualPercent() / 100.0).toFloat()

private fun formatPercent(value: Double): String =
    String.format(Locale.US, "%.2f%%", value.safeNonNegative())

private fun money(value: Double): String =
    String.format(Locale.US, "\$%.2f", value.safeNonNegative())
