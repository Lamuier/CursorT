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
            "09/02 03:37 GMT",
            DisplayTime.formatDateTime(instant, ZoneOffset.UTC, includeTime = true),
        )
        assertEquals(
            "09/02 11:37 GMT+8",
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
    fun timeZones_searchMatchesIdLabelAndAlias() {
        assertEquals(emptyList<DisplayTimeZones.Option>(), DisplayTimeZones.search("   "))
        assertEquals(emptyList<DisplayTimeZones.Option>(), DisplayTimeZones.search("跟随系统"))

        val shanghai = DisplayTimeZones.search("上海").map { it.id }
        assertEquals(listOf("Asia/Shanghai"), shanghai)
        assertEquals(listOf("Asia/Shanghai"), DisplayTimeZones.search("beijing").map { it.id })
        assertEquals(listOf("Asia/Tokyo"), DisplayTimeZones.search("东京").map { it.id })
        assertEquals(listOf("America/New_York"), DisplayTimeZones.search("纽约").map { it.id })
        assertEquals(listOf("Asia/Seoul"), DisplayTimeZones.search("Asia/Seoul").map { it.id })
        assertEquals(
            listOf("America/Los_Angeles"),
            DisplayTimeZones.search("los angeles").map { it.id },
        )
    }

    @Test
    fun formatRange_putsOffsetOnlyOnEnd() {
        val zone = ZoneOffset.ofHours(8)
        val start = LocalDateTime.of(2026, 9, 9, 15, 41).atZone(zone).toInstant()
        val end = LocalDateTime.of(2026, 10, 9, 15, 41).atZone(zone).toInstant()
        val parts = DisplayTime.formatRangeParts(
            start,
            end,
            zone,
            startIncludeTime = false,
            endIncludeTime = true,
        )
        assertEquals("09/09", parts.startDate)
        assertEquals(null, parts.startTime)
        assertEquals("10/09", parts.endDate)
        assertEquals("15:41", parts.endTime)
        assertEquals("GMT+8", parts.offset)
        assertEquals("09/09 — 10/09 15:41 GMT+8", parts.asLine())
        assertEquals(
            "09/09 — 10/09 15:41 GMT+8",
            DisplayTime.formatRange(start, end, zone, startIncludeTime = false, endIncludeTime = true),
        )
        val nextYear = LocalDateTime.of(2027, 1, 1, 0, 0).atZone(zone).toInstant()
        val nextYearParts = DisplayTime.formatRangeParts(
            start,
            nextYear,
            zone,
            startIncludeTime = false,
            endIncludeTime = true,
        )
        assertEquals("2026/09/09", nextYearParts.startDate)
        assertEquals("2027/01/01", nextYearParts.endDate)
        assertEquals("00:00", nextYearParts.endTime)
        assertEquals(
            "2026/09/09 — 2027/01/01 00:00 GMT+8",
            DisplayTime.formatRange(start, nextYear, zone, startIncludeTime = false, endIncludeTime = true),
        )
    }

    @Test
    fun formatRangeParts_omitsEndTimeWhenAbsent() {
        val zone = ZoneOffset.ofHours(8)
        val start = LocalDateTime.of(2026, 7, 1, 0, 0).atZone(zone).toInstant()
        val end = LocalDateTime.of(2026, 7, 31, 0, 0).atZone(zone).toInstant()
        val parts = DisplayTime.formatRangeParts(
            start,
            end,
            zone,
            startIncludeTime = false,
            endIncludeTime = false,
        )
        assertEquals("07/01", parts.startDate)
        assertEquals("07/31", parts.endDate)
        assertEquals(null, parts.endTime)
        assertEquals("GMT+8", parts.offset)
        assertEquals("07/01 — 07/31 GMT+8", parts.asLine())
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
