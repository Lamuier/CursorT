package com.lamuier.cursorT.util

import com.lamuier.cursorT.model.AgentTask
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
    fun groups(
        tasks: List<AgentTask>,
        mode: TaskGroupMode,
        nowMs: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<AgentTaskGroup> {
        if (tasks.isEmpty()) return emptyList()
        return when (mode) {
            TaskGroupMode.Repository -> groupByRepository(tasks)
            TaskGroupMode.Status -> groupByStatus(tasks)
            TaskGroupMode.Recency -> groupByRecency(tasks, nowMs, zone)
            TaskGroupMode.Source -> groupBySource(tasks)
        }
    }

    private fun groupByRepository(tasks: List<AgentTask>): List<AgentTaskGroup> {
        val buckets = linkedMapOf<String, MutableList<AgentTask>>()
        val titles = linkedMapOf<String, String>()
        tasks.forEach { task ->
            val display = AgentTaskPresentation.repoDisplayName(task.repoUrl)
            val key = display?.lowercase().orEmpty().ifBlank { UNASSIGNED_REPO_KEY }
            val title = display?.takeIf { it.isNotBlank() } ?: UNASSIGNED_REPO_TITLE
            buckets.getOrPut(key) { mutableListOf() }.add(task)
            titles.putIfAbsent(key, title)
        }
        return orderedGroups(buckets, titles)
    }

    private fun groupByStatus(tasks: List<AgentTask>): List<AgentTaskGroup> {
        val byStatus = tasks.groupBy { it.status }
        return STATUS_ORDER.mapNotNull { status ->
            val items = byStatus[status].orEmpty()
            if (items.isEmpty()) {
                null
            } else {
                AgentTaskGroup(
                    key = "status:${status.name}",
                    title = AgentTaskPresentation.statusLabel(status),
                    tasks = items.sortedByDescending { activityMs(it) },
                )
            }
        }
    }

    private fun groupByRecency(
        tasks: List<AgentTask>,
        nowMs: Long,
        zone: ZoneId,
    ): List<AgentTaskGroup> {
        val byBucket = tasks.groupBy { recencyBucket(activityMs(it), nowMs, zone) }
        return RecencyBucket.entries.mapNotNull { bucket ->
            val items = byBucket[bucket].orEmpty()
            if (items.isEmpty()) {
                null
            } else {
                AgentTaskGroup(
                    key = "recency:${bucket.name}",
                    title = bucket.label,
                    tasks = items.sortedByDescending { activityMs(it) },
                )
            }
        }
    }

    private fun groupBySource(tasks: List<AgentTask>): List<AgentTaskGroup> {
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
                    title = AgentTaskPresentation.sourceLabel(source),
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

    internal fun activityMs(task: AgentTask): Long =
        sequenceOf(task.lastActivityMs ?: 0L, task.updatedAtMs, task.createdAtMs)
            .firstOrNull { it > 0L } ?: 0L

    enum class RecencyBucket(val label: String) {
        Today("今天"),
        Yesterday("昨天"),
        Last7Days("近 7 天"),
        Older("更早"),
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
