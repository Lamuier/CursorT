package com.lamuier.cursorT.model

import androidx.annotation.StringRes
import com.lamuier.cursorT.R

/**
 * 主界面功能页签。顺序可在设置中自定义；存储用稳定 [id]，缺项按默认顺序补齐。
 */
enum class DashboardTab(val id: String, @StringRes val labelRes: Int) {
    Overview("overview", R.string.tab_overview),
    Usage("usage", R.string.tab_usage),
    Billing("billing", R.string.tab_billing),
    Tasks("tasks", R.string.tab_tasks),
    Status("status", R.string.tab_status);

    companion object {
        val DEFAULT_ORDER: List<DashboardTab> = entries.toList()

        fun fromId(id: String): DashboardTab? =
            entries.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) }

        /**
         * 小组件点击打开应用时解析目标页签：优先 extra 里的页签 id，
         * 否则按小组件种类回退（状态迷你/详情 → 状态，用量迷你/详情 → 概览）。
         */
        fun fromWidgetLaunch(tabId: String?, widgetKind: String?): DashboardTab? {
            fromId(tabId.orEmpty())?.let { return it }
            return when (widgetKind?.trim()?.lowercase()) {
                "statusmini", "statustall" -> Status
                "mini", "tall" -> Overview
                else -> null
            }
        }

        /** 解析已存顺序：忽略未知 id、去重，并按默认顺序补上缺失页签。 */
        fun resolveOrder(storedIds: List<String>?): List<DashboardTab> {
            val seen = LinkedHashSet<DashboardTab>()
            storedIds.orEmpty().forEach { token ->
                fromId(token)?.let { seen += it }
            }
            entries.forEach { seen += it }
            return seen.toList()
        }

        fun resolveOrder(raw: String?): List<DashboardTab> = resolveOrder(
            raw?.split(',')
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() },
        )

        fun serialize(order: List<DashboardTab>): String =
            resolveOrder(order.map { it.id }).joinToString(",") { it.id }

        fun move(order: List<DashboardTab>, index: Int, delta: Int): List<DashboardTab> {
            val items = resolveOrder(order.map { it.id }).toMutableList()
            val target = index + delta
            if (index !in items.indices || target !in items.indices) return items
            val item = items.removeAt(index)
            items.add(target, item)
            return items
        }
    }
}
