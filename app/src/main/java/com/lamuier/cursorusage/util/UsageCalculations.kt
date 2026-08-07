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
        return BillingProgress(
            totalDays = totalDays,
            elapsedDays = elapsedDays,
            remainingDays = (totalDays - elapsedDays).coerceAtLeast(0),
            percent = (elapsedMillis.toDouble() / totalMillis * 100.0).toFloat().coerceIn(0f, 100f),
            startLabel = start?.take(10) ?: "—",
            endLabel = end?.take(10) ?: "—",
        )
    }

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
}

