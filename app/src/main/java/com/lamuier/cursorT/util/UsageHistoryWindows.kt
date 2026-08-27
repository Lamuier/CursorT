package com.lamuier.cursorT.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class EpochRange(
    val startMs: Long,
    val endMs: Long,
)

object UsageHistoryWindows {
    fun previousBillingCycle(startMs: Long?, endMs: Long?): EpochRange? {
        if (startMs == null || endMs == null) return null
        if (endMs <= startMs) return null
        val duration = endMs - startMs
        val prevEnd = startMs - 1L
        val prevStart = startMs - duration
        if (prevEnd < prevStart) return null
        return EpochRange(prevStart, prevEnd)
    }

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
        val month = YearMonth.from(LocalDate.ofInstant(java.time.Instant.ofEpochMilli(nowMs), zone))
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
