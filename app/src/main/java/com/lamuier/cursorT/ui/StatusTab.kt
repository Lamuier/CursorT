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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lamuier.cursorT.R
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
            title = stringResource(R.string.status_loading_title),
            description = stringResource(R.string.status_loading_body),
            loading = true,
        )
        status == null -> DashboardState(
            icon = Icons.Outlined.CloudOff,
            title = stringResource(R.string.status_unavailable_title),
            description = error ?: stringResource(R.string.network_retry_hint),
            primaryActionLabel = stringResource(R.string.action_reload),
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
            title = stringResource(R.string.status_components_title),
            supporting = stringResource(R.string.status_components_supporting),
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
                title = stringResource(R.string.status_current_events),
                supporting = stringResource(R.string.status_active_count, status.activeIncidents.size),
            )
            status.activeIncidents.forEach { incident ->
                IncidentCard(incident, compact, uriHandler)
            }
        }

        if (status.scheduledMaintenances.isNotEmpty()) {
            SectionHeading(
                icon = Icons.Outlined.Build,
                title = stringResource(R.string.status_scheduled_maintenance),
                supporting = stringResource(R.string.status_item_count, status.scheduledMaintenances.size),
            )
            status.scheduledMaintenances.forEach { incident ->
                IncidentCard(incident, compact, uriHandler)
            }
        }

        SectionHeading(
            icon = Icons.Outlined.History,
            title = stringResource(R.string.status_recent_events),
            supporting = if (status.partialHistory) {
                stringResource(R.string.status_history_partial)
            } else {
                stringResource(R.string.status_history_complete)
            },
        )
        if (status.recentIncidents.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Text(
                    stringResource(R.string.status_recent_empty),
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
            Text(stringResource(R.string.status_open_official))
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
    val resources = LocalContext.current.resources
    val indicatorLabel = StatusPresentation.indicatorLabel(status.indicator, resources)
    val heroA11y = stringResource(R.string.status_current_a11y, indicatorLabel)
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
                contentDescription = heroA11y
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
                        stringResource(R.string.status_availability),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        indicatorLabel,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                }
            }
            val degraded = status.components.count { it.status != ComponentStatus.Operational }
            Text(
                if (degraded == 0) {
                    stringResource(R.string.status_all_components_ok, status.components.size)
                } else {
                    stringResource(
                        R.string.status_degraded_components,
                        degraded,
                        status.components.size - degraded,
                    )
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
    val label = StatusPresentation.componentLabel(status, LocalContext.current.resources)
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
                        contentDescription = stringResource(R.string.status_open_incident),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                StatusChip(label = StatusPresentation.incidentStatusLabel(incident.status, LocalContext.current.resources))
                if (incident.impact.isNotBlank() && incident.impact != "none") {
                    StatusChip(label = StatusPresentation.impactLabel(incident.impact, LocalContext.current.resources))
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
            listOfNotNull(
                stringResource(if (status.fromCache) R.string.freshness_cached else R.string.freshness_updated),
                stamp,
                stringResource(R.string.freshness_pull_hint),
            ).joinToString(" · "),
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
