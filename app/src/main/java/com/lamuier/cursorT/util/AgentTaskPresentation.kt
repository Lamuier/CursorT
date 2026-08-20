package com.lamuier.cursorT.util

import com.lamuier.cursorT.model.AgentTask
import com.lamuier.cursorT.model.AgentTaskPrStatus
import com.lamuier.cursorT.model.AgentTaskStatus
import java.net.URI
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object AgentTaskPresentation {
    fun statusLabel(status: AgentTaskStatus): String = when (status) {
        AgentTaskStatus.Creating -> "创建中"
        AgentTaskStatus.Running -> "运行中"
        AgentTaskStatus.Finished -> "已完成"
        AgentTaskStatus.Error -> "已出错"
        AgentTaskStatus.Expired -> "已过期"
        AgentTaskStatus.Unknown -> "未知状态"
    }

    fun prStatusLabel(status: AgentTaskPrStatus): String = when (status) {
        AgentTaskPrStatus.Open -> "PR 开启"
        AgentTaskPrStatus.Draft -> "PR 草稿"
        AgentTaskPrStatus.Merged -> "PR 已合并"
        AgentTaskPrStatus.Closed -> "PR 已关闭"
        AgentTaskPrStatus.Unknown -> "PR"
    }

    /** "cursor-grok-4.6-high" → "grok-4.6-high"；Max 模式追加标记。 */
    fun displayModel(modelName: String?, maxMode: Boolean): String? {
        val name = modelName?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val shortName = name.removePrefix("cursor-")
        return if (maxMode) "$shortName · Max" else shortName
    }

    fun formatRelative(epochMs: Long, nowMs: Long = System.currentTimeMillis()): String? {
        if (epochMs <= 0L) return null
        val diff = (nowMs - epochMs).coerceAtLeast(0L)
        return when {
            diff < MINUTE_MS -> "刚刚"
            diff < HOUR_MS -> "${diff / MINUTE_MS} 分钟前"
            diff < DAY_MS -> "${diff / HOUR_MS} 小时前"
            diff < 7L * DAY_MS -> "${diff / DAY_MS} 天前"
            else -> DATE_TIME.format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()))
        }
    }

    /** 仅允许 https 的 github.com 链接（PR 页），禁止 query / fragment / userinfo。 */
    fun isSafeAgentUrl(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        if (!uri.rawQuery.isNullOrBlank() || !uri.rawFragment.isNullOrBlank() || uri.userInfo != null) {
            return false
        }
        val host = uri.host?.lowercase(Locale.US) ?: return false
        return host == "github.com" || host.endsWith(".github.com")
    }

    fun agentsPageUrl(): String = "https://cursor.com/agents"

    fun isSafeBcId(bcId: String): Boolean = BC_ID.matches(bcId.trim())

    /** 仅由本机校验过的 bcId 拼接，避免把未校验查询串送进浏览器。 */
    fun agentConversationUrl(bcId: String): String? {
        val id = bcId.trim()
        if (!isSafeBcId(id)) return null
        return "https://cursor.com/agents?id=$id"
    }

    /**
     * 将用户输入的仓库地址规范为 `https://github.com/owner/repo` 或 GitLab 等价形式。
     * 仅允许 github.com / gitlab.com，禁止 query / fragment / userinfo。
     */
    fun normalizeRepositoryUrl(raw: String): String? {
        val trimmed = raw.trim().removeSuffix(".git")
        if (trimmed.isBlank() || trimmed.length > 400) return null
        val withScheme = when {
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            trimmed.startsWith("http://", ignoreCase = true) -> return null
            trimmed.contains("://") -> return null
            else -> "https://$trimmed"
        }
        val uri = runCatching { URI(withScheme) }.getOrNull() ?: return null
        if (!uri.scheme.equals("https", ignoreCase = true)) return null
        if (uri.userInfo != null || !uri.rawQuery.isNullOrBlank() || !uri.rawFragment.isNullOrBlank()) {
            return null
        }
        val host = uri.host?.lowercase(Locale.US) ?: return null
        val canonicalHost = when (host) {
            "github.com", "www.github.com" -> "github.com"
            "gitlab.com", "www.gitlab.com" -> "gitlab.com"
            else -> return null
        }
        val segments = uri.path.orEmpty().trim('/').split('/').filter { it.isNotBlank() }
        when (canonicalHost) {
            "github.com" -> if (segments.size != 2) return null
            "gitlab.com" -> if (segments.size !in 2..4) return null
            else -> return null
        }
        if (segments.any { !REPO_SEGMENT.matches(it) }) return null
        return "https://$canonicalHost/${segments.joinToString("/")}"
    }

    /** Cloud Agents 网页快照接口使用的无 scheme 仓库标识，如 `github.com/owner/repo`。 */
    fun snapshotRepository(httpsUrl: String): String? {
        val normalized = normalizeRepositoryUrl(httpsUrl) ?: return null
        return normalized.removePrefix("https://")
    }

    fun suggestedRepositories(tasks: List<AgentTask>): List<String> =
        tasks.mapNotNull { it.repoUrl?.let(::normalizeRepositoryUrl) }.distinct()

    fun canSendFollowup(status: AgentTaskStatus): Boolean = status != AgentTaskStatus.Expired

    fun sendDisabledReason(status: AgentTaskStatus): String? = when (status) {
        AgentTaskStatus.Expired -> "任务已过期，无法继续发送"
        else -> null
    }

    /** cursor.com 官网链接（「在网页打开」入口）。允许无查询串，或仅 `id=<bcId>`。 */
    fun isSafeCursorUrl(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        if (uri.userInfo != null || !uri.rawFragment.isNullOrBlank()) return false
        val host = uri.host?.lowercase(Locale.US) ?: return false
        if (host != "cursor.com" && !host.endsWith(".cursor.com")) return false
        val query = uri.rawQuery
        if (query.isNullOrBlank()) return true
        val path = uri.path.orEmpty().ifBlank { "/" }
        if (path != "/agents") return false
        val id = query.removePrefix("id=").takeIf { it.length == query.length - 3 } ?: return false
        return isSafeBcId(id)
    }

    private const val MINUTE_MS = 60_000L
    private const val HOUR_MS = 3_600_000L
    private const val DAY_MS = 86_400_000L
    private val BC_ID = Regex("^bc[-_][A-Za-z0-9_-]{1,80}$")
    private val REPO_SEGMENT = Regex("^[A-Za-z0-9._~-]+$")
    private val DATE_TIME: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.US)
}
