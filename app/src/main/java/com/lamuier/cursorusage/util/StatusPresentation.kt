package com.lamuier.cursorusage.util

import com.lamuier.cursorusage.model.ComponentStatus
import com.lamuier.cursorusage.model.StatusIndicator
import java.net.URI
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object StatusPresentation {
    fun indicatorLabel(indicator: StatusIndicator): String = when (indicator) {
        StatusIndicator.None -> "全部系统正常"
        StatusIndicator.Minor -> "部分性能下降"
        StatusIndicator.Major -> "部分服务中断"
        StatusIndicator.Critical -> "严重故障"
        StatusIndicator.Maintenance -> "计划维护中"
    }

    fun componentLabel(status: ComponentStatus): String = when (status) {
        ComponentStatus.Operational -> "正常"
        ComponentStatus.DegradedPerformance -> "性能下降"
        ComponentStatus.PartialOutage -> "部分中断"
        ComponentStatus.MajorOutage -> "严重中断"
        ComponentStatus.UnderMaintenance -> "维护中"
        ComponentStatus.Unknown -> "未知"
    }

    fun incidentStatusLabel(status: String): String = when (status.lowercase(Locale.US)) {
        "investigating" -> "调查中"
        "identified" -> "已定位"
        "monitoring" -> "观察中"
        "resolved" -> "已恢复"
        "postmortem" -> "事后分析"
        "scheduled" -> "已计划"
        "in_progress" -> "进行中"
        "verifying" -> "验证中"
        "completed" -> "已完成"
        else -> status.ifBlank { "更新" }
    }

    fun impactLabel(impact: String): String = when (impact.lowercase(Locale.US)) {
        "none" -> "无影响"
        "minor" -> "轻微"
        "major" -> "较大"
        "critical" -> "严重"
        "maintenance" -> "维护"
        else -> impact.ifBlank { "未知" }
    }

    fun formatInstant(iso: String?, zoneId: ZoneId = ZoneId.systemDefault()): String? {
        val instant = parseIso(iso) ?: return null
        return DATE_TIME.format(instant.atZone(zoneId))
    }

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

    private fun parseIso(value: String?): Instant? {
        if (value.isNullOrBlank()) return null
        return runCatching { Instant.parse(value) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
    }

    private val DATE_TIME: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.US)
    private val INCIDENT_ID = Regex("^[A-Za-z0-9_-]+$")
}
