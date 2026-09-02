package com.lamuier.cursorT.util

import android.content.res.Resources
import com.lamuier.cursorT.R
import com.lamuier.cursorT.model.ComponentStatus
import com.lamuier.cursorT.model.StatusIndicator
import java.net.URI
import java.time.ZoneId
import java.util.Locale

object StatusPresentation {
    fun indicatorLabel(indicator: StatusIndicator, resources: Resources? = null): String {
        val id = when (indicator) {
            StatusIndicator.None -> R.string.status_indicator_none
            StatusIndicator.Minor -> R.string.status_indicator_minor
            StatusIndicator.Major -> R.string.status_indicator_major
            StatusIndicator.Critical -> R.string.status_indicator_critical
            StatusIndicator.Maintenance -> R.string.status_indicator_maintenance
        }
        return resources?.getString(id) ?: fallbackZh(id)
    }

    fun compactIndicatorLabel(indicator: StatusIndicator, resources: Resources? = null): String {
        val id = when (indicator) {
            StatusIndicator.None -> R.string.status_indicator_none_compact
            StatusIndicator.Minor -> R.string.status_indicator_minor_compact
            StatusIndicator.Major -> R.string.status_indicator_major_compact
            StatusIndicator.Critical -> R.string.status_indicator_critical_compact
            StatusIndicator.Maintenance -> R.string.status_indicator_maintenance_compact
        }
        return resources?.getString(id) ?: fallbackZh(id)
    }

    fun componentLabel(status: ComponentStatus, resources: Resources? = null): String {
        val id = when (status) {
            ComponentStatus.Operational -> R.string.status_component_operational
            ComponentStatus.DegradedPerformance -> R.string.status_component_degraded
            ComponentStatus.PartialOutage -> R.string.status_component_partial_outage
            ComponentStatus.MajorOutage -> R.string.status_component_major_outage
            ComponentStatus.UnderMaintenance -> R.string.status_component_maintenance
            ComponentStatus.Unknown -> R.string.status_component_unknown
        }
        return resources?.getString(id) ?: fallbackZh(id)
    }

    fun incidentStatusLabel(status: String, resources: Resources? = null): String {
        val id = when (status.lowercase(Locale.US)) {
            "investigating" -> R.string.status_incident_investigating
            "identified" -> R.string.status_incident_identified
            "monitoring" -> R.string.status_incident_monitoring
            "resolved" -> R.string.status_incident_resolved
            "postmortem" -> R.string.status_incident_postmortem
            "scheduled" -> R.string.status_incident_scheduled
            "in_progress" -> R.string.status_incident_in_progress
            "verifying" -> R.string.status_incident_verifying
            "completed" -> R.string.status_incident_completed
            else -> null
        }
        return when {
            id != null -> resources?.getString(id) ?: fallbackZh(id)
            status.isNotBlank() -> status
            else -> resources?.getString(R.string.status_incident_update) ?: "更新"
        }
    }

    fun impactLabel(impact: String, resources: Resources? = null): String {
        val id = when (impact.lowercase(Locale.US)) {
            "none" -> R.string.status_impact_none
            "minor" -> R.string.status_impact_minor
            "major" -> R.string.status_impact_major
            "critical" -> R.string.status_impact_critical
            "maintenance" -> R.string.status_impact_maintenance
            else -> null
        }
        return when {
            id != null -> resources?.getString(id) ?: fallbackZh(id)
            impact.isNotBlank() -> impact
            else -> resources?.getString(R.string.status_impact_unknown) ?: "未知"
        }
    }

    fun formatInstant(iso: String?, zoneId: ZoneId = ZoneId.systemDefault()): String? =
        DisplayTime.formatIso(iso, zoneId)

    fun incidentUrl(shortlink: String?, incidentId: String): String? {
        shortlink?.takeIf(::isSafeStatusUrl)?.let { return it }
        if (!INCIDENT_ID.matches(incidentId)) return null
        val fallback = "https://status.cursor.com/incidents/$incidentId"
        return fallback.takeIf(::isSafeStatusUrl)
    }

    fun isSafeStatusUrl(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        if (!uri.rawQuery.isNullOrBlank() || !uri.rawFragment.isNullOrBlank() || uri.userInfo != null) {
            return false
        }
        val host = uri.host?.lowercase(Locale.US) ?: return false
        return host == "status.cursor.com" || host == "stspg.io" || host.endsWith(".stspg.io")
    }

    private val INCIDENT_ID = Regex("^[A-Za-z0-9_-]+$")

    private fun fallbackZh(id: Int): String = when (id) {
        R.string.status_indicator_none -> "全部系统正常"
        R.string.status_indicator_minor -> "部分性能下降"
        R.string.status_indicator_major -> "部分服务中断"
        R.string.status_indicator_critical -> "严重故障"
        R.string.status_indicator_maintenance -> "计划维护中"
        R.string.status_indicator_none_compact -> "全部正常"
        R.string.status_indicator_minor_compact -> "性能下降"
        R.string.status_indicator_major_compact -> "部分中断"
        R.string.status_indicator_critical_compact -> "严重故障"
        R.string.status_indicator_maintenance_compact -> "维护中"
        R.string.status_component_operational -> "正常"
        R.string.status_component_degraded -> "性能下降"
        R.string.status_component_partial_outage -> "部分中断"
        R.string.status_component_major_outage -> "严重中断"
        R.string.status_component_maintenance -> "维护中"
        R.string.status_component_unknown -> "未知"
        R.string.status_incident_investigating -> "调查中"
        R.string.status_incident_identified -> "已定位"
        R.string.status_incident_monitoring -> "观察中"
        R.string.status_incident_resolved -> "已恢复"
        R.string.status_incident_postmortem -> "事后分析"
        R.string.status_incident_scheduled -> "已计划"
        R.string.status_incident_in_progress -> "进行中"
        R.string.status_incident_verifying -> "验证中"
        R.string.status_incident_completed -> "已完成"
        R.string.status_impact_none -> "无影响"
        R.string.status_impact_minor -> "轻微"
        R.string.status_impact_major -> "较大"
        R.string.status_impact_critical -> "严重"
        R.string.status_impact_maintenance -> "维护"
        else -> ""
    }
}
