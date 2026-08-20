package com.lamuier.cursorT.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskCreatePayloadTest {
    @Test
    fun webSnapshot_includesPromptAndRepo() {
        val payload = TaskCreatePayload.webSnapshot(
            bcId = "bc-aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
            prompt = "给 README 加上安装说明",
            snapshotRepo = "github.com/example/demo",
            httpsRepo = "https://github.com/example/demo",
            ref = "main",
            modelName = "cursor-grok-4.6-high",
            autoCreatePr = true,
        )
        assertEquals("bc-aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", payload.getString("bcId"))
        assertEquals("github.com/example/demo", payload.getString("repoUrl"))
        assertTrue(payload.getBoolean("autoCreatePr"))
        val history = payload.getJSONArray("conversationHistory")
        assertEquals(1, history.length())
        assertEquals("给 README 加上安装说明", history.getJSONObject(0).getString("text"))
        assertEquals("MESSAGE_TYPE_HUMAN", history.getJSONObject(0).getString("type"))
        assertEquals(
            "cursor-grok-4.6-high",
            payload.getJSONObject("modelDetails").getString("modelName"),
        )
    }

    @Test
    fun cloudAgent_omitsBlankRef() {
        val payload = TaskCreatePayload.cloudAgent(
            prompt = "修登录",
            httpsRepo = "https://github.com/example/demo",
            ref = "  ",
            autoCreatePr = false,
        )
        assertEquals("修登录", payload.getJSONObject("prompt").getString("text"))
        assertEquals(
            "https://github.com/example/demo",
            payload.getJSONObject("source").getString("repository"),
        )
        assertTrue(!payload.getJSONObject("source").has("ref"))
        assertEquals(false, payload.getJSONObject("target").getBoolean("autoCreatePr"))
    }

    @Test
    fun createdBcId_prefersResponseId() {
        val payload = org.json.JSONObject("""{"id":"bc_abc123","requestedBcId":"bc-local"}""")
        assertEquals("bc_abc123", TaskCreatePayload.createdBcId(payload, "bc-fallback"))
    }

    @Test
    fun newBcId_matchesSafePattern() {
        val id = TaskCreatePayload.newBcId()
        assertTrue(id.startsWith("bc-"))
        assertTrue(com.lamuier.cursorT.util.AgentTaskPresentation.isSafeBcId(id))
    }
}
