package com.lamuier.cursorT.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lamuier.cursorT.R
import com.lamuier.cursorT.data.DashboardPreferences
import com.lamuier.cursorT.model.AgentTask
import com.lamuier.cursorT.model.AgentTaskPrStatus
import com.lamuier.cursorT.model.AgentTaskSource
import com.lamuier.cursorT.model.AgentTaskStatus
import com.lamuier.cursorT.model.CursorTasks
import com.lamuier.cursorT.model.TaskGroupMode
import com.lamuier.cursorT.ui.theme.LocalDisplayZone
import com.lamuier.cursorT.ui.theme.LocalPulseChartColors
import com.lamuier.cursorT.util.AgentTaskGrouping
import com.lamuier.cursorT.util.AgentTaskPresentation
import com.lamuier.cursorT.util.CursorCustomTabs
import com.lamuier.cursorT.util.DisplayTime
import java.util.Locale

@Composable
internal fun TasksTab(
    tasks: CursorTasks?,
    loading: Boolean,
    refreshing: Boolean,
    error: String?,
    onRetry: () -> Unit,
    onOpenFailed: (String) -> Unit,
) {
    val openUrl = rememberOpenCursorUrl(onOpenFailed)
    when {
        tasks == null && loading -> DashboardState(
            icon = null,
            title = stringResource(R.string.tasks_loading_title),
            description = stringResource(R.string.tasks_loading_body),
            loading = true,
        )
        tasks == null -> DashboardState(
            icon = Icons.Outlined.CloudOff,
            title = stringResource(R.string.tasks_unavailable_title),
            description = error ?: stringResource(R.string.network_retry_hint),
            primaryActionLabel = stringResource(R.string.action_reload),
            onPrimaryAction = onRetry,
        )
        else -> TasksContent(
            tasks = tasks,
            error = error?.takeIf { !refreshing },
            openUrl = openUrl,
            onOpenFailed = onOpenFailed,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TasksContent(
    tasks: CursorTasks,
    error: String?,
    openUrl: (String) -> Unit,
    onOpenFailed: (String) -> Unit,
) {
    val context = LocalContext.current
    val preferences = remember { DashboardPreferences.get(context) }
    val groupMode by preferences.taskGroupMode.collectAsStateWithLifecycle()
    val zone = LocalDisplayZone.current
    val resources = context.resources
    val visibleTasks = remember(tasks.tasks) { AgentTaskGrouping.visibleTasks(tasks.tasks) }
    val hiddenMergedCount = tasks.tasks.size - visibleTasks.size
    val activeCount = visibleTasks.count {
        it.status == AgentTaskStatus.Running || it.status == AgentTaskStatus.Creating
    }
    val grokBotCount = visibleTasks.count { it.source == AgentTaskSource.GrokBot }
    val groups = remember(visibleTasks, groupMode, zone, resources) {
        AgentTaskGrouping.groups(visibleTasks, groupMode, zone = zone, resources = resources)
    }
    var collapsed by remember(groupMode, tasks.accountId) { mutableStateOf(emptySet<String>()) }
    AdaptiveTabContent { compact ->
        SectionHeading(
            icon = Icons.Outlined.SmartToy,
            title = stringResource(R.string.tasks_heading),
            supporting = when {
                tasks.tasks.isEmpty() -> stringResource(R.string.tasks_empty_records)
                visibleTasks.isEmpty() -> stringResource(R.string.tasks_hidden_merged_only, hiddenMergedCount)
                else -> stringResource(R.string.tasks_summary, visibleTasks.size, activeCount) +
                    (if (grokBotCount > 0) stringResource(R.string.tasks_summary_grok, grokBotCount) else "") +
                    (if (hiddenMergedCount > 0) stringResource(R.string.tasks_summary_hidden, hiddenMergedCount) else "")
            },
        )
        if (error != null) {
            Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (visibleTasks.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Text(
                    if (hiddenMergedCount > 0) {
                        stringResource(R.string.tasks_empty_all_merged)
                    } else {
                        stringResource(R.string.tasks_empty_none)
                    },
                    modifier = Modifier.padding(if (compact) 14.dp else 18.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TaskGroupMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = groupMode == mode,
                        onClick = { preferences.setTaskGroupMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = TaskGroupMode.entries.size,
                        ),
                        icon = {},
                    ) {
                        Text(stringResource(mode.labelRes), maxLines = 1)
                    }
                }
            }
            groups.forEach { group ->
                val expanded = group.key !in collapsed
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
                ) {
                    TaskGroupHeader(
                        title = group.title,
                        count = group.tasks.size,
                        activeCount = group.tasks.count {
                            it.status == AgentTaskStatus.Running ||
                                it.status == AgentTaskStatus.Creating
                        },
                        expanded = expanded,
                        onToggle = {
                            collapsed = if (expanded) {
                                collapsed + group.key
                            } else {
                                collapsed - group.key
                            }
                        },
                    )
                    if (expanded) {
                        group.tasks.forEach { task ->
                            TaskCard(task, compact, openUrl, onOpenFailed)
                        }
                    }
                }
            }
        }

        TextButton(
            onClick = { openUrl(AgentTaskPresentation.agentsPageUrl()) },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.size(6.dp))
            Text(stringResource(R.string.tasks_open_agents))
        }
        TasksFreshnessRow(tasks)
    }
}

@Composable
private fun TaskGroupHeader(
    title: String,
    count: Int,
    activeCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val groupCount = stringResource(R.string.tasks_group_count, count)
    val supporting = if (activeCount > 0) {
        groupCount + stringResource(R.string.tasks_group_active, activeCount)
    } else {
        groupCount
    }
    val description = stringResource(
        if (expanded) R.string.tasks_collapse_group else R.string.tasks_expand_group,
        title,
        supporting,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                heading()
                contentDescription = description
            }
            .clickable(onClick = onToggle)
            .padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            supporting,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TaskCard(
    task: AgentTask,
    compact: Boolean,
    openUrl: (String) -> Unit,
    onOpenFailed: (String) -> Unit,
) {
    val resources = LocalContext.current.resources
    val displayName = task.name.ifBlank { stringResource(R.string.task_untitled) }
    val statusLabel = AgentTaskPresentation.statusLabel(task.status, resources)
    val conversationA11y = stringResource(R.string.tasks_open_conversation_a11y, displayName, statusLabel)
    val prUrl = task.prUrl?.takeIf(AgentTaskPresentation::isSafeAgentUrl)
    val statusColor = taskStatusColor(task.status)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = conversationA11y
            }
            .clickable {
                val url = AgentTaskPresentation.agentConversationUrl(task.id)
                if (url == null) {
                    onOpenFailed(resources.getString(R.string.tasks_invalid_id))
                } else {
                    openUrl(url)
                }
            },
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
                    displayName,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                TaskChip(
                    label = statusLabel,
                    color = statusColor,
                )
                Icon(
                    Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val location = listOfNotNull(
                AgentTaskPresentation.repoDisplayName(task.repoUrl),
                task.branchName?.trim()?.takeIf { it.isNotBlank() },
            ).joinToString(" · ")
            if (location.isNotBlank()) {
                Text(
                    location,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (task.source != AgentTaskSource.Unknown) {
                    TaskChip(
                        label = AgentTaskPresentation.sourceLabel(task.source, resources),
                        color = if (task.source == AgentTaskSource.GrokBot) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        },
                    )
                }
                task.prStatus?.let { prStatus ->
                    TaskChip(
                        label = AgentTaskPresentation.prStatusLabel(prStatus, resources),
                        color = prStatusColor(prStatus),
                    )
                }
                if (task.linesAdded > 0 || task.linesRemoved > 0) {
                    TaskChip(
                        label = String.format(
                            Locale.US,
                            "+%,d / -%,d",
                            task.linesAdded,
                            task.linesRemoved,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (task.filesChanged > 0) {
                    TaskChip(
                        label = stringResource(R.string.tasks_files_changed, task.filesChanged),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AgentTaskPresentation.displayModel(task.modelName, task.maxMode)?.let { model ->
                    TaskChip(label = model, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val activity = AgentTaskPresentation.formatRelative(
                    task.latestTimeMs,
                    zone = LocalDisplayZone.current,
                    resources = resources,
                )
                activity?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (prUrl != null) {
                    val openPrDescription = stringResource(R.string.tasks_open_pr)
                    Spacer(Modifier.weight(1f))
                    Row(
                        modifier = Modifier
                            .semantics {
                                role = Role.Button
                                contentDescription = openPrDescription
                            }
                            .clickable { openUrl(prUrl) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            stringResource(R.string.tasks_view_pr),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun TaskChip(label: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.12f),
        contentColor = color,
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun TasksFreshnessRow(tasks: CursorTasks) {
    val stamp = DisplayTime.formatStoredClock(tasks.fetchedAt, LocalDisplayZone.current)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            if (tasks.fromCache) Icons.Outlined.Cached else Icons.Outlined.CloudDone,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            listOfNotNull(
                stringResource(if (tasks.fromCache) R.string.freshness_cached else R.string.freshness_updated),
                stamp,
                stringResource(R.string.freshness_pull_hint),
            ).joinToString(" · "),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun taskStatusColor(status: AgentTaskStatus): Color {
    val chart = LocalPulseChartColors.current
    return when (status) {
        AgentTaskStatus.Running -> MaterialTheme.colorScheme.primary
        AgentTaskStatus.Creating -> MaterialTheme.colorScheme.tertiary
        AgentTaskStatus.Finished -> chart.healthy
        AgentTaskStatus.Error -> chart.critical
        AgentTaskStatus.Expired, AgentTaskStatus.Unknown ->
            MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
internal fun prStatusColor(status: AgentTaskPrStatus): Color {
    val chart = LocalPulseChartColors.current
    return when (status) {
        AgentTaskPrStatus.Merged -> MaterialTheme.colorScheme.primary
        AgentTaskPrStatus.Open -> chart.healthy
        AgentTaskPrStatus.Draft -> chart.warning
        AgentTaskPrStatus.Closed, AgentTaskPrStatus.Unknown ->
            MaterialTheme.colorScheme.onSurfaceVariant
    }
}

@Composable
private fun rememberOpenCursorUrl(onOpenFailed: (String) -> Unit): (String) -> Unit {
    val context = LocalContext.current
    val toolbarColor = MaterialTheme.colorScheme.surface.toArgb()
    return remember(context, toolbarColor, onOpenFailed) {
        { url ->
            if (!CursorCustomTabs.open(context, url, toolbarColor)) {
                onOpenFailed(context.getString(R.string.tasks_open_page_failed))
            }
        }
    }
}
