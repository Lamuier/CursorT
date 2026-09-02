package com.lamuier.cursorT.util

import android.content.res.Resources
import androidx.annotation.StringRes
import com.lamuier.cursorT.R
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/** 界面展示用时区。`system` 表示跟随设备。 */
object DisplayTimeZones {
    const val SYSTEM_ID = "system"

    data class Option(val id: String, @StringRes val labelRes: Int)

    val OPTIONS = listOf(
        Option(SYSTEM_ID, R.string.timezone_system),
        Option("Asia/Shanghai", R.string.timezone_china),
        Option("Asia/Hong_Kong", R.string.timezone_hong_kong),
        Option("Asia/Taipei", R.string.timezone_taipei),
        Option("Asia/Tokyo", R.string.timezone_tokyo),
        Option("Asia/Seoul", R.string.timezone_seoul),
        Option("Asia/Singapore", R.string.timezone_singapore),
        Option("UTC", R.string.timezone_utc),
        Option("Europe/London", R.string.timezone_london),
        Option("Europe/Paris", R.string.timezone_paris),
        Option("America/New_York", R.string.timezone_new_york),
        Option("America/Chicago", R.string.timezone_chicago),
        Option("America/Los_Angeles", R.string.timezone_los_angeles),
        Option("Australia/Sydney", R.string.timezone_sydney),
    )

    fun fromStorage(value: String?): String {
        val raw = value?.trim().orEmpty()
        if (raw.isEmpty() || raw.equals(SYSTEM_ID, ignoreCase = true)) return SYSTEM_ID
        OPTIONS.firstOrNull { it.id.equals(raw, ignoreCase = true) }?.let { return it.id }
        return runCatching {
            ZoneId.of(raw)
            raw
        }.getOrElse { SYSTEM_ID }
    }

    fun resolve(id: String?, nowZone: ZoneId = ZoneId.systemDefault()): ZoneId {
        val stored = fromStorage(id)
        if (stored == SYSTEM_ID) return nowZone
        return runCatching { ZoneId.of(stored) }.getOrElse { nowZone }
    }

    fun label(id: String, resources: Resources? = null): String {
        val option = OPTIONS.firstOrNull { it.id == id }
        return when {
            option != null -> resources?.getString(option.labelRes)
                ?: resourcesFallback(option.labelRes)
            id == SYSTEM_ID -> resources?.getString(R.string.timezone_system) ?: "跟随系统"
            else -> id
        }
    }

    private fun resourcesFallback(id: Int): String = when (id) {
        R.string.timezone_system -> "跟随系统"
        R.string.timezone_china -> "中国"
        R.string.timezone_hong_kong -> "香港"
        R.string.timezone_taipei -> "台北"
        R.string.timezone_tokyo -> "东京"
        R.string.timezone_seoul -> "首尔"
        R.string.timezone_singapore -> "新加坡"
        R.string.timezone_utc -> "UTC"
        R.string.timezone_london -> "伦敦"
        R.string.timezone_paris -> "巴黎"
        R.string.timezone_new_york -> "纽约"
        R.string.timezone_chicago -> "芝加哥"
        R.string.timezone_los_angeles -> "洛杉矶"
        R.string.timezone_sydney -> "悉尼"
        else -> ""
    }
}

/**
 * 把时刻格式化为「本地时间 + 时区偏移」（如 `09-02 11:37 GMT+8`）。
 * 本机缓存的无时区字符串按 [storageZone]（默认系统时区，即写入时的时区）解读。
 */
object DisplayTime {
    fun offsetLabel(zone: ZoneId, atMs: Long = System.currentTimeMillis()): String =
        OFFSET.format(Instant.ofEpochMilli(atMs).atZone(zone))

    fun formatClock(instant: Instant, zone: ZoneId): String =
        withOffset(CLOCK.format(instant.atZone(zone)), instant, zone)

    fun formatDateTime(
        instant: Instant,
        zone: ZoneId,
        withYear: Boolean = false,
        includeTime: Boolean = true,
    ): String {
        val local = instant.atZone(zone)
        val body = when {
            withYear && includeTime -> DATE_TIME_YEAR.format(local)
            withYear -> DATE_YEAR.format(local)
            includeTime -> DATE_TIME.format(local)
            else -> DATE.format(local)
        }
        return withOffset(body, instant, zone)
    }

    fun formatEpoch(
        epochMs: Long,
        zone: ZoneId,
        withYear: Boolean = false,
        includeTime: Boolean = true,
    ): String? {
        if (epochMs <= 0L) return null
        return formatDateTime(Instant.ofEpochMilli(epochMs), zone, withYear, includeTime)
    }

    fun formatIso(iso: String?, zone: ZoneId): String? {
        val instant = parseInstant(iso) ?: return null
        return formatDateTime(instant, zone, withYear = false, includeTime = true)
    }

    /** 刷新时间等：只展示钟点与时区。 */
    fun formatStoredClock(
        value: String?,
        displayZone: ZoneId,
        storageZone: ZoneId = ZoneId.systemDefault(),
    ): String? {
        val instant = parseStoredLocal(value, storageZone) ?: return null
        return formatClock(instant, displayZone)
    }

    fun formatStoredDateTime(
        value: String?,
        displayZone: ZoneId,
        storageZone: ZoneId = ZoneId.systemDefault(),
        withYear: Boolean = false,
        includeTime: Boolean? = null,
    ): String? {
        val instant = parseStoredLocal(value, storageZone) ?: return null
        val hasTime = includeTime ?: ((value?.trim()?.length ?: 0) >= 16)
        return formatDateTime(instant, displayZone, withYear, hasTime)
    }

    fun parseStoredLocal(
        value: String?,
        storageZone: ZoneId = ZoneId.systemDefault(),
    ): Instant? {
        parseInstant(value)?.let { return it }
        if (value.isNullOrBlank()) return null
        val normalized = if (value.length == 10) {
            value + "T00:00:00"
        } else {
            value.trim().replace(' ', 'T')
        }
        return try {
            LocalDateTime.parse(normalized, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .atZone(storageZone)
                .toInstant()
        } catch (_: DateTimeParseException) {
            runCatching {
                LocalDate.parse(value.take(10)).atStartOfDay(storageZone).toInstant()
            }.getOrNull()
        }
    }

    fun parseInstant(value: String?): Instant? {
        if (value.isNullOrBlank()) return null
        return runCatching { Instant.parse(value) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
    }

    private fun withOffset(body: String, instant: Instant, zone: ZoneId): String =
        "$body ${offsetLabel(zone, instant.toEpochMilli())}"

    private val OFFSET: DateTimeFormatter =
        DateTimeFormatter.ofPattern("O", Locale.US)
    private val CLOCK: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm", Locale.US)
    private val DATE: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MM-dd", Locale.US)
    private val DATE_YEAR: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    private val DATE_TIME: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.US)
    private val DATE_TIME_YEAR: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US)
}
