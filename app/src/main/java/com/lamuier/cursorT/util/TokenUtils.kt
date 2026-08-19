package com.lamuier.cursorT.util

import java.util.Base64

object TokenUtils {
    private val expPattern = Regex("\"exp\"\\s*:\\s*(\\d+)")
    private val subPattern = Regex("\"sub\"\\s*:\\s*\"([^\"]+)\"")
    private val accessTokenPattern = Regex("[A-Za-z0-9._~=-]+")
    private val userIdPattern = Regex("[A-Za-z0-9._@-]{1,256}")

    fun requireValidAccessToken(token: String): String {
        val value = token.trim()
        require(value.length in 32..8192) { "Access Token 长度不正确" }
        require(accessTokenPattern.matches(value)) { "Access Token 包含不安全字符" }
        return value
    }

    fun isExpired(
        token: String,
        nowEpochSeconds: Long = System.currentTimeMillis() / 1_000L,
        skewSeconds: Long = 60L,
    ): Boolean {
        val exp = expPattern.find(payloadJson(token))?.groupValues?.getOrNull(1)?.toLongOrNull()
            ?: return false
        return nowEpochSeconds >= exp - skewSeconds
    }

    fun userId(token: String): String {
        val subject = subPattern.find(payloadJson(token))?.groupValues?.getOrNull(1).orEmpty()
        return subject.substringAfter('|', subject).takeIf(userIdPattern::matches).orEmpty()
    }

    private fun payloadJson(token: String): String {
        val payload = token.split('.').getOrNull(1) ?: return ""
        return try {
            val padded = payload + "=".repeat((4 - payload.length % 4) % 4)
            Base64.getUrlDecoder().decode(padded).toString(Charsets.UTF_8)
        } catch (_: IllegalArgumentException) {
            ""
        }
    }
}
