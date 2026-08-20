package com.lamuier.cursorT.util

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
    fun normalizeRepositoryUrl_acceptsGithubAndGitlab() {
        assertEquals(
            "https://github.com/example/demo",
            AgentTaskPresentation.normalizeRepositoryUrl("github.com/example/demo.git"),
        )
        assertEquals(
            "https://gitlab.com/group/sub/project",
            AgentTaskPresentation.normalizeRepositoryUrl("https://gitlab.com/group/sub/project"),
        )
        assertEquals(
            "github.com/example/demo",
            AgentTaskPresentation.snapshotRepository("https://github.com/example/demo"),
        )
        assertNull(AgentTaskPresentation.normalizeRepositoryUrl("http://github.com/example/demo"))
        assertNull(AgentTaskPresentation.normalizeRepositoryUrl("https://evil.test/example/demo"))
        assertNull(AgentTaskPresentation.normalizeRepositoryUrl("https://github.com/example/demo/issues/1"))
        assertNull(AgentTaskPresentation.normalizeRepositoryUrl("https://github.com/example/demo?next=https://evil.test"))
    }

    @Test
    fun isAllowedCustomTabUrl_allowsCursorAgentsAndGithubPr() {
        assertTrue(AgentTaskPresentation.isAllowedCustomTabUrl("https://cursor.com/agents"))
        assertTrue(AgentTaskPresentation.isAllowedCustomTabUrl("https://cursor.com/agents?id=bc-finished-0001"))
        assertTrue(AgentTaskPresentation.isAllowedCustomTabUrl("https://github.com/example/demo/pull/12"))
        assertFalse(AgentTaskPresentation.isAllowedCustomTabUrl("https://evil.test/agents?id=bc-finished-0001"))
        assertFalse(AgentTaskPresentation.isAllowedCustomTabUrl("https://github.com/example/demo/pull/12?next=https://evil.test"))
    }
}
