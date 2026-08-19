package com.lamuier.cursorT.data

import android.content.Context
import com.lamuier.cursorT.BuildConfig
import org.json.JSONObject

/** 云端任务列表缓存：与用量缓存同为账号敏感数据，按 账号+凭据修订号 加密存储。 */
class TasksCacheStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val crypto = KeystoreCrypto(KEY_ALIAS)

    fun read(accountId: Int, revision: Long): CacheEntry? {
        val encrypted = preferences.getString(key(accountId, revision), null) ?: return null
        return try {
            val wrapper = JSONObject(crypto.decrypt(encrypted, aad(accountId, revision)))
            if (wrapper.optInt("schema") != CACHE_SCHEMA) return null
            CacheEntry(
                rawJson = wrapper.getJSONObject("tasks").toString(),
                storedAtMillis = wrapper.getLong("stored_at"),
            )
        } catch (_: Exception) {
            preferences.edit().remove(key(accountId, revision)).apply()
            null
        }
    }

    fun write(accountId: Int, revision: Long, rawJson: String, storedAtMillis: Long): Boolean {
        val wrapper = JSONObject()
            .put("schema", CACHE_SCHEMA)
            .put("stored_at", storedAtMillis)
            .put("tasks", JSONObject(rawJson))
        val encrypted = crypto.encrypt(wrapper.toString(), aad(accountId, revision))
        val prefix = prefix(accountId)
        val editor = preferences.edit()
        preferences.all.keys.filter { it.startsWith(prefix) }.forEach(editor::remove)
        return editor.putString(key(accountId, revision), encrypted).commit()
    }

    fun remove(accountId: Int): Boolean {
        val prefix = prefix(accountId)
        val editor = preferences.edit()
        preferences.all.keys.filter { it.startsWith(prefix) }.forEach(editor::remove)
        return editor.commit()
    }

    fun remove(accountId: Int, revision: Long): Boolean =
        preferences.edit().remove(key(accountId, revision)).commit()

    fun clear(): Boolean = preferences.edit().clear().commit()

    private fun key(accountId: Int, revision: Long): String = "tasks_${accountId}_r$revision"

    private fun prefix(accountId: Int): String = "tasks_${accountId}_r"

    private fun aad(accountId: Int, revision: Long): String =
        "${BuildConfig.APPLICATION_ID}|tasks-cache-v1|$accountId|$revision"

    private companion object {
        const val CACHE_SCHEMA = 1
        const val PREFERENCES_NAME = "cursor_tasks_cache_v1"
        const val KEY_ALIAS = "cursor_tasks_cache_key_v1"
    }

    data class CacheEntry(
        val rawJson: String,
        val storedAtMillis: Long,
    )
}
