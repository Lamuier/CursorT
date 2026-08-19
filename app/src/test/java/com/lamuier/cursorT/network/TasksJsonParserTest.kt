package com.lamuier.cursorT.network

import com.lamuier.cursorT.model.AgentTaskPrStatus
import com.lamuier.cursorT.model.AgentTaskStatus
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TasksJsonParserTest {
    @Test
    fun parse_mapsStatusesAndSortsByUpdateDesc() {
        val tasks = TasksJsonParser.parse(
            payload = JSONObject(resource("agent_tasks_list.json")),
            accountId = 3,
            fetchedAt = "2026-08-19 12:00:00",
        )
        assertEquals(3, tasks.accountId)
        // 缺少 bcId 的条目被跳过；按 updatedAtMs 降序。
        assertEquals(listOf("bc-running-0002", "bc-finished-0001"), tasks.tasks.map { it.id })

        val running = tasks.tasks.first()
        assertEquals(AgentTaskStatus.Running, running.status)
        assertEquals("修复登录重试", running.name)
        assertEquals("cursor/login-retry", running.branchName)
        assertNull(running.prUrl)
        assertNull(running.prStatus)
        assertNull(running.lastActivityMs)
        assertFalse(running.maxMode)

        val finished = tasks.tasks[1]
        assertEquals(AgentTaskStatus.Finished, finished.status)
        assertEquals(AgentTaskPrStatus.Merged, finished.prStatus)
        assertEquals("https://github.com/example/demo/pull/1", finished.prUrl)
        assertEquals(2812, finished.linesAdded)
        assertEquals(425, finished.linesRemoved)
        assertEquals(67, finished.filesChanged)
        assertEquals("cursor-grok-4.6-high", finished.modelName)
        assertTrue(finished.maxMode)
        assertEquals(1787144151489L, finished.lastActivityMs)
    }

    @Test
    fun parse_emptyPayload_returnsEmptyList() {
        val tasks = TasksJsonParser.parse(JSONObject(), accountId = 1)
        assertTrue(tasks.tasks.isEmpty())
    }

    @Test
    fun parse_unknownStatus_degradesToUnknown() {
        val payload = JSONObject(
            """
            {"composers":[{"bcId":"bc-x","name":"t","status":"BACKGROUND_COMPOSER_STATUS_WHATEVER"}]}
            """.trimIndent(),
        )
        val tasks = TasksJsonParser.parse(payload, accountId = 1)
        assertEquals(AgentTaskStatus.Unknown, tasks.tasks.single().status)
    }

    @Test
    fun statusEnums_acceptPrefixedAndBareValues() {
        assertEquals(AgentTaskStatus.Running, TasksJsonParser.parseStatus("BACKGROUND_COMPOSER_STATUS_RUNNING"))
        assertEquals(AgentTaskStatus.Finished, TasksJsonParser.parseStatus("finished"))
        assertEquals(AgentTaskStatus.Unknown, TasksJsonParser.parseStatus(null))
        assertEquals(AgentTaskPrStatus.Merged, TasksJsonParser.parsePrStatus("PR_STATUS_MERGED"))
        assertEquals(AgentTaskPrStatus.Open, TasksJsonParser.parsePrStatus("open"))
        assertNull(TasksJsonParser.parsePrStatus(null))
    }

    @Test
    fun cacheRoundTrip_preservesTasks() {
        val parsed = TasksJsonParser.parse(
            payload = JSONObject(resource("agent_tasks_list.json")),
            accountId = 3,
            fetchedAt = "2026-08-19 12:00:00",
        )
        val restored = TasksJsonParser.parseStored(
            rawJson = TasksJsonParser.toJson(parsed),
            accountId = 3,
            fetchedAt = "2026-08-19 12:00:00",
            cacheAgeSeconds = 42,
        )
        assertTrue(restored.fromCache)
        assertEquals(42, restored.cacheAgeSeconds)
        assertEquals(parsed.tasks, restored.tasks)
    }

    private fun resource(name: String): String =
        requireNotNull(javaClass.classLoader?.getResourceAsStream(name)) { "missing $name" }
            .bufferedReader()
            .use { it.readText() }
}
