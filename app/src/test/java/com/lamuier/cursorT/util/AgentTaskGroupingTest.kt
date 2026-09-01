package com.lamuier.cursorT.util

import com.lamuier.cursorT.model.AgentTask
import com.lamuier.cursorT.model.AgentTaskSource
import com.lamuier.cursorT.model.AgentTaskStatus
import com.lamuier.cursorT.model.TaskGroupMode
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneOffset

class AgentTaskGroupingTest {
    @Test
    fun groupsByRepository_sortsByRecentActivityAndHidesHost() {
        val groups = AgentTaskGrouping.groups(
            tasks = listOf(
                task("a", repo = "github.com/acme/old", updated = 10, status = AgentTaskStatus.Finished),
                task("b", repo = "https://github.com/acme/new.git", updated = 30, status = AgentTaskStatus.Running),
                task("c", repo = null, updated = 20, status = AgentTaskStatus.Running),
            ),
            mode = TaskGroupMode.Repository,
        )
        assertEquals(listOf("acme/new", "未关联仓库", "acme/old"), groups.map { it.title })
        assertEquals(listOf("b"), groups[0].tasks.map { it.id })
        assertEquals(listOf("c"), groups[1].tasks.map { it.id })
    }

    @Test
    fun groupsByStatus_keepsRunningFirst() {
        val groups = AgentTaskGrouping.groups(
            tasks = listOf(
                task("done", status = AgentTaskStatus.Finished, updated = 50),
                task("run", status = AgentTaskStatus.Running, updated = 10),
                task("err", status = AgentTaskStatus.Error, updated = 40),
            ),
            mode = TaskGroupMode.Status,
        )
        assertEquals(listOf("运行中", "已出错", "已完成"), groups.map { it.title })
    }

    @Test
    fun groupsByRecency_usesLocalDayBuckets() {
        val now = 1_720_000_000_000L
        val zone = ZoneOffset.UTC
        val groups = AgentTaskGrouping.groups(
            tasks = listOf(
                task("today", updated = now - 3_600_000L),
                task("yesterday", updated = now - 86_400_000L - 3_600_000L),
                task("week", updated = now - 3L * 86_400_000L),
                task("old", updated = now - 20L * 86_400_000L),
            ),
            mode = TaskGroupMode.Recency,
            nowMs = now,
            zone = zone,
        )
        assertEquals(listOf("今天", "昨天", "近 7 天", "更早"), groups.map { it.title })
        assertEquals(listOf("today"), groups[0].tasks.map { it.id })
        assertEquals(listOf("yesterday"), groups[1].tasks.map { it.id })
        assertEquals(listOf("week"), groups[2].tasks.map { it.id })
        assertEquals(listOf("old"), groups[3].tasks.map { it.id })
    }

    @Test
    fun groupsBySource_pinsGrokBotFirst() {
        val groups = AgentTaskGrouping.groups(
            tasks = listOf(
                task("web", source = AgentTaskSource.Website, updated = 90),
                task("bot", source = AgentTaskSource.GrokBot, updated = 10),
                task("api", source = AgentTaskSource.Api, updated = 50),
            ),
            mode = TaskGroupMode.Source,
        )
        assertEquals(listOf("Grok Bot", "网页", "API"), groups.map { it.title })
        assertEquals(listOf("bot"), groups[0].tasks.map { it.id })
    }

    @Test
    fun taskGroupMode_fallsBackToRepository() {
        assertEquals(TaskGroupMode.Source, TaskGroupMode.fromStorage("source"))
        assertEquals(TaskGroupMode.Repository, TaskGroupMode.fromStorage("nope"))
        assertEquals(TaskGroupMode.Repository, TaskGroupMode.fromStorage(null))
    }

    private fun task(
        id: String,
        repo: String? = "github.com/example/demo",
        status: AgentTaskStatus = AgentTaskStatus.Finished,
        source: AgentTaskSource = AgentTaskSource.Website,
        updated: Long = 1L,
    ): AgentTask = AgentTask(
        id = id,
        name = id,
        status = status,
        repoUrl = repo,
        branchName = null,
        prUrl = null,
        prStatus = null,
        linesAdded = 0,
        linesRemoved = 0,
        filesChanged = 0,
        modelName = null,
        maxMode = false,
        createdAtMs = updated,
        updatedAtMs = updated,
        lastActivityMs = updated,
        source = source,
    )
}
