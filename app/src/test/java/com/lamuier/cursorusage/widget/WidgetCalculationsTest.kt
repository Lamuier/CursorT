package com.lamuier.cursorusage.widget

import com.lamuier.cursorusage.model.BillingCycle
import com.lamuier.cursorusage.model.ComponentStatus
import com.lamuier.cursorusage.model.Credits
import com.lamuier.cursorusage.model.CursorServiceStatus
import com.lamuier.cursorusage.model.CursorUsageOverview
import com.lamuier.cursorusage.model.PlanInfo
import com.lamuier.cursorusage.model.StatusComponent
import com.lamuier.cursorusage.model.StatusIncident
import com.lamuier.cursorusage.model.StatusIndicator
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

    @Test
    fun operationalPercent_countsHealthyComponents() {
        val status = serviceStatus(
            components = listOf(
                component("ide", "IDE", ComponentStatus.Operational),
                component("agents", "Cloud Agents", ComponentStatus.PartialOutage),
                component("cli", "CLI", ComponentStatus.Operational),
                component("web", "Web", ComponentStatus.DegradedPerformance),
            ),
        )
        assertEquals(50.0, WidgetCalculations.operationalPercent(status), 0.001)
        assertEquals("2/4", WidgetCalculations.summaryChip(status))
        assertEquals(50, WidgetCalculations.progress(WidgetCalculations.operationalPercent(status)))
    }

    @Test
    fun emptyComponents_renderPlaceholder() {
        val status = serviceStatus()
        assertEquals(0.0, WidgetCalculations.operationalPercent(status), 0.001)
        assertEquals("—", WidgetCalculations.summaryChip(status))
        assertEquals("暂无事件", WidgetCalculations.incidentHeadline(status))
    }

    @Test
    fun highlightedComponents_preferDegradedThenFillOperational() {
        val status = serviceStatus(
            components = listOf(
                component("ide", "IDE", ComponentStatus.Operational),
                component("agents", "Cloud Agents", ComponentStatus.PartialOutage),
                component("cli", "CLI", ComponentStatus.Operational),
                component("web", "Web", ComponentStatus.DegradedPerformance),
            ),
        )
        assertEquals(
            listOf("Cloud Agents" to ComponentStatus.PartialOutage, "Web" to ComponentStatus.DegradedPerformance),
            WidgetCalculations.highlightedComponents(status, 2),
        )
        assertEquals(
            listOf(
                "Cloud Agents" to ComponentStatus.PartialOutage,
                "Web" to ComponentStatus.DegradedPerformance,
                "IDE" to ComponentStatus.Operational,
            ),
            WidgetCalculations.highlightedComponents(status, 3),
        )
    }

    @Test
    fun incidentHeadline_prefersActiveThenMaintenanceThenRecent() {
        val active = incident("inc-active", "Agents delayed")
        val maintenance = incident("mnt-1", "Scheduled CLI window")
        val recent = incident("inc-old", "Brief IDE blip")
        assertEquals("Agents delayed", WidgetCalculations.incidentHeadline(serviceStatus(active = listOf(active))))
        assertEquals(
            "Scheduled CLI window",
            WidgetCalculations.incidentHeadline(serviceStatus(maintenance = listOf(maintenance))),
        )
        assertEquals(
            "近期 Brief IDE blip",
            WidgetCalculations.incidentHeadline(serviceStatus(recent = listOf(recent))),
        )
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

    private fun serviceStatus(
        components: List<StatusComponent> = emptyList(),
        active: List<StatusIncident> = emptyList(),
        maintenance: List<StatusIncident> = emptyList(),
        recent: List<StatusIncident> = emptyList(),
    ) = CursorServiceStatus(
        description = "",
        indicator = StatusIndicator.None,
        pageUpdatedAt = null,
        pageUrl = "https://status.cursor.com",
        components = components,
        activeIncidents = active,
        scheduledMaintenances = maintenance,
        recentIncidents = recent,
        fetchedAt = "2026-08-19 12:00:00",
        fromCache = false,
    )

    private fun component(id: String, name: String, status: ComponentStatus) = StatusComponent(
        id = id,
        name = name,
        status = status,
        position = 0,
    )

    private fun incident(id: String, name: String) = StatusIncident(
        id = id,
        name = name,
        status = "investigating",
        impact = "major",
        createdAt = null,
        updatedAt = null,
        resolvedAt = null,
        scheduledFor = null,
        scheduledUntil = null,
        shortlink = null,
        affectedComponents = emptyList(),
        updates = emptyList(),
    )
}
