package com.lamuier.cursorusage.data

import android.content.Context
import org.json.JSONObject

/** 公开状态页数据，无需加密；仅用于离线回显最近一次成功结果。 */
class StatusCacheStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun read(): CacheEntry? {
        val raw = preferences.getString(KEY_PAYLOAD, null) ?: return null
        val storedAt = preferences.getLong(KEY_STORED_AT, 0L)
        if (raw.isBlank() || storedAt <= 0L) return null
        return try {
            val wrapper = JSONObject(raw)
            if (wrapper.optInt("schema") != CACHE_SCHEMA) return null
            CacheEntry(rawJson = raw, storedAtMillis = storedAt)
        } catch (_: Exception) {
            preferences.edit().clear().apply()
            null
        }
    }

    fun write(rawJson: String, storedAtMillis: Long): Boolean =
        preferences.edit()
            .putString(KEY_PAYLOAD, rawJson)
            .putLong(KEY_STORED_AT, storedAtMillis)
            .commit()

    fun clear(): Boolean = preferences.edit().clear().commit()

    data class CacheEntry(
        val rawJson: String,
        val storedAtMillis: Long,
    )

    private companion object {
        const val CACHE_SCHEMA = 1
        const val PREFERENCES_NAME = "cursor_status_cache_v1"
        const val KEY_PAYLOAD = "payload"
        const val KEY_STORED_AT = "stored_at"
    }
}
