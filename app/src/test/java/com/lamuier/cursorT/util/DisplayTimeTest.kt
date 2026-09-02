package com.lamuier.cursorT.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class DisplayTimeTest {
    @Test
    fun formatDateTime_appendsGmtOffset() {
        val instant = Instant.parse("2026-09-02T03:37:00Z")
        assertEquals(
            "09-02 03:37 GMT",
            DisplayTime.formatDateTime(instant, ZoneOffset.UTC, includeTime = true),
        )
        assertEquals(
            "09-02 11:37 GMT+8",
            DisplayTime.formatDateTime(instant, ZoneOffset.ofHours(8), includeTime = true),
        )
        assertEquals(
            "03:37 GMT",
            DisplayTime.formatClock(instant, ZoneOffset.UTC),
        )
    }

    @Test
    fun formatStoredClock_convertsNaiveLocalFromStorageZone() {
        val stored = "2026-09-02 11:37:00"
        val shanghai = ZoneOffset.ofHours(8)
        assertEquals("11:37 GMT+8", DisplayTime.formatStoredClock(stored, shanghai, shanghai))
        assertEquals("03:37 GMT", DisplayTime.formatStoredClock(stored, ZoneOffset.UTC, shanghai))
    }

    @Test
    fun parseStoredLocal_acceptsIsoInstant() {
        val instant = Instant.parse("2026-08-19T02:43:55.847Z")
        assertEquals(instant, DisplayTime.parseStoredLocal("2026-08-19T02:43:55.847Z"))
    }

    @Test
    fun timeZones_systemAndUnknownFallBack() {
        assertEquals(DisplayTimeZones.SYSTEM_ID, DisplayTimeZones.fromStorage(null))
        assertEquals(DisplayTimeZones.SYSTEM_ID, DisplayTimeZones.fromStorage("nope"))
        assertEquals("Asia/Shanghai", DisplayTimeZones.fromStorage("Asia/Shanghai"))
        assertEquals("UTC", DisplayTimeZones.resolve("UTC", nowZone = ZoneOffset.ofHours(8)).id)
        assertEquals(ZoneOffset.ofHours(8), DisplayTimeZones.resolve("system", nowZone = ZoneOffset.ofHours(8)))
    }

    @Test
    fun formatStoredDateTime_omitsClockWhenDateOnly() {
        val zone = ZoneOffset.ofHours(8)
        val local = LocalDateTime.of(2026, 7, 31, 0, 0).atZone(zone).toInstant()
        assertEquals(
            DisplayTime.formatDateTime(local, zone, includeTime = false),
            DisplayTime.formatStoredDateTime("2026-07-31", zone, zone),
        )
        assertNull(DisplayTime.formatStoredClock(" ", zone))
    }
}
