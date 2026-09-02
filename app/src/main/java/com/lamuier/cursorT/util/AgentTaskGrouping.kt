package com.lamuier.cursorT.util

import android.content.res.Resources
import androidx.annotation.StringRes
import com.lamuier.cursorT.R
import com.lamuier.cursorT.model.AgentTask
import com.lamuier.cursorT.model.AgentTaskPrStatus
import com.lamuier.cursorT.model.AgentTaskSource
import com.lamuier.cursorT.model.AgentTaskStatus
import com.lamuier.cursorT.model.TaskGroupMode
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class AgentTaskGroup(
    val key: String,
    val title: String,
    val tasks: List<AgentTask>,
)

object AgentTaskGrouping {
    fun visibleTasks(tasks: List<AgentTask>): List<AgentTask> =
        tasks.filter { it.prStatus != AgentTaskPrStatus.Merged }

    fun groups(
        tasks: List<AgentTask>,
        mode: TaskGroupMode,
        nowMs: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
        resources: Resources? = null,
    ): List<AgentTaskGroup> {
        if (tasks.isEmpty()) return emptyList()
        return when (mode) {
            TaskGroupMode.Repository -> groupByRepository(tasks, resources)
            TaskGroupMode.Status -> groupByStatus(tasks, resources)
            TaskGroupMode.Recency -> groupByRecency(tasks, nowMs, zone, resources)
            TaskGroupMode.Source -> groupBySource(tasks, resources)
        }
    }

    private fun groupByRepository(tasks: List<AgentTask>, resources: Resources?): List<AgentTaskGroup> {
        val buckets = linkedMapOf<String, MutableList<AgentTask>>()
        val titles = linkedMapOf<String, String>()
        tasks.forEach { task ->
            val display = AgentTaskPresentation.repoDisplayName(task.repoUrl)
            val key = display?.lowercase().orEmpty().ifBlank { UNASSIGNED_REPO_KEY }
            val title = display?.takeIf { it.isNotBlank() }
                ?: (resources?.getString(R.string.task_unassigned_repo) ?: UNASSIGNED_REPO_TITLE)
            buckets.getOrPut(key) { mutableListOf() }.add(task)
            titles.putIfAbsent(key, title)
        }
        return orderedGroups(buckets, titles)
    }

    private fun groupByStatus(tasks: List<AgentTask>, resources: Resources?): List<AgentTaskGroup> {
        val byStatus = tasks.groupBy { it.status }
        return STATUS_ORDER.mapNotNull { status ->
            val items = byStatus[status].orEmpty()
            if (items.isEmpty()) {
                null
            } else {
                AgentTaskGroup(
                    key = "status:${status.name}",
                    title = AgentTaskPresentation.statusLabel(status, resources),
                    tasks = items.sortedByDescending { activityMs(it) },
                )
            }
        }
    }

    private fun groupByRecency(
        tasks: List<AgentTask>,
        nowMs: Long,
        zone: ZoneId,
        resources: Resources?,
    ): List<AgentTaskGroup> {
        val byBucket = tasks.groupBy { recencyBucket(activityMs(it), nowMs, zone) }
        return RecencyBucket.entries.mapNotNull { bucket ->
            val items = byBucket[bucket].orEmpty()
            if (items.isEmpty()) {
                null
            } else {
                AgentTaskGroup(
                    key = "recency:${bucket.name}",
                    title = resources?.getString(bucket.titleRes) ?: bucket.label,
                    tasks = items.sortedByDescending { activityMs(it) },
                )
            }
        }
    }

    private fun groupBySource(tasks: List<AgentTask>, resources: Resources?): List<AgentTaskGroup> {
        val bySource = tasks.groupBy { it.source }
        val ordered = buildList {
            add(AgentTaskSource.GrokBot)
            SOURCE_ORDER.forEach { if (it != AgentTaskSource.GrokBot) add(it) }
        }
        return ordered.mapNotNull { source ->
            val items = bySource[source].orEmpty()
            if (items.isEmpty()) {
                null
            } else {
                AgentTaskGroup(
                    key = "source:${source.name}",
                    title = AgentTaskPresentation.sourceLabel(source, resources),
                    tasks = items.sortedByDescending { activityMs(it) },
                )
            }
        }
    }

    private fun orderedGroups(
        buckets: Map<String, List<AgentTask>>,
        titles: Map<String, String>,
    ): List<AgentTaskGroup> =
        buckets.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, List<AgentTask>>> { entry ->
                    entry.value.maxOf { activityMs(it) }
                }.thenBy { titles[it.key].orEmpty().lowercase() },
            )
            .map { (key, items) ->
                AgentTaskGroup(
                    key = "repo:$key",
                    title = titles[key] ?: UNASSIGNED_REPO_TITLE,
                    tasks = items.sortedByDescending { activityMs(it) },
                )
            }

    internal fun recencyBucket(epochMs: Long, nowMs: Long, zone: ZoneId): RecencyBucket {
        if (epochMs <= 0L) return RecencyBucket.Older
        val nowDate = Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate()
        val thenDate = Instant.ofEpochMilli(epochMs).atZone(zone).toLocalDate()
        val days = ChronoUnit.DAYS.between(thenDate, nowDate)
        return when {
            days <= 0L -> RecencyBucket.Today
            days == 1L -> RecencyBucket.Yesterday
            days < 7L -> RecencyBucket.Last7Days
            else -> RecencyBucket.Older
        }
    }

    internal fun activityMs(task: AgentTask): Long = task.latestTimeMs

    enum class RecencyBucket(@StringRes val titleRes: Int, val label: String) {
        Today(R.string.task_recency_today, "今天"),
        Yesterday(R.string.task_recency_yesterday, "昨天"),
        Last7Days(R.string.task_recency_last_7_days, "近 7 天"),
        Older(R.string.task_recency_older, "更早"),
    }

    private val STATUS_ORDER = listOf(
        AgentTaskStatus.Running,
        AgentTaskStatus.Creating,
        AgentTaskStatus.Error,
        AgentTaskStatus.Finished,
        AgentTaskStatus.Expired,
        AgentTaskStatus.Unknown,
    )

    private val SOURCE_ORDER = AgentTaskSource.entries

    const val UNASSIGNED_REPO_KEY = "_unassigned"
    const val UNASSIGNED_REPO_TITLE = "未关联仓库"
}
