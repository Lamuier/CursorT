package com.lamuier.cursorT.util

import android.content.res.Resources
import com.lamuier.cursorT.R
import com.lamuier.cursorT.model.AgentTaskPrStatus
import com.lamuier.cursorT.model.AgentTaskSource
import com.lamuier.cursorT.model.AgentTaskStatus
import java.net.URI
import java.time.ZoneId
import java.util.Locale

object AgentTaskPresentation {
    fun statusLabel(status: AgentTaskStatus, resources: Resources? = null): String {
        val id = when (status) {
            AgentTaskStatus.Creating -> R.string.task_status_creating
            AgentTaskStatus.Running -> R.string.task_status_running
            AgentTaskStatus.Finished -> R.string.task_status_finished
            AgentTaskStatus.Error -> R.string.task_status_error
            AgentTaskStatus.Expired -> R.string.task_status_expired
            AgentTaskStatus.Unknown -> R.string.task_status_unknown
        }
        return resources?.getString(id) ?: fallbackZh(id)
    }

    fun prStatusLabel(status: AgentTaskPrStatus, resources: Resources? = null): String {
        val id = when (status) {
            AgentTaskPrStatus.Open -> R.string.task_pr_open
            AgentTaskPrStatus.Draft -> R.string.task_pr_draft
            AgentTaskPrStatus.Merged -> R.string.task_pr_merged
            AgentTaskPrStatus.Closed -> R.string.task_pr_closed
            AgentTaskPrStatus.Unknown -> R.string.task_pr_unknown
        }
        return resources?.getString(id) ?: fallbackZh(id)
    }

    fun sourceLabel(source: AgentTaskSource, resources: Resources? = null): String {
        val id = when (source) {
            AgentTaskSource.Website -> R.string.task_source_website
            AgentTaskSource.Editor -> R.string.task_source_editor
            AgentTaskSource.Slack -> R.string.task_source_slack
            AgentTaskSource.Linear -> R.string.task_source_linear
            AgentTaskSource.Ios -> R.string.task_source_ios
            AgentTaskSource.Api -> R.string.task_source_api
            AgentTaskSource.GitHub -> R.string.task_source_github
            AgentTaskSource.Cli -> R.string.task_source_cli
            AgentTaskSource.GitHubCi -> R.string.task_source_github_ci
            AgentTaskSource.GitLab -> R.string.task_source_gitlab
            AgentTaskSource.EnvSetup -> R.string.task_source_env_setup
            AgentTaskSource.Grind -> R.string.task_source_grind
            AgentTaskSource.Bugbot -> R.string.task_source_bugbot
            AgentTaskSource.Automations -> R.string.task_source_automations
            AgentTaskSource.Sdk -> R.string.task_source_sdk
            AgentTaskSource.GrokBot -> R.string.task_source_grok_bot
            AgentTaskSource.Unknown -> R.string.task_source_unknown
        }
        return resources?.getString(id) ?: fallbackZh(id)
    }

    /** `github.com/owner/repo` / 带协议的 URL → `owner/repo`。 */
    fun repoDisplayName(repoUrl: String?): String? {
        val raw = repoUrl?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val trimmed = raw
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .removeSuffix(".git")
            .trimEnd('/')
        val withoutHost = when {
            trimmed.startsWith("github.com/", ignoreCase = true) ->
                trimmed.substringAfter('/')
            trimmed.startsWith("gitlab.com/", ignoreCase = true) ->
                trimmed.substringAfter('/')
            else -> trimmed
        }
        return withoutHost.ifBlank { trimmed }
    }

    /** "cursor-grok-4.6-high" → "grok-4.6-high"；Max 模式追加标记。 */
    fun displayModel(modelName: String?, maxMode: Boolean): String? {
        val name = modelName?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val shortName = name.removePrefix("cursor-")
        return if (maxMode) "$shortName · Max" else shortName
    }

    fun formatRelative(
        epochMs: Long,
        nowMs: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
        resources: Resources? = null,
    ): String? {
        if (epochMs <= 0L) return null
        val diff = (nowMs - epochMs).coerceAtLeast(0L)
        return when {
            diff < MINUTE_MS -> resources?.getString(R.string.relative_just_now) ?: "刚刚"
            diff < HOUR_MS -> resources?.getString(R.string.relative_minutes_ago, diff / MINUTE_MS)
                ?: "${diff / MINUTE_MS} 分钟前"
            diff < DAY_MS -> resources?.getString(R.string.relative_hours_ago, diff / HOUR_MS)
                ?: "${diff / HOUR_MS} 小时前"
            diff < 7L * DAY_MS -> resources?.getString(R.string.relative_days_ago, diff / DAY_MS)
                ?: "${diff / DAY_MS} 天前"
            else -> DisplayTime.formatEpoch(epochMs, zone)
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

    /** Custom Tabs 仅允许 Cursor Agents 页或已校验的 GitHub PR 链接。 */
    fun isAllowedCustomTabUrl(url: String): Boolean = isSafeCursorUrl(url) || isSafeAgentUrl(url)

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

    private fun fallbackZh(id: Int): String = when (id) {
        R.string.task_status_creating -> "创建中"
        R.string.task_status_running -> "运行中"
        R.string.task_status_finished -> "已完成"
        R.string.task_status_error -> "已出错"
        R.string.task_status_expired -> "已过期"
        R.string.task_status_unknown -> "未知状态"
        R.string.task_pr_open -> "PR 开启"
        R.string.task_pr_draft -> "PR 草稿"
        R.string.task_pr_merged -> "PR 已合并"
        R.string.task_pr_closed -> "PR 已关闭"
        R.string.task_pr_unknown -> "PR"
        R.string.task_source_website -> "网页"
        R.string.task_source_editor -> "编辑器"
        R.string.task_source_slack -> "Slack"
        R.string.task_source_linear -> "Linear"
        R.string.task_source_ios -> "移动端"
        R.string.task_source_api -> "API"
        R.string.task_source_github -> "GitHub"
        R.string.task_source_cli -> "CLI"
        R.string.task_source_github_ci -> "GitHub CI"
        R.string.task_source_gitlab -> "GitLab"
        R.string.task_source_env_setup -> "环境配置"
        R.string.task_source_grind -> "Grind"
        R.string.task_source_bugbot -> "Bugbot"
        R.string.task_source_automations -> "自动化"
        R.string.task_source_sdk -> "SDK"
        R.string.task_source_grok_bot -> "Grok Bot"
        R.string.task_source_unknown -> "其他来源"
        else -> ""
    }
}
