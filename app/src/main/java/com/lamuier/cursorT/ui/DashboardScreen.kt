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
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
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
import com.lamuier.cursorT.R
import com.lamuier.cursorT.model.AppUiState
import com.lamuier.cursorT.model.CursorAccount
import com.lamuier.cursorT.model.CursorTOverview
import com.lamuier.cursorT.model.DashboardTab
import com.lamuier.cursorT.model.ModelTokenUsage
import com.lamuier.cursorT.model.TokenUsageBreakdown
import com.lamuier.cursorT.model.UsageWindow
import com.lamuier.cursorT.ui.theme.LocalDisplayZone
import com.lamuier.cursorT.ui.theme.LocalPulseChartColors
import com.lamuier.cursorT.util.BillingProgress
import com.lamuier.cursorT.util.DisplayTime
import com.lamuier.cursorT.util.UsageCalculations
import com.lamuier.cursorT.util.UsageHistoryWindows
import com.lamuier.cursorT.util.UsageLevel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import java.util.Locale
import kotlinx.coroutines.delay
import java.time.YearMonth
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
    onLoadHistoryWindow: (String, Long, Long, String?) -> Unit,
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
                            stringResource(R.string.app_name),
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
                    val refreshBusy = state.refreshing || state.refreshingStatus || state.refreshingTasks
                    val refreshDescription = if (refreshBusy) {
                        stringResource(R.string.action_refreshing)
                    } else {
                        stringResource(R.string.action_refresh)
                    }
                    IconButton(
                        modifier = Modifier.semantics {
                            contentDescription = refreshDescription
                            if (refreshBusy) liveRegion = LiveRegionMode.Polite
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
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.action_settings))
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
                        title = stringResource(R.string.dashboard_loading_accounts_title),
                        description = stringResource(R.string.dashboard_loading_accounts_body),
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
                                            extraHistory = state.extraHistory,
                                            loadingHistoryKey = state.loadingHistoryKey,
                                            onLoadHistoryWindow = onLoadHistoryWindow,
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
                    label = "${tab.id} container",
                )
                val content by animateColorAsState(
                    targetValue = if (active) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(240, easing = FastOutSlowInEasing),
                    label = "${tab.id} content",
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
                            stringResource(tab.labelRes),
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
            title = stringResource(R.string.dashboard_add_account_title),
            description = stringResource(R.string.dashboard_add_account_usage_body),
            primaryActionLabel = stringResource(R.string.action_add_account),
            onPrimaryAction = onManageAccount,
            secondaryActionLabel = stringResource(R.string.action_token_help),
            onSecondaryAction = onShowTokenHelp,
        )
        account.tokenExpired -> DashboardState(
            icon = Icons.Outlined.KeyOff,
            title = stringResource(R.string.dashboard_token_expired_title),
            description = stringResource(R.string.dashboard_token_expired_usage_body),
            primaryActionLabel = stringResource(R.string.action_update_token),
            onPrimaryAction = onManageAccount,
            secondaryActionLabel = stringResource(R.string.action_token_help_short),
            onSecondaryAction = onShowTokenHelp,
        )
        state.loadingUsage && state.usage == null -> DashboardState(
            icon = null,
            title = stringResource(R.string.dashboard_loading_usage_title),
            description = stringResource(R.string.dashboard_loading_usage_body),
            loading = true,
        )
        state.usage == null -> DashboardState(
            icon = Icons.Outlined.CloudOff,
            title = stringResource(R.string.dashboard_usage_unavailable_title),
            description = stringResource(R.string.dashboard_usage_unavailable_body),
            primaryActionLabel = stringResource(R.string.action_reload),
            onPrimaryAction = onRefresh,
            secondaryActionLabel = stringResource(R.string.action_manage_account),
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
            title = stringResource(R.string.dashboard_add_account_title),
            description = stringResource(R.string.dashboard_add_account_tasks_body),
            primaryActionLabel = stringResource(R.string.action_add_account),
            onPrimaryAction = onManageAccount,
            secondaryActionLabel = stringResource(R.string.action_token_help),
            onSecondaryAction = onShowTokenHelp,
        )
        account.tokenExpired -> DashboardState(
            icon = Icons.Outlined.KeyOff,
            title = stringResource(R.string.dashboard_token_expired_title),
            description = stringResource(R.string.dashboard_token_expired_tasks_body),
            primaryActionLabel = stringResource(R.string.action_update_token),
            onPrimaryAction = onManageAccount,
            secondaryActionLabel = stringResource(R.string.action_token_help_short),
            onSecondaryAction = onShowTokenHelp,
        )
        else -> content()
    }
}

