package com.lamuier.cursorT.util

import com.lamuier.cursorT.model.ComponentStatus
import com.lamuier.cursorT.model.StatusIndicator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset

class StatusPresentationTest {
    @Test
    fun labels_coverOfficialStatuspageValues() {
        assertEquals("全部系统正常", StatusPresentation.indicatorLabel(StatusIndicator.None))
        assertEquals("全部正常", StatusPresentation.compactIndicatorLabel(StatusIndicator.None))
        assertEquals("部分中断", StatusPresentation.compactIndicatorLabel(StatusIndicator.Major))
        assertEquals("部分服务中断", StatusPresentation.indicatorLabel(StatusIndicator.Major))
        assertEquals("正常", StatusPresentation.componentLabel(ComponentStatus.Operational))
        assertEquals("部分中断", StatusPresentation.componentLabel(ComponentStatus.PartialOutage))
        assertEquals("调查中", StatusPresentation.incidentStatusLabel("investigating"))
        assertEquals("已恢复", StatusPresentation.incidentStatusLabel("resolved"))
        assertEquals("轻微", StatusPresentation.impactLabel("minor"))
    }

    @Test
    fun formatInstant_usesUtcClockForKnownTimestamp() {
        assertEquals(
            "08-19 02:43",
            StatusPresentation.formatInstant("2026-08-19T02:43:55.847Z", ZoneOffset.UTC),
        )
    }

    @Test
    fun safeStatusUrls_allowOfficialHostsOnly() {
        assertTrue(StatusPresentation.isSafeStatusUrl("https://status.cursor.com"))
        assertTrue(StatusPresentation.isSafeStatusUrl("https://status.cursor.com/incidents/abc"))
        assertTrue(StatusPresentation.isSafeStatusUrl("https://stspg.io/btnkb0vddj40"))
        assertFalse(StatusPresentation.isSafeStatusUrl("http://status.cursor.com"))
        assertFalse(StatusPresentation.isSafeStatusUrl("https://evil.example/status.cursor.com"))
        assertFalse(StatusPresentation.isSafeStatusUrl("https://status.cursor.com.evil.test"))
        assertFalse(StatusPresentation.isSafeStatusUrl("https://status.cursor.com/?next=https://evil.test"))
        assertEquals(
            "https://status.cursor.com/incidents/abc",
            StatusPresentation.incidentUrl("https://evil.test/phish", "abc"),
        )
        assertNull(StatusPresentation.incidentUrl(null, "../etc"))
        assertEquals(
            "https://status.cursor.com/incidents/abc",
            StatusPresentation.incidentUrl(null, "abc"),
        )
    }
}
