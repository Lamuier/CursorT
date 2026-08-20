package com.lamuier.cursorT.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DashboardTabTest {
    @Test
    fun resolveOrder_nullOrBlankUsesDefault() {
        assertEquals(DashboardTab.DEFAULT_ORDER, DashboardTab.resolveOrder(null as String?))
        assertEquals(DashboardTab.DEFAULT_ORDER, DashboardTab.resolveOrder(""))
        assertEquals(DashboardTab.DEFAULT_ORDER, DashboardTab.resolveOrder("   ,  ,"))
    }

    @Test
    fun resolveOrder_keepsCustomPrefixAndAppendsMissing() {
        val order = DashboardTab.resolveOrder("status,tasks")
        assertEquals(
            listOf(
                DashboardTab.Status,
                DashboardTab.Tasks,
                DashboardTab.Overview,
                DashboardTab.Usage,
                DashboardTab.Billing,
            ),
            order,
        )
    }

    @Test
    fun resolveOrder_ignoresUnknownDuplicatesAndWhitespace() {
        val order = DashboardTab.resolveOrder(" TASKS , status , tasks , evil,overview ")
        assertEquals(
            listOf(
                DashboardTab.Tasks,
                DashboardTab.Status,
                DashboardTab.Overview,
                DashboardTab.Usage,
                DashboardTab.Billing,
            ),
            order,
        )
    }

    @Test
    fun serialize_roundTripsNormalizedOrder() {
        val custom = listOf(DashboardTab.Status, DashboardTab.Overview)
        val serialized = DashboardTab.serialize(custom)
        assertEquals("status,overview,usage,billing,tasks", serialized)
        assertEquals(DashboardTab.resolveOrder(serialized), DashboardTab.resolveOrder(custom.map { it.id }))
    }

    @Test
    fun move_swapsWithinBoundsAndNoopsAtEnds() {
        val start = DashboardTab.DEFAULT_ORDER
        assertEquals(start, DashboardTab.move(start, 0, -1))
        assertEquals(start, DashboardTab.move(start, start.lastIndex, 1))
        assertEquals(start, DashboardTab.move(start, -1, 1))

        assertEquals(
            listOf(
                DashboardTab.Usage,
                DashboardTab.Overview,
                DashboardTab.Billing,
                DashboardTab.Tasks,
                DashboardTab.Status,
            ),
            DashboardTab.move(start, 0, 1),
        )
        assertEquals(
            listOf(
                DashboardTab.Overview,
                DashboardTab.Usage,
                DashboardTab.Tasks,
                DashboardTab.Billing,
                DashboardTab.Status,
            ),
            DashboardTab.move(start, 3, -1),
        )
    }
}
