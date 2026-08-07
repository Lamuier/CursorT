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

