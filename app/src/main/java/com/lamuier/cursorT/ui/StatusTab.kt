package com.lamuier.cursorT.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lamuier.cursorT.model.ComponentStatus
import com.lamuier.cursorT.model.CursorServiceStatus
import com.lamuier.cursorT.model.StatusIncident
import com.lamuier.cursorT.model.StatusIndicator
import com.lamuier.cursorT.ui.theme.LocalDisplayZone
import com.lamuier.cursorT.ui.theme.LocalPulseChartColors
import com.lamuier.cursorT.util.DisplayTime
import com.lamuier.cursorT.util.StatusPresentation

@Composable
internal fun StatusTab(
    status: CursorServiceStatus?,
    loading: Boolean,
    refreshing: Boolean,
    error: String?,
    onRetry: () -> Unit,
) {
    when {
        status == null && loading -> DashboardState(
            icon = null,
            title = "正在获取 Cursor 状态",
            description = "正在连接官方状态页…",
            loading = true,
        )
        status == null -> DashboardState(
            icon = Icons.Outlined.CloudOff,
            title = "暂时无法显示状态",
            description = error ?: "请检查网络后重试。",
            primaryActionLabel = "重新加载",
            onPrimaryAction = onRetry,
        )
        else -> StatusContent(
            status = status,
            error = error?.takeIf { !refreshing },
        )
    }
}

