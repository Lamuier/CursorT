package com.lamuier.cursorT.data

import android.content.Context
import com.lamuier.cursorT.model.DashboardTab
import com.lamuier.cursorT.model.TaskGroupMode
import com.lamuier.cursorT.util.AppLanguage
import com.lamuier.cursorT.util.AppLocale
import com.lamuier.cursorT.util.DisplayTimeZones
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 概览英雄卡外环样式。默认分列自有 / 三方；[Combined] 为改版前的总用量单环。
 */
enum class OverviewUsageRingMode(val storageKey: String) {
    Split("split"),
    Combined("combined");

    companion object {
        fun fromStorage(value: String?): OverviewUsageRingMode {
            val raw = value?.trim().orEmpty()
            if (raw.isEmpty()) return Split
            return entries.firstOrNull { it.storageKey.equals(raw, ignoreCase = true) } ?: Split
        }
    }
}

/** 主界面功能页签顺序、概览圆环样式、任务分组方式、展示时区与界面语言，保存在本机 SharedPreferences。 */
class DashboardPreferences(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _order = MutableStateFlow(read())
    val order: StateFlow<List<DashboardTab>> = _order.asStateFlow()

    private val _taskGroupMode = MutableStateFlow(readTaskGroupMode())
    val taskGroupMode: StateFlow<TaskGroupMode> = _taskGroupMode.asStateFlow()

    private val _timeZoneId = MutableStateFlow(readTimeZoneId())
    val timeZoneId: StateFlow<String> = _timeZoneId.asStateFlow()

    private val _language = MutableStateFlow(readLanguage())
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _overviewUsageRingMode = MutableStateFlow(readOverviewUsageRingMode())
    val overviewUsageRingMode: StateFlow<OverviewUsageRingMode> = _overviewUsageRingMode.asStateFlow()

    private val _overviewRingCollapseKeys = MutableStateFlow(readOverviewRingCollapseKeys())
    val overviewRingCollapseKeys: StateFlow<Set<String>> = _overviewRingCollapseKeys.asStateFlow()

    fun read(): List<DashboardTab> =
        DashboardTab.resolveOrder(preferences.getString(KEY_TAB_ORDER, null))

    fun setOrder(order: List<DashboardTab>) {
        val serialized = DashboardTab.serialize(order)
        preferences.edit().putString(KEY_TAB_ORDER, serialized).apply()
        _order.value = DashboardTab.resolveOrder(serialized)
    }

    fun resetOrder() {
        preferences.edit().remove(KEY_TAB_ORDER).apply()
        _order.value = DashboardTab.DEFAULT_ORDER
    }

    fun readTaskGroupMode(): TaskGroupMode =
        TaskGroupMode.fromStorage(preferences.getString(KEY_TASK_GROUP_MODE, null))

    fun setTaskGroupMode(mode: TaskGroupMode) {
        preferences.edit().putString(KEY_TASK_GROUP_MODE, mode.storageKey).apply()
        _taskGroupMode.value = mode
    }

    fun readTimeZoneId(): String =
        DisplayTimeZones.fromStorage(preferences.getString(KEY_TIME_ZONE, null))

    fun setTimeZoneId(id: String) {
        val stored = DisplayTimeZones.fromStorage(id)
        preferences.edit().putString(KEY_TIME_ZONE, stored).apply()
        _timeZoneId.value = stored
    }

    fun readLanguage(): AppLanguage =
        AppLanguage.fromStorage(preferences.getString(AppLocale.KEY_LANGUAGE, null))

    fun setLanguage(language: AppLanguage) {
        preferences.edit().putString(AppLocale.KEY_LANGUAGE, language.storageKey).apply()
        _language.value = language
    }

    fun readOverviewUsageRingMode(): OverviewUsageRingMode =
        OverviewUsageRingMode.fromStorage(preferences.getString(KEY_OVERVIEW_USAGE_RING, null))

    fun setOverviewUsageRingMode(mode: OverviewUsageRingMode) {
        preferences.edit().putString(KEY_OVERVIEW_USAGE_RING, mode.storageKey).apply()
        _overviewUsageRingMode.value = mode
    }

    fun syncOverviewRingCollapse(accountId: Int, cycleStart: String?, markNow: Boolean) {
        val next = OverviewRingCollapse.syncedKeys(
            stored = _overviewRingCollapseKeys.value,
            accountId = accountId,
            cycleStart = cycleStart,
            markNow = markNow,
        )
        if (next == _overviewRingCollapseKeys.value) return
        preferences.edit().putStringSet(KEY_OVERVIEW_RING_COLLAPSE_KEYS, next).apply()
        _overviewRingCollapseKeys.value = next
    }

    private fun readOverviewRingCollapseKeys(): Set<String> =
        preferences.getStringSet(KEY_OVERVIEW_RING_COLLAPSE_KEYS, emptySet())?.toSet().orEmpty()

    companion object {
        private const val PREFERENCES_NAME = AppLocale.PREFERENCES_NAME
        private const val KEY_TAB_ORDER = "tab_order"
        private const val KEY_TASK_GROUP_MODE = "task_group_mode"
        private const val KEY_TIME_ZONE = "time_zone"
        private const val KEY_OVERVIEW_USAGE_RING = "overview_usage_ring"
        private const val KEY_OVERVIEW_RING_COLLAPSE_KEYS = "overview_ring_collapse_keys"

        @Volatile
        private var instance: DashboardPreferences? = null

        fun get(context: Context): DashboardPreferences {
            return instance ?: synchronized(this) {
                instance ?: DashboardPreferences(context).also { instance = it }
            }
        }
    }
}
