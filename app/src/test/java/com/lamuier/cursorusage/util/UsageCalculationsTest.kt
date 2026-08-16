package com.lamuier.cursorusage.util

import com.lamuier.cursorusage.model.BillingCycle
import com.lamuier.cursorusage.model.Credits
import com.lamuier.cursorusage.model.CursorUsageOverview
import com.lamuier.cursorusage.model.PlanInfo
import com.lamuier.cursorusage.model.Subscription
import com.lamuier.cursorusage.model.TotalFormat
import com.lamuier.cursorusage.model.UsageMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class UsageCalculationsTest {
    @Test
    fun individualAccount_usesServerPercentage() {
        assertEquals(72.5, UsageCalculations.usagePercent(overview(TotalFormat.Percent, 72.5)), 0.001)
    }

    @Test
    fun teamAccount_computesRingFromIncludedSpendAndLimit() {
        val overview = overview(
            format = TotalFormat.Dollars,
            totalUsed = 240.0,
            includedSpend = 150.0,
            limit = 200.0,
        )
        assertEquals(75.0, UsageCalculations.usagePercent(overview), 0.001)
    }

    @Test
    fun thresholdsMatchPulse() {
        assertEquals(UsageLevel.Healthy, UsageCalculations.level(69.99))
        assertEquals(UsageLevel.Warning, UsageCalculations.level(70.0))
        assertEquals(UsageLevel.Critical, UsageCalculations.level(90.0))
    }

    @Test
    fun exhaustedAtFullUsage() {
        assertEquals(UsageLevel.Exhausted, UsageCalculations.level(100.0))
        assertEquals(UsageLevel.Exhausted, UsageCalculations.level(120.0))
        // 99% 仍属「即将用尽」，避免与 100% 的「已用尽」混淆。
        assertEquals(UsageLevel.Critical, UsageCalculations.level(99.0))
    }

    @Test
    fun billingProgress_reportsElapsedAndRemainingDays() {
        val zone = ZoneId.systemDefault()
        val start = LocalDateTime.of(2026, 7, 1, 0, 0)
        val end = start.plusDays(30)
        val now = start.plusDays(10).plusHours(12).atZone(zone).toInstant().toEpochMilli()
        val result = UsageCalculations.billingProgress(
            start = "2026-07-01 00:00:00",
            end = "2026-07-31 00:00:00",
            nowMillis = now,
        )
        assertNotNull(result)
        assertEquals(30, result!!.totalDays)
        assertEquals(10, result.elapsedDays)
        assertEquals(20, result.remainingDays)
        assertEquals(35f, result.percent, 0.01f)
        // 结束时间精确到分钟，剩余时间精确到毫秒；同年周期省略年份。
        assertEquals("07-01", result.startLabel)
        assertEquals("07-31 00:00", result.endLabel)
        assertEquals(19L * 24 * 60 * 60_000 + 12 * 60 * 60_000, result.remainingMillis)
    }

    @Test
    fun billingProgress_labelsKeepYearAcrossYears() {
        val zone = ZoneId.systemDefault()
        val start = LocalDateTime.of(2026, 12, 1, 0, 0)
        val now = start.plusDays(10).atZone(zone).toInstant().toEpochMilli()
        val result = UsageCalculations.billingProgress(
            start = "2026-12-01 00:00:00",
            end = "2027-01-01 00:00:00",
            nowMillis = now,
        )
        assertNotNull(result)
        // 跨年周期保留年份避免歧义。
        assertEquals("2026-12-01", result!!.startLabel)
        assertEquals("2027-01-01 00:00", result.endLabel)
    }

    @Test
    fun billingProgress_endLabelOmitsTimeWhenAbsent() {
        val zone = ZoneId.systemDefault()
        val start = LocalDateTime.of(2026, 7, 1, 0, 0)
        val now = start.atZone(zone).toInstant().toEpochMilli()
        val result = UsageCalculations.billingProgress(
            start = "2026-07-01 00:00:00",
            end = "2026-07-31",
            nowMillis = now,
        )
        assertNotNull(result)
        // 结束值只有日期时不追加时刻。
        assertEquals("07-31", result!!.endLabel)
    }

    @Test
    fun formatRemaining_usesTwoLevelUnits() {
        val day = 24 * 60 * 60_000L
        val hour = 60 * 60_000L
        val minute = 60_000L
        assertEquals("3 天 4 小时", UsageCalculations.formatRemaining(3 * day + 4 * hour + 31 * minute))
        assertEquals("2 天", UsageCalculations.formatRemaining(2 * day))
        assertEquals("4 小时 31 分", UsageCalculations.formatRemaining(4 * hour + 31 * minute))
        assertEquals("5 小时", UsageCalculations.formatRemaining(5 * hour))
        assertEquals("12 分钟", UsageCalculations.formatRemaining(12 * minute))
        assertEquals("0 分钟", UsageCalculations.formatRemaining(0))
    }

    @Test
    fun formatRemaining_compactDropsSpaces() {
        val day = 24 * 60 * 60_000L
        val hour = 60 * 60_000L
        val minute = 60_000L
        assertEquals("3天4时", UsageCalculations.formatRemaining(3 * day + 4 * hour, compact = true))
        assertEquals("2天", UsageCalculations.formatRemaining(2 * day, compact = true))
        assertEquals("4时31分", UsageCalculations.formatRemaining(4 * hour + 31 * minute, compact = true))
        assertEquals("12 分", UsageCalculations.formatRemaining(12 * minute, compact = true))
    }

    private fun overview(
        format: TotalFormat,
        totalUsed: Double,
        includedSpend: Double = 0.0,
        limit: Double = 0.0,
    ) = CursorUsageOverview(
        accountId = 1,
        alias = "test",
        isTeam = format == TotalFormat.Dollars,
        plan = PlanInfo(null, null, 0.0, null),
        billingCycle = BillingCycle(null, null),
        usage = UsageMetrics(
            totalUsed = totalUsed,
            totalFormat = format,
            totalSpendDollars = includedSpend,
            includedSpendDollars = includedSpend,
            bonusSpendDollars = 0.0,
            limitDollars = limit,
            remainingDollars = 0.0,
            autoPercentUsed = null,
            apiPercentUsed = null,
            displayMessage = null,
            remainingBonus = false,
        ),
        credits = Credits(0.0, 0.0, 0.0),
        onDemand = null,
        subscription = Subscription(null, null),
        fetchedAt = "",
        fromCache = false,
        cacheAgeSeconds = 0,
    )
}

