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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lamuier.cursorT.data.DashboardPreferences
import com.lamuier.cursorT.model.AgentTask
import com.lamuier.cursorT.model.AgentTaskPrStatus
import com.lamuier.cursorT.model.AgentTaskSource
import com.lamuier.cursorT.model.AgentTaskStatus
import com.lamuier.cursorT.model.CursorTasks
import com.lamuier.cursorT.model.TaskGroupMode
import com.lamuier.cursorT.ui.theme.LocalPulseChartColors
import com.lamuier.cursorT.util.AgentTaskGrouping
import com.lamuier.cursorT.util.AgentTaskPresentation
import com.lamuier.cursorT.util.CursorCustomTabs
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
            title = "正在获取云端任务",
            description = "正在连接 Cursor 官方接口…",
            loading = true,
        )
        tasks == null -> DashboardState(
            icon = Icons.Outlined.CloudOff,
            title = "暂时无法显示任务",
            description = error ?: "请检查网络后重试。",
            primaryActionLabel = "重新加载",
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
    val orderedTasks = remember(tasks.tasks) {
        tasks.tasks.sortedByDescending { it.latestTimeMs }
    }
    val activeCount = orderedTasks.count {
        it.status == AgentTaskStatus.Running || it.status == AgentTaskStatus.Creating
    }
    val grokBotCount = orderedTasks.count { it.source == AgentTaskSource.GrokBot }
    val groups = remember(orderedTasks, groupMode) {
        AgentTaskGrouping.groups(orderedTasks, groupMode)
    }
    var collapsed by remember(groupMode, tasks.accountId) { mutableStateOf(emptySet<String>()) }
    AdaptiveTabContent { compact ->
        SectionHeading(
            icon = Icons.Outlined.SmartToy,
            title = "云端任务",
            supporting = if (orderedTasks.isEmpty()) {
                "暂无任务记录"
            } else {
                buildString {
                    append("共 ${orderedTasks.size} 项 · $activeCount 项进行中")
                    if (grokBotCount > 0) append(" · $grokBotCount 项来自 Grok Bot")
                }
            },
        )
        if (error != null) {
            Text(
                error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (orderedTasks.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Text(
                    "暂无云端任务。可在 Cursor 网页或 Grok Bot 启动后台智能体后下拉刷新；点击任务会用 Chrome 打开官方对话页。",
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
                    ) {
                        Text(mode.label)
                    }
                }
            }
            groups.forEach { group ->
                val expanded = group.key !in collapsed
                TaskGroupHeader(
                    title = group.title,
                    count = group.tasks.size,
                    activeCount = group.tasks.count {
                        it.status == AgentTaskStatus.Running ||
                            it.status == AgentTaskStatus.Creating
                    },
                    expanded = expanded,
                    onToggle = {
                        collapsed = if (expanded) collapsed + group.key else collapsed - group.key
                    },
                )
                if (expanded) {
                    group.tasks.forEach { task ->
                        TaskCard(task, compact, openUrl, onOpenFailed)
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
            Text("在网页打开 Cursor Agents")
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
    val supporting = buildString {
        append("$count 项")
        if (activeCount > 0) append(" · $activeCount 项进行中")
    }
    val description = if (expanded) "收起 $title，$supporting" else "展开 $title，$supporting"
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
    val prUrl = task.prUrl?.takeIf(AgentTaskPresentation::isSafeAgentUrl)
    val statusColor = taskStatusColor(task.status)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = "${task.name}，${AgentTaskPresentation.statusLabel(task.status)}，在网页查看完整对话"
            }
            .clickable {
                val url = AgentTaskPresentation.agentConversationUrl(task.id)
                if (url == null) {
                    onOpenFailed("云端任务标识无效")
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
                    task.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                TaskChip(
                    label = AgentTaskPresentation.statusLabel(task.status),
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
                        label = AgentTaskPresentation.sourceLabel(task.source),
                        color = if (task.source == AgentTaskSource.GrokBot) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        },
                    )
                }
                task.prStatus?.let { prStatus ->
                    TaskChip(
                        label = AgentTaskPresentation.prStatusLabel(prStatus),
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
                        label = "${task.filesChanged} 个文件",
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
                val activity = AgentTaskPresentation.formatRelative(task.latestTimeMs)
                activity?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (prUrl != null) {
                    Spacer(Modifier.weight(1f))
                    Row(
                        modifier = Modifier
                            .semantics {
                                role = Role.Button
                                contentDescription = "在浏览器中打开 PR"
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
                            "查看 PR",
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
    val stamp = tasks.fetchedAt.takeIf { it.length >= 16 }?.substring(11, 16)
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
            buildString {
                append(if (tasks.fromCache) "缓存数据" else "已更新")
                stamp?.let { append(" · $it") }
                append(" · 下拉可刷新")
            },
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
                onOpenFailed("无法打开页面，请确认已安装 Chrome 或其他浏览器")
            }
        }
    }
}
