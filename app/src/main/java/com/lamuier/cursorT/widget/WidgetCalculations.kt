package com.lamuier.cursorT.widget

import android.content.res.Resources
import com.lamuier.cursorT.R
import com.lamuier.cursorT.model.ComponentStatus
import com.lamuier.cursorT.model.CursorServiceStatus
import com.lamuier.cursorT.model.CursorTOverview
import com.lamuier.cursorT.model.StatusIndicator
import com.lamuier.cursorT.util.UsageCalculations
import java.util.Locale
import kotlin.math.roundToInt

internal object WidgetCalculations {
    fun totalPercent(usage: CursorTOverview): Double {
        val limit = UsageCalculations.effectiveLimit(usage)
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

    fun totalLabel(usage: CursorTOverview): String = if (usage.isTeam) {
        money(usage.usage.totalUsed)
    } else {
        percent(usage.usage.totalUsed)
    }

    fun modeLabel(prefix: String, value: Double?): String = value
        ?.takeIf { it.isFinite() }
        ?.let { "$prefix ${percent(it)}" }
        ?: "$prefix —"

    fun money(value: Double): String = String.format(Locale.US, "\$%.2f", value.safeNonNegative())

    fun percent(value: Double): String = String.format(Locale.US, "%.2f%%", value.safeNonNegative())

    fun operationalCount(status: CursorServiceStatus): Int =
        status.components.count { it.status == ComponentStatus.Operational }

    fun operationalPercent(status: CursorServiceStatus): Double {
        val total = status.components.size
        if (total <= 0) return 0.0
        return operationalCount(status) * 100.0 / total
    }

    fun summaryChip(status: CursorServiceStatus): String {
        val total = status.components.size
        if (total <= 0) return "—"
        return "${operationalCount(status)}/$total"
    }

    fun highlightedComponents(status: CursorServiceStatus, limit: Int): List<Pair<String, ComponentStatus>> {
        if (limit <= 0) return emptyList()
        val degraded = status.components.filter { it.status != ComponentStatus.Operational }
        val operational = status.components.filter { it.status == ComponentStatus.Operational }
        return (degraded + operational).take(limit).map { it.name to it.status }
    }

    fun incidentHeadline(status: CursorServiceStatus, resources: Resources? = null): String {
        val active = status.activeIncidents.firstOrNull()
        if (active != null) return active.name
        val maintenance = status.scheduledMaintenances.firstOrNull()
        if (maintenance != null) return maintenance.name
        val recent = status.recentIncidents.firstOrNull()
        if (recent != null) {
            return resources?.getString(R.string.widget_recent_incident, recent.name)
                ?: "近期 ${recent.name}"
        }
        return resources?.getString(R.string.widget_no_incidents) ?: "暂无事件"
    }

    fun indicatorColor(indicator: StatusIndicator): Int = when (indicator) {
        StatusIndicator.None -> COLOR_HEALTHY
        StatusIndicator.Minor, StatusIndicator.Maintenance -> COLOR_WARNING
        StatusIndicator.Major, StatusIndicator.Critical -> COLOR_CRITICAL
    }

    fun componentColor(status: ComponentStatus): Int = when (status) {
        ComponentStatus.Operational -> COLOR_HEALTHY
        ComponentStatus.DegradedPerformance, ComponentStatus.UnderMaintenance -> COLOR_WARNING
        ComponentStatus.PartialOutage, ComponentStatus.MajorOutage, ComponentStatus.Unknown -> COLOR_CRITICAL
    }

    private fun Double.safeNonNegative(): Double = takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0

    private const val COLOR_HEALTHY = 0xFF10B981.toInt()
    private const val COLOR_WARNING = 0xFFF59E0B.toInt()
    private const val COLOR_CRITICAL = 0xFFEF4444.toInt()
}
