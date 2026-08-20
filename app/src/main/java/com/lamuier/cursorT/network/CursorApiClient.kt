package com.lamuier.cursorT.network

import com.lamuier.cursorT.BuildConfig
import com.lamuier.cursorT.util.AgentTaskPresentation
import com.lamuier.cursorT.util.TokenUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

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
            ),
        )
    }

    /** 单个云端任务详情（含可能出现的对话字段）。 */
    suspend fun agentTaskDetail(accessToken: String, bcId: String): JSONObject = withContext(Dispatchers.IO) {
        val id = requireBcId(bcId)
        parseJson(
            request(
                url = "$CURSOR_WEB_BASE/api/background-composer/get-detailed-composer",
                cookie = sessionCookie(accessToken),
                jsonBody = JSONObject()
                    .put("bcId", id)
                    .put("n", 1)
                    .put("includeDiff", false)
                    .put("includeTeamWide", true)
                    .put("includeConversation", true)
                    .put("includeFollowups", true),
                webContext = true,
                kind = ApiKind.Tasks,
                maxBytes = MAX_CONVERSATION_BYTES,
            ),
        )
    }

    /**
     * 官方 Cloud Agents 对话：包含创建后的助手回复与全部跟进。
     * 网页详情接口通常只有创建时的种子消息，因此需要这条作为主来源。
     */
    suspend fun agentTaskCloudConversationOrNull(accessToken: String, bcId: String): JSONObject? =
        withContext(Dispatchers.IO) {
            val id = requireBcId(bcId)
            val cloudUrl = "$CURSOR_CLOUD_API/v0/agents/$id/conversation"
            requestOptionalJson(
                url = cloudUrl,
                accessToken = accessToken,
                kind = ApiKind.Tasks,
                maxBytes = MAX_CONVERSATION_BYTES,
            ) ?: requestOptionalJson(
                url = cloudUrl,
                cookie = sessionCookie(accessToken),
                webContext = true,
                kind = ApiKind.Tasks,
                maxBytes = MAX_CONVERSATION_BYTES,
            ) ?: requestOptionalJson(
                url = cloudUrl,
                basicAuth = accessToken,
                kind = ApiKind.Tasks,
                maxBytes = MAX_CONVERSATION_BYTES,
            ) ?: requestOptionalJson(
                url = "$CURSOR_WEB_BASE/api/v0/agents/$id/conversation",
                cookie = sessionCookie(accessToken),
                webContext = true,
                kind = ApiKind.Tasks,
                maxBytes = MAX_CONVERSATION_BYTES,
            )
        }

    suspend fun agentTaskGetComposerOrNull(accessToken: String, bcId: String): JSONObject? =
        withContext(Dispatchers.IO) {
            val id = requireBcId(bcId)
            requestOptionalJson(
                url = "$CURSOR_WEB_BASE/api/background-composer/get-composer",
                cookie = sessionCookie(accessToken),
                jsonBody = JSONObject().put("bcId", id),
                webContext = true,
                kind = ApiKind.Tasks,
                maxBytes = MAX_CONVERSATION_BYTES,
            )
        }

    suspend fun agentTaskComposerRpcOrNull(accessToken: String, bcId: String): JSONObject? =
        withContext(Dispatchers.IO) {
            val id = requireBcId(bcId)
            requestOptionalJson(
                url = rpcUrl(BACKGROUND_COMPOSER_SERVICE, "GetComposer"),
                accessToken = accessToken,
                jsonBody = JSONObject().put("bcId", id),
                connectProtocol = true,
                kind = ApiKind.Tasks,
                maxBytes = MAX_CONVERSATION_BYTES,
            )
        }
    suspend fun agentTaskConversationOrNull(accessToken: String, bcId: String): JSONObject? =
        withContext(Dispatchers.IO) {
            val id = requireBcId(bcId)
            requestOptionalJson(
                url = "$CURSOR_WEB_BASE/api/background-composer/get-conversation",
                cookie = sessionCookie(accessToken),
                jsonBody = JSONObject().put("bcId", id),
                webContext = true,
                kind = ApiKind.Tasks,
                maxBytes = MAX_CONVERSATION_BYTES,
            )
        }

    suspend fun agentTaskComposerConversationOrNull(accessToken: String, bcId: String): JSONObject? =
        withContext(Dispatchers.IO) {
            val id = requireBcId(bcId)
            requestOptionalJson(
                url = "$CURSOR_WEB_BASE/api/background-composer/get-composer-conversation",
                cookie = sessionCookie(accessToken),
                jsonBody = JSONObject().put("bcId", id),
                webContext = true,
                kind = ApiKind.Tasks,
                maxBytes = MAX_CONVERSATION_BYTES,
            )
        }

    suspend fun agentTaskConversationRpcOrNull(accessToken: String, bcId: String): JSONObject? =
        withContext(Dispatchers.IO) {
            val id = requireBcId(bcId)
            requestOptionalJson(
                url = rpcUrl(BACKGROUND_COMPOSER_SERVICE, "GetComposerConversation"),
                accessToken = accessToken,
                jsonBody = JSONObject().put("bcId", id),
                connectProtocol = true,
                kind = ApiKind.Tasks,
                maxBytes = MAX_CONVERSATION_BYTES,
            )
        }

    suspend fun agentTaskDetailRpcOrNull(accessToken: String, bcId: String): JSONObject? =
        withContext(Dispatchers.IO) {
            val id = requireBcId(bcId)
            requestOptionalJson(
                url = rpcUrl(BACKGROUND_COMPOSER_SERVICE, "GetDetailedComposer"),
                accessToken = accessToken,
                jsonBody = JSONObject()
                    .put("bcId", id)
                    .put("n", AGENT_TASKS_PAGE_SIZE)
                    .put("includeDiff", false),
                connectProtocol = true,
                kind = ApiKind.Tasks,
                maxBytes = MAX_CONVERSATION_BYTES,
            )
        }

    /** 向云端任务发送跟进消息。优先网页接口，再尝试官方 Cloud Agents API，最后 Connect RPC。 */
    suspend fun addAgentFollowup(accessToken: String, bcId: String, text: String): JSONObject =
        withContext(Dispatchers.IO) {
            val id = requireBcId(bcId)
            val body = followupBody(id, text)
            val cloudBody = TaskCreatePayload.cloudFollowup(text)
            val web = requestOrNullOn404(
                url = "$CURSOR_WEB_BASE/api/background-composer/add-followup",
                cookie = sessionCookie(accessToken),
                jsonBody = body,
                webContext = true,
                kind = ApiKind.Tasks,
            )
            if (web != null) return@withContext parseJson(web)
            val cloud = requestOrNullOnUnavailable(
                url = "$CURSOR_CLOUD_API/v0/agents/$id/followup",
                accessToken = accessToken,
                jsonBody = cloudBody,
                kind = ApiKind.Tasks,
            ) ?: requestOrNullOnUnavailable(
                url = "$CURSOR_CLOUD_API/v0/agents/$id/followup",
                basicAuth = accessToken,
                jsonBody = cloudBody,
                kind = ApiKind.Tasks,
            ) ?: requestOrNullOnUnavailable(
                url = "$CURSOR_CLOUD_API/v1/agents/$id/runs",
                accessToken = accessToken,
                jsonBody = cloudBody,
                kind = ApiKind.Tasks,
            )
            if (cloud != null) return@withContext parseJson(cloud)
            parseJson(
                request(
                    url = rpcUrl(BACKGROUND_COMPOSER_SERVICE, "AddAsyncFollowupBackgroundComposer"),
                    accessToken = accessToken,
                    jsonBody = body,
                    connectProtocol = true,
                    kind = ApiKind.Tasks,
                ),
            )
        }

    /**
     * 新建云端任务。优先网页版 `startBackgroundComposerFromSnapshot`（与列表同一套会话 Cookie），
     * 缺失时回退官方 Cloud Agents API 与 Connect RPC。
     */
    suspend fun createAgentTask(
        accessToken: String,
        prompt: String,
        snapshotRepo: String,
        httpsRepo: String,
        ref: String?,
        modelName: String?,
        autoCreatePr: Boolean,
    ): JSONObject = withContext(Dispatchers.IO) {
        val bcId = TaskCreatePayload.newBcId()
        val webBody = TaskCreatePayload.webSnapshot(
            bcId = bcId,
            prompt = prompt,
            snapshotRepo = snapshotRepo,
            httpsRepo = httpsRepo,
            ref = ref,
            modelName = modelName,
            autoCreatePr = autoCreatePr,
        )
        val web = requestOrNullOn404(
            url = "$CURSOR_WEB_BASE/api/auth/startBackgroundComposerFromSnapshot",
            cookie = sessionCookie(accessToken),
            jsonBody = webBody,
            webContext = true,
            kind = ApiKind.Tasks,
        )
        if (web != null) {
            val parsed = parseJson(web)
            if (TaskCreatePayload.createdBcId(parsed, "") == "") parsed.put("bcId", bcId)
            return@withContext parsed.put("requestedBcId", bcId)
        }
        val cloudBody = TaskCreatePayload.cloudAgent(
            prompt = prompt,
            httpsRepo = httpsRepo,
            ref = ref,
            autoCreatePr = autoCreatePr,
        )
        val cloud = requestOrNullOnUnavailable(
            url = "$CURSOR_CLOUD_API/v0/agents",
            accessToken = accessToken,
            jsonBody = cloudBody,
            kind = ApiKind.Tasks,
        ) ?: requestOrNullOnUnavailable(
            url = "$CURSOR_CLOUD_API/v0/agents",
            basicAuth = accessToken,
            jsonBody = cloudBody,
            kind = ApiKind.Tasks,
        )
        if (cloud != null) return@withContext parseJson(cloud)
        parseJson(
            request(
                url = rpcUrl(BACKGROUND_COMPOSER_SERVICE, "StartBackgroundComposerFromSnapshot"),
                accessToken = accessToken,
                jsonBody = webBody,
                connectProtocol = true,
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
        basicAuth: String? = null,
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
            basicAuth?.let { token ->
                val encoded = Base64.getEncoder().encodeToString("$token:".toByteArray(Charsets.UTF_8))
                connection.setRequestProperty("Authorization", "Basic $encoded")
            }
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

    private fun requestOrNullOn404(
        url: String,
        accessToken: String? = null,
        cookie: String? = null,
        basicAuth: String? = null,
        jsonBody: JSONObject? = null,
        connectProtocol: Boolean = false,
        webContext: Boolean = false,
        kind: ApiKind,
        maxBytes: Int = MAX_RESPONSE_BYTES,
    ): String? = try {
        request(
            url = url,
            accessToken = accessToken,
            cookie = cookie,
            basicAuth = basicAuth,
            jsonBody = jsonBody,
            connectProtocol = connectProtocol,
            webContext = webContext,
            kind = kind,
            maxBytes = maxBytes,
        )
    } catch (error: ApiException) {
        if (error.statusCode == 404 || error.statusCode == 405) null else throw error
    }

    /** 备用官方接口：鉴权方式可能与会话 Token 不同，4xx 视为该路径不可用。 */
    private fun requestOrNullOnUnavailable(
        url: String,
        accessToken: String? = null,
        cookie: String? = null,
        basicAuth: String? = null,
        jsonBody: JSONObject? = null,
        connectProtocol: Boolean = false,
        webContext: Boolean = false,
        kind: ApiKind,
        maxBytes: Int = MAX_RESPONSE_BYTES,
    ): String? = try {
        request(
            url = url,
            accessToken = accessToken,
            cookie = cookie,
            basicAuth = basicAuth,
            jsonBody = jsonBody,
            connectProtocol = connectProtocol,
            webContext = webContext,
            kind = kind,
            maxBytes = maxBytes,
        )
    } catch (error: ApiException) {
        if (error.statusCode in OPTIONAL_HTTP) null else throw error
    }

    /** 对话补充接口：路径不稳定，4xx/501 视为不可用而不是整页失败。 */
    private fun requestOptionalJson(
        url: String,
        accessToken: String? = null,
        cookie: String? = null,
        basicAuth: String? = null,
        jsonBody: JSONObject? = null,
        connectProtocol: Boolean = false,
        webContext: Boolean = false,
        kind: ApiKind,
        maxBytes: Int = MAX_RESPONSE_BYTES,
    ): JSONObject? {
        val raw = try {
            request(
                url = url,
                accessToken = accessToken,
                cookie = cookie,
                basicAuth = basicAuth,
                jsonBody = jsonBody,
                connectProtocol = connectProtocol,
                webContext = webContext,
                kind = kind,
                maxBytes = maxBytes,
            )
        } catch (error: ApiException) {
            if (error.statusCode in OPTIONAL_HTTP) null else throw error
        } ?: return null
        return runCatching { parseJson(raw) }.getOrNull()
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
                404 -> "找不到该云端任务，或对话接口已变化"
                409 -> "智能体正在处理上一轮对话，请稍后再发送"
                429 -> "Cursor 请求过于频繁，请稍后重试"
                in 500..599 -> "Cursor 服务暂时不可用（HTTP $status）"
                else -> "无法完成云端任务操作（HTTP $status）"
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
        ApiKind.Tasks -> "云端任务对话响应超过 2 MiB 安全上限"
    }

    private fun sessionCookie(accessToken: String): String {
        val userId = TokenUtils.userId(accessToken)
        if (userId.isBlank()) throw ApiException(400, "Access Token 中缺少 Cursor 用户标识")
        return "WorkosCursorSessionToken=$userId%3A%3A$accessToken"
    }

    private fun requireBcId(bcId: String): String {
        val id = bcId.trim()
        if (!AgentTaskPresentation.isSafeBcId(id)) {
            throw ApiException(400, "云端任务标识无效")
        }
        return id
    }

    private fun followupBody(bcId: String, text: String): JSONObject = JSONObject()
        .put("bcId", bcId)
        .put(
            "followupMessage",
            JSONObject()
                .put("text", text)
                .put("type", "MESSAGE_TYPE_HUMAN"),
        )

    private fun rpcUrl(service: String, method: String): String {
        val allowedService = when (service) {
            DASHBOARD_SERVICE, BACKGROUND_COMPOSER_SERVICE -> service
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
        const val CURSOR_CLOUD_API = "https://api.cursor.com"
        const val DASHBOARD_SERVICE = "aiserver.v1.DashboardService"
        const val BACKGROUND_COMPOSER_SERVICE = "aiserver.v1.BackgroundComposerService"
        const val STATUS_SUMMARY_URL = "https://status.cursor.com/api/v2/summary.json"
        const val STATUS_INCIDENTS_URL = "https://status.cursor.com/api/v2/incidents.json"
        const val CONNECT_TIMEOUT_MS = 15_000
        const val READ_TIMEOUT_MS = 30_000
        const val MAX_RESPONSE_BYTES = 1024 * 1024
        const val MAX_CONVERSATION_BYTES = 2 * 1024 * 1024
        const val AGENT_TASKS_PAGE_SIZE = 100
        val OPTIONAL_HTTP = setOf(400, 401, 403, 404, 405, 415, 422, 501)
        val RPC_METHOD = Regex("^[A-Za-z][A-Za-z0-9]{1,80}$")

        // UA 版本号自动跟随 versionName（debug 构建带 -debug 后缀），便于服务端识别与问题排查
        val USER_AGENT = "CursorTAndroid/${BuildConfig.VERSION_NAME}"
    }
}
