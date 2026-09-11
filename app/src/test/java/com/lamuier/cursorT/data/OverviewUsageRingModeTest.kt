package com.lamuier.cursorT.data

import org.junit.Assert.assertEquals
import org.junit.Test

class OverviewUsageRingModeTest {
    @Test
    fun fromStorage_defaultsToSplit() {
        assertEquals(OverviewUsageRingMode.Split, OverviewUsageRingMode.fromStorage(null))
        assertEquals(OverviewUsageRingMode.Split, OverviewUsageRingMode.fromStorage(""))
        assertEquals(OverviewUsageRingMode.Split, OverviewUsageRingMode.fromStorage("   "))
        assertEquals(OverviewUsageRingMode.Split, OverviewUsageRingMode.fromStorage("unknown"))
    }

    @Test
    fun fromStorage_readsKnownKeysCaseInsensitively() {
        assertEquals(OverviewUsageRingMode.Split, OverviewUsageRingMode.fromStorage("split"))
        assertEquals(OverviewUsageRingMode.Split, OverviewUsageRingMode.fromStorage(" SPLIT "))
        assertEquals(OverviewUsageRingMode.Combined, OverviewUsageRingMode.fromStorage("combined"))
        assertEquals(OverviewUsageRingMode.Combined, OverviewUsageRingMode.fromStorage("Combined"))
    }
}
