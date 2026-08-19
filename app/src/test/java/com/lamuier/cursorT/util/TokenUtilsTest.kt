package com.lamuier.cursorT.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class TokenUtilsTest {
    @Test
    fun extractsAuth0UserId() {
        val token = token("""{"sub":"auth0|user-123","exp":2000}""")
        assertEquals("user-123", TokenUtils.userId(token))
    }

    @Test
    fun expiresWithinSafetyWindow() {
        val token = token("""{"sub":"user","exp":1060}""")
        assertTrue(TokenUtils.isExpired(token, nowEpochSeconds = 1000, skewSeconds = 60))
    }

    @Test
    fun validTokenIsNotExpired() {
        val token = token("""{"sub":"user","exp":2000}""")
        assertFalse(TokenUtils.isExpired(token, nowEpochSeconds = 1000, skewSeconds = 60))
    }

    @Test
    fun opaqueTokenWithoutExpiryIsNotRejectedLocally() {
        assertFalse(TokenUtils.isExpired("opaque-token", nowEpochSeconds = 1000))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsHeaderInjectionCharacters() {
        TokenUtils.requireValidAccessToken("a".repeat(40) + "\r\nInjected: value")
    }

    private fun token(payload: String): String {
        val encoded = Base64.getUrlEncoder().withoutPadding().encodeToString(payload.toByteArray())
        return "header.$encoded.signature"
    }
}
