package com.lamuier.cursorT.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OverviewRingCollapseTest {
    @Test
    fun cycleKey_requiresNonBlankStart() {
        assertNull(OverviewRingCollapse.cycleKey(1, null))
        assertNull(OverviewRingCollapse.cycleKey(1, ""))
        assertNull(OverviewRingCollapse.cycleKey(1, "   "))
        assertEquals("3|2026-09-01T00:00:00Z", OverviewRingCollapse.cycleKey(3, " 2026-09-01T00:00:00Z "))
    }

    @Test
    fun syncedKeys_marksCurrentCycleAndPrunesOlderCyclesForSameAccount() {
        val stored = setOf("1|old", "2|keep")
        val next = OverviewRingCollapse.syncedKeys(
            stored = stored,
            accountId = 1,
            cycleStart = "new",
            markNow = true,
        )
        assertEquals(setOf("1|new", "2|keep"), next)
    }

    @Test
    fun syncedKeys_keepsExistingMarkWhenConditionClearsThisCycle() {
        val stored = setOf("1|current", "2|other")
        val next = OverviewRingCollapse.syncedKeys(
            stored = stored,
            accountId = 1,
            cycleStart = "current",
            markNow = false,
        )
        assertEquals(setOf("1|current", "2|other"), next)
    }

    @Test
    fun syncedKeys_newCycleWithoutConditionDropsPreviousMark() {
        val next = OverviewRingCollapse.syncedKeys(
            stored = setOf("1|old"),
            accountId = 1,
            cycleStart = "new",
            markNow = false,
        )
        assertTrue(next.isEmpty())
    }

    @Test
    fun syncedKeys_missingCycleStartDropsThisAccount() {
        val next = OverviewRingCollapse.syncedKeys(
            stored = setOf("1|old", "2|keep"),
            accountId = 1,
            cycleStart = null,
            markNow = true,
        )
        assertEquals(setOf("2|keep"), next)
    }

    @Test
    fun collapsedThisCycle_matchesExactKey() {
        val stored = setOf("1|2026-09-01")
        assertTrue(OverviewRingCollapse.collapsedThisCycle(stored, 1, "2026-09-01"))
        assertFalse(OverviewRingCollapse.collapsedThisCycle(stored, 1, "2026-10-01"))
        assertFalse(OverviewRingCollapse.collapsedThisCycle(stored, 2, "2026-09-01"))
        assertFalse(OverviewRingCollapse.collapsedThisCycle(stored, 1, null))
    }

    @Test
    fun effectiveMode_collapsesSplitWhenOwnExhaustedOrAlreadyMarked() {
        assertEquals(
            OverviewUsageRingMode.Combined,
            OverviewRingCollapse.effectiveMode(
                preferred = OverviewUsageRingMode.Combined,
                ownPercent = 10.0,
                thirdPartyPercent = 10.0,
                collapsedThisCycle = false,
            ),
        )
        assertEquals(
            OverviewUsageRingMode.Combined,
            OverviewRingCollapse.effectiveMode(
                preferred = OverviewUsageRingMode.Split,
                ownPercent = 100.0,
                thirdPartyPercent = 20.0,
                collapsedThisCycle = false,
            ),
        )
        assertEquals(
            OverviewUsageRingMode.Combined,
            OverviewRingCollapse.effectiveMode(
                preferred = OverviewUsageRingMode.Split,
                ownPercent = 100.0,
                thirdPartyPercent = 100.0,
                collapsedThisCycle = true,
            ),
        )
        assertEquals(
            OverviewUsageRingMode.Split,
            OverviewRingCollapse.effectiveMode(
                preferred = OverviewUsageRingMode.Split,
                ownPercent = 80.0,
                thirdPartyPercent = 20.0,
                collapsedThisCycle = false,
            ),
        )
        assertEquals(
            OverviewUsageRingMode.Split,
            OverviewRingCollapse.effectiveMode(
                preferred = OverviewUsageRingMode.Split,
                ownPercent = 100.0,
                thirdPartyPercent = 100.0,
                collapsedThisCycle = false,
            ),
        )
    }
}
