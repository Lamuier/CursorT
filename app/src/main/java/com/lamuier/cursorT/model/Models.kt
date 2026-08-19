package com.lamuier.cursorT.model

import androidx.compose.runtime.Immutable

@Immutable
data class CursorAccount(
    val id: Int,
    val alias: String,
    val tokenExpired: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

@Immutable
data class CursorTOverview(
    val accountId: Int,
    val alias: String,
    val isTeam: Boolean,
    val plan: PlanInfo,
    val billingCycle: BillingCycle,
    val usage: UsageMetrics,
    val credits: Credits,
    val onDemand: OnDemandUsage?,
    val subscription: Subscription,
    val fetchedAt: String,
    val fromCache: Boolean,
    val cacheAgeSeconds: Int,
    val isLocalCache: Boolean = false,
    val partialData: Boolean = false,
)

@Immutable
data class PlanInfo(
    val name: String?,
    val price: String?,
    val includedAmountDollars: Double,
    val billingCycleEnd: String?,
)

@Immutable
data class BillingCycle(
    val start: String?,
    val end: String?,
)

@Immutable
data class UsageMetrics(
    val totalUsed: Double,
    val totalFormat: TotalFormat,
    val totalSpendDollars: Double,
    val includedSpendDollars: Double,
    val bonusSpendDollars: Double,
    val limitDollars: Double,
    val remainingDollars: Double,
    val autoPercentUsed: Double?,
    val apiPercentUsed: Double?,
    val displayMessage: String?,
    val remainingBonus: Boolean,
)

enum class TotalFormat {
    Percent,
    Dollars,
}

@Immutable
data class Credits(
    val totalDollars: Double,
    val grantTotalDollars: Double,
    val stripeBalanceDollars: Double,
)

@Immutable
data class OnDemandUsage(
    val limitType: String?,
    val totalSpendDollars: Double,
    val individualLimitDollars: Double,
    val individualUsedDollars: Double,
    val individualRemainingDollars: Double,
    val pooledLimitDollars: Double,
    val pooledUsedDollars: Double,
    val pooledRemainingDollars: Double,
)

@Immutable
data class Subscription(
    val membershipType: String?,
    val status: String?,
)

enum class AppStage {
    Booting,
    Dashboard,
}

/** 云端任务（Cursor 后台智能体）的执行状态。 */
enum class AgentTaskStatus {
    Creating,
    Running,
    Finished,
    Error,
    Expired,
    Unknown,
}

/** 云端任务关联的 Pull Request 状态。 */
enum class AgentTaskPrStatus {
    Open,
    Draft,
    Merged,
    Closed,
    Unknown,
}

@Immutable
data class AgentTask(
    val id: String,
    val name: String,
    val status: AgentTaskStatus,
    val repoUrl: String?,
    val branchName: String?,
    val prUrl: String?,
    val prStatus: AgentTaskPrStatus?,
    val linesAdded: Int,
    val linesRemoved: Int,
    val filesChanged: Int,
    val modelName: String?,
    val maxMode: Boolean,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val lastActivityMs: Long?,
)

enum class AgentTaskMessageRole {
    User,
    Assistant,
    Unknown,
}

@Immutable
data class AgentTaskMessage(
    val id: String,
    val role: AgentTaskMessageRole,
    val text: String,
    val createdAtMs: Long? = null,
    val pending: Boolean = false,
)

@Immutable
data class AgentTaskConversation(
    val accountId: Int,
    val task: AgentTask,
    val messages: List<AgentTaskMessage>,
    val fetchedAt: String,
    val fromCache: Boolean = false,
)

@Immutable
data class CursorTasks(
    val accountId: Int,
    val tasks: List<AgentTask>,
    val fetchedAt: String,
    val fromCache: Boolean,
    val cacheAgeSeconds: Int = 0,
)

/** 长按图标静态 Shortcut 对应的应用内动作。 */
enum class ShortcutAction(val intentAction: String) {
    RevealToken(INTENT_ACTION_PREFIX + ".SHORTCUT_REVEAL_TOKEN"),
    ManageAccounts(INTENT_ACTION_PREFIX + ".SHORTCUT_MANAGE_ACCOUNTS"),
    Settings(INTENT_ACTION_PREFIX + ".SHORTCUT_SETTINGS");

    companion object {
        fun fromIntentAction(action: String?): ShortcutAction? =
            entries.firstOrNull { it.intentAction == action }
    }
}

private const val INTENT_ACTION_PREFIX = "com.lamuier.cursorT.action"

@Immutable
data class AppUiState(
    val stage: AppStage = AppStage.Booting,
    val accounts: List<CursorAccount> = emptyList(),
    val selectedAccountId: Int? = null,
    val usage: CursorTOverview? = null,
    val tasks: CursorTasks? = null,
    val serviceStatus: CursorServiceStatus? = null,
    val loadingAccounts: Boolean = false,
    val loadingUsage: Boolean = false,
    val loadingTasks: Boolean = false,
    val loadingStatus: Boolean = false,
    val refreshing: Boolean = false,
    val refreshingTasks: Boolean = false,
    val refreshingStatus: Boolean = false,
    val submitting: Boolean = false,
    val error: String? = null,
    val tasksError: String? = null,
    val statusError: String? = null,
    val selectedTask: AgentTask? = null,
    val conversation: AgentTaskConversation? = null,
    val loadingConversation: Boolean = false,
    val refreshingConversation: Boolean = false,
    val sendingFollowup: Boolean = false,
    val conversationError: String? = null,
)

/** Cursor 官方状态页（Statuspage）的整体指示灯。 */
enum class StatusIndicator {
    None,
    Minor,
    Major,
    Critical,
    Maintenance,
}

/** 单个服务组件的可用状态。 */
enum class ComponentStatus {
    Operational,
    DegradedPerformance,
    PartialOutage,
    MajorOutage,
    UnderMaintenance,
    Unknown,
}

@Immutable
data class CursorServiceStatus(
    val description: String,
    val indicator: StatusIndicator,
    val pageUpdatedAt: String?,
    val pageUrl: String,
    val components: List<StatusComponent>,
    val activeIncidents: List<StatusIncident>,
    val scheduledMaintenances: List<StatusIncident>,
    val recentIncidents: List<StatusIncident>,
    val fetchedAt: String,
    val fromCache: Boolean,
    val cacheAgeSeconds: Int = 0,
    val partialHistory: Boolean = false,
)

@Immutable
data class StatusComponent(
    val id: String,
    val name: String,
    val status: ComponentStatus,
    val position: Int,
)

@Immutable
data class StatusIncident(
    val id: String,
    val name: String,
    val status: String,
    val impact: String,
    val createdAt: String?,
    val updatedAt: String?,
    val resolvedAt: String?,
    val scheduledFor: String?,
    val scheduledUntil: String?,
    val shortlink: String?,
    val affectedComponents: List<String>,
    val updates: List<StatusIncidentUpdate>,
)

@Immutable
data class StatusIncidentUpdate(
    val status: String,
    val body: String,
    val displayAt: String?,
)
