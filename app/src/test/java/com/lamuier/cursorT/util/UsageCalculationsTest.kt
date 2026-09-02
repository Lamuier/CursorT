package com.lamuier.cursorT.util

import com.lamuier.cursorT.model.BillingCycle
import com.lamuier.cursorT.model.Credits
import com.lamuier.cursorT.model.CursorTOverview
import com.lamuier.cursorT.model.ModelTokenUsage
import com.lamuier.cursorT.model.PlanInfo
import com.lamuier.cursorT.model.Subscription
import com.lamuier.cursorT.model.TokenUsageBreakdown
import com.lamuier.cursorT.model.TotalFormat
import com.lamuier.cursorT.model.UsageMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

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
        val zone = ZoneOffset.ofHours(8)
        val start = LocalDateTime.of(2026, 7, 1, 0, 0)
        val now = start.plusDays(10).plusHours(12).atZone(zone).toInstant().toEpochMilli()
        val result = UsageCalculations.billingProgress(
            start = "2026-07-01 00:00:00",
            end = "2026-07-31 00:00:00",
            nowMillis = now,
            displayZone = zone,
            storageZone = zone,
        )
        assertNotNull(result)
        assertEquals(30, result!!.totalDays)
        assertEquals(10, result.elapsedDays)
        assertEquals(20, result.remainingDays)
        assertEquals(35f, result.percent, 0.01f)
        assertEquals("07-01 GMT+8", result.startLabel)
        assertEquals("07-31 00:00 GMT+8", result.endLabel)
        assertEquals(19L * 24 * 60 * 60_000 + 12 * 60 * 60_000, result.remainingMillis)
    }

    @Test
    fun billingProgress_labelsKeepYearAcrossYears() {
        val zone = ZoneOffset.ofHours(8)
        val start = LocalDateTime.of(2026, 12, 1, 0, 0)
        val now = start.plusDays(10).atZone(zone).toInstant().toEpochMilli()
        val result = UsageCalculations.billingProgress(
            start = "2026-12-01 00:00:00",
            end = "2027-01-01 00:00:00",
            nowMillis = now,
            displayZone = zone,
            storageZone = zone,
        )
        assertNotNull(result)
        assertEquals("2026-12-01 GMT+8", result!!.startLabel)
        assertEquals("2027-01-01 00:00 GMT+8", result.endLabel)
    }

    @Test
    fun billingProgress_endLabelOmitsTimeWhenAbsent() {
        val zone = ZoneOffset.ofHours(8)
        val start = LocalDateTime.of(2026, 7, 1, 0, 0)
        val now = start.atZone(zone).toInstant().toEpochMilli()
        val result = UsageCalculations.billingProgress(
            start = "2026-07-01 00:00:00",
            end = "2026-07-31",
            nowMillis = now,
            displayZone = zone,
            storageZone = zone,
        )
        assertNotNull(result)
        assertEquals("07-31 GMT+8", result!!.endLabel)
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

    @Test
    fun formatTokens_usesCompactUnits() {
        assertEquals("999", UsageCalculations.formatTokens(999))
        assertEquals("1.50K", UsageCalculations.formatTokens(1500))
        assertEquals("12.3K", UsageCalculations.formatTokens(12300))
        assertEquals("1.25M", UsageCalculations.formatTokens(1_250_000))
        assertEquals("0", UsageCalculations.formatTokens(-5))
    }

    @Test
    fun isCursorOwnedModel_matchesComposerGrokAndAuto() {
        assertTrue(UsageCalculations.isCursorOwnedModel("composer-2.5"))
        assertTrue(UsageCalculations.isCursorOwnedModel("Composer-2.5-Fast"))
        assertTrue(UsageCalculations.isCursorOwnedModel("grok-4.6"))
        assertTrue(UsageCalculations.isCursorOwnedModel("Grok 4.5"))
        assertTrue(UsageCalculations.isCursorOwnedModel("auto"))
        assertTrue(UsageCalculations.isCursorOwnedModel("auto-cost"))
        assertTrue(UsageCalculations.isCursorOwnedModel("cursor-small"))
        assertFalse(UsageCalculations.isCursorOwnedModel("claude-4-sonnet"))
        assertFalse(UsageCalculations.isCursorOwnedModel("gpt-5"))
        assertFalse(UsageCalculations.isCursorOwnedModel("gemini-2.5-pro"))
        assertFalse(UsageCalculations.isCursorOwnedModel(""))
    }

    @Test
    fun poolSpend_splitsOwnAndThirdPartyCosts() {
        val breakdown = TokenUsageBreakdown(
            models = listOf(
                model("composer-2.5", 8.5),
                model("grok-4.5", 1.25),
                model("claude-4-sonnet", 3.4),
                model("gpt-5", 0.6),
            ),
            totalInputTokens = 0L,
            totalOutputTokens = 0L,
            totalCacheWriteTokens = 0L,
            totalCacheReadTokens = 0L,
            totalCostDollars = 13.75,
        )
        val result = UsageCalculations.poolSpend(breakdown)
        assertNotNull(result)
        assertEquals(9.75, result!!.ownPoolDollars, 0.001)
        assertEquals(4.0, result.thirdPartyDollars, 0.001)
        val percents = UsageCalculations.poolPercents(breakdown)
        assertNotNull(percents)
        assertEquals(9.75 / 13.75 * 100.0, percents!!.ownPercent, 0.001)
        assertEquals(4.0 / 13.75 * 100.0, percents.thirdPartyPercent, 0.001)
    }

    @Test
    fun poolSpend_returnsNullWhenTokenUsageMissing() {
        assertNull(UsageCalculations.poolSpend(null))
    }

    @Test
    fun poolSpend_returnsZerosWhenModelsEmpty() {
        val result = UsageCalculations.poolSpend(
            TokenUsageBreakdown(
                models = emptyList(),
                totalInputTokens = 0L,
                totalOutputTokens = 0L,
                totalCacheWriteTokens = 0L,
                totalCacheReadTokens = 0L,
                totalCostDollars = 0.0,
            ),
        )
        assertNotNull(result)
        assertEquals(0.0, result!!.ownPoolDollars, 0.001)
        assertEquals(0.0, result.thirdPartyDollars, 0.001)
    }

    private fun model(intent: String, cost: Double) = ModelTokenUsage(
        modelIntent = intent,
        inputTokens = 0L,
        outputTokens = 0L,
        cacheWriteTokens = 0L,
        cacheReadTokens = 0L,
        costDollars = cost,
        tier = null,
    )

    private fun overview(
        format: TotalFormat,
        totalUsed: Double,
        includedSpend: Double = 0.0,
        limit: Double = 0.0,
    ) = CursorTOverview(
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

