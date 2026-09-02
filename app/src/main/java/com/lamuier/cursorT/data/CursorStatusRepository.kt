package com.lamuier.cursorT.data

import android.content.Context
import com.lamuier.cursorT.model.CursorServiceStatus
import com.lamuier.cursorT.network.CursorApiClient
import com.lamuier.cursorT.network.StatusJsonParser
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class CursorStatusRepository(
    context: Context,
) {
    private val api = CursorApiClient(context.applicationContext)
    private val cacheStore = StatusCacheStore(context.applicationContext)

    fun cached(): CursorServiceStatus? {
        synchronized(memoryLock) {
            memoryCache?.takeIf { ageMillis(it.storedAtMillis) in 0 until MEMORY_TTL_MILLIS }
                ?.let { return it.status.copy(fromCache = true, cacheAgeSeconds = ageSeconds(it.storedAtMillis)) }
        }
        val disk = cacheStore.read() ?: return null
        return runCatching {
            StatusJsonParser.parseCache(disk.rawJson, ageSeconds(disk.storedAtMillis))
        }.getOrElse {
            cacheStore.clear()
            null
        }
    }

    suspend fun fetch(forceRefresh: Boolean): CursorServiceStatus {
        if (!forceRefresh) {
            synchronized(memoryLock) {
                memoryCache?.takeIf { ageMillis(it.storedAtMillis) in 0 until MEMORY_TTL_MILLIS }
                    ?.let { return it.status.copy(fromCache = true, cacheAgeSeconds = ageSeconds(it.storedAtMillis)) }
            }
        }
        return singleFlight {
            val live = fetchLive()
            val storedAtMillis = System.currentTimeMillis()
            synchronized(memoryLock) {
                memoryCache = MemoryCacheEntry(storedAtMillis, live)
            }
            live
        }
    }

    private suspend fun fetchLive(): CursorServiceStatus = coroutineScope {
        val summaryDeferred = async { api.statusSummary() }
        val incidentsDeferred = async {
            runCatching { api.statusIncidents() }.getOrNull()
        }
        val summary = summaryDeferred.await()
        val incidents = incidentsDeferred.await()
        val fetchedAt = StatusJsonParser.nowStamp()
        runCatching {
            cacheStore.write(StatusJsonParser.toCacheJson(summary, incidents, fetchedAt), System.currentTimeMillis())
        }
        StatusJsonParser.parse(
            summary = summary,
            incidentsPayload = incidents,
            fetchedAt = fetchedAt,
            fromCache = false,
            partialHistory = incidents == null,
        )
    }

    private suspend fun singleFlight(block: suspend () -> CursorServiceStatus): CursorServiceStatus {
        val candidate = CompletableDeferred<CursorServiceStatus>()
        val previous = inFlight.getAndSet(candidate)
        if (previous != null) {
            inFlight.compareAndSet(candidate, previous)
            return previous.await()
        }
        requestScope.launch {
            try {
                candidate.complete(block())
            } catch (error: Throwable) {
                candidate.completeExceptionally(error)
            } finally {
                inFlight.compareAndSet(candidate, null)
            }
        }
        return candidate.await()
    }

    private fun ageMillis(storedAtMillis: Long): Long =
        (System.currentTimeMillis() - storedAtMillis).coerceAtLeast(0L)

    private fun ageSeconds(storedAtMillis: Long): Int =
        (ageMillis(storedAtMillis) / 1_000L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    private data class MemoryCacheEntry(
        val storedAtMillis: Long,
        val status: CursorServiceStatus,
    )

    private companion object {
        const val MEMORY_TTL_MILLIS = 60_000L
        val memoryLock = Any()
        var memoryCache: MemoryCacheEntry? = null
        val requestScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val inFlight = AtomicReference<CompletableDeferred<CursorServiceStatus>?>(null)
    }
}
