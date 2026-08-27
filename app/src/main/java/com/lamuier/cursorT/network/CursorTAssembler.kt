package com.lamuier.cursorT.network

import com.lamuier.cursorT.model.BillingCycle
import com.lamuier.cursorT.model.Credits
import com.lamuier.cursorT.model.GrokBotUsage
import com.lamuier.cursorT.model.CursorTOverview
import com.lamuier.cursorT.model.ModelTokenUsage
import com.lamuier.cursorT.model.OnDemandUsage
import com.lamuier.cursorT.model.PlanInfo
import com.lamuier.cursorT.model.Subscription
import com.lamuier.cursorT.model.TokenUsageBreakdown
import com.lamuier.cursorT.model.TotalFormat
import com.lamuier.cursorT.model.UsageMetrics
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CursorTAssembler {
    fun assemble(
        accountId: Int,
        alias: String,
        periodUsage: JSONObject,
        planPayload: JSONObject,
        grantsPayload: JSONObject?,
        stripePayload: JSONObject?,
        aggregationsPayload: JSONObject?,
        grokBotPayload: JSONObject? = null,
        partialData: Boolean,
    ): CursorTOverview {
        val plan = planPayload.optJSONObject("planInfo") ?: JSONObject()
        val planUsage = periodUsage.optJSONObject("planUsage") ?: JSONObject()
        val spendLimit = periodUsage.optJSONObject("spendLimitUsage") ?: JSONObject()
        val planName = plan.optNullableString("planName")
        val isTeam = isTeam(planName, spendLimit)

        val includedSpend = centsToDollars(planUsage.optNumber("includedSpend"))
        val limitDollars = centsToDollars(planUsage.optNumber("limit"))
        val totalSpend = centsToDollars(planUsage.optNumber("totalSpend"))
        val totalPercent = totalUsagePercent(planUsage)
        val totalUsed = if (isTeam) totalSpend else totalPercent ?: 0.0

        val grantTotalCents = grantsPayload?.let(::extractGrantTotal) ?: 0.0
        val customerBalance = stripePayload?.optNumber("customerBalance") ?: 0.0
        val stripeCreditCents = if (customerBalance < 0) -customerBalance else 0.0

        return CursorTOverview(
            accountId = accountId,
            alias = alias,
            isTeam = isTeam,
            plan = PlanInfo(
                name = planName,
                price = plan.optNullableString("price"),
                includedAmountDollars = centsToDollars(plan.optNumber("includedAmountCents")),
                billingCycleEnd = millisecondsToLocalTime(plan.opt("billingCycleEnd")),
            ),
            billingCycle = BillingCycle(
                start = millisecondsToLocalTime(periodUsage.opt("billingCycleStart")),
                end = millisecondsToLocalTime(periodUsage.opt("billingCycleEnd")),
            ),
            usage = UsageMetrics(
                totalUsed = round2(totalUsed),
                totalFormat = if (isTeam) TotalFormat.Dollars else TotalFormat.Percent,
                totalSpendDollars = totalSpend,
                includedSpendDollars = includedSpend,
                bonusSpendDollars = centsToDollars(planUsage.optNumber("bonusSpend")),
                limitDollars = limitDollars,
                remainingDollars = centsToDollars(planUsage.optNumber("remaining")),
                autoPercentUsed = planUsage.optFinite("autoPercentUsed")?.let(::round2),
                apiPercentUsed = planUsage.optFinite("apiPercentUsed")?.let(::round2),
                displayMessage = periodUsage.optNullableString("displayMessage"),
                remainingBonus = planUsage.optBoolean("remainingBonus"),
            ),
            credits = Credits(
                totalDollars = centsToDollars(grantTotalCents + stripeCreditCents),
                grantTotalDollars = centsToDollars(grantTotalCents),
                stripeBalanceDollars = centsToDollars(stripeCreditCents),
            ),
            onDemand = buildOnDemand(spendLimit),
            subscription = Subscription(
                membershipType = stripePayload?.optNullableString("membershipType"),
                status = stripePayload?.optNullableString("subscriptionStatus"),
            ),
            tokenUsage = aggregationsPayload?.let(::parseTokenUsage),
            grokBot = grokBotPayload?.let(::parseGrokBotUsage),
            fetchedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            fromCache = false,
            cacheAgeSeconds = 0,
            partialData = partialData,
        )
    }

    fun parseGrokBotUsage(root: JSONObject): GrokBotUsage? {
        val candidates = buildList {
            add(root)
            listOf("sandUsage", "usageStatus", "status", "data").forEach { key ->
                root.optJSONObject(key)?.let(::add)
            }
        }
        for (obj in candidates) {
            parseGrokBotUsageObject(obj)?.let { return it }
        }
        return null
    }

    fun parseTokenUsage(root: JSONObject): TokenUsageBreakdown {
        val aggregations = root.optJSONArray("aggregations") ?: JSONArray()
        val models = buildList {
            for (index in 0 until aggregations.length()) {
                val item = aggregations.optJSONObject(index) ?: continue
                val modelIntent = item.optNullableString("modelIntent")?.trim().orEmpty()
                if (modelIntent.isEmpty()) continue
                val input = item.tokenCount("inputTokens")
                val output = item.tokenCount("outputTokens")
                val cacheWrite = item.tokenCount("cacheWriteTokens")
                val cacheRead = item.tokenCount("cacheReadTokens")
                val costCents = item.optNumber("totalCents") ?: 0.0
                val tier = item.optInt("tier", 0).takeIf { it > 0 }
                if (input == 0L && output == 0L && cacheWrite == 0L && cacheRead == 0L && costCents <= 0.0) {
                    continue
                }
                add(
                    ModelTokenUsage(
                        modelIntent = modelIntent,
                        inputTokens = input,
                        outputTokens = output,
                        cacheWriteTokens = cacheWrite,
                        cacheReadTokens = cacheRead,
                        costDollars = centsToDollars(costCents),
                        tier = tier,
                    ),
                )
            }
        }.sortedWith(
            compareByDescending<ModelTokenUsage> { it.costDollars }
                .thenByDescending { it.inputTokens + it.outputTokens }
                .thenBy { it.modelIntent.lowercase(Locale.US) },
        )

        val totalInput = root.tokenCount("totalInputTokens").takeIf { it > 0 }
            ?: models.sumOf { it.inputTokens }
        val totalOutput = root.tokenCount("totalOutputTokens").takeIf { it > 0 }
            ?: models.sumOf { it.outputTokens }
        val totalCacheWrite = root.tokenCount("totalCacheWriteTokens").takeIf { it > 0 }
            ?: models.sumOf { it.cacheWriteTokens }
        val totalCacheRead = root.tokenCount("totalCacheReadTokens").takeIf { it > 0 }
            ?: models.sumOf { it.cacheReadTokens }
        val totalCost = root.optNumber("totalCostCents")?.let(::centsToDollars)
            ?: models.sumOf { it.costDollars }.let(::round2)

        return TokenUsageBreakdown(
            models = models,
            totalInputTokens = totalInput,
            totalOutputTokens = totalOutput,
            totalCacheWriteTokens = totalCacheWrite,
            totalCacheReadTokens = totalCacheRead,
            totalCostDollars = totalCost,
        )
    }

    private fun isTeam(planName: String?, spendLimit: JSONObject): Boolean {
        if (planName?.trim().equals("team", ignoreCase = true)) return true
        if (spendLimit.optString("limitType").equals("team", ignoreCase = true)) return true
        return (spendLimit.optNumber("pooledLimit") ?: 0.0) > 0.0
    }

    private fun totalUsagePercent(planUsage: JSONObject): Double? {
        planUsage.optFinite("totalPercentUsed")?.let { return round2(it) }
        val limit = planUsage.optFinite("limit") ?: return null
        val remaining = planUsage.optFinite("remaining") ?: return null
        if (limit <= 0) return null
        return round2((limit - remaining) / limit * 100.0)
    }

    private fun buildOnDemand(spendLimit: JSONObject): OnDemandUsage? {
        val individualLimit = spendLimit.optNumber("individualLimit") ?: 0.0
        val pooledLimit = spendLimit.optNumber("pooledLimit") ?: 0.0
        if (individualLimit <= 0.0 && pooledLimit <= 0.0) return null
        return OnDemandUsage(
            limitType = spendLimit.optNullableString("limitType"),
            totalSpendDollars = centsToDollars(spendLimit.optNumber("totalSpend")),
            individualLimitDollars = centsToDollars(individualLimit),
            individualUsedDollars = centsToDollars(spendLimit.optNumber("individualUsed")),
            individualRemainingDollars = centsToDollars(spendLimit.optNumber("individualRemaining")),
            pooledLimitDollars = centsToDollars(pooledLimit),
            pooledUsedDollars = centsToDollars(spendLimit.optNumber("pooledUsed")),
            pooledRemainingDollars = centsToDollars(spendLimit.optNumber("pooledRemaining")),
        )
    }

    private fun extractGrantTotal(root: JSONObject): Double {
        var max = 0.0
        fun walk(value: Any?, keyHint: String = "") {
            when (value) {
                is JSONObject -> value.keys().forEach { key -> walk(value.opt(key), key) }
                is JSONArray -> for (index in 0 until value.length()) walk(value.opt(index), keyHint)
                is Number -> if (listOf("balance", "grant", "credit").any { keyHint.lowercase().contains(it) }) {
                    max = maxOf(max, value.toDouble())
                }
            }
        }
        walk(root)
        return max
    }

    private fun parseGrokBotUsageObject(obj: JSONObject): GrokBotUsage? {
        if (obj.optBoolean("usesPooledEnterpriseAllowance", false)) return null
        if (obj.has("hasNonZeroIncludedLimit") && !obj.optBoolean("hasNonZeroIncludedLimit")) return null
        if (obj.optBoolean("includedLimitZero", false)) return null
        val percent = obj.optFinite("usagePercent")
            ?: obj.optFinite("percentUsed")
            ?: grokBotPercentFromLimit(obj)
            ?: return null
        return GrokBotUsage(
            percentUsed = round2(percent.coerceAtLeast(0.0)),
            periodStart = timestampToLocalTime(
                firstPresent(obj, "currentPeriodStart", "periodStart", "startTimestampUtc"),
            ),
            resetsAt = timestampToLocalTime(
                firstPresent(obj, "nextResetTimestampUtc", "resetsAt", "periodEnd", "currentPeriodEnd"),
            ),
        )
    }

    private fun grokBotPercentFromLimit(obj: JSONObject): Double? {
        val used = obj.optFinite("used") ?: obj.optFinite("includedUsed")
        val limit = obj.optFinite("includedLimit") ?: obj.optFinite("limit")
        if (used == null || limit == null || limit <= 0.0) return null
        return (used / limit) * 100.0
    }

    private fun firstPresent(obj: JSONObject, vararg keys: String): Any? {
        for (key in keys) {
            if (obj.has(key) && !obj.isNull(key)) return obj.opt(key)
        }
        return null
    }

    private fun timestampToLocalTime(value: Any?): String? {
        when (value) {
            null, JSONObject.NULL -> return null
            is Number -> {
                val raw = value.toLong()
                val millis = if (kotlin.math.abs(raw) < 1_000_000_000_000L) raw * 1000L else raw
                return formatMillis(millis)
            }
            is JSONObject -> {
                val seconds = value.optNumber("seconds") ?: value.optNumber("epochSeconds")
                if (seconds != null) return formatMillis((seconds * 1000.0).toLong())
            }
            is String -> {
                value.toLongOrNull()?.let { raw ->
                    val millis = if (kotlin.math.abs(raw) < 1_000_000_000_000L) raw * 1000L else raw
                    return formatMillis(millis)
                }
                return isoToLocalTime(value)
            }
        }
        return millisecondsToLocalTime(value)
    }

    private fun isoToLocalTime(text: String): String? {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return null
        val instant = runCatching { java.time.Instant.parse(trimmed) }.getOrNull()
            ?: runCatching { java.time.OffsetDateTime.parse(trimmed).toInstant() }.getOrNull()
            ?: return null
        return formatMillis(instant.toEpochMilli())
    }

    private fun formatMillis(milliseconds: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(milliseconds))

    private fun millisecondsToLocalTime(value: Any?): String? {
        val milliseconds = value?.toString()?.toLongOrNull() ?: return null
        return formatMillis(milliseconds)
    }

    private fun centsToDollars(cents: Double?): Double = round2((cents ?: 0.0) / 100.0)

    private fun round2(value: Double): Double = BigDecimal.valueOf(value)
        .setScale(2, RoundingMode.HALF_UP)
        .toDouble()

    private fun JSONObject.optNumber(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        return runCatching { getDouble(key) }.getOrNull()?.takeIf { it.isFinite() }
    }

    private fun JSONObject.optFinite(key: String): Double? = optNumber(key)

    private fun JSONObject.optNullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).takeIf { it.isNotBlank() }
    }

    /** Token 字段可能是数字或带千分位的字符串。 */
    private fun JSONObject.tokenCount(key: String): Long {
        if (!has(key) || isNull(key)) return 0L
        return when (val value = opt(key)) {
            is Number -> value.toLong().coerceAtLeast(0L)
            is String -> value
                .trim()
                .replace(",", "")
                .replace("_", "")
                .toLongOrNull()
                ?.coerceAtLeast(0L)
                ?: 0L
            else -> 0L
        }
    }
}
