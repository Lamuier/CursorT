package com.lamuier.cursorusage.widget

import com.lamuier.cursorusage.model.BillingCycle
import com.lamuier.cursorusage.model.Credits
import com.lamuier.cursorusage.model.CursorUsageOverview
import com.lamuier.cursorusage.model.PlanInfo
import com.lamuier.cursorusage.model.Subscription
import com.lamuier.cursorusage.model.TotalFormat
import com.lamuier.cursorusage.model.UsageMetrics
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetCalculationsTest {
    @Test
    fun individualProgress_isClampedForRemoteViews() {
        val usage = overview(isTeam = false, totalUsed = 118.4)
        assertEquals(118.4, WidgetCalculations.totalPercent(usage), 0.001)
        assertEquals(100, WidgetCalculations.progress(WidgetCalculations.totalPercent(usage)))
    }

    @Test
    fun teamProgress_usesPlanFallbackLimit() {
        val usage = overview(
            isTeam = true,
            totalUsed = 80.0,
            includedSpend = 30.0,
            limit = 0.0,
            planLimit = 60.0,
        )
        assertEquals(50.0, WidgetCalculations.totalPercent(usage), 0.001)
        assertEquals("\$80.00", WidgetCalculations.totalLabel(usage))
    }

    @Test
    fun invalidPercent_rendersEmptyProgress() {
        assertEquals(0, WidgetCalculations.progress(Double.NaN))
        assertEquals(0, WidgetCalculations.progress(null))
    }

    private fun overview(
        isTeam: Boolean,
        totalUsed: Double,
        includedSpend: Double = 0.0,
        limit: Double = 0.0,
        planLimit: Double = 0.0,
    ) = CursorUsageOverview(
        accountId = 1,
        alias = "test",
        isTeam = isTeam,
        plan = PlanInfo(null, null, planLimit, null),
        billingCycle = BillingCycle(null, null),
        usage = UsageMetrics(
            totalUsed = totalUsed,
            totalFormat = if (isTeam) TotalFormat.Dollars else TotalFormat.Percent,
            totalSpendDollars = totalUsed,
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
