package com.lamuier.cursorT.data

import com.lamuier.cursorT.util.UsageCalculations

/**
 * 分列圆环在本周期因自有池用尽而折叠为合并显示。
 * 按账号 + 周期起点记住折叠，下个周期自动解除；同周期内三方随后用尽也不改回去。
 */
object OverviewRingCollapse {
    fun cycleKey(accountId: Int, cycleStart: String?): String? {
        val start = cycleStart?.trim().orEmpty()
        if (start.isEmpty()) return null
        return "$accountId|$start"
    }

    fun collapsedThisCycle(
        storedKeys: Set<String>,
        accountId: Int,
        cycleStart: String?,
    ): Boolean {
        val key = cycleKey(accountId, cycleStart) ?: return false
        return key in storedKeys
    }

    fun syncedKeys(
        stored: Set<String>,
        accountId: Int,
        cycleStart: String?,
        markNow: Boolean,
    ): Set<String> {
        val cycleKey = cycleKey(accountId, cycleStart)
        val prefix = "$accountId|"
        val next = stored.toMutableSet()
        if (cycleKey != null && markNow) next.add(cycleKey)
        next.removeAll { it.startsWith(prefix) && it != cycleKey }
        return next.toSet()
    }

    fun effectiveMode(
        preferred: OverviewUsageRingMode,
        ownPercent: Double?,
        thirdPartyPercent: Double?,
        collapsedThisCycle: Boolean,
    ): OverviewUsageRingMode {
        if (preferred != OverviewUsageRingMode.Split) return preferred
        if (collapsedThisCycle) return OverviewUsageRingMode.Combined
        return if (UsageCalculations.shouldCollapseSplitOverviewRing(ownPercent, thirdPartyPercent)) {
            OverviewUsageRingMode.Combined
        } else {
            OverviewUsageRingMode.Split
        }
    }
}
