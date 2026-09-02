package com.lamuier.cursorT.util

import com.lamuier.cursorT.model.AgentTaskSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentTaskPresentationTest {
    @Test
    fun agentConversationUrl_onlyAllowsSafeBcId() {
        assertEquals(
            "https://cursor.com/agents?id=bc-finished-0001",
            AgentTaskPresentation.agentConversationUrl("bc-finished-0001"),
        )
        assertNull(AgentTaskPresentation.agentConversationUrl("https://evil.test"))
        assertNull(AgentTaskPresentation.agentConversationUrl("bc-finished-0001/../x"))
        assertFalse(AgentTaskPresentation.isSafeBcId(""))
        assertTrue(AgentTaskPresentation.isSafeBcId("bc_abc123"))
    }

    @Test
    fun isSafeCursorUrl_allowsAgentsIdQuery() {
        assertTrue(AgentTaskPresentation.isSafeCursorUrl("https://cursor.com/agents"))
        assertTrue(AgentTaskPresentation.isSafeCursorUrl("https://cursor.com/agents?id=bc-finished-0001"))
        assertFalse(AgentTaskPresentation.isSafeCursorUrl("https://cursor.com/agents?id=bc-finished-0001&next=https://evil.test"))
        assertFalse(AgentTaskPresentation.isSafeCursorUrl("https://cursor.com/agents?next=https://evil.test"))
        assertFalse(AgentTaskPresentation.isSafeCursorUrl("http://cursor.com/agents?id=bc-finished-0001"))
    }

    @Test
    fun isAllowedCustomTabUrl_allowsCursorAgentsAndGithubPr() {
        assertTrue(AgentTaskPresentation.isAllowedCustomTabUrl("https://cursor.com/agents"))
        assertTrue(AgentTaskPresentation.isAllowedCustomTabUrl("https://cursor.com/agents?id=bc-finished-0001"))
        assertTrue(AgentTaskPresentation.isAllowedCustomTabUrl("https://github.com/example/demo/pull/12"))
        assertFalse(AgentTaskPresentation.isAllowedCustomTabUrl("https://evil.test/agents?id=bc-finished-0001"))
        assertFalse(AgentTaskPresentation.isAllowedCustomTabUrl("https://github.com/example/demo/pull/12?next=https://evil.test"))
    }

    @Test
    fun sourceLabel_coversGrokBotAndWebsite() {
        assertEquals("Grok Bot", AgentTaskPresentation.sourceLabel(AgentTaskSource.GrokBot))
        assertEquals("网页", AgentTaskPresentation.sourceLabel(AgentTaskSource.Website))
        assertEquals("其他来源", AgentTaskPresentation.sourceLabel(AgentTaskSource.Unknown))
    }

    @Test
    fun repoDisplayName_stripsHostAndGitSuffix() {
        assertEquals("example/demo", AgentTaskPresentation.repoDisplayName("github.com/example/demo"))
        assertEquals("example/demo", AgentTaskPresentation.repoDisplayName("https://github.com/example/demo.git"))
        assertEquals("acme/app", AgentTaskPresentation.repoDisplayName("https://gitlab.com/acme/app"))
        assertNull(AgentTaskPresentation.repoDisplayName("  "))
        assertNull(AgentTaskPresentation.repoDisplayName(null))
    }

    @Test
    fun formatRelative_oldTimestampIncludesZone() {
        val now = java.time.Instant.parse("2026-08-25T00:00:00Z").toEpochMilli()
        val old = java.time.Instant.parse("2026-08-15T00:00:00Z").toEpochMilli()
        assertEquals(
            "08-15 00:00 GMT",
            AgentTaskPresentation.formatRelative(old, nowMs = now, zone = java.time.ZoneOffset.UTC),
        )
        assertEquals("刚刚", AgentTaskPresentation.formatRelative(now - 1_000L, nowMs = now))
    }
}
