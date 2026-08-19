package com.lamuier.cursorT.network

import com.lamuier.cursorT.model.BillingCycle
import com.lamuier.cursorT.model.Credits
import com.lamuier.cursorT.model.CursorTOverview
import com.lamuier.cursorT.model.OnDemandUsage
import com.lamuier.cursorT.model.PlanInfo
import com.lamuier.cursorT.model.Subscription
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
            fetchedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
            fromCache = false,
            cacheAgeSeconds = 0,
            partialData = partialData,
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

    private fun millisecondsToLocalTime(value: Any?): String? {
        val milliseconds = value?.toString()?.toLongOrNull() ?: return null
        return SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(milliseconds))
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
}
