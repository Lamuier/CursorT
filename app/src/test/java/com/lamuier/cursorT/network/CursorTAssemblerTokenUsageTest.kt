package com.lamuier.cursorT.network

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CursorTAssemblerTokenUsageTest {
    @Test
    fun parseTokenUsage_readsStringAndNumericFields() {
        val payload = JSONObject(
            """
            {
              "aggregations": [
                {
                  "modelIntent": "claude-4-sonnet",
                  "inputTokens": "12,345",
                  "outputTokens": 678,
                  "cacheWriteTokens": "1_000",
                  "cacheReadTokens": "2000",
                  "totalCents": 123.4,
                  "tier": 2
                },
                {
                  "modelIntent": "composer-2",
                  "inputTokens": "500",
                  "outputTokens": "100",
                  "cacheWriteTokens": "0",
                  "cacheReadTokens": "0",
                  "totalCents": 50
                },
                {
                  "modelIntent": "",
                  "inputTokens": "9",
                  "outputTokens": "9",
                  "totalCents": 1
                }
              ],
              "totalInputTokens": "12845",
              "totalOutputTokens": "778",
              "totalCacheWriteTokens": "1000",
              "totalCacheReadTokens": "2000",
              "totalCostCents": 173.4
            }
            """.trimIndent(),
        )

        val result = CursorTAssembler.parseTokenUsage(payload)
        assertEquals(2, result.models.size)
        assertEquals("claude-4-sonnet", result.models[0].modelIntent)
        assertEquals(12345L, result.models[0].inputTokens)
        assertEquals(678L, result.models[0].outputTokens)
        assertEquals(1000L, result.models[0].cacheWriteTokens)
        assertEquals(2000L, result.models[0].cacheReadTokens)
        assertEquals(1.23, result.models[0].costDollars, 0.001)
        assertEquals(2, result.models[0].tier)
        assertEquals("composer-2", result.models[1].modelIntent)
        assertEquals(12845L, result.totalInputTokens)
        assertEquals(778L, result.totalOutputTokens)
        assertEquals(1000L, result.totalCacheWriteTokens)
        assertEquals(2000L, result.totalCacheReadTokens)
        assertEquals(1.73, result.totalCostDollars, 0.001)
    }

    @Test
    fun parseTokenUsage_fallsBackToModelSumsWhenTotalsMissing() {
        val payload = JSONObject(
            """
            {
              "aggregations": [
                {
                  "modelIntent": "gpt-5",
                  "inputTokens": 100,
                  "outputTokens": 20,
                  "cacheWriteTokens": 5,
                  "cacheReadTokens": 3,
                  "totalCents": 10
                }
              ]
            }
            """.trimIndent(),
        )
        val result = CursorTAssembler.parseTokenUsage(payload)
        assertEquals(100L, result.totalInputTokens)
        assertEquals(20L, result.totalOutputTokens)
        assertEquals(5L, result.totalCacheWriteTokens)
        assertEquals(3L, result.totalCacheReadTokens)
        assertEquals(0.10, result.totalCostDollars, 0.001)
    }

    @Test
    fun assemble_attachesTokenUsageAndMarksPartialWhenMissing() {
        val period = JSONObject(
            """
            {
              "billingCycleStart": "1768399334000",
              "billingCycleEnd": "1771077734000",
              "planUsage": {
                "totalSpend": 1000,
                "includedSpend": 800,
                "bonusSpend": 200,
                "remaining": 3000,
                "limit": 4000,
                "totalPercentUsed": 25.0,
                "autoPercentUsed": 10.0,
                "apiPercentUsed": 15.0,
                "remainingBonus": true
              },
              "spendLimitUsage": {}
            }
            """.trimIndent(),
        )
        val plan = JSONObject(
            """
            {
              "planInfo": {
                "planName": "Pro",
                "price": "$20/mo",
                "includedAmountCents": 4000,
                "billingCycleEnd": "1771077734000"
              }
            }
            """.trimIndent(),
        )
        val aggregations = JSONObject(
            """
            {
              "aggregations": [
                {
                  "modelIntent": "auto",
                  "inputTokens": "42",
                  "outputTokens": "8",
                  "totalCents": 1.5
                }
              ],
              "totalInputTokens": "42",
              "totalOutputTokens": "8",
              "totalCostCents": 1.5
            }
            """.trimIndent(),
        )

        val withTokens = CursorTAssembler.assemble(
            accountId = 1,
            alias = "me",
            periodUsage = period,
            planPayload = plan,
            grantsPayload = null,
            stripePayload = null,
            aggregationsPayload = aggregations,
            partialData = false,
        )
        assertEquals(1, withTokens.tokenUsage?.models?.size)
        assertEquals(42L, withTokens.tokenUsage?.totalInputTokens)
        assertEquals(false, withTokens.partialData)

        val withoutTokens = CursorTAssembler.assemble(
            accountId = 1,
            alias = "me",
            periodUsage = period,
            planPayload = plan,
            grantsPayload = null,
            stripePayload = null,
            aggregationsPayload = null,
            partialData = true,
        )
        assertNull(withoutTokens.tokenUsage)
        assertTrue(withoutTokens.partialData)
    }

    @Test
    fun usageJsonParser_roundTripsTokenUsage() {
        val period = JSONObject(
            """
            {
              "planUsage": {
                "totalSpend": 0,
                "includedSpend": 0,
                "bonusSpend": 0,
                "remaining": 100,
                "limit": 100,
                "totalPercentUsed": 0
              },
              "spendLimitUsage": {}
            }
            """.trimIndent(),
        )
        val plan = JSONObject("""{"planInfo":{"planName":"Pro","includedAmountCents":100}}""")
        val aggregations = JSONObject(
            """
            {
              "aggregations": [
                {
                  "modelIntent": "composer-2",
                  "inputTokens": "11",
                  "outputTokens": "2",
                  "cacheWriteTokens": "3",
                  "cacheReadTokens": "4",
                  "totalCents": 7,
                  "tier": 1
                }
              ],
              "totalInputTokens": "11",
              "totalOutputTokens": "2",
              "totalCacheWriteTokens": "3",
              "totalCacheReadTokens": "4",
              "totalCostCents": 7
            }
            """.trimIndent(),
        )
        val overview = CursorTAssembler.assemble(
            accountId = 7,
            alias = "demo",
            periodUsage = period,
            planPayload = plan,
            grantsPayload = null,
            stripePayload = null,
            aggregationsPayload = aggregations,
            partialData = false,
        )
        val restored = UsageJsonParser.parseUsage(UsageJsonParser.toJson(overview))
        assertEquals(overview.tokenUsage?.totalInputTokens, restored.tokenUsage?.totalInputTokens)
        assertEquals(overview.tokenUsage?.models?.first()?.modelIntent, restored.tokenUsage?.models?.first()?.modelIntent)
        assertEquals(overview.tokenUsage?.models?.first()?.tier, restored.tokenUsage?.models?.first()?.tier)
        assertEquals(0.07, restored.tokenUsage?.totalCostDollars ?: -1.0, 0.001)
    }
}
