package com.lamuier.cursorusage.network

import com.lamuier.cursorusage.BuildConfig
import com.lamuier.cursorusage.util.TokenUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class ApiException(
    val statusCode: Int,
    override val message: String,
) : Exception(message)

class CursorApiClient {
    suspend fun connectRpc(accessToken: String, method: String): JSONObject = withContext(Dispatchers.IO) {
        val response = request(
            url = "$CURSOR_API_BASE/aiserver.v1.DashboardService/$method",
            accessToken = accessToken,
            jsonBody = JSONObject(),
            connectProtocol = true,
        )
        JSONObject(response)
    }

    suspend fun stripe(accessToken: String): JSONObject = withContext(Dispatchers.IO) {
        val userId = TokenUtils.userId(accessToken)
        if (userId.isBlank()) throw ApiException(400, "Access Token 中缺少 Cursor 用户标识")
        val cookie = "WorkosCursorSessionToken=$userId%3A%3A$accessToken"
        JSONObject(
            request(
                url = "$CURSOR_WEB_BASE/api/auth/stripe",
                cookie = cookie,
            ),
        )
    }

    /** Cursor 官方 Statuspage：当前总览、组件状态、未解决事件与计划维护。无需 Token。 */
    suspend fun statusSummary(): JSONObject = withContext(Dispatchers.IO) {
        JSONObject(request(url = STATUS_SUMMARY_URL, kind = ApiKind.Status))
    }

    /** Cursor 官方 Statuspage：近期事件历史（含已恢复）。无需 Token。 */
    suspend fun statusIncidents(): JSONObject = withContext(Dispatchers.IO) {
        JSONObject(request(url = STATUS_INCIDENTS_URL, kind = ApiKind.Status))
    }

    private fun request(
        url: String,
        accessToken: String? = null,
        cookie: String? = null,
        jsonBody: JSONObject? = null,
        connectProtocol: Boolean = false,
        kind: ApiKind = ApiKind.Usage,
    ): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = if (jsonBody == null) "GET" else "POST"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", USER_AGENT)
            accessToken?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            cookie?.let { connection.setRequestProperty("Cookie", it) }
            if (connectProtocol) connection.setRequestProperty("Connect-Protocol-Version", "1")
            if (jsonBody != null) {
                val bytes = jsonBody.toString().toByteArray(Charsets.UTF_8)
                connection.doOutput = true
                connection.setFixedLengthStreamingMode(bytes.size)
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(bytes) }
            }

            val status = connection.responseCode
            if (status in 300..399) {
                throw ApiException(status, "Cursor API 发生了不安全的重定向")
            }
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = readBody(stream, connection.contentLengthLong, status, kind)
            if (status !in 200..299) throw ApiException(status, errorMessage(status, kind))
            return body
        } finally {
            connection.disconnect()
        }
    }

    private fun readBody(
        stream: InputStream?,
        declaredLength: Long,
        status: Int,
        kind: ApiKind,
    ): String {
        if (declaredLength > MAX_RESPONSE_BYTES) {
            throw ApiException(status, overflowMessage(kind))
        }
        if (stream == null) return ""
        return stream.use { input ->
            val initialSize = declaredLength
                .takeIf { it in 1..MAX_RESPONSE_BYTES.toLong() }
                ?.toInt()
                ?: DEFAULT_BUFFER_SIZE
            val output = ByteArrayOutputStream(initialSize)
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                if (total > MAX_RESPONSE_BYTES) {
                    throw ApiException(status, overflowMessage(kind))
                }
                output.write(buffer, 0, count)
            }
            output.toString(Charsets.UTF_8.name())
        }
    }

    private fun errorMessage(status: Int, kind: ApiKind): String = when (kind) {
        ApiKind.Status -> when (status) {
            404 -> "Cursor 状态接口已发生变化，请升级应用"
            429 -> "Cursor 状态页请求过于频繁，请稍后重试"
            in 500..599 -> "Cursor 状态页暂时不可用（HTTP $status）"
            else -> "Cursor 状态页请求失败（HTTP $status）"
        }
        ApiKind.Usage -> when (status) {
            401, 403 -> "Cursor Access Token 已过期或无效，请更新 Token"
            404 -> "Cursor 用量接口已发生变化，请升级应用"
            429 -> "Cursor 请求过于频繁，请稍后重试"
            in 500..599 -> "Cursor 服务暂时不可用（HTTP $status）"
            else -> "Cursor API 请求失败（HTTP $status）"
        }
    }

    private fun overflowMessage(kind: ApiKind): String = when (kind) {
        ApiKind.Status -> "Cursor 状态页响应超过 1 MiB 安全上限"
        ApiKind.Usage -> "Cursor API 响应超过 1 MiB 安全上限"
    }

    private enum class ApiKind { Usage, Status }

    private companion object {
        const val CURSOR_API_BASE = "https://api2.cursor.sh"
        const val CURSOR_WEB_BASE = "https://cursor.com"
        const val STATUS_SUMMARY_URL = "https://status.cursor.com/api/v2/summary.json"
        const val STATUS_INCIDENTS_URL = "https://status.cursor.com/api/v2/incidents.json"
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 30_000
        const val MAX_RESPONSE_BYTES = 1024 * 1024

        // UA 版本号自动跟随 versionName（debug 构建带 -debug 后缀），便于服务端识别与问题排查
        val USER_AGENT = "CursorUsageAndroid/${BuildConfig.VERSION_NAME}"
    }
}
