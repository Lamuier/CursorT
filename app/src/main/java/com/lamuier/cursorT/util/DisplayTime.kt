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

    data class Option(
        val id: String,
        @StringRes val labelRes: Int,
        val aliases: List<String> = emptyList(),
    )

    val OPTIONS = listOf(
        Option(SYSTEM_ID, R.string.timezone_system),
        Option(
            "Asia/Shanghai",
            R.string.timezone_china,
            listOf("北京", "上海", "中国", "CST", "Beijing", "Shanghai", "China"),
        ),
        Option(
            "Asia/Hong_Kong",
            R.string.timezone_hong_kong,
            listOf("香港", "Hong Kong", "HK"),
        ),
        Option(
            "Asia/Taipei",
            R.string.timezone_taipei,
            listOf("台北", "台湾", "Taipei", "Taiwan"),
        ),
        Option(
            "Asia/Tokyo",
            R.string.timezone_tokyo,
            listOf("东京", "日本", "Tokyo", "Japan", "JST"),
        ),
        Option(
            "Asia/Seoul",
            R.string.timezone_seoul,
            listOf("首尔", "韩国", "Seoul", "Korea", "KST"),
        ),
        Option(
            "Asia/Singapore",
            R.string.timezone_singapore,
            listOf("新加坡", "Singapore", "SGT"),
        ),
        Option("UTC", R.string.timezone_utc, listOf("GMT", "世界时", "协调世界时")),
        Option(
            "Europe/London",
            R.string.timezone_london,
            listOf("伦敦", "英国", "London", "UK", "BST"),
        ),
        Option(
            "Europe/Paris",
            R.string.timezone_paris,
            listOf("巴黎", "法国", "Paris", "France", "CET"),
        ),
        Option(
            "America/New_York",
            R.string.timezone_new_york,
            listOf("纽约", "美东", "New York", "EST", "EDT", "Eastern"),
        ),
        Option(
            "America/Chicago",
            R.string.timezone_chicago,
            listOf("芝加哥", "美中", "Chicago", "Central"),
        ),
        Option(
            "America/Los_Angeles",
            R.string.timezone_los_angeles,
            listOf("洛杉矶", "美西", "旧金山", "Los Angeles", "PST", "PDT", "Pacific"),
        ),
        Option(
            "Australia/Sydney",
            R.string.timezone_sydney,
            listOf("悉尼", "澳大利亚", "Sydney", "Australia", "AEST"),
        ),
    )

    /** 固定展示「跟随系统」之外、可供搜索选择的时区。 */
    val SEARCHABLE_OPTIONS: List<Option> = OPTIONS.filter { it.id != SYSTEM_ID }

    fun option(id: String): Option? = OPTIONS.firstOrNull { it.id == id }

    /**
     * 按城市名、时区 id 或别名筛选目录里的时区；空白查询不返回结果。
     * [localizedLabel] 传入界面文案，便于中英文都可搜到。
     */
    fun search(
        query: String,
        localizedLabel: (Option) -> String = { resourcesFallback(it.labelRes) },
    ): List<Option> {
        val needle = query.trim()
        if (needle.isEmpty()) return emptyList()
        return SEARCHABLE_OPTIONS.filter { matches(it, needle, localizedLabel(it)) }
    }

    fun matches(option: Option, query: String, localizedLabel: String): Boolean {
        val needle = query.trim()
        if (needle.isEmpty() || option.id == SYSTEM_ID) return false
        if (option.id.contains(needle, ignoreCase = true)) return true
        if (localizedLabel.contains(needle, ignoreCase = true)) return true
        return option.aliases.any { it.contains(needle, ignoreCase = true) }
    }

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
 * 把时刻格式化为「本地时间 + 时区偏移」（如 `09/02 11:37 GMT+8`）。
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
        includeOffset: Boolean = true,
    ): String {
        val local = instant.atZone(zone)
        val body = when {
            withYear && includeTime -> DATE_TIME_YEAR.format(local)
            withYear -> DATE_YEAR.format(local)
            includeTime -> DATE_TIME.format(local)
            else -> DATE.format(local)
        }
        return if (includeOffset) withOffset(body, instant, zone) else body
    }

    /**
     * 周期起止的结构化展示：日期、重置时刻与时区分开，便于分层排版。
     * [asLine] 仍是「起点不重复时区、只在终点标一次偏移」的一行文案。
     */
    data class RangeParts(
        val startDate: String,
        val startTime: String? = null,
        val endDate: String,
        val endTime: String? = null,
        val offset: String,
    ) {
        fun asLine(): String = buildString {
            append(startDate)
            if (!startTime.isNullOrBlank()) {
                append(' ')
                append(startTime)
            }
            append(" — ")
            append(endDate)
            if (!endTime.isNullOrBlank()) {
                append(' ')
                append(endTime)
            }
            append(' ')
            append(offset)
        }
    }

    fun formatRangeParts(
        start: Instant,
        end: Instant,
        zone: ZoneId,
        withYear: Boolean = start.atZone(zone).year != end.atZone(zone).year,
        startIncludeTime: Boolean = false,
        endIncludeTime: Boolean = true,
    ): RangeParts {
        val startLocal = start.atZone(zone)
        val endLocal = end.atZone(zone)
        val datePattern = if (withYear) DATE_YEAR else DATE
        return RangeParts(
            startDate = datePattern.format(startLocal),
            startTime = CLOCK.format(startLocal).takeIf { startIncludeTime },
            endDate = datePattern.format(endLocal),
            endTime = CLOCK.format(endLocal).takeIf { endIncludeTime },
            offset = offsetLabel(zone, end.toEpochMilli()),
        )
    }

    /**
     * 周期起止拼成一行：起点不重复时区，只在终点标一次偏移。
     * 例如 `09/09 — 10/09 15:41 GMT+8`；跨年保留年份。
     */
    fun formatRange(
        start: Instant,
        end: Instant,
        zone: ZoneId,
        withYear: Boolean = start.atZone(zone).year != end.atZone(zone).year,
        startIncludeTime: Boolean = false,
        endIncludeTime: Boolean = true,
    ): String = formatRangeParts(
        start,
        end,
        zone,
        withYear = withYear,
        startIncludeTime = startIncludeTime,
        endIncludeTime = endIncludeTime,
    ).asLine()

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
        DateTimeFormatter.ofPattern("MM/dd", Locale.US)
    private val DATE_YEAR: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy/MM/dd", Locale.US)
    private val DATE_TIME: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MM/dd HH:mm", Locale.US)
    private val DATE_TIME_YEAR: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm", Locale.US)
}
