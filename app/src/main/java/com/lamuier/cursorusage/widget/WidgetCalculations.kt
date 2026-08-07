package com.lamuier.cursorusage.widget

import com.lamuier.cursorusage.model.CursorUsageOverview
import com.lamuier.cursorusage.util.UsageCalculations
import java.util.Locale
import kotlin.math.roundToInt

internal object WidgetCalculations {
    fun totalPercent(usage: CursorUsageOverview): Double {
        val limit = effectiveLimit(usage)
        val raw = if (usage.isTeam && limit > 0.0) {
            usage.usage.includedSpendDollars.safeNonNegative() / limit * 100.0
        } else {
            UsageCalculations.usagePercent(usage)
        }
        return raw.safeNonNegative()
    }

    fun progress(percent: Double?): Int = percent
        ?.takeIf { it.isFinite() }
        ?.coerceIn(0.0, 100.0)
        ?.roundToInt()
        ?: 0

    fun totalLabel(usage: CursorUsageOverview): String = if (usage.isTeam) {
        money(usage.usage.totalUsed)
    } else {
        percent(usage.usage.totalUsed)
    }

    fun modeLabel(prefix: String, value: Double?): String = value
        ?.takeIf { it.isFinite() }
        ?.let { "$prefix ${percent(it)}" }
        ?: "$prefix —"

    fun money(value: Double): String = String.format(Locale.US, "\$%.2f", value.safeNonNegative())

    fun percent(value: Double): String = String.format(Locale.US, "%.1f%%", value.safeNonNegative())

    private fun effectiveLimit(usage: CursorUsageOverview): Double = when {
        usage.usage.limitDollars > 0.0 -> usage.usage.limitDollars
        usage.plan.includedAmountDollars > 0.0 -> usage.plan.includedAmountDollars
        else -> 0.0
    }.safeNonNegative()

    private fun Double.safeNonNegative(): Double = takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
}
