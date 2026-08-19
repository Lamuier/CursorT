package com.lamuier.cursorT.network

import com.lamuier.cursorT.model.BillingCycle
import com.lamuier.cursorT.model.Credits
import com.lamuier.cursorT.model.CursorAccount
import com.lamuier.cursorT.model.CursorTOverview
import com.lamuier.cursorT.model.OnDemandUsage
import com.lamuier.cursorT.model.PlanInfo
import com.lamuier.cursorT.model.Subscription
import com.lamuier.cursorT.model.TotalFormat
import com.lamuier.cursorT.model.UsageMetrics
import org.json.JSONObject

object UsageJsonParser {
    fun toJson(usage: CursorTOverview): String {
        val root = JSONObject()
            .put("account_id", usage.accountId)
            .put("alias", usage.alias)
            .put("is_team", usage.isTeam)
            .put(
                "plan",
                JSONObject()
                    .putNullable("name", usage.plan.name)
                    .putNullable("price", usage.plan.price)
                    .put("included_amount_dollars", usage.plan.includedAmountDollars)
                    .putNullable("billing_cycle_end", usage.plan.billingCycleEnd),
            )
            .put(
                "billing_cycle",
                JSONObject()
                    .putNullable("start", usage.billingCycle.start)
                    .putNullable("end", usage.billingCycle.end),
            )
            .put(
                "usage",
                JSONObject()
                    .put("total_used", usage.usage.totalUsed)
                    .put("total_format", if (usage.usage.totalFormat == TotalFormat.Dollars) "dollars" else "percent")
                    .put("total_spend_dollars", usage.usage.totalSpendDollars)
                    .put("included_spend_dollars", usage.usage.includedSpendDollars)
                    .put("bonus_spend_dollars", usage.usage.bonusSpendDollars)
                    .put("limit_dollars", usage.usage.limitDollars)
                    .put("remaining_dollars", usage.usage.remainingDollars)
                    .putNullable("auto_percent_used", usage.usage.autoPercentUsed)
                    .putNullable("api_percent_used", usage.usage.apiPercentUsed)
                    .putNullable("display_message", usage.usage.displayMessage)
                    .put("remaining_bonus", usage.usage.remainingBonus),
            )
            .put(
                "credits",
                JSONObject()
                    .put("total_dollars", usage.credits.totalDollars)
                    .put("grant_total_dollars", usage.credits.grantTotalDollars)
                    .put("stripe_balance_dollars", usage.credits.stripeBalanceDollars),
            )
            .put(
                "subscription",
                JSONObject()
                    .putNullable("membership_type", usage.subscription.membershipType)
                    .putNullable("status", usage.subscription.status),
            )
            .put("fetched_at", usage.fetchedAt)
            .put("from_cache", false)
            .put("cache_age_seconds", 0)
            .put("partial_data", usage.partialData)
        root.put(
            "on_demand",
            usage.onDemand?.let {
                JSONObject()
                    .putNullable("limit_type", it.limitType)
                    .put("total_spend_dollars", it.totalSpendDollars)
                    .put("individual_limit_dollars", it.individualLimitDollars)
                    .put("individual_used_dollars", it.individualUsedDollars)
                    .put("individual_remaining_dollars", it.individualRemainingDollars)
                    .put("pooled_limit_dollars", it.pooledLimitDollars)
                    .put("pooled_used_dollars", it.pooledUsedDollars)
                    .put("pooled_remaining_dollars", it.pooledRemainingDollars)
            } ?: JSONObject.NULL,
        )
        return root.toString()
    }

    fun parseAccounts(rawJson: String): List<CursorAccount> {
        val items = JSONObject(rawJson).getJSONArray("items")
        return buildList(items.length()) {
            for (index in 0 until items.length()) {
                add(parseAccount(items.getJSONObject(index)))
            }
        }
    }

    fun parseAccount(rawJson: String): CursorAccount = parseAccount(JSONObject(rawJson))