@Composable
private fun AccountStatusRow(account: CursorAccount?, loading: Boolean) {
    when {
        loading -> Text(
            stringResource(R.string.dashboard_account_loading),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        account == null -> Text(
            stringResource(R.string.dashboard_account_empty),
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
            StatusChip(label = stringResource(R.string.dashboard_token_expired_chip), error = true)
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
        val percentKnown = !usage.isTeam || limit > 0.0
        val nowMillis = rememberNowMillis()
        val zone = LocalDisplayZone.current
        val billing = remember(usage, nowMillis, zone) {
            UsageCalculations.billingProgress(
                usage.billingCycle.start,
                usage.billingCycle.end,
                nowMillis,
                displayZone = zone,
            )
        }
        val level = remember(percent, billing?.percent) {
            UsageCalculations.level(percent, billing?.percent?.toDouble())
        }
        val planQuotaLabel = stringResource(R.string.label_plan_quota)
        val ownPoolLabel = stringResource(R.string.label_own_pool_spend)
        val thirdPartyLabel = stringResource(R.string.label_third_party_spend)
        val quotaTiles = remember(usage, limit, chartColors, planQuotaLabel, ownPoolLabel, thirdPartyLabel) {
            val pools = UsageCalculations.poolSpend(usage.tokenUsage)
            listOf(
                MetricTile(
                    label = planQuotaLabel,
                    value = limit.takeIf { it > 0.0 }?.let(::money) ?: "—",
                    icon = Icons.Outlined.Savings,
                    accent = chartColors.chart1,
                ),
                MetricTile(
                    label = ownPoolLabel,
                    value = pools?.let { money(it.ownPoolDollars) } ?: "—",
                    icon = Icons.Outlined.Layers,
                    accent = chartColors.chart2,
                ),
                MetricTile(
                    label = thirdPartyLabel,
                    value = pools?.let { money(it.thirdPartyDollars) } ?: "—",
                    icon = Icons.Outlined.Hub,
                    accent = chartColors.chart3,
                ),
            )
        }
        val inputTokensLabel = stringResource(R.string.label_input_tokens)
        val outputTokensLabel = stringResource(R.string.label_output_tokens)
        val cacheTokensLabel = stringResource(R.string.label_cache_tokens)
        val tokenTiles = remember(usage, chartColors, inputTokensLabel, outputTokensLabel, cacheTokensLabel) {
            val tokenUsage = usage.tokenUsage
            listOf(
                MetricTile(
                    label = inputTokensLabel,
                    value = tokenUsage
                        ?.let { UsageCalculations.formatTokens(it.totalInputTokens) }
                        ?: "—",
                    icon = Icons.Outlined.Input,
                    accent = chartColors.chart2,
                ),
                MetricTile(
                    label = outputTokensLabel,
                    value = tokenUsage
                        ?.let { UsageCalculations.formatTokens(it.totalOutputTokens) }
                        ?: "—",
                    icon = Icons.Outlined.Output,
                    accent = chartColors.chart3,
                ),
                MetricTile(
                    label = cacheTokensLabel,
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
                            stringResource(R.string.label_this_cycle),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            usage.plan.name ?: stringResource(R.string.label_cursor_plan),
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
                val teamSpendPrefix = stringResource(R.string.label_team_spend_prefix, money(usage.usage.totalUsed))
                val planQuotaUsed = stringResource(R.string.label_plan_quota_used, formatPercent(percent))
                val planQuotaUnknown = stringResource(R.string.label_plan_quota_unknown)
                val totalUsageLevel = stringResource(
                    R.string.label_total_usage_level,
                    formatPercent(percent),
                    if (percentKnown) level.label() else stringResource(R.string.usage_level_unknown),
                )
                val cycleRemaining = billing?.let {
                    stringResource(R.string.label_cycle_remaining_clause, formatRemainingLabel(it.remainingMillis))
                }.orEmpty()
                val usageDescription = buildString {
                    if (usage.isTeam) {
                        append(teamSpendPrefix)
                        append(if (percentKnown) planQuotaUsed else planQuotaUnknown)
                    } else if (percentKnown) {
                        append(totalUsageLevel)
                    }
                    append(cycleRemaining)
                }
                UsageRing(
                    percent = percent,
                    size = chartSize,
                    progressColor = usageColor,
                    centerValue = if (usage.isTeam) money(usage.usage.totalUsed) else formatPercent(percent),
                    caption = if (percentKnown) level.label() else stringResource(R.string.usage_level_unknown),
                    description = usageDescription,
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
                        text = if (percentKnown) stringResource(R.string.label_usage_percent, formatPercent(percent)) else stringResource(R.string.label_usage_placeholder),
                    )
                    DotLabel(
                        color = MaterialTheme.colorScheme.primary,
                        text = billing?.let { stringResource(R.string.label_cycle_percent, formatPercent(it.percent.toDouble())) } ?: stringResource(R.string.label_cycle_placeholder),
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
            val grokProgress = remember(grok, nowMillis, zone) {
                UsageCalculations.billingProgress(
                    grok.periodStart,
                    grok.resetsAt,
                    nowMillis,
                    displayZone = zone,
                )
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
                        label = stringResource(R.string.label_grok_bot_week),
                        percent = grok.percentUsed,
                        color = chartColors.chart3,
                        caption = grokProgress?.let {
                            stringResource(R.string.label_grok_weekly_quota_reset, formatRemainingLabel(it.remainingMillis))
                        } ?: stringResource(R.string.label_grok_weekly_quota),
                    )
                }
            }
        }
        FreshnessRow(usage)
        if (usage.partialData) {
            Text(
                stringResource(R.string.label_partial_data),
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
                stringResource(R.string.label_billing_cycle),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                when {
                    billing != null -> "${billing.startLabel} — ${billing.endLabel}"
                    !planCycleEnd.isNullOrBlank() ->
                        stringResource(
                            R.string.label_ends_at,
                            DisplayTime.formatStoredDateTime(planCycleEnd, LocalDisplayZone.current) ?: planCycleEnd.take(10),
                        )
                    else -> stringResource(R.string.label_cycle_unknown)
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        billing?.let {
            StatusChip(label = stringResource(R.string.label_remaining_chip, formatRemainingLabel(it.remainingMillis)))
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
    val stamp = DisplayTime.formatStoredClock(usage.fetchedAt, LocalDisplayZone.current)
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
            listOfNotNull(
                stringResource(if (usage.fromCache) R.string.freshness_cached else R.string.freshness_updated),
                stamp,
                stringResource(R.string.freshness_pull_hint),
            ).joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private enum class HistoryQueryMode { CalendarMonth, BillingCycle }

@Composable
private fun UsageTab(
    usage: CursorTOverview,
    extraHistory: Map<String, UsageWindow>,
    loadingHistoryKey: String?,
    onLoadHistoryWindow: (String, Long, Long, String?) -> Unit,
) {
    AdaptiveTabContent { compact ->
        val chartColors = LocalPulseChartColors.current
        val includedUsageLabel = stringResource(R.string.label_included_usage)
        val remainingQuotaLabel = stringResource(R.string.label_remaining_quota)
        val segments = remember(usage, chartColors, includedUsageLabel, remainingQuotaLabel) {
            listOf(
                ChartSegment(includedUsageLabel, usage.usage.includedSpendDollars.safeNonNegative(), chartColors.chart1),
                ChartSegment("Bonus", usage.usage.bonusSpendDollars.safeNonNegative(), chartColors.chart2),
                ChartSegment(remainingQuotaLabel, usage.usage.remainingDollars.safeNonNegative(), chartColors.chart3),
            )
        }

        SectionHeading(
            icon = Icons.Outlined.Layers,
            title = stringResource(R.string.label_usage_pools),
            supporting = stringResource(R.string.label_usage_pools_supporting),
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
                    label = stringResource(R.string.label_cursor_models),
                    percent = usage.usage.autoPercentUsed,
                    color = chartColors.chart1,
                    caption = stringResource(R.string.label_cursor_models_caption),
                )
                HorizontalUsageChart(
                    label = stringResource(R.string.label_other_models),
                    percent = usage.usage.apiPercentUsed,
                    color = chartColors.chart2,
                    caption = stringResource(R.string.label_other_models_caption),
                )
            }
        }

        usage.grokBot?.let { grok ->
            val nowMillis = rememberNowMillis()
            val zone = LocalDisplayZone.current
            val grokProgress = remember(grok, nowMillis, zone) {
                UsageCalculations.billingProgress(
                    grok.periodStart,
                    grok.resetsAt,
                    nowMillis,
                    displayZone = zone,
                )
            }
            SectionHeading(
                icon = Icons.Outlined.SmartToy,
                title = "Grok Bot",
                supporting = grokProgress?.let {
                    stringResource(R.string.label_grok_weekly_quota_reset, formatRemainingLabel(it.remainingMillis))
                } ?: stringResource(R.string.label_grok_weekly_quota_usage),
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
                        label = stringResource(R.string.label_week_usage),
                        percent = grok.percentUsed,
                        color = chartColors.chart3,
                        caption = stringResource(R.string.label_grok_on_demand_caption),
                    )
                }
            }
        }

        SectionHeading(
            icon = Icons.Outlined.PieChart,
            title = stringResource(R.string.label_quota_composition),
            supporting = stringResource(R.string.label_quota_composition_supporting),
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
                    description = buildSegmentDescription(stringResource(R.string.label_quota_composition), segments),
                    height = if (compact) 14.dp else 16.dp,
                )
                SegmentLegend(segments)
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.label_total_spend),
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
            extraHistory = extraHistory,
            loadingHistoryKey = loadingHistoryKey,
            onLoadHistoryWindow = onLoadHistoryWindow,
            compact = compact,
        )
    }
}

@Composable
private fun TokenUsageSection(usage: CursorTOverview, compact: Boolean) {
    val tokenUsage = usage.tokenUsage
    SectionHeading(
        icon = Icons.AutoMirrored.Outlined.TrendingUp,
        title = stringResource(R.string.label_token_usage),
        supporting = stringResource(R.string.label_token_section_supporting),
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
                        text = stringResource(R.string.label_token_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                tokenUsage.models.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.label_token_empty),
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
    extraHistory: Map<String, UsageWindow>,
    loadingHistoryKey: String?,
    onLoadHistoryWindow: (String, Long, Long, String?) -> Unit,
    compact: Boolean,
) {
    val chartColors = LocalPulseChartColors.current
    val zone = LocalDisplayZone.current
    val nowMonth = remember(zone) { YearMonth.now(zone) }
    var mode by remember { mutableStateOf(HistoryQueryMode.CalendarMonth) }
    var selectedMonth by remember { mutableStateOf(nowMonth) }
    var cycleOffset by remember { mutableIntStateOf(-1) }
    val selectedMonthKey = UsageHistoryWindows.yearMonthKey(selectedMonth)
    val cycleStartMs = remember(usage.billingCycle.start) {
        UsageHistoryWindows.parseLocalDateTimeMs(usage.billingCycle.start)
    }
    val cycleEndMs = remember(usage.billingCycle.end) {
        UsageHistoryWindows.parseLocalDateTimeMs(usage.billingCycle.end)
    }
    val selectedCycleRange = remember(cycleStartMs, cycleEndMs, cycleOffset) {
        if (cycleStartMs == null || cycleEndMs == null) {
            null
        } else {
            UsageHistoryWindows.billingCycleOffset(cycleStartMs, cycleEndMs, cycleOffset)
        }
    }
    val selectedCycleKey = selectedCycleRange?.let { UsageHistoryWindows.cycleKey(it.startMs) }
    LaunchedEffect(selectedMonthKey, mode, usage.accountId, zone) {
        if (mode != HistoryQueryMode.CalendarMonth) return@LaunchedEffect
        val monthRange = UsageHistoryWindows.calendarMonth(selectedMonth, zone = zone)
        if (usage.history?.calendarMonth?.yearMonth != selectedMonthKey &&
            extraHistory[selectedMonthKey] == null
        ) {
            onLoadHistoryWindow(selectedMonthKey, monthRange.startMs, monthRange.endMs, selectedMonthKey)
        }
    }
    LaunchedEffect(selectedCycleKey, mode, usage.accountId) {
        if (mode != HistoryQueryMode.BillingCycle) return@LaunchedEffect
        val range = selectedCycleRange ?: return@LaunchedEffect
        val key = selectedCycleKey ?: return@LaunchedEffect
        if (cycleOffset == -1 && usage.history?.previousCycle != null) return@LaunchedEffect
        if (extraHistory[key] == null) {
            onLoadHistoryWindow(key, range.startMs, range.endMs, null)
        }
    }
    val extraCycle = selectedCycleKey?.let { extraHistory[it] }
    val window = when (mode) {
        HistoryQueryMode.BillingCycle -> {
            if (cycleOffset == -1) extraCycle ?: usage.history?.previousCycle else extraCycle
        }
        HistoryQueryMode.CalendarMonth -> {
            if (usage.history?.calendarMonth?.yearMonth == selectedMonthKey) {
                usage.history?.calendarMonth
            } else {
                extraHistory[selectedMonthKey]
            }
        }
    }
    val loadingKey = when (mode) {
        HistoryQueryMode.CalendarMonth -> selectedMonthKey
        HistoryQueryMode.BillingCycle -> selectedCycleKey
    }
    val loading = loadingHistoryKey == loadingKey && window == null
    val earliestMonth = nowMonth.minusMonths(12)
    val pools = remember(window?.tokenUsage) { UsageCalculations.poolPercents(window?.tokenUsage) }
    SectionHeading(
        icon = Icons.Outlined.History,
        title = stringResource(R.string.label_history_title),
        supporting = stringResource(R.string.label_history_supporting),
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
                    HistoryQueryMode.CalendarMonth to stringResource(R.string.label_history_calendar_month),
                    HistoryQueryMode.BillingCycle to stringResource(R.string.label_history_billing_cycle),
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
                        enabled = selectedMonth.isAfter(earliestMonth),
                    ) {
                        Icon(Icons.Outlined.ChevronLeft, contentDescription = stringResource(R.string.label_history_prev_month))
                    }
                    Text(
                        stringResource(R.string.label_year_month, selectedMonth.year, selectedMonth.monthValue),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(
                        onClick = { selectedMonth = selectedMonth.plusMonths(1) },
                        enabled = selectedMonth.isBefore(nowMonth),
                    ) {
                        Icon(Icons.Outlined.ChevronRight, contentDescription = stringResource(R.string.label_history_next_month))
                    }
                }
            } else {
                val labels = selectedCycleRange?.let { UsageHistoryWindows.formatRange(it, zone) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { cycleOffset -= 1 },
                        enabled = selectedCycleRange != null && cycleOffset > -12,
                    ) {
                        Icon(Icons.Outlined.ChevronLeft, contentDescription = stringResource(R.string.label_history_prev_cycle))
                    }
                    Text(
                        text = if (labels != null) {
                            "${labels.first} → ${labels.second}"
                        } else {
                            stringResource(R.string.label_history_cycle_unknown)
                        },
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(
                        onClick = { cycleOffset += 1 },
                        enabled = cycleOffset < -1,
                    ) {
                        Icon(Icons.Outlined.ChevronRight, contentDescription = stringResource(R.string.label_history_next_cycle))
                    }
                }
            }
            when {
                loading -> {
                    Text(
                        stringResource(R.string.label_history_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                window?.tokenUsage == null -> {
                    Text(
                        stringResource(R.string.label_history_empty_tokens),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    val tokenUsage = window.tokenUsage!!
                    if (pools != null) {
                        HorizontalUsageChart(
                            label = stringResource(R.string.label_cursor_models),
                            percent = pools.ownPercent,
                            color = chartColors.chart1,
                            caption = stringResource(R.string.label_history_pool_share_caption, money(pools.ownPoolDollars)),
                        )
                        HorizontalUsageChart(
                            label = stringResource(R.string.label_other_models),
                            percent = pools.thirdPartyPercent,
                            color = chartColors.chart2,
                            caption = stringResource(R.string.label_history_pool_share_caption, money(pools.thirdPartyDollars)),
                        )
                    } else {
                        Text(
                            stringResource(R.string.label_history_pool_share_unavailable),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    val cachedTokens = tokenUsage.totalCacheWriteTokens + tokenUsage.totalCacheReadTokens
                    val tokenLine = listOfNotNull(
                        stringResource(R.string.label_in_tokens, UsageCalculations.formatTokens(tokenUsage.totalInputTokens)),
                        stringResource(R.string.label_out_tokens, UsageCalculations.formatTokens(tokenUsage.totalOutputTokens)),
                        cachedTokens.takeIf { it > 0L }?.let {
                            stringResource(R.string.label_cache_tokens_short, UsageCalculations.formatTokens(it))
                        },
                        money(tokenUsage.totalCostDollars),
                    ).joinToString(" · ")
                    Text(
                        tokenLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (tokenUsage.models.isNotEmpty()) {
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
}

@Composable
private fun ModelTokenRow(
    model: ModelTokenUsage,
    compact: Boolean,
) {
    val cached = model.cacheWriteTokens + model.cacheReadTokens
    val modelA11y = if (cached > 0L) {
        stringResource(
            R.string.label_model_token_a11y_cache,
            model.modelIntent,
            model.inputTokens.toString(),
            model.outputTokens.toString(),
            cached.toString(),
            money(model.costDollars),
        )
    } else {
        stringResource(
            R.string.label_model_token_a11y,
            model.modelIntent,
            model.inputTokens.toString(),
            model.outputTokens.toString(),
            money(model.costDollars),
        )
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = modelA11y
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
        val tokenLine = listOfNotNull(
            stringResource(R.string.label_in_tokens, UsageCalculations.formatTokens(model.inputTokens)),
            stringResource(R.string.label_out_tokens, UsageCalculations.formatTokens(model.outputTokens)),
            cached.takeIf { it > 0L }?.let {
                stringResource(R.string.label_cache_tokens_short, UsageCalculations.formatTokens(it))
            },
        ).joinToString(" · ")
        Text(
            text = tokenLine,
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
        val otherLabel = stringResource(R.string.label_other)
        val segments = remember(usage, chartColors, otherLabel) {
            val unclassified = (
                usage.credits.totalDollars -
                    usage.credits.grantTotalDollars -
                    usage.credits.stripeBalanceDollars
                ).coerceAtLeast(0.0)
            listOf(
                ChartSegment("Grant", usage.credits.grantTotalDollars, chartColors.chart1),
                ChartSegment("Stripe", usage.credits.stripeBalanceDollars, chartColors.chart2),
                ChartSegment(otherLabel, unclassified, chartColors.chart3),
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
                    title = stringResource(R.string.label_credits_composition),
                    supporting = stringResource(R.string.label_credits_grant_stripe),
                )
                if (usage.partialData) {
                    Text(
                        stringResource(R.string.label_partial_sources),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (visibleSegments.isEmpty()) {
                    Text(stringResource(R.string.label_credits_unavailable), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    StackedBar(
                        segments = visibleSegments,
                        description = buildSegmentDescription(
                            if (usage.partialData) stringResource(R.string.label_credits_partial) else stringResource(R.string.label_credits_composition),
                            visibleSegments,
                        ),
                        height = if (compact) 14.dp else 16.dp,
                    )
                    SegmentLegend(visibleSegments)
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        if (usage.partialData) stringResource(R.string.label_credits_partial_total) else stringResource(R.string.label_credits_total),
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
    val poolDescription = stringResource(
        R.string.pool_progress_description,
        label,
        value?.let(::formatPercent) ?: stringResource(R.string.no_data),
    )
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
                contentDescription = poolDescription
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
    val zone = LocalDisplayZone.current
    val billing = remember(usage, nowMillis, zone) {
        UsageCalculations.billingProgress(
            usage.billingCycle.start,
            usage.billingCycle.end,
            nowMillis,
            displayZone = zone,
        )
    }
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
            SectionHeading(icon = Icons.Outlined.CalendarMonth, title = stringResource(R.string.label_billing_cycle))
            if (billing == null) {
                Text(
                    usage.plan.billingCycleEnd?.let {
                        stringResource(
                            R.string.label_cycle_end,
                            DisplayTime.formatStoredDateTime(it, zone) ?: it,
                        )
                    } ?: stringResource(R.string.label_cycle_unknown),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val cycleA11y = stringResource(
                    R.string.label_cycle_progress_a11y,
                    formatPercent(billing.percent.toDouble()),
                    formatRemainingLabel(billing.remainingMillis),
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${billing.startLabel} — ${billing.endLabel}",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        stringResource(R.string.label_remaining, formatRemainingLabel(billing.remainingMillis)),
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
                        contentDescription = cycleA11y
                    },
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        stringResource(R.string.label_cycle_elapsed_days, billing.elapsedDays, billing.totalDays),
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
            SectionHeading(icon = Icons.AutoMirrored.Outlined.ReceiptLong, title = stringResource(R.string.label_billing_status))
            if (onDemand != null) {
                BillingRow("On-demand", money(onDemand.totalSpendDollars))
                if (onDemand.individualLimitDollars > 0.0) {
                    BudgetProgress(
                        label = stringResource(R.string.label_personal_quota),
                        used = onDemand.individualUsedDollars,
                        limit = onDemand.individualLimitDollars,
                    )
                }
                if (onDemand.pooledLimitDollars > 0.0) {
                    BudgetProgress(
                        label = stringResource(R.string.label_shared_quota),
                        used = onDemand.pooledUsedDollars,
                        limit = onDemand.pooledLimitDollars,
                    )
                }
            }
            if (membership != null || status != null) {
                BillingRow(stringResource(R.string.label_subscription), listOfNotNull(membership, status).joinToString(" · "))
            }
            if (onDemand == null && membership == null && status == null) {
                Text(stringResource(R.string.label_billing_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    val budgetDescription = stringResource(
        R.string.label_used_percent_a11y,
        label,
        formatPercent(percent),
    )
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
                contentDescription = budgetDescription
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
            append(" · ${segment.label} ${money(segment.value)}")
        }
    }

@Composable
private fun formatRemainingLabel(millis: Long, compact: Boolean = false): String =
    UsageCalculations.formatRemaining(millis, compact, LocalContext.current.resources)

@Composable
private fun UsageLevel.label(): String = stringResource(
    when (this) {
        UsageLevel.Healthy -> R.string.usage_level_healthy
        UsageLevel.Warning -> R.string.usage_level_warning
        UsageLevel.Critical -> R.string.usage_level_critical
        UsageLevel.Exhausted -> R.string.usage_level_exhausted
    },
)

private fun Double.safeNonNegative(): Double = takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0

private fun Double.visualPercent(): Double = safeNonNegative().coerceAtMost(100.0)

private fun Double.toProgress(): Float = (visualPercent() / 100.0).toFloat()

private fun formatPercent(value: Double): String =
    String.format(Locale.US, "%.2f%%", value.safeNonNegative())

private fun money(value: Double): String =
    String.format(Locale.US, "\$%.2f", value.safeNonNegative())
