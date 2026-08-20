package com.lamuier.cursorT.model

/**
 * 主界面功能页签。顺序可在设置中自定义；存储用稳定 [id]，缺项按默认顺序补齐。
 */
enum class DashboardTab(val id: String, val label: String) {
    Overview("overview", "概览"),
    Usage("usage", "用量"),
    Billing("billing", "账单"),
    Tasks("tasks", "任务"),
    Status("status", "状态");

    companion object {
        val DEFAULT_ORDER: List<DashboardTab> = entries.toList()

        fun fromId(id: String): DashboardTab? =
            entries.firstOrNull { it.id.equals(id.trim(), ignoreCase = true) }

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