    fun parseUsage(rawJson: String, isLocalCache: Boolean = false): CursorTOverview {
        val root = JSONObject(rawJson)
        val plan = root.optJSONObject("plan") ?: JSONObject()
        val cycle = root.optJSONObject("billing_cycle") ?: JSONObject()
        val usage = root.optJSONObject("usage") ?: JSONObject()
        val credits = root.optJSONObject("credits") ?: JSONObject()
        val subscription = root.optJSONObject("subscription") ?: JSONObject()
        val onDemand = root.optJSONObject("on_demand")
        return CursorTOverview(
            accountId = root.optInt("account_id"),
            alias = root.optString("alias"),
            isTeam = root.optBoolean("is_team"),
            plan = PlanInfo(
                name = plan.nullableString("name"),
                price = plan.nullableString("price"),
                includedAmountDollars = plan.number("included_amount_dollars"),
                billingCycleEnd = plan.nullableString("billing_cycle_end"),
            ),
            billingCycle = BillingCycle(
                start = cycle.nullableString("start"),
                end = cycle.nullableString("end"),
            ),
            usage = UsageMetrics(
                totalUsed = usage.number("total_used"),
                totalFormat = if (usage.optString("total_format") == "dollars") {
                    TotalFormat.Dollars
                } else {
                    TotalFormat.Percent
                },
                totalSpendDollars = usage.number("total_spend_dollars"),
                includedSpendDollars = usage.number("included_spend_dollars"),
                bonusSpendDollars = usage.number("bonus_spend_dollars"),
                limitDollars = usage.number("limit_dollars"),
                remainingDollars = usage.number("remaining_dollars"),
                autoPercentUsed = usage.nullableNumber("auto_percent_used"),
                apiPercentUsed = usage.nullableNumber("api_percent_used"),
                displayMessage = usage.nullableString("display_message"),
                remainingBonus = usage.optBoolean("remaining_bonus"),
            ),
            credits = Credits(
                totalDollars = credits.number("total_dollars"),
                grantTotalDollars = credits.number("grant_total_dollars"),
                stripeBalanceDollars = credits.number("stripe_balance_dollars"),
            ),
            onDemand = onDemand?.let {
                OnDemandUsage(
                    limitType = it.nullableString("limit_type"),
                    totalSpendDollars = it.number("total_spend_dollars"),
                    individualLimitDollars = it.number("individual_limit_dollars"),
                    individualUsedDollars = it.number("individual_used_dollars"),
                    individualRemainingDollars = it.number("individual_remaining_dollars"),
                    pooledLimitDollars = it.number("pooled_limit_dollars"),
                    pooledUsedDollars = it.number("pooled_used_dollars"),
                    pooledRemainingDollars = it.number("pooled_remaining_dollars"),
                )
            },
            subscription = Subscription(
                membershipType = subscription.nullableString("membership_type"),
                status = subscription.nullableString("status"),
            ),
            fetchedAt = root.optString("fetched_at"),
            fromCache = root.optBoolean("from_cache"),
            cacheAgeSeconds = root.optInt("cache_age_seconds"),
            isLocalCache = isLocalCache,
            partialData = root.optBoolean("partial_data"),
        )
    }

    private fun parseAccount(json: JSONObject): CursorAccount = CursorAccount(
        id = json.getInt("id"),
        alias = json.getString("alias"),
        tokenExpired = json.optBoolean("token_expired"),
        createdAt = json.optString("created_at"),
        updatedAt = json.optString("updated_at"),
    )

    private fun JSONObject.nullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).takeIf { it.isNotBlank() }
    }

    private fun JSONObject.number(key: String): Double = nullableNumber(key) ?: 0.0

    private fun JSONObject.nullableNumber(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        return runCatching { getDouble(key) }.getOrNull()?.takeIf { it.isFinite() }
    }

    private fun JSONObject.putNullable(key: String, value: Any?): JSONObject = put(key, value ?: JSONObject.NULL)
}
