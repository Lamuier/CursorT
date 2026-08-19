package com.lamuier.cursorT.network

import com.lamuier.cursorT.model.AgentTask
import com.lamuier.cursorT.model.AgentTaskMessageRole
import com.lamuier.cursorT.model.AgentTaskStatus
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskConversationJsonParserTest {
    private val fallback = AgentTask(
        id = "bc-finished-0001",
        name = "Cursor可用性状态",
        status = AgentTaskStatus.Finished,
        repoUrl = null,
        branchName = null,
        prUrl = null,
        prStatus = null,
        linesAdded = 0,
        linesRemoved = 0,
        filesChanged = 0,
        modelName = null,
        maxMode = false,
        createdAtMs = 0L,
        updatedAtMs = 0L,
        lastActivityMs = null,
    )

    @Test
    fun parse_v0Messages_mergesConsecutiveAssistants() {
        val conversation = TaskConversationJsonParser.parse(
            payload = JSONObject(resource("agent_task_conversation_v0.json")),
            fallbackTask = fallback,
            accountId = 3,
            fetchedAt = "2026-08-19 12:00:00",
        )
        assertEquals("bc-finished-0001", conversation.task.id)
        assertEquals(2, conversation.messages.size)
        assertEquals(AgentTaskMessageRole.User, conversation.messages[0].role)
        assertEquals("给 README 加上安装说明", conversation.messages[0].text)
        assertEquals(AgentTaskMessageRole.Assistant, conversation.messages[1].role)
        assertTrue(conversation.messages[1].text.contains("先看一下项目结构。"))
        assertTrue(conversation.messages[1].text.contains("已经写好 README 的安装步骤。"))
    }

    @Test
    fun parse_detailedComposer_readsNestedHistory() {
        val conversation = TaskConversationJsonParser.parse(
            payload = JSONObject(resource("agent_task_conversation_detailed.json")),
            fallbackTask = fallback,
            accountId = 3,
            fetchedAt = "2026-08-19 12:00:00",
        )
        assertEquals(AgentTaskStatus.Finished, conversation.task.status)
        assertEquals("cursor/cursor-status-tab-f839", conversation.task.branchName)
        assertEquals(listOf("给 README 加上安装说明", "已经写好 README 的安装步骤。"), conversation.messages.map { it.text })
        assertEquals(
            listOf(AgentTaskMessageRole.User, AgentTaskMessageRole.Assistant),
            conversation.messages.map { it.role },
        )
    }

    @Test
    fun parse_emptyPayload_keepsFallbackTask() {
        val conversation = TaskConversationJsonParser.parse(
            payload = JSONObject(),
            fallbackTask = fallback,
            accountId = 1,
            fetchedAt = "2026-08-19 12:00:00",
        )
        assertEquals(fallback, conversation.task)
        assertTrue(conversation.messages.isEmpty())
    }

    @Test
    fun parse_skipsParticipantsAndEmptyToolBubbles() {
        val payload = JSONObject(
            """
            {
              "participants": [{"userId": 1, "displayName": "tester"}],
              "messages": [
                {"id": "u1", "type": "user_message", "text": "修复登录"},
                {"id": "t1", "type": 2, "text": ""},
                {"id": "a1", "type": 2, "text": "已修好。"}
              ]
            }
            """.trimIndent(),
        )
        val messages = TaskConversationJsonParser.parseMessages(payload)
        assertEquals(listOf("修复登录", "已修好。"), messages.map { it.text })
    }

    @Test
    fun parse_prefersFullBubblesOverSeedHistory() {
        val payload = JSONObject(
            """
            {
              "conversationHistory": [
                {"type": "MESSAGE_TYPE_HUMAN", "text": "只出现在创建快照里"}
              ],
              "bubbles": [
                {"type": 1, "text": "给 README 加上安装说明"},
                {"type": 2, "text": "已经写好 README 的安装步骤。"},
                {"type": 1, "text": "再补一个徽章"},
                {"type": 2, "text": "徽章已加上。"}
              ]
            }
            """.trimIndent(),
        )
        val messages = TaskConversationJsonParser.parseMessages(payload)
        assertEquals(
            listOf("给 README 加上安装说明", "已经写好 README 的安装步骤。", "再补一个徽章", "徽章已加上。"),
            messages.map { it.text },
        )
    }

    @Test
    fun parse_concatenatesHistoryAndFollowups() {
        val payload = JSONObject(
            """
            {
              "conversationHistory": [
                {"type": "MESSAGE_TYPE_HUMAN", "text": "初始任务"}
              ],
              "followupMessages": [
                {"type": "MESSAGE_TYPE_AI", "text": "先处理初始任务。"},
                {"type": "MESSAGE_TYPE_HUMAN", "text": "继续"},
                {"type": "MESSAGE_TYPE_AI", "text": "已继续。"}
              ]
            }
            """.trimIndent(),
        )
        val messages = TaskConversationJsonParser.parseMessages(payload)
        assertEquals(
            listOf("初始任务", "先处理初始任务。", "继续", "已继续。"),
            messages.map { it.text },
        )
    }

    @Test
    fun parsePreferred_usesLongerExtraPayload() {
        val primary = JSONObject(
            """
            {"conversationHistory":[{"type":"MESSAGE_TYPE_HUMAN","text":"种子消息"}]}
            """.trimIndent(),
        )
        val extra = JSONObject(
            """
            {
              "messages": [
                {"id": "u1", "type": "user_message", "text": "种子消息"},
                {"id": "a1", "type": "assistant_message", "text": "助手完整回复"}
              ]
            }
            """.trimIndent(),
        )
        val conversation = TaskConversationJsonParser.parsePreferred(
            primary = primary,
            extras = listOf(extra),
            fallbackTask = fallback,
            accountId = 3,
            fetchedAt = "2026-08-19 12:00:00",
        )
        assertEquals(
            listOf("种子消息", "助手完整回复"),
            conversation.messages.map { it.text },
        )
    }

    @Test
    fun parse_turnsAndThinkingFallback() {
        val payload = JSONObject(
            """
            {
              "turns": [
                {"user": {"text": "修登录"}, "assistant": {"thinking": {"text": "先看鉴权再改重试。"}}}
              ]
            }
            """.trimIndent(),
        )
        val messages = TaskConversationJsonParser.parseMessages(payload)
        assertEquals(listOf("修登录", "先看鉴权再改重试。"), messages.map { it.text })
        assertEquals(
            listOf(AgentTaskMessageRole.User, AgentTaskMessageRole.Assistant),
            messages.map { it.role },
        )
    }

    @Test
    fun parse_lexicalRichText() {
        val payload = JSONObject(
            """
            {
              "conversationHistory": [{
                "type": "MESSAGE_TYPE_HUMAN",
                "richText": "{\"root\":{\"children\":[{\"children\":[{\"text\":\"来自富文本\",\"type\":\"text\"}],\"type\":\"paragraph\"}],\"type\":\"root\"}}"
              }]
            }
            """.trimIndent(),
        )
        val messages = TaskConversationJsonParser.parseMessages(payload)
        assertEquals("来自富文本", messages.single().text)
        assertEquals(AgentTaskMessageRole.User, messages.single().role)
    }

    private fun resource(name: String): String =
        requireNotNull(javaClass.classLoader?.getResourceAsStream(name)) { "missing $name" }
            .bufferedReader()
            .use { it.readText() }
}
