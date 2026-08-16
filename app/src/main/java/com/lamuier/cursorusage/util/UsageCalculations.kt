package com.lamuier.cursorusage.util

import com.lamuier.cursorusage.model.CursorUsageOverview
import com.lamuier.cursorusage.model.TotalFormat
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.math.ceil
import kotlin.math.floor

enum class UsageLevel {
    Healthy,
    Warning,
    Critical,
    Exhausted,
}

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
    fun usagePercent(overview: CursorUsageOverview): Double {
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
    ): BillingProgress? {
        val startMillis = parseDate(start) ?: return null
        val endMillis = parseDate(end) ?: return null
        if (endMillis <= startMillis) return null

        val totalMillis = endMillis - startMillis
        val elapsedMillis = (nowMillis - startMillis).coerceIn(0, totalMillis)
        val dayMillis = Duration.ofDays(1).toMillis().toDouble()
        val totalDays = ceil(totalMillis / dayMillis).toInt().coerceAtLeast(1)
        val elapsedDays = floor(elapsedMillis / dayMillis).toInt().coerceIn(0, totalDays)
        // 同一年内省略年份以缩短展示；跨年周期才保留年份避免歧义。
        val withYear = start?.take(4) != end?.take(4)
        return BillingProgress(
            totalDays = totalDays,
            elapsedDays = elapsedDays,
            remainingDays = (totalDays - elapsedDays).coerceAtLeast(0),
            percent = (elapsedMillis.toDouble() / totalMillis * 100.0).toFloat().coerceIn(0f, 100f),
            startLabel = start?.let { cycleDateLabel(it, withYear) } ?: "—",
            // 结束时间精确到分钟，展示重置的具体时刻而非仅日期。
            endLabel = end?.let { cycleDateLabel(it, withYear) + cycleTimeLabel(it) } ?: "—",
            remainingMillis = (endMillis - nowMillis).coerceIn(0, totalMillis),
        )
    }

    private fun cycleDateLabel(value: String, withYear: Boolean): String {
        val date = value.take(10)
        return if (withYear) date else date.substring(5)
    }

    private fun cycleTimeLabel(value: String): String =
        if (value.length >= 16) " " + value.substring(11, 16) else ""

    /**
     * 精确到分钟的重置倒计时文案：优先「天 + 小时」，不足一天用「小时 + 分」，不足一小时用「分」。
     * [compact] 为 true 时输出窄版（如「3天4时」），供桌面小组件等空间受限场景使用。
     */
    fun formatRemaining(remainingMillis: Long, compact: Boolean = false): String {
        val totalMinutes = remainingMillis.coerceAtLeast(0) / 60_000
        val days = totalMinutes / (24 * 60)
        val hours = totalMinutes % (24 * 60) / 60
        val minutes = totalMinutes % 60
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

    private fun parseDate(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        // 兼容纯日期（yyyy-MM-dd），按当天零点处理。
        val normalized = if (value.length == 10) {
            value + "T00:00:00"
        } else {
            value.replace(' ', 'T')
        }
        return try {
            LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        } catch (_: DateTimeParseException) {
            null
        }
    }
}

