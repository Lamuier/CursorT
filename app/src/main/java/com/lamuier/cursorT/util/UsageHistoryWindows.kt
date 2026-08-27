package com.lamuier.cursorT.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class EpochRange(
    val startMs: Long,
    val endMs: Long,
)

object UsageHistoryWindows {
    fun previousBillingCycle(startMs: Long?, endMs: Long?): EpochRange? {
        if (startMs == null || endMs == null) return null
        return billingCycleOffset(startMs, endMs, -1)
    }

    /**
     * [offset] = 0 当前周期，-1 上一周期，以此类推。
     * 长度取自当前 [startMs, endMs)，过去周期的结束时刻为下一段开始前 1ms。
     */
    fun billingCycleOffset(startMs: Long, endMs: Long, offset: Int): EpochRange? {
        if (endMs <= startMs || offset > 0) return null
        val duration = endMs - startMs
        val cycleStart = startMs + offset.toLong() * duration
        val cycleEnd = if (offset == 0) endMs else cycleStart + duration - 1L
        if (cycleEnd < cycleStart) return null
        return EpochRange(cycleStart, cycleEnd)
    }

    fun cycleKey(startMs: Long): String = "c:$startMs"

    fun calendarMonth(
        yearMonth: YearMonth,
        nowMs: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): EpochRange {
        val start = yearMonth.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val next = yearMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = minOf(next - 1L, nowMs)
        return EpochRange(start, maxOf(start, end))
    }

    fun currentCalendarMonth(
        nowMs: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): Pair<YearMonth, EpochRange> {
        val month = YearMonth.from(LocalDate.ofInstant(Instant.ofEpochMilli(nowMs), zone))
        return month to calendarMonth(month, nowMs, zone)
    }

    fun yearMonthKey(yearMonth: YearMonth): String =
        "%04d-%02d".format(yearMonth.year, yearMonth.monthValue)

    fun parseYearMonth(key: String?): YearMonth? {
        if (key.isNullOrBlank() || key.length < 7) return null
        val year = key.substring(0, 4).toIntOrNull() ?: return null
        val month = key.substring(5, 7).toIntOrNull() ?: return null
        return runCatching { YearMonth.of(year, month) }.getOrNull()
    }

    fun parseLocalDateTimeMs(
        value: String?,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Long? {
        if (value.isNullOrBlank()) return null
        val normalized = if (value.length == 10) {
            value + "T00:00:00"
        } else {
            value.replace(' ', 'T')
        }
        return try {
            LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
        } catch (_: DateTimeParseException) {
            null
        }
    }

    fun formatRange(
        range: EpochRange,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Pair<String, String> {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(zone)
        return fmt.format(Instant.ofEpochMilli(range.startMs)) to
            fmt.format(Instant.ofEpochMilli(range.endMs))
    }

    fun parseEpochMillis(value: Any?): Long? {
        when (value) {
            null -> return null
            is Number -> {
                val raw = value.toLong()
                return if (kotlin.math.abs(raw) < 1_000_000_000_000L) raw * 1000L else raw
            }
            is String -> {
                val raw = value.trim().toLongOrNull() ?: return null
                return if (kotlin.math.abs(raw) < 1_000_000_000_000L) raw * 1000L else raw
            }
            else -> return null
        }
    }
}
