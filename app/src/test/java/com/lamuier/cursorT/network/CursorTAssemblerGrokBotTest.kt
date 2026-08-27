package com.lamuier.cursorT.network

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CursorTAssemblerGrokBotTest {
    @Test
    fun parseGrokBotUsage_readsPercentAndEpochTimestamps() {
        val startMs = 1_724_630_400_000L // 2024-08-26 00:00:00 UTC
        val endMs = 1_725_235_200_000L
        val payload = JSONObject(
            """
            {
              "usagePercent": 42.5,
              "currentPeriodStart": "$startMs",
              "nextResetTimestampUtc": "$endMs",
              "hasNonZeroIncludedLimit": true
            }
            """.trimIndent(),
        )
        val result = CursorTAssembler.parseGrokBotUsage(payload)
        assertNotNull(result)
        assertEquals(42.5, result!!.percentUsed, 0.001)
        assertEquals(localTime(startMs), result.periodStart)
        assertEquals(localTime(endMs), result.resetsAt)
    }

    @Test
    fun parseGrokBotUsage_readsIsoTimestampsAndNestedObject() {
        val payload = JSONObject(
            """
            {
              "sandUsage": {
                "usagePercent": 10,
                "currentPeriodStart": "2026-08-20T00:00:00Z",
                "nextResetTimestampUtc": "2026-08-27T00:00:00Z"
              }
            }
            """.trimIndent(),
        )
        val result = CursorTAssembler.parseGrokBotUsage(payload)
        assertNotNull(result)
        assertEquals(10.0, result!!.percentUsed, 0.001)
        assertNotNull(result.periodStart)
        assertNotNull(result.resetsAt)
        assertTrue(result.periodStart!!.startsWith("2026-08-"))
        assertTrue(result.resetsAt!!.startsWith("2026-08-"))
    }

    @Test
    fun parseGrokBotUsage_hidesPooledEnterpriseAndZeroLimit() {
        assertNull(
            CursorTAssembler.parseGrokBotUsage(
                JSONObject("""{"usagePercent": 5, "usesPooledEnterpriseAllowance": true}"""),
            ),
        )
        assertNull(
            CursorTAssembler.parseGrokBotUsage(
                JSONObject("""{"usagePercent": 5, "hasNonZeroIncludedLimit": false}"""),
            ),
        )
        assertNull(
            CursorTAssembler.parseGrokBotUsage(
                JSONObject("""{"usagePercent": 5, "includedLimitZero": true}"""),
            ),
        )
        assertNull(CursorTAssembler.parseGrokBotUsage(JSONObject("{}")))
    }

    @Test
    fun assemble_attachesGrokBotIndependentlyOfTokenUsage() {
        val withGrok = CursorTAssembler.assemble(
            accountId = 1,
            alias = "me",
            periodUsage = period(),
            planPayload = plan(),
            grantsPayload = null,
            stripePayload = null,
            aggregationsPayload = null,
            grokBotPayload = JSONObject("""{"usagePercent": 33.3, "hasNonZeroIncludedLimit": true}"""),
            partialData = false,
        )
        assertEquals(33.3, withGrok.grokBot?.percentUsed ?: -1.0, 0.001)
        assertNull(withGrok.tokenUsage)
        assertEquals(false, withGrok.partialData)

        val missing = CursorTAssembler.assemble(
            accountId = 1,
            alias = "me",
            periodUsage = period(),
            planPayload = plan(),
            grantsPayload = null,
            stripePayload = null,
            aggregationsPayload = null,
            grokBotPayload = null,
            partialData = true,
        )
        assertNull(missing.grokBot)
        assertTrue(missing.partialData)
    }

    @Test
    fun usageJsonParser_roundTripsGrokBot() {
        val overview = CursorTAssembler.assemble(
            accountId = 7,
            alias = "demo",
            periodUsage = period(),
            planPayload = plan(),
            grantsPayload = null,
            stripePayload = null,
            aggregationsPayload = null,
            grokBotPayload = JSONObject(
                """
                {
                  "usagePercent": 8,
                  "currentPeriodStart": "1756243200000",
                  "nextResetTimestampUtc": "1756848000000"
                }
                """.trimIndent(),
            ),
            partialData = false,
        )
        val restored = UsageJsonParser.parseUsage(UsageJsonParser.toJson(overview))
        assertEquals(overview.grokBot?.percentUsed, restored.grokBot?.percentUsed)
        assertEquals(overview.grokBot?.periodStart, restored.grokBot?.periodStart)
        assertEquals(overview.grokBot?.resetsAt, restored.grokBot?.resetsAt)
    }

    @Test
    fun usageJsonParser_oldCacheWithoutGrokBotStaysNull() {
        val overview = CursorTAssembler.assemble(
            accountId = 1,
            alias = "me",
            periodUsage = period(),
            planPayload = plan(),
            grantsPayload = null,
            stripePayload = null,
            aggregationsPayload = null,
            partialData = false,
        )
        val json = JSONObject(UsageJsonParser.toJson(overview)).apply { remove("grok_bot") }
        val restored = UsageJsonParser.parseUsage(json.toString())
        assertNull(restored.grokBot)
    }

    private fun localTime(millis: Long): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(millis))

    private fun period() = JSONObject(
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

    private fun plan() = JSONObject("""{"planInfo":{"planName":"Pro","includedAmountCents":100}}""")
}
