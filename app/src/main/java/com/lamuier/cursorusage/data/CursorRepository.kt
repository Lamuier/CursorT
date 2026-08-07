package com.lamuier.cursorusage.data

import android.content.Context
import com.lamuier.cursorusage.model.CursorAccount
import com.lamuier.cursorusage.model.CursorUsageOverview
import com.lamuier.cursorusage.network.ApiException
import com.lamuier.cursorusage.network.CursorApiClient
import com.lamuier.cursorusage.network.CursorUsageAssembler
import com.lamuier.cursorusage.network.UsageJsonParser
import com.lamuier.cursorusage.util.TokenUtils
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException

class AccountRevisionChangedException : IOException("Cursor 账号已发生变化，已忽略旧请求结果")

class CursorRepository(context: Context) {
    private val api = CursorApiClient()
    private val accountStore = EncryptedAccountStore(context.applicationContext)
    private val usageCache = UsageCacheStore(context.applicationContext)

    init {
        clearMigratedAccountUsageCaches()
    }

    fun listAccounts(): List<CursorAccount> = accountStore.listAccounts()

    fun addAccount(alias: String, accessToken: String): CursorAccount =
        accountStore.create(alias, accessToken)

    fun updateAccount(accountId: Int, alias: String?, accessToken: String?): CursorAccount {
        val updated = accountStore.update(accountId, alias, accessToken)
        invalidateUsage(accountId)
        return updated
    }

    fun deleteAccount(accountId: Int) {
        require(accountStore.delete(accountId)) { "账号不存在" }
        invalidateUsage(accountId)
    }

    fun selectedAccountId(): Int? = accountStore.selectedAccountId()

    fun revealAccessToken(accountId: Int): String = accountStore.revealAccessToken(accountId)

    fun saveSelectedAccountId(accountId: Int?) {
        accountStore.saveSelectedAccountId(accountId)
    }

    fun accountRevision(accountId: Int): Long = accountStore.revision(accountId)

    fun cachedUsage(
        accountId: Int,
        allowInvalidCredential: Boolean = false,
    ): CursorUsageOverview? {
        val snapshot = runCatching { accountStore.snapshot(accountId) }.getOrNull() ?: return null
        if (snapshot.tokenInvalid || TokenUtils.isExpired(snapshot.accessToken)) {
            markCredentialInvalid(snapshot)
            if (!allowInvalidCredential) return null
        }
        val cached = usageCache.read(accountId, snapshot.revision) ?: return null
        val overview = runCatching { UsageJsonParser.parseUsage(cached.rawJson, isLocalCache = true) }
            .getOrElse {
                usageCache.remove(accountId)
                return null
            }
        if (overview.accountId != accountId || !accountStore.isCurrent(accountId, snapshot.revision)) {
            usageCache.remove(accountId)
            return null
        }
        return overview.asCached(ageSeconds(cached.storedAtMillis), isLocal = true)
    }

    suspend fun fetchUsage(accountId: Int, forceRefresh: Boolean): CursorUsageOverview {
        val snapshot = accountStore.snapshot(accountId)
        if (TokenUtils.isExpired(snapshot.accessToken) || (snapshot.tokenInvalid && !forceRefresh)) {
            markCredentialInvalid(snapshot)
            throw ApiException(401, "Cursor Access Token 已过期或无效，请更新 Token")
        }

        if (!forceRefresh) {
            freshMemoryEntry(snapshot)?.let { entry ->
                return entry.overview.asCached(ageSeconds(entry.storedAtMillis), isLocal = false)
            }
        }

        val requestKey = NetworkRequestKey(accountId, snapshot.revision)
        val live = singleFlight(requestKey) {
            try {
                val fetched = fetchLive(snapshot)
                ensureCurrent(snapshot)
                if (!accountStore.markTokenInvalid(accountId, snapshot.revision, invalid = false)) {
                    throw AccountRevisionChangedException()
                }
                val storedAtMillis = System.currentTimeMillis()
                val normalizedJson = UsageJsonParser.toJson(fetched)
                runCatching {
                    usageCache.write(accountId, snapshot.revision, normalizedJson, storedAtMillis)
                }
                if (!accountStore.isCurrent(accountId, snapshot.revision)) {
                    usageCache.remove(accountId, snapshot.revision)
                    throw AccountRevisionChangedException()
                }
                fetched
            } catch (error: ApiException) {
                if (error.statusCode == 401 || error.statusCode == 403) {
                    if (accountStore.isCurrent(accountId, snapshot.revision)) {
                        if (!accountStore.markTokenInvalid(accountId, snapshot.revision, invalid = true)) {
                            throw AccountRevisionChangedException()
                        }
                    } else {
                        throw AccountRevisionChangedException()
                    }
                }
                throw error
            }
        }
        ensureCurrent(snapshot)
        val storedAtMillis = System.currentTimeMillis()
        synchronized(memoryLock) {
            memoryCache[accountId] = MemoryCacheEntry(snapshot.revision, storedAtMillis, live)
        }
        if (!accountStore.isCurrent(accountId, snapshot.revision)) {
            invalidateUsage(accountId, snapshot.revision)
            throw AccountRevisionChangedException()
        }
        return live
    }

