package com.lamuier.cursorusage.data

import android.content.Context
import com.lamuier.cursorusage.BuildConfig
import com.lamuier.cursorusage.model.CursorAccount
import com.lamuier.cursorusage.util.TokenUtils
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class EncryptedAccountStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val crypto = KeystoreCrypto(KEY_ALIAS)
    private val lock = Any()

    init {
        migrateFromPulseClient(context.applicationContext)
        migrateToSingleAccount()
    }

    fun listAccounts(): List<CursorAccount> = synchronized(lock) {
        readStoredAccounts().map(::publicView)
    }

    fun create(alias: String, accessToken: String): CursorAccount = synchronized(lock) {
        val accounts = readStoredAccounts().toMutableList()
        require(accounts.isEmpty()) { "当前版本仅支持保存一个 Cursor 账号" }
        val normalizedAlias = normalizeAlias(alias)
        require(accounts.none { decryptAlias(it) == normalizedAlias }) { "别名已存在" }
        val normalizedToken = TokenUtils.requireValidAccessToken(accessToken)
        val now = System.currentTimeMillis()
        val uuid = UUID.randomUUID().toString()
        val minimumNextId = (accounts.maxOfOrNull { it.id } ?: 0) + 1
        val nextId = maxOf(preferences.getInt(KEY_NEXT_ID, minimumNextId), minimumNextId)
        val account = StoredAccount(
            id = nextId,
            uuid = uuid,
            encryptedAlias = crypto.encrypt(normalizedAlias, aad(uuid, "alias")),
            encryptedAccessToken = crypto.encrypt(normalizedToken, aad(uuid, "access_token")),
            tokenInvalid = false,
            revision = 1L,
            createdAt = now,
            updatedAt = now,
        )
        accounts += account
        saveStoredAccounts(accounts, nextId = nextId + 1)
        publicView(account)
    }

    fun update(accountId: Int, alias: String?, accessToken: String?): CursorAccount = synchronized(lock) {
        val accounts = readStoredAccounts().toMutableList()
        val index = accounts.indexOfFirst { it.id == accountId }
        require(index >= 0) { "账号不存在" }
        val current = accounts[index]
        require(alias != null || accessToken != null) { "别名或 Access Token 至少填写一项" }
        val nextAlias = alias?.let(::normalizeAlias)
        if (nextAlias != null) {
            require(
                accounts.none { it.id != accountId && decryptAlias(it) == nextAlias },
            ) { "别名已存在" }
        }
        val nextToken = accessToken?.let(TokenUtils::requireValidAccessToken)
        val updated = current.copy(
            encryptedAlias = nextAlias
                ?.let { crypto.encrypt(it, aad(current.uuid, "alias")) }
                ?: current.encryptedAlias,
            encryptedAccessToken = nextToken
                ?.let { crypto.encrypt(it, aad(current.uuid, "access_token")) }
                ?: current.encryptedAccessToken,
            tokenInvalid = if (nextToken != null) false else current.tokenInvalid,
            revision = current.revision + 1L,
            updatedAt = System.currentTimeMillis(),
        )
        accounts[index] = updated
        saveStoredAccounts(accounts)
        publicView(updated)
    }

    fun delete(accountId: Int): Boolean = synchronized(lock) {
        val accounts = readStoredAccounts()
        val remaining = accounts.filterNot { it.id == accountId }
        if (remaining.size == accounts.size) return@synchronized false
        val editor = preferences.edit().putString(KEY_ACCOUNTS, serializeAccounts(remaining).toString())
        if (preferences.getInt(KEY_SELECTED_ACCOUNT, -1) == accountId) {
            editor.remove(KEY_SELECTED_ACCOUNT)
        }
        check(editor.commit()) { "无法安全删除 Cursor 账号" }
        true
    }

    internal fun snapshot(accountId: Int): AccountSnapshot = synchronized(lock) {
        val account = readStoredAccounts().firstOrNull { it.id == accountId }
            ?: throw IllegalArgumentException("账号不存在")
        AccountSnapshot(
            id = account.id,
            alias = decryptAlias(account),
            accessToken = decryptAccessToken(account),
            revision = account.revision,
            tokenInvalid = account.tokenInvalid,
        )
    }

    fun revision(accountId: Int): Long = synchronized(lock) {
        readStoredAccounts().firstOrNull { it.id == accountId }?.revision
            ?: throw IllegalArgumentException("账号不存在")
    }

    fun revealAccessToken(accountId: Int): String = synchronized(lock) {
        val account = readStoredAccounts().firstOrNull { it.id == accountId }
            ?: throw IllegalArgumentException("账号不存在")
        decryptAccessToken(account)
    }

    internal fun isCurrent(accountId: Int, revision: Long): Boolean = synchronized(lock) {
        readStoredAccounts().any { it.id == accountId && it.revision == revision }
    }

    fun selectedAccountId(): Int? = synchronized(lock) {
        preferences.getInt(KEY_SELECTED_ACCOUNT, -1).takeIf { it > 0 }
    }

    fun saveSelectedAccountId(accountId: Int?) = synchronized(lock) {
        val editor = preferences.edit()
        if (accountId == null) editor.remove(KEY_SELECTED_ACCOUNT) else editor.putInt(KEY_SELECTED_ACCOUNT, accountId)
        check(editor.commit()) { "无法保存账号选择" }
    }

    internal fun pendingUsageCacheCleanupAccountIds(): List<Int> = synchronized(lock) {
        readPendingUsageCacheCleanupAccountIds()
    }

    internal fun acknowledgeUsageCacheCleanup(accountIds: Collection<Int>): Boolean = synchronized(lock) {
        if (accountIds.isEmpty()) return@synchronized true
        val clearedIds = accountIds.toSet()
        val remainingIds = readPendingUsageCacheCleanupAccountIds().filterNot(clearedIds::contains)
        val editor = preferences.edit()
        if (remainingIds.isEmpty()) {
            editor.remove(KEY_PENDING_USAGE_CACHE_CLEANUP)
        } else {
            editor.putString(KEY_PENDING_USAGE_CACHE_CLEANUP, serializeAccountIds(remainingIds).toString())
        }
        editor.commit()
    }

    internal fun markTokenInvalid(accountId: Int, revision: Long, invalid: Boolean): Boolean = synchronized(lock) {
        val accounts = readStoredAccounts().toMutableList()
        val index = accounts.indexOfFirst { it.id == accountId }
        if (index < 0 || accounts[index].revision != revision) return@synchronized false
        if (accounts[index].tokenInvalid == invalid) return@synchronized true
        accounts[index] = accounts[index].copy(tokenInvalid = invalid)
        saveStoredAccounts(accounts)
        true
    }

    private fun publicView(account: StoredAccount): CursorAccount {
        val alias = decryptAlias(account)
        val expired = account.tokenInvalid || try {
            TokenUtils.isExpired(decryptAccessToken(account))
        } catch (_: Exception) {
            true
        }
        return CursorAccount(
            id = account.id,
            alias = alias,
            tokenExpired = expired,
            createdAt = formatTime(account.createdAt),
            updatedAt = formatTime(account.updatedAt),
        )
    }

    private fun decryptAlias(account: StoredAccount): String = try {
        crypto.decrypt(account.encryptedAlias, aad(account.uuid, "alias"))
    } catch (error: Exception) {
        throw IllegalStateException("本地账号别名解密失败，请清除应用数据后重试", error)
    }

    private fun decryptAccessToken(account: StoredAccount): String = try {
        crypto.decrypt(account.encryptedAccessToken, aad(account.uuid, "access_token"))
    } catch (error: Exception) {
        throw IllegalStateException("账号凭据解密失败，请重新录入 Access Token", error)
    }

    private fun normalizeAlias(alias: String): String {
        val value = alias.trim()
        require(value.isNotBlank()) { "别名不能为空" }
        require(value.length <= 64) { "别名不能超过 64 个字符" }
        return value
    }

    private fun readStoredAccounts(): List<StoredAccount> {
        val raw = preferences.getString(KEY_ACCOUNTS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList(array.length()) {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        StoredAccount(
                            id = item.getInt("id"),
                            uuid = item.getString("uuid"),
                            encryptedAlias = item.getString("alias_enc"),
                            encryptedAccessToken = item.getString("access_token_enc"),
                            tokenInvalid = item.optBoolean("token_invalid"),
                            revision = item.optLong("revision", 1L),
                            createdAt = item.optLong("created_at"),
                            updatedAt = item.optLong("updated_at"),
                        ),
                    )
                }
            }
        } catch (error: Exception) {
            throw IllegalStateException("本地账号数据损坏，请清除应用数据后重试", error)
        }
    }

    private fun saveStoredAccounts(accounts: List<StoredAccount>, nextId: Int? = null) {
        val editor = preferences.edit().putString(KEY_ACCOUNTS, serializeAccounts(accounts).toString())
        if (nextId != null) editor.putInt(KEY_NEXT_ID, nextId)
        check(editor.commit()) {
            "无法安全保存 Cursor 账号"
        }
    }

    private fun serializeAccounts(accounts: List<StoredAccount>): JSONArray {
        val array = JSONArray()
        accounts.forEach { account ->
            array.put(
                JSONObject()
                    .put("id", account.id)
                    .put("uuid", account.uuid)
                    .put("alias_enc", account.encryptedAlias)
                    .put("access_token_enc", account.encryptedAccessToken)
                    .put("token_invalid", account.tokenInvalid)
                    .put("revision", account.revision)
                    .put("created_at", account.createdAt)
                    .put("updated_at", account.updatedAt),
            )
        }
        return array
    }

    private fun migrateToSingleAccount() = synchronized(lock) {
        val accounts = readStoredAccounts()
        if (accounts.size <= 1) return@synchronized

        val selectedAccountId = preferences.getInt(KEY_SELECTED_ACCOUNT, -1)
        val retainedIndex = accounts.indexOfFirst { it.id == selectedAccountId }
            .takeIf { it >= 0 }
            ?: 0
        val retainedAccount = accounts[retainedIndex]
        val evictedAccountIds = accounts
            .filterIndexed { index, _ -> index != retainedIndex }
            .map { it.id }
        val pendingCleanupIds = (
            readPendingUsageCacheCleanupAccountIds() + evictedAccountIds
        ).distinct()

        val editor = preferences.edit()
            .putString(KEY_ACCOUNTS, serializeAccounts(listOf(retainedAccount)).toString())
            .putInt(KEY_SELECTED_ACCOUNT, retainedAccount.id)
        if (pendingCleanupIds.isEmpty()) {
            editor.remove(KEY_PENDING_USAGE_CACHE_CLEANUP)
        } else {
            editor.putString(
                KEY_PENDING_USAGE_CACHE_CLEANUP,
                serializeAccountIds(pendingCleanupIds).toString(),
            )
        }
        check(editor.commit()) { "无法安全迁移为单 Cursor 账号" }
    }

    private fun readPendingUsageCacheCleanupAccountIds(): List<Int> {
        val raw = preferences.getString(KEY_PENDING_USAGE_CACHE_CLEANUP, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList(array.length()) {
                for (index in 0 until array.length()) {
                    add(array.getInt(index))
                }
            }.distinct()
        } catch (error: Exception) {
            throw IllegalStateException("本地账号迁移状态损坏，请清除应用数据后重试", error)
        }
    }

    private fun serializeAccountIds(accountIds: Collection<Int>): JSONArray {
        val array = JSONArray()
        accountIds.forEach { accountId -> array.put(accountId) }
        return array
    }

    private fun migrateFromPulseClient(context: Context) {
        if (preferences.getBoolean(KEY_MIGRATED_FROM_PULSE, false)) return
        context.getSharedPreferences("pulse_session_v1", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSharedPreferences("cursor_usage_cache_v1", Context.MODE_PRIVATE).edit().clear().apply()
        runCatching {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (keyStore.containsAlias("cursor_pulse_session_key_v1")) {
                keyStore.deleteEntry("cursor_pulse_session_key_v1")
            }
        }
        preferences.edit().putBoolean(KEY_MIGRATED_FROM_PULSE, true).apply()
    }

    private fun formatTime(epochMillis: Long): String = SimpleDateFormat(
        "yyyy-MM-dd HH:mm:ss",
        Locale.getDefault(),
    ).format(Date(epochMillis))

    private fun aad(uuid: String, field: String): String =
        "${BuildConfig.APPLICATION_ID}|accounts-v2|$uuid|$field"

    private data class StoredAccount(
        val id: Int,
        val uuid: String,
        val encryptedAlias: String,
        val encryptedAccessToken: String,
        val tokenInvalid: Boolean,
        val revision: Long,
        val createdAt: Long,
        val updatedAt: Long,
    )

    internal class AccountSnapshot(
        val id: Int,
        val alias: String,
        val accessToken: String,
        val revision: Long,
        val tokenInvalid: Boolean,
    )

    private companion object {
        const val PREFERENCES_NAME = "cursor_direct_accounts_v2"
        const val KEY_ACCOUNTS = "accounts"
        const val KEY_SELECTED_ACCOUNT = "selected_account"
        const val KEY_NEXT_ID = "next_id"
        const val KEY_PENDING_USAGE_CACHE_CLEANUP = "pending_usage_cache_cleanup"
        const val KEY_MIGRATED_FROM_PULSE = "migrated_from_pulse_v1"
        const val KEY_ALIAS = "cursor_direct_accounts_key_v2"
    }
}