@Composable
private fun StatusContent(
    status: CursorServiceStatus,
    error: String?,
) {
    val uriHandler = LocalUriHandler.current
    val accent = indicatorColor(status.indicator)
    AdaptiveTabContent { compact ->
        OverallStatusHero(
            status = status,
            accent = accent,
            compact = compact,
        )
        if (error != null) {
            Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        SectionHeading(
            icon = Icons.Outlined.CheckCircle,
            title = "服务组件",
            supporting = "来自 Cursor 官方状态页",
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            shadowElevation = 1.dp,
        ) {
            Column(modifier = Modifier.padding(if (compact) 6.dp else 8.dp)) {
                status.components.forEachIndexed { index, component ->
                    ComponentRow(
                        name = component.name,
                        status = component.status,
                    )
                    if (index < status.components.lastIndex) {
                        Spacer(Modifier.height(2.dp))
                    }
                }
            }
        }

        if (status.activeIncidents.isNotEmpty()) {
            SectionHeading(
                icon = Icons.Outlined.ReportProblem,
                title = "当前事件",
                supporting = "${status.activeIncidents.size} 项进行中",
            )
            status.activeIncidents.forEach { incident ->
                IncidentCard(incident, compact, uriHandler)
            }
        }

        if (status.scheduledMaintenances.isNotEmpty()) {
            SectionHeading(
                icon = Icons.Outlined.Build,
                title = "计划维护",
                supporting = "${status.scheduledMaintenances.size} 项",
            )
            status.scheduledMaintenances.forEach { incident ->
                IncidentCard(incident, compact, uriHandler)
            }
        }

        SectionHeading(
            icon = Icons.Outlined.History,
            title = "近期事件",
            supporting = if (status.partialHistory) "历史记录不完整" else "含已恢复的事件",
        )
        if (status.recentIncidents.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Text(
                    "暂无近期事件",
                    modifier = Modifier.padding(if (compact) 14.dp else 18.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            status.recentIncidents.forEach { incident ->
                IncidentCard(incident, compact, uriHandler)
            }
        }

        TextButton(
            onClick = { openSafeUrl(uriHandler, status.pageUrl) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.size(6.dp))
            Text("打开官方状态页")
        }
        StatusFreshnessRow(status)
    }
}

@Composable
private fun OverallStatusHero(
    status: CursorServiceStatus,
    accent: Color,
    compact: Boolean,
) {
    val heroBrush = Brush.linearGradient(
        listOf(
            accent.copy(alpha = 0.18f),
            MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    )
    val icon = when (status.indicator) {
        StatusIndicator.None -> Icons.Outlined.CheckCircle
        StatusIndicator.Minor -> Icons.Outlined.WarningAmber
        StatusIndicator.Major, StatusIndicator.Critical -> Icons.Outlined.ErrorOutline
        StatusIndicator.Maintenance -> Icons.Outlined.Build
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "Cursor 当前状态：${StatusPresentation.indicatorLabel(status.indicator)}"
            },
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier
                .background(heroBrush)
                .padding(if (compact) 16.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(if (compact) 44.dp else 52.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(if (compact) 24.dp else 28.dp),
                        tint = accent,
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Cursor 可用状态",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        StatusPresentation.indicatorLabel(status.indicator),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                }
            }
            val degraded = status.components.count { it.status != ComponentStatus.Operational }
            Text(
                if (degraded == 0) {
                    "全部 ${status.components.size} 个组件运行正常"
                } else {
                    "${degraded} 个组件异常 · ${status.components.size - degraded} 个正常"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ComponentRow(name: String, status: ComponentStatus) {
    val color = componentColor(status)
    val label = StatusPresentation.componentLabel(status)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "$name $label" }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IncidentCard(
    incident: StatusIncident,
    compact: Boolean,
    uriHandler: UriHandler,
) {
    val url = StatusPresentation.incidentUrl(incident.shortlink, incident.id)
    val latest = incident.updates.firstOrNull()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (url != null) {
                    Modifier
                        .semantics { role = Role.Button }
                        .clickable { openSafeUrl(uriHandler, url) }
                } else {
                    Modifier
                },
            ),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shadowElevation = 1.dp,
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 14.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    incident.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (url != null) {
                    Icon(
                        Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = "在浏览器中打开事件",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StatusChip(label = StatusPresentation.incidentStatusLabel(incident.status))
                if (incident.impact.isNotBlank() && incident.impact != "none") {
                    StatusChip(label = StatusPresentation.impactLabel(incident.impact))
                }
                StatusPresentation.formatInstant(
                    incident.updatedAt ?: incident.resolvedAt ?: incident.createdAt,
                    LocalDisplayZone.current,
                )
                    ?.let { StatusChip(label = it) }
            }
            if (incident.affectedComponents.isNotEmpty()) {
                Text(
                    incident.affectedComponents.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            latest?.body?.takeIf { it.isNotBlank() }?.let { body ->
                Text(
                    body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun StatusFreshnessRow(status: CursorServiceStatus) {
    val stamp = DisplayTime.formatStoredClock(status.fetchedAt, LocalDisplayZone.current)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            if (status.fromCache) Icons.Outlined.Cached else Icons.Outlined.CloudDone,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            buildString {
                append(if (status.fromCache) "缓存数据" else "已更新")
                stamp?.let { append(" · $it") }
                append(" · 下拉可刷新")
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun indicatorColor(indicator: StatusIndicator): Color {
    val chart = LocalPulseChartColors.current
    return when (indicator) {
        StatusIndicator.None -> chart.healthy
        StatusIndicator.Minor -> chart.warning
        StatusIndicator.Major, StatusIndicator.Critical -> chart.critical
        StatusIndicator.Maintenance -> MaterialTheme.colorScheme.tertiary
    }
}

@Composable
private fun componentColor(status: ComponentStatus): Color {
    val chart = LocalPulseChartColors.current
    return when (status) {
        ComponentStatus.Operational -> chart.healthy
        ComponentStatus.DegradedPerformance, ComponentStatus.UnderMaintenance -> chart.warning
        ComponentStatus.PartialOutage, ComponentStatus.MajorOutage, ComponentStatus.Unknown -> chart.critical
    }
}

private fun openSafeUrl(uriHandler: UriHandler, url: String) {
    if (StatusPresentation.isSafeStatusUrl(url)) {
        runCatching { uriHandler.openUri(url) }
    }
}
