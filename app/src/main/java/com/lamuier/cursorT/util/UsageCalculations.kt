package com.lamuier.cursorT.util

import android.content.res.Resources
import com.lamuier.cursorT.R
import com.lamuier.cursorT.model.CursorTOverview
import com.lamuier.cursorT.model.TokenUsageBreakdown
import com.lamuier.cursorT.model.TotalFormat
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

enum class UsageLevel {
    Healthy,
    Warning,
    Critical,
    Exhausted,
}

/** 本周期按用量池拆分的 Token 费用（自有 Cursor 模型 / 第三方模型）。 */
data class PoolSpend(
    val ownPoolDollars: Double,
    val thirdPartyDollars: Double,
)

/** 历史窗口里官方两池百分比不可用时，用 Token 费用占比近似。 */
data class PoolPercents(
    val ownPercent: Double,
    val thirdPartyPercent: Double,
    val ownPoolDollars: Double,
    val thirdPartyDollars: Double,
)

data class BillingProgress(
    val totalDays: Int,
    val elapsedDays: Int,
    val remainingDays: Int,
    val percent: Float,
    val startLabel: String,
    val endLabel: String,
    /** 距离周期重置的精确毫秒数（已按周期范围收敛）。 */
    val remainingMillis: Long,
)

object UsageCalculations {
    fun usagePercent(overview: CursorTOverview): Double {
        val usage = overview.usage
        return when {
            usage.totalFormat == TotalFormat.Percent -> usage.totalUsed
            usage.limitDollars > 0 -> usage.includedSpendDollars / usage.limitDollars * 100.0
            else -> 0.0
        }.coerceAtLeast(0.0)
    }

    fun level(percent: Double): UsageLevel = when {
        percent >= 100.0 -> UsageLevel.Exhausted
        percent >= 90.0 -> UsageLevel.Critical
        percent >= 70.0 -> UsageLevel.Warning
        else -> UsageLevel.Healthy
    }

    fun billingProgress(
        start: String?,
        end: String?,
        nowMillis: Long = System.currentTimeMillis(),
        displayZone: ZoneId = ZoneId.systemDefault(),
        storageZone: ZoneId = ZoneId.systemDefault(),
    ): BillingProgress? {
        val startMillis = parseDate(start, storageZone) ?: return null
        val endMillis = parseDate(end, storageZone) ?: return null
        if (endMillis <= startMillis) return null

        val totalMillis = endMillis - startMillis
        val elapsedMillis = (nowMillis - startMillis).coerceIn(0, totalMillis)
        val dayMillis = Duration.ofDays(1).toMillis().toDouble()
        val totalDays = ceil(totalMillis / dayMillis).toInt().coerceAtLeast(1)
        val elapsedDays = floor(elapsedMillis / dayMillis).toInt().coerceIn(0, totalDays)
        val startInstant = Instant.ofEpochMilli(startMillis)
        val endInstant = Instant.ofEpochMilli(endMillis)
        val withYear = startInstant.atZone(displayZone).year != endInstant.atZone(displayZone).year
        return BillingProgress(
            totalDays = totalDays,
            elapsedDays = elapsedDays,
            remainingDays = (totalDays - elapsedDays).coerceAtLeast(0),
            percent = (elapsedMillis.toDouble() / totalMillis * 100.0).toFloat().coerceIn(0f, 100f),
            startLabel = DisplayTime.formatDateTime(
                startInstant,
                displayZone,
                withYear = withYear,
                includeTime = false,
            ),
            endLabel = DisplayTime.formatDateTime(
                endInstant,
                displayZone,
                withYear = withYear,
                includeTime = (end?.trim()?.length ?: 0) >= 16,
            ),
            remainingMillis = (endMillis - nowMillis).coerceIn(0, totalMillis),
        )
    }

