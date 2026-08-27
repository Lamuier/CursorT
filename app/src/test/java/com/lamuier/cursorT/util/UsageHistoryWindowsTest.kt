package com.lamuier.cursorT.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneOffset
import java.time.YearMonth

class UsageHistoryWindowsTest {
    @Test
    fun previousBillingCycle_shiftsBySameDuration() {
        val start = 1_724_630_400_000L
        val end = start + 30L * 24 * 60 * 60 * 1000
        val range = UsageHistoryWindows.previousBillingCycle(start, end)
        assertNotNull(range)
        assertEquals(start - (end - start), range!!.startMs)
        assertEquals(start - 1L, range.endMs)
    }

    @Test
    fun previousBillingCycle_rejectsInvalid() {
        assertNull(UsageHistoryWindows.previousBillingCycle(null, 1L))
        assertNull(UsageHistoryWindows.previousBillingCycle(10L, 10L))
        assertNull(UsageHistoryWindows.previousBillingCycle(20L, 10L))
    }

    @Test
    fun calendarMonth_usesLocalBoundsAndClampsToNow() {
        val zone = ZoneOffset.UTC
        val month = YearMonth.of(2026, 8)
        val now = month.atDay(10).atStartOfDay(zone).toInstant().toEpochMilli()
        val range = UsageHistoryWindows.calendarMonth(month, nowMs = now, zone = zone)
        assertEquals(month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli(), range.startMs)
        assertEquals(now, range.endMs)
        assertTrue(range.endMs >= range.startMs)
    }

    @Test
    fun parseEpochMillis_acceptsSecondsAndMillis() {
        assertEquals(1_724_630_400_000L, UsageHistoryWindows.parseEpochMillis("1724630400000"))
        assertEquals(1_724_630_400_000L, UsageHistoryWindows.parseEpochMillis(1_724_630_400L))
    }
}
