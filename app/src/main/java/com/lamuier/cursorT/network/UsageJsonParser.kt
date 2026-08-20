package com.lamuier.cursorT.network

import com.lamuier.cursorT.model.BillingCycle
import com.lamuier.cursorT.model.Credits
import com.lamuier.cursorT.model.CursorAccount
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
        root.put(
            "token_usage",
            usage.tokenUsage?.let { breakdown ->
                JSONObject()
                    .put("total_input_tokens", breakdown.totalInputTokens)
                    .put("total_output_tokens", breakdown.totalOutputTokens)
                    .put("total_cache_write_tokens", breakdown.totalCacheWriteTokens)
                    .put("total_cache_read_tokens", breakdown.totalCacheReadTokens)
                    .put("total_cost_dollars", breakdown.totalCostDollars)
                    .put(
                        "models",
                        JSONArray().also { array ->
                            breakdown.models.forEach { model ->
                                array.put(
                                    JSONObject()
                                        .put("model_intent", model.modelIntent)
                                        .put("input_tokens", model.inputTokens)
                                        .put("output_tokens", model.outputTokens)
                                        .put("cache_write_tokens", model.cacheWriteTokens)
                                        .put("cache_read_tokens", model.cacheReadTokens)
                                        .put("cost_dollars", model.costDollars)
                                        .putNullable("tier", model.tier),
                                )
                            }
                        },
                    )
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
            tokenUsage = root.optJSONObject("token_usage")?.let(::parseTokenUsage),
            fetchedAt = root.optString("fetched_at"),
            fromCache = root.optBoolean("from_cache"),
            cacheAgeSeconds = root.optInt("cache_age_seconds"),
            isLocalCache = isLocalCache,
            partialData = root.optBoolean("partial_data"),
        )
    }

    private fun parseTokenUsage(json: JSONObject): TokenUsageBreakdown {
        val modelsJson = json.optJSONArray("models") ?: JSONArray()
        val models = buildList {
            for (index in 0 until modelsJson.length()) {
                val item = modelsJson.optJSONObject(index) ?: continue
                val intent = item.nullableString("model_intent") ?: continue
                add(
                    ModelTokenUsage(
                        modelIntent = intent,
                        inputTokens = item.longNumber("input_tokens"),
                        outputTokens = item.longNumber("output_tokens"),
                        cacheWriteTokens = item.longNumber("cache_write_tokens"),
                        cacheReadTokens = item.longNumber("cache_read_tokens"),
                        costDollars = item.number("cost_dollars"),
                        tier = item.nullableInt("tier"),
                    ),
                )
            }
        }
        return TokenUsageBreakdown(
            models = models,
            totalInputTokens = json.longNumber("total_input_tokens"),
            totalOutputTokens = json.longNumber("total_output_tokens"),
            totalCacheWriteTokens = json.longNumber("total_cache_write_tokens"),
            totalCacheReadTokens = json.longNumber("total_cache_read_tokens"),
            totalCostDollars = json.number("total_cost_dollars"),
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

    private fun JSONObject.longNumber(key: String): Long {
        if (!has(key) || isNull(key)) return 0L
        return runCatching { getLong(key) }.getOrNull()?.coerceAtLeast(0L) ?: 0L
    }

    private fun JSONObject.nullableInt(key: String): Int? {
        if (!has(key) || isNull(key)) return null
        return runCatching { getInt(key) }.getOrNull()
    }

    private fun JSONObject.nullableNumber(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        return runCatching { getDouble(key) }.getOrNull()?.takeIf { it.isFinite() }
    }

    private fun JSONObject.putNullable(key: String, value: Any?): JSONObject = put(key, value ?: JSONObject.NULL)
}
