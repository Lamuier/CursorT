package com.lamuier.cursorT.data

import android.content.Context
import com.lamuier.cursorT.model.DashboardTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 主界面功能页签顺序，保存在本机 SharedPreferences。 */
class DashboardPreferences(context: Context) {
    private val preferences = context.applicationContext
        .getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    private val _order = MutableStateFlow(read())
    val order: StateFlow<List<DashboardTab>> = _order.asStateFlow()

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

    companion object {
        private const val PREFERENCES_NAME = "cursor_pulse_dashboard_v1"
        private const val KEY_TAB_ORDER = "tab_order"

        @Volatile
        private var instance: DashboardPreferences? = null

        fun get(context: Context): DashboardPreferences {
            return instance ?: synchronized(this) {
                instance ?: DashboardPreferences(context).also { instance = it }
            }
        }
    }
}
