package com.lamuier.cursorusage.model

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
data class CursorUsageOverview(
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

@Immutable
data class AppUiState(
    val stage: AppStage = AppStage.Booting,
    val accounts: List<CursorAccount> = emptyList(),
    val selectedAccountId: Int? = null,
    val usage: CursorUsageOverview? = null,
    val loadingAccounts: Boolean = false,
    val loadingUsage: Boolean = false,
    val refreshing: Boolean = false,
    val submitting: Boolean = false,
    val error: String? = null,
)
