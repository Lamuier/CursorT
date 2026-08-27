package com.lamuier.cursorT.data

import android.content.Context
import com.lamuier.cursorT.model.CursorAccount
import com.lamuier.cursorT.model.CursorTasks
import com.lamuier.cursorT.model.CursorTOverview
import com.lamuier.cursorT.network.ApiException
import com.lamuier.cursorT.network.CursorApiClient
import com.lamuier.cursorT.network.CursorTAssembler
import com.lamuier.cursorT.network.TasksJsonParser
import com.lamuier.cursorT.model.UsageWindow
import com.lamuier.cursorT.network.UsageJsonParser
import com.lamuier.cursorT.util.TokenUtils
import com.lamuier.cursorT.util.UsageHistoryWindows
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
    private val tasksCache = TasksCacheStore(context.applicationContext)

    init {
        clearMigratedAccountUsageCaches()
    }

    fun listAccounts(): List<CursorAccount> = accountStore.listAccounts()

    fun addAccount(alias: String, accessToken: String): CursorAccount =
        accountStore.create(alias, accessToken)

    fun updateAccount(accountId: Int, alias: String?, accessToken: String?): CursorAccount {
        val updated = accountStore.update(accountId, alias, accessToken)
        invalidateUsage(accountId)
        invalidateTasks(accountId)
        return updated
    }

    fun deleteAccount(accountId: Int) {
        require(accountStore.delete(accountId)) { "账号不存在" }
        invalidateUsage(accountId)
        invalidateTasks(accountId)
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
    ): CursorTOverview? {
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

    suspend fun fetchUsage(accountId: Int, forceRefresh: Boolean): CursorTOverview {
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

    fun cachedTasks(
        accountId: Int,
        allowInvalidCredential: Boolean = false,
    ): CursorTasks? {
        val snapshot = runCatching { accountStore.snapshot(accountId) }.getOrNull() ?: return null
        if (snapshot.tokenInvalid || TokenUtils.isExpired(snapshot.accessToken)) {
            markCredentialInvalid(snapshot)
            if (!allowInvalidCredential) return null
        }
        val cached = tasksCache.read(accountId, snapshot.revision) ?: return null
        val stored = runCatching {
            TasksJsonParser.parseStored(
                cached.rawJson,
                accountId = accountId,
                fetchedAt = TasksJsonParser.nowStamp(),
                cacheAgeSeconds = ageSeconds(cached.storedAtMillis),
            )
        }.getOrElse {
            tasksCache.remove(accountId)
            return null
        }
        if (stored.accountId != accountId || !accountStore.isCurrent(accountId, snapshot.revision)) {
            tasksCache.remove(accountId)
            return null
        }
        return stored
    }

    suspend fun fetchTasks(accountId: Int, forceRefresh: Boolean): CursorTasks {
        val snapshot = accountStore.snapshot(accountId)
        if (TokenUtils.isExpired(snapshot.accessToken) || (snapshot.tokenInvalid && !forceRefresh)) {
            markCredentialInvalid(snapshot)
            throw ApiException(401, "Cursor Access Token 已过期或无效，请更新 Token")
        }

        if (!forceRefresh) {
            freshTasksMemoryEntry(snapshot)?.let { entry ->
                return entry.tasks.copy(
                    fromCache = true,
                    cacheAgeSeconds = ageSeconds(entry.storedAtMillis),
                )
            }
        }

        val requestKey = NetworkRequestKey(accountId, snapshot.revision)
        val live = singleFlightTasks(requestKey) {
            try {
                val payload = api.agentTasks(snapshot.accessToken)
                ensureCurrent(snapshot)
                if (!accountStore.markTokenInvalid(accountId, snapshot.revision, invalid = false)) {
                    throw AccountRevisionChangedException()
                }
                val fetched = TasksJsonParser.parse(payload, accountId = snapshot.id)
                val storedAtMillis = System.currentTimeMillis()
                runCatching {
                    tasksCache.write(accountId, snapshot.revision, TasksJsonParser.toJson(fetched), storedAtMillis)
                }
                if (!accountStore.isCurrent(accountId, snapshot.revision)) {
                    tasksCache.remove(accountId, snapshot.revision)
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
        synchronized(tasksMemoryLock) {
            tasksMemoryCache[accountId] = TasksMemoryEntry(snapshot.revision, storedAtMillis, live)
        }
        if (!accountStore.isCurrent(accountId, snapshot.revision)) {
            invalidateTasks(accountId, snapshot.revision)
            throw AccountRevisionChangedException()
        }
        return live
    }

    private suspend fun singleFlight(
        key: NetworkRequestKey,
        block: suspend () -> CursorTOverview,
    ): CursorTOverview {
        val candidate = CompletableDeferred<CursorTOverview>()
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

    private suspend fun singleFlightTasks(
        key: NetworkRequestKey,
        block: suspend () -> CursorTasks,
    ): CursorTasks {
        val candidate = CompletableDeferred<CursorTasks>()
        val active = activeTasksRequests.putIfAbsent(key, candidate)
        if (active != null) return active.await()
        requestScope.launch {
            try {
                candidate.complete(block())
            } catch (error: Throwable) {
                candidate.completeExceptionally(error)
            } finally {
                activeTasksRequests.remove(key, candidate)
            }
        }
        return candidate.await()
    }

    private suspend fun fetchLive(
        snapshot: EncryptedAccountStore.AccountSnapshot,
    ): CursorTOverview = coroutineScope {
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
        val aggregations = async {
            // Token 明细为增强信息：失败时仍展示花费 / 百分比用量。
            optionalPayload(authenticationFailureIsFatal = false) {
                api.connectRpc(snapshot.accessToken, "GetAggregatedUsageEvents")
            }
        }
        val grokBot = async {
            // Grok Bot 周额度独立于月度用量池：401/403 不得拖垮整次刷新。
            optionalPayload(authenticationFailureIsFatal = false) {
                api.connectRpc(snapshot.accessToken, "GetSandUsageStatus")
            }
        }
        val stripe = async {
            // Stripe is supplementary. Its cookie contract may fail independently
            // even while the primary Bearer endpoints still accept the token.
            optionalPayload(authenticationFailureIsFatal = false) { api.stripe(snapshot.accessToken) }
        }

        val periodUsage = period.await()
        val prevRange = UsageHistoryWindows.previousBillingCycle(
            UsageHistoryWindows.parseEpochMillis(periodUsage.opt("billingCycleStart")),
            UsageHistoryWindows.parseEpochMillis(periodUsage.opt("billingCycleEnd")),
        )
        val (monthKeyYm, monthRange) = UsageHistoryWindows.currentCalendarMonth()
        val previousCycle = async {
            if (prevRange == null) {
                OptionalPayload(payload = null, partial = true)
            } else {
                optionalPayload(authenticationFailureIsFatal = false) {
                    api.connectRpc(
                        snapshot.accessToken,
                        "GetAggregatedUsageEvents",
                        aggregationsRangeBody(prevRange.startMs, prevRange.endMs),
                    )
                }
            }
        }
        val calendarMonth = async {
            optionalPayload(authenticationFailureIsFatal = false) {
                api.connectRpc(
                    snapshot.accessToken,
                    "GetAggregatedUsageEvents",
                    aggregationsRangeBody(monthRange.startMs, monthRange.endMs),
                )
            }
        }

        val grantsResult = grants.await()
        val aggregationsResult = aggregations.await()
        val grokBotResult = grokBot.await()
        val stripeResult = stripe.await()
        val previousCycleResult = previousCycle.await()
        val calendarMonthResult = calendarMonth.await()
        CursorTAssembler.assemble(
            accountId = snapshot.id,
            alias = snapshot.alias,
            periodUsage = periodUsage,
            planPayload = plan.await(),
            grantsPayload = grantsResult.payload,
            stripePayload = stripeResult.payload,
            aggregationsPayload = aggregationsResult.payload,
            grokBotPayload = grokBotResult.payload,
            previousCyclePayload = previousCycleResult.payload,
            previousCycleStartMs = prevRange?.startMs,
            previousCycleEndMs = prevRange?.endMs,
            calendarMonthPayload = calendarMonthResult.payload,
            calendarMonthStartMs = monthRange.startMs,
            calendarMonthEndMs = monthRange.endMs,
            calendarMonthKey = UsageHistoryWindows.yearMonthKey(monthKeyYm),
            partialData = grantsResult.partial || stripeResult.partial ||
                aggregationsResult.partial || grokBotResult.partial,
        )
    }

    suspend fun fetchMonthHistory(accountId: Int, yearMonthKey: String): UsageWindow {
        val snapshot = accountStore.snapshot(accountId)
        val yearMonth = UsageHistoryWindows.parseYearMonth(yearMonthKey)
            ?: throw ApiException(400, "无效的自然月")
        val range = UsageHistoryWindows.calendarMonth(yearMonth)
        val payload = optionalPayload(authenticationFailureIsFatal = false) {
            api.connectRpc(
                snapshot.accessToken,
                "GetAggregatedUsageEvents",
                aggregationsRangeBody(range.startMs, range.endMs),
            )
        }
        return CursorTAssembler.usageWindow(
            startMs = range.startMs,
            endMs = range.endMs,
            payload = payload.payload,
            yearMonth = yearMonthKey,
        ) ?: UsageWindow(
            start = null,
            end = null,
            yearMonth = yearMonthKey,
            tokenUsage = null,
        )
    }

    private fun aggregationsRangeBody(startMs: Long, endMs: Long): JSONObject =
        JSONObject()
            .put("startDate", startMs.toString())
            .put("endDate", endMs.toString())

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

    private fun freshTasksMemoryEntry(
        snapshot: EncryptedAccountStore.AccountSnapshot,
    ): TasksMemoryEntry? = synchronized(tasksMemoryLock) {
        val entry = tasksMemoryCache[snapshot.id] ?: return@synchronized null
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

    private fun invalidateTasks(accountId: Int, revision: Long) {
        synchronized(tasksMemoryLock) {
            if (tasksMemoryCache[accountId]?.revision == revision) tasksMemoryCache.remove(accountId)
        }
        tasksCache.remove(accountId, revision)
    }

    private fun invalidateTasks(accountId: Int) {
        synchronized(tasksMemoryLock) { tasksMemoryCache.remove(accountId) }
        tasksCache.remove(accountId)
    }

    private fun clearMigratedAccountUsageCaches() {
        val pendingAccountIds = accountStore.pendingUsageCacheCleanupAccountIds()
        val clearedAccountIds = pendingAccountIds.filter { accountId -> usageCache.remove(accountId) }
        if (clearedAccountIds.isNotEmpty()) {
            accountStore.acknowledgeUsageCacheCleanup(clearedAccountIds)
        }
    }

    private fun CursorTOverview.asCached(ageSeconds: Int, isLocal: Boolean): CursorTOverview = copy(
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
        val overview: CursorTOverview,
    )

    private data class TasksMemoryEntry(
        val revision: Long,
        val storedAtMillis: Long,
        val tasks: CursorTasks,
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
        val tasksMemoryCache = mutableMapOf<Int, TasksMemoryEntry>()
        val tasksMemoryLock = Any()
        val requestScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val activeUsageRequests = ConcurrentHashMap<
            NetworkRequestKey,
            CompletableDeferred<CursorTOverview>
        >()
        val activeTasksRequests = ConcurrentHashMap<
            NetworkRequestKey,
            CompletableDeferred<CursorTasks>
        >()
    }
}
