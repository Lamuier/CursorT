package com.lamuier.cursorusage.network

import com.lamuier.cursorusage.model.ComponentStatus
import com.lamuier.cursorusage.model.StatusIndicator
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatusJsonParserTest {
    @Test
    fun operationalSummary_skipsGroupComponents() {
        val status = StatusJsonParser.parse(
            summary = JSONObject(resource("status_summary_operational.json")),
            incidentsPayload = JSONObject(resource("status_incidents.json")),
            fetchedAt = "2026-08-19 12:00:00",
        )
        assertEquals(StatusIndicator.None, status.indicator)
        assertEquals("All Systems Operational", status.description)
        assertEquals(listOf("IDE", "Cloud Agents"), status.components.map { it.name })
        assertTrue(status.components.all { it.status == ComponentStatus.Operational })
        assertTrue(status.activeIncidents.isEmpty())
        assertEquals(listOf("inc-resolved"), status.recentIncidents.map { it.id })
        assertFalse(status.partialHistory)
        assertEquals("https://status.cursor.com", status.pageUrl)
    }

    @Test
    fun degradedSummary_keepsActiveIncidentAndHidesQuietComponent() {
        val status = StatusJsonParser.parse(
            summary = JSONObject(resource("status_summary_degraded.json")),
            incidentsPayload = JSONObject(resource("status_incidents.json")),
            fetchedAt = "2026-08-19 12:00:00",
        )
        assertEquals(StatusIndicator.Major, status.indicator)
        assertEquals(listOf("IDE", "Cloud Agents"), status.components.map { it.name })
        assertEquals(ComponentStatus.PartialOutage, status.components.single { it.id == "agents" }.status)
        assertEquals(listOf("inc-active"), status.activeIncidents.map { it.id })
        assertEquals("investigating", status.activeIncidents.single().status)
        assertEquals(listOf("Cloud Agents"), status.activeIncidents.single().affectedComponents)
        assertEquals(listOf("mnt-1"), status.scheduledMaintenances.map { it.id })
        assertEquals(listOf("inc-resolved"), status.recentIncidents.map { it.id })
    }

    @Test
    fun missingIncidentHistory_marksPartial() {
        val status = StatusJsonParser.parse(
            summary = JSONObject(resource("status_summary_operational.json")),
            incidentsPayload = null,
            fetchedAt = "2026-08-19 12:00:00",
            partialHistory = true,
        )
        assertTrue(status.partialHistory)
        assertTrue(status.recentIncidents.isEmpty())
    }

    @Test
    fun cacheRoundTrip_preservesSummaryAndHistory() {
        val summary = JSONObject(resource("status_summary_degraded.json"))
        val incidents = JSONObject(resource("status_incidents.json"))
        val cached = StatusJsonParser.toCacheJson(summary, incidents, "2026-08-19 12:00:00")
        val restored = StatusJsonParser.parseCache(cached, cacheAgeSeconds = 42)
        assertTrue(restored.fromCache)
        assertEquals(42, restored.cacheAgeSeconds)
        assertEquals(StatusIndicator.Major, restored.indicator)
        assertEquals("inc-active", restored.activeIncidents.single().id)
        assertEquals("inc-resolved", restored.recentIncidents.single().id)
        assertEquals("2026-08-19 12:00:00", restored.fetchedAt)
    }

    private fun resource(name: String): String =
        requireNotNull(javaClass.classLoader?.getResourceAsStream(name)) { "missing $name" }
            .bufferedReader()
            .use { it.readText() }
}