    private suspend fun singleFlight(
        key: NetworkRequestKey,
        block: suspend () -> CursorUsageOverview,
    ): CursorUsageOverview {
        val candidate = CompletableDeferred<CursorUsageOverview>()
        val active = activeUsageRequests.putIfAbsent(key, candidate)
        if (active != null) return active.await()
        requestScope.launch {
            try {
                candidate.complete(block())
            } catch (error: Throwable) {
                candidate.completeExceptionally(error)
            } finally {
                activeUsageRequests.remove(key, candidate)
            }
        }
        return candidate.await()
    }

    private suspend fun fetchLive(
        snapshot: EncryptedAccountStore.AccountSnapshot,
    ): CursorUsageOverview = coroutineScope {
        val period = async {
            api.connectRpc(snapshot.accessToken, "GetCurrentPeriodUsage")
        }
        val plan = async {
            api.connectRpc(snapshot.accessToken, "GetPlanInfo")
        }
        val grants = async {
            optionalPayload(authenticationFailureIsFatal = false) {
                api.connectRpc(snapshot.accessToken, "GetUsageLimitStatusAndActiveGrants")
            }
        }
        val stripe = async {
            // Stripe is supplementary. Its cookie contract may fail independently
            // even while the primary Bearer endpoints still accept the token.
            optionalPayload(authenticationFailureIsFatal = false) { api.stripe(snapshot.accessToken) }
        }

        val grantsResult = grants.await()
        val stripeResult = stripe.await()
        CursorUsageAssembler.assemble(
            accountId = snapshot.id,
            alias = snapshot.alias,
            periodUsage = period.await(),
            planPayload = plan.await(),
            grantsPayload = grantsResult.payload,
            stripePayload = stripeResult.payload,
            partialData = grantsResult.partial || stripeResult.partial,
        )
    }

    private suspend fun optionalPayload(
        authenticationFailureIsFatal: Boolean,
        block: suspend () -> JSONObject,
    ): OptionalPayload = try {
        OptionalPayload(block(), partial = false)
    } catch (error: CancellationException) {
        throw error
    } catch (error: ApiException) {
        if (authenticationFailureIsFatal && (error.statusCode == 401 || error.statusCode == 403)) throw error
        OptionalPayload(payload = null, partial = true)
    } catch (_: Exception) {
        OptionalPayload(payload = null, partial = true)
    }

    private fun freshMemoryEntry(
        snapshot: EncryptedAccountStore.AccountSnapshot,
    ): MemoryCacheEntry? = synchronized(memoryLock) {
        val entry = memoryCache[snapshot.id] ?: return@synchronized null
        val age = System.currentTimeMillis() - entry.storedAtMillis
        entry.takeIf { it.revision == snapshot.revision && age in 0 until MEMORY_TTL_MILLIS }
    }

    private fun ensureCurrent(snapshot: EncryptedAccountStore.AccountSnapshot) {
        if (!accountStore.isCurrent(snapshot.id, snapshot.revision)) {
            throw AccountRevisionChangedException()
        }
    }

    private fun markCredentialInvalid(snapshot: EncryptedAccountStore.AccountSnapshot) {
        if (accountStore.isCurrent(snapshot.id, snapshot.revision)) {
            accountStore.markTokenInvalid(snapshot.id, snapshot.revision, invalid = true)
        }
    }

    private fun invalidateUsage(accountId: Int, revision: Long) {
        synchronized(memoryLock) {
            if (memoryCache[accountId]?.revision == revision) memoryCache.remove(accountId)
        }
        usageCache.remove(accountId, revision)
    }

    private fun invalidateUsage(accountId: Int) {
        synchronized(memoryLock) { memoryCache.remove(accountId) }
        usageCache.remove(accountId)
    }

    private fun clearMigratedAccountUsageCaches() {
        val pendingAccountIds = accountStore.pendingUsageCacheCleanupAccountIds()
        val clearedAccountIds = pendingAccountIds.filter { accountId -> usageCache.remove(accountId) }
        if (clearedAccountIds.isNotEmpty()) {
            accountStore.acknowledgeUsageCacheCleanup(clearedAccountIds)
        }
    }

    private fun CursorUsageOverview.asCached(ageSeconds: Int, isLocal: Boolean): CursorUsageOverview = copy(
        fromCache = true,
        cacheAgeSeconds = ageSeconds,
        isLocalCache = isLocal,
    )

    private fun ageSeconds(storedAtMillis: Long): Int =
        ((System.currentTimeMillis() - storedAtMillis).coerceAtLeast(0L) / 1_000L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()

    private data class MemoryCacheEntry(
        val revision: Long,
        val storedAtMillis: Long,
        val overview: CursorUsageOverview,
    )

    private data class OptionalPayload(
        val payload: JSONObject?,
        val partial: Boolean,
    )

    private data class NetworkRequestKey(
        val accountId: Int,
        val revision: Long,
    )

    private companion object {
        const val MEMORY_TTL_MILLIS = 60_000L
        val memoryCache = mutableMapOf<Int, MemoryCacheEntry>()
        val memoryLock = Any()
        val requestScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val activeUsageRequests = ConcurrentHashMap<
            NetworkRequestKey,
            CompletableDeferred<CursorUsageOverview>
        >()
    }
}
