package com.lamuier.cursorT.network

import com.lamuier.cursorT.BuildConfig
import com.lamuier.cursorT.util.TokenUtils
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
    suspend fun connectRpc(
        accessToken: String,
        method: String,
        jsonBody: JSONObject = JSONObject(),
        service: String = DASHBOARD_SERVICE,
        kind: ApiKind = ApiKind.Usage,
        maxBytes: Int = MAX_RESPONSE_BYTES,
    ): JSONObject = withContext(Dispatchers.IO) {
        parseJson(
            request(
                url = rpcUrl(service, method),
                accessToken = accessToken,
                jsonBody = jsonBody,
                connectProtocol = true,
                kind = kind,
                maxBytes = maxBytes,
            ),
        )
    }

    suspend fun stripe(accessToken: String): JSONObject = withContext(Dispatchers.IO) {
        parseJson(
            request(
                url = "$CURSOR_WEB_BASE/api/auth/stripe",
                cookie = sessionCookie(accessToken),
            ),
        )
    }

    /** 云端任务（后台智能体）列表，认证方式与账单接口一致：Cursor 会话 Cookie。 */
    suspend fun agentTasks(accessToken: String): JSONObject = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("n", AGENT_TASKS_PAGE_SIZE)
            .put("include_status", true)
            .put("include_archived", false)
            .put("should_include_collaborators", true)
        parseJson(
            request(
                url = "$CURSOR_WEB_BASE/api/background-composer/list",
                cookie = sessionCookie(accessToken),
                jsonBody = body,
                webContext = true,
                kind = ApiKind.Tasks,
            ),
        )
    }

    /** Cursor 官方 Statuspage：当前总览、组件状态、未解决事件与计划维护。无需 Token。 */
    suspend fun statusSummary(): JSONObject = withContext(Dispatchers.IO) {
        parseJson(request(url = STATUS_SUMMARY_URL, kind = ApiKind.Status))
    }

    /** Cursor 官方 Statuspage：近期事件历史（含已恢复）。无需 Token。 */
    suspend fun statusIncidents(): JSONObject = withContext(Dispatchers.IO) {
        parseJson(request(url = STATUS_INCIDENTS_URL, kind = ApiKind.Status))
    }

    private fun request(
        url: String,
        accessToken: String? = null,
        cookie: String? = null,
        jsonBody: JSONObject? = null,
        connectProtocol: Boolean = false,
        webContext: Boolean = false,
        kind: ApiKind = ApiKind.Usage,
        maxBytes: Int = MAX_RESPONSE_BYTES,
    ): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = if (jsonBody == null) "GET" else "POST"
            connection.connectTimeout = CONNECT_TIMEOUT_MS
            connection.readTimeout = READ_TIMEOUT_MS
            connection.instanceFollowRedirects = false
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", USER_AGENT)
            if (webContext) {
                connection.setRequestProperty("Origin", CURSOR_WEB_BASE)
                connection.setRequestProperty("Referer", "$CURSOR_WEB_BASE/agents")
            }
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
            val body = readBody(stream, connection.contentLengthLong, status, kind, maxBytes)
            if (status !in 200..299) {
                android.util.Log.w("CursorApi", "HTTP $status for ${connection.url.path}")
                throw ApiException(status, errorMessage(status, kind, body))
            }
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
        maxBytes: Int,
    ): String {
        if (declaredLength > maxBytes) {
            throw ApiException(status, overflowMessage(kind))
        }
        if (stream == null) return ""
        return stream.use { input ->
            val initialSize = declaredLength
                .takeIf { it in 1..maxBytes.toLong() }
                ?.toInt()
                ?: DEFAULT_BUFFER_SIZE
            val output = ByteArrayOutputStream(initialSize)
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                if (total > maxBytes) {
                    throw ApiException(status, overflowMessage(kind))
                }
                output.write(buffer, 0, count)
            }
            output.toString(Charsets.UTF_8.name())
        }
    }

    private fun errorMessage(status: Int, kind: ApiKind, body: String = ""): String {
        extractCursorError(body)?.let { return it }
        return when (kind) {
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
            ApiKind.Tasks -> when (status) {
                401, 403 -> "Cursor Access Token 已过期或无效，请更新 Token"
                404 -> "云端任务接口已发生变化，请升级应用"
                429 -> "Cursor 请求过于频繁，请稍后重试"
                in 500..599 -> "Cursor 服务暂时不可用（HTTP $status）"
                else -> "无法加载云端任务（HTTP $status）"
            }
        }
    }

    private fun extractCursorError(body: String): String? {
        val json = runCatching { JSONObject(body.trim()) }.getOrNull() ?: return null
        val error = json.optJSONObject("error")
        error?.optJSONArray("details")?.let { details ->
            for (index in 0 until details.length()) {
                val item = details.optJSONObject(index) ?: continue
                val inner = item.optJSONObject("details")
                val detail = inner?.optString("detail")?.ifBlank { inner.optString("title") }
                if (!detail.isNullOrBlank()) return detail
            }
        }
        error?.optString("message")?.takeIf { it.isNotBlank() && !it.equals("Error", true) }?.let { return it }
        json.optString("message").takeIf { it.isNotBlank() && !it.equals("Error", true) }?.let { return it }
        return null
    }

    private fun overflowMessage(kind: ApiKind): String = when (kind) {
        ApiKind.Status -> "Cursor 状态页响应超过 1 MiB 安全上限"
        ApiKind.Usage -> "Cursor API 响应超过 1 MiB 安全上限"
        ApiKind.Tasks -> "云端任务响应超过 1 MiB 安全上限"
    }

    private fun sessionCookie(accessToken: String): String {
        val userId = TokenUtils.userId(accessToken)
        if (userId.isBlank()) throw ApiException(400, "Access Token 中缺少 Cursor 用户标识")
        return "WorkosCursorSessionToken=$userId%3A%3A$accessToken"
    }

    private fun rpcUrl(service: String, method: String): String {
        val allowedService = when (service) {
            DASHBOARD_SERVICE -> service
            else -> throw ApiException(400, "不支持的 Cursor 接口")
        }
        if (!RPC_METHOD.matches(method)) throw ApiException(400, "不支持的 Cursor 接口")
        return "$CURSOR_API_BASE/$allowedService/$method"
    }

    private fun parseJson(raw: String): JSONObject {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return JSONObject()
        return JSONObject(trimmed)
    }

    enum class ApiKind { Usage, Status, Tasks }

    private companion object {
        const val CURSOR_API_BASE = "https://api2.cursor.sh"
        const val CURSOR_WEB_BASE = "https://cursor.com"
        const val DASHBOARD_SERVICE = "aiserver.v1.DashboardService"
        const val STATUS_SUMMARY_URL = "https://status.cursor.com/api/v2/summary.json"
        const val STATUS_INCIDENTS_URL = "https://status.cursor.com/api/v2/incidents.json"
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 30_000
        const val MAX_RESPONSE_BYTES = 1024 * 1024
        const val AGENT_TASKS_PAGE_SIZE = 100
        val RPC_METHOD = Regex("^[A-Za-z][A-Za-z0-9]{1,80}$")

        // UA 版本号自动跟随 versionName（debug 构建带 -debug 后缀），便于服务端识别与问题排查
        val USER_AGENT = "CursorTAndroid/${BuildConfig.VERSION_NAME}"
    }
}