    /**
     * 精确到分钟的重置倒计时文案：优先「天 + 小时」，不足一天用「小时 + 分」，不足一小时用「分」。
     * [compact] 为 true 时输出窄版（如「3天4时」），供桌面小组件等空间受限场景使用。
     */
    fun formatRemaining(
        remainingMillis: Long,
        compact: Boolean = false,
        resources: Resources? = null,
    ): String {
        val totalMinutes = remainingMillis.coerceAtLeast(0) / 60_000
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60) / 60).toInt()
        val minutes = (totalMinutes % 60).toInt()
        val dayCount = days.toInt()
        if (resources != null) {
            return when {
                dayCount > 0 -> if (compact) {
                    if (hours > 0) {
                        resources.getString(R.string.duration_compact_days_hours, dayCount, hours)
                    } else {
                        resources.getString(R.string.duration_compact_days, dayCount)
                    }
                } else if (hours > 0) {
                    resources.getString(R.string.duration_days_hours, dayCount, hours)
                } else {
                    resources.getString(R.string.duration_days, dayCount)
                }
                hours > 0 -> if (compact) {
                    if (minutes > 0) {
                        resources.getString(R.string.duration_compact_hours_minutes, hours, minutes)
                    } else {
                        resources.getString(R.string.duration_compact_hours, hours)
                    }
                } else if (minutes > 0) {
                    resources.getString(R.string.duration_hours_minutes, hours, minutes)
                } else {
                    resources.getString(R.string.duration_hours, hours)
                }
                else -> if (compact) {
                    resources.getString(R.string.duration_compact_minutes, minutes)
                } else {
                    resources.getString(R.string.duration_minutes, minutes)
                }
            }
        }
        return when {
            days > 0 -> if (compact) {
                buildString {
                    append(days)
                    append("天")
                    if (hours > 0) {
                        append(hours)
                        append("时")
                    }
                }
            } else if (hours > 0) {
                "$days 天 $hours 小时"
            } else {
                "$days 天"
            }
            hours > 0 -> if (compact) {
                buildString {
                    append(hours)
                    append("时")
                    if (minutes > 0) {
                        append(minutes)
                        append("分")
                    }
                }
            } else if (minutes > 0) {
                "$hours 小时 $minutes 分"
            } else {
                "$hours 小时"
            }
            else -> if (compact) "$minutes 分" else "$minutes 分钟"
        }
    }

    /** Token 数量紧凑展示：≥1M 用 M，≥1K 用 K，否则原样。 */
    fun formatTokens(value: Long): String {
        val safe = value.coerceAtLeast(0L)
        return when {
            safe >= 1_000_000_000L -> String.format(Locale.US, "%.2fB", safe / 1_000_000_000.0)
            safe >= 1_000_000L -> String.format(Locale.US, "%.2fM", safe / 1_000_000.0)
            safe >= 10_000L -> String.format(Locale.US, "%.1fK", safe / 1_000.0)
            safe >= 1_000L -> String.format(Locale.US, "%.2fK", safe / 1_000.0)
            else -> safe.toString()
        }
    }

    /**
     * 按模型拆分本周期费用：Composer / Grok / Auto 等 Cursor 自有模型计入自有池，
     * 其余第三方模型计入三方池。Token 明细不可用时返回 null。
     */
    fun poolSpend(tokenUsage: TokenUsageBreakdown?): PoolSpend? {
        if (tokenUsage == null) return null
        var own = 0.0
        var thirdParty = 0.0
        for (model in tokenUsage.models) {
            val cost = model.costDollars.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
            if (isCursorOwnedModel(model.modelIntent)) {
                own += cost
            } else {
                thirdParty += cost
            }
        }
        return PoolSpend(ownPoolDollars = own, thirdPartyDollars = thirdParty)
    }

    fun poolPercents(tokenUsage: TokenUsageBreakdown?): PoolPercents? {
        val spend = poolSpend(tokenUsage) ?: return null
        val total = spend.ownPoolDollars + spend.thirdPartyDollars
        if (total <= 0.0) return null
        return PoolPercents(
            ownPercent = spend.ownPoolDollars / total * 100.0,
            thirdPartyPercent = spend.thirdPartyDollars / total * 100.0,
            ownPoolDollars = spend.ownPoolDollars,
            thirdPartyDollars = spend.thirdPartyDollars,
        )
    }

    /** Cursor 自有用量池：Composer、Grok 与 Auto；其余按第三方模型计费。 */
    fun isCursorOwnedModel(modelIntent: String): Boolean {
        val name = modelIntent.trim().lowercase(Locale.US)
        if (name.isEmpty()) return false
        return name == "auto" ||
            name.startsWith("auto-") ||
            name.startsWith("composer") ||
            name.startsWith("grok") ||
            name.startsWith("cursor")
    }

    private fun parseDate(value: String?, storageZone: ZoneId): Long? =
        DisplayTime.parseStoredLocal(value, storageZone)?.toEpochMilli()
}

