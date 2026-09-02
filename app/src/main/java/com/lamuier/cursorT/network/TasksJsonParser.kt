package com.lamuier.cursorT.network

import com.lamuier.cursorT.model.AgentTask
import com.lamuier.cursorT.model.AgentTaskPrStatus
import com.lamuier.cursorT.model.AgentTaskSource
import com.lamuier.cursorT.model.AgentTaskStatus
import com.lamuier.cursorT.model.CursorTasks
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 解析 cursor.com 云端任务（后台智能体）列表接口响应。
 *
 * 数据源为网页版 `cursor.com/agents` 使用的 `/api/background-composer/list`，
 * 以会话 Cookie 认证（与账单接口一致），无需额外凭据。接口无公开文档，
 * 解析全部采用防御式读取：字段缺失或结构变化时降级为空值而不是崩溃。
 */
object TasksJsonParser {
    fun parse(
        payload: JSONObject,
        accountId: Int,
        fetchedAt: String = nowStamp(),
        fromCache: Boolean = false,
        cacheAgeSeconds: Int = 0,
    ): CursorTasks {
        val tasks = parseTasksArray(payload.optJSONArray("composers"))
        return CursorTasks(
            accountId = accountId,
            tasks = tasks,
            fetchedAt = fetchedAt,
            fromCache = fromCache,
            cacheAgeSeconds = cacheAgeSeconds,
        )
    }

    /** 归一化为缓存 JSON（仅任务数据，缓存元信息由 TasksCacheStore 包装）。 */
    fun toJson(model: CursorTasks): String = JSONObject()
        .put("tasks", JSONArray(model.tasks.map(::taskToJson)))
        .toString()

    fun parseStored(
        rawJson: String,
        accountId: Int,
        fetchedAt: String,
        cacheAgeSeconds: Int,
    ): CursorTasks {
        val root = JSONObject(rawJson)
        val tasks = parseTasksArray(root.optJSONArray("tasks"))
        return CursorTasks(
            accountId = accountId,
            tasks = tasks,
            fetchedAt = fetchedAt,
            fromCache = true,
            cacheAgeSeconds = cacheAgeSeconds,
        )
    }

    fun nowStamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

    fun parseStatus(raw: String?): AgentTaskStatus =
        when (normalize(raw)?.removePrefix(STATUS_PREFIX)) {
            "creating" -> AgentTaskStatus.Creating
            "running" -> AgentTaskStatus.Running
            "finished" -> AgentTaskStatus.Finished
            "error" -> AgentTaskStatus.Error
            "expired" -> AgentTaskStatus.Expired
            else -> AgentTaskStatus.Unknown
        }

    /** 详情接口可能把任务包在 `composer` 里，也兼容列表那种扁平结构。 */
    fun parseComposerItem(item: JSONObject): AgentTask? {
        val nested = item.optJSONObject("composer")
        return parseTask(nested ?: item)
    }

    fun parsePrStatus(raw: String?): AgentTaskPrStatus? {
        val normalized = normalize(raw) ?: return null
        return when (normalized.removePrefix(PR_PREFIX)) {
            "open" -> AgentTaskPrStatus.Open
            "draft" -> AgentTaskPrStatus.Draft
            "merged" -> AgentTaskPrStatus.Merged
            "closed" -> AgentTaskPrStatus.Closed
            else -> AgentTaskPrStatus.Unknown
        }
    }

    fun parseSource(raw: String?): AgentTaskSource {
        val token = normalize(raw)?.removePrefix(SOURCE_PREFIX) ?: return AgentTaskSource.Unknown
        return when (token) {
            "website", "web" -> AgentTaskSource.Website
            "editor", "ide", "desktop" -> AgentTaskSource.Editor
            "slack" -> AgentTaskSource.Slack
            "linear" -> AgentTaskSource.Linear
            "ios_app", "ios", "android_app", "mobile" -> AgentTaskSource.Ios
            "api" -> AgentTaskSource.Api
            "github" -> AgentTaskSource.GitHub
            "cli" -> AgentTaskSource.Cli
            "github_ci_autofix", "github_ci", "ci_autofix" -> AgentTaskSource.GitHubCi
            "gitlab" -> AgentTaskSource.GitLab
            "environment_setup_web", "environment_setup" -> AgentTaskSource.EnvSetup
            "grind_web", "grind" -> AgentTaskSource.Grind
            "bugbot_autofix", "bugbot" -> AgentTaskSource.Bugbot
            "automations", "automation" -> AgentTaskSource.Automations
            "sdk" -> AgentTaskSource.Sdk
            "grok_bot", "grokbot", "grok", "sand" -> AgentTaskSource.GrokBot
            "unspecified" -> AgentTaskSource.Unknown
            else -> AgentTaskSource.Unknown
        }
    }

    private fun parseTasksArray(array: JSONArray?): List<AgentTask> {
        if (array == null) return emptyList()
        return buildList(array.length()) {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                val task = parseTask(item) ?: continue
                add(task)
            }
        }.sortedByDescending { it.latestTimeMs }
    }

    private fun parseTask(item: JSONObject): AgentTask? {
        val id = item.optString("bcId")
            .ifBlank { item.optString("id") }
            .takeIf { it.isNotBlank() } ?: return null
        val modelDetails = item.optJSONObject("modelDetails")
        return AgentTask(
            id = id,
            name = item.optString("name").ifBlank { "未命名任务" },
            status = parseStatus(item.nullableString("status")),
            repoUrl = item.nullableString("repoUrl"),
            branchName = item.nullableString("branchName"),
            prUrl = item.nullableString("prUrl"),
            prStatus = parsePrStatus(item.nullableString("prStatus")),
            linesAdded = item.optInt("linesAdded", 0).coerceAtLeast(0),
            linesRemoved = item.optInt("linesRemoved", 0).coerceAtLeast(0),
            filesChanged = item.optInt("filesChanged", 0).coerceAtLeast(0),
            modelName = modelDetails?.nullableString("modelName")
                ?: item.nullableString("modelName"),
            maxMode = modelDetails?.optBoolean("maxMode", false)
                ?: item.optBoolean("maxMode", false),
            createdAtMs = item.optLong("createdAtMs", 0L),
            updatedAtMs = item.optLong("updatedAtMs", 0L),
            lastActivityMs = item.nullableLong("lastMessageActivityAtMs")
                ?: item.nullableLong("lastActivityMs"),
            source = parseSource(item.nullableString("source")),
        )
    }

    private fun taskToJson(task: AgentTask): JSONObject = JSONObject()
        .put("id", task.id)
        .put("name", task.name)
        .put("status", statusWireName(task.status))
        .put("repoUrl", task.repoUrl ?: JSONObject.NULL)
        .put("branchName", task.branchName ?: JSONObject.NULL)
        .put("prUrl", task.prUrl ?: JSONObject.NULL)
        .put("prStatus", task.prStatus?.let(::prStatusWireName) ?: JSONObject.NULL)
        .put("linesAdded", task.linesAdded)
        .put("linesRemoved", task.linesRemoved)
        .put("filesChanged", task.filesChanged)
        .put("modelName", task.modelName ?: JSONObject.NULL)
        .put("maxMode", task.maxMode)
        .put("createdAtMs", task.createdAtMs)
        .put("updatedAtMs", task.updatedAtMs)
        .put("lastActivityMs", task.lastActivityMs ?: JSONObject.NULL)
        .put("source", sourceWireName(task.source))

    private fun statusWireName(status: AgentTaskStatus): String = when (status) {
        AgentTaskStatus.Creating -> "creating"
        AgentTaskStatus.Running -> "running"
        AgentTaskStatus.Finished -> "finished"
        AgentTaskStatus.Error -> "error"
        AgentTaskStatus.Expired -> "expired"
        AgentTaskStatus.Unknown -> "unknown"
    }

    private fun prStatusWireName(status: AgentTaskPrStatus): String = when (status) {
        AgentTaskPrStatus.Open -> "open"
        AgentTaskPrStatus.Draft -> "draft"
        AgentTaskPrStatus.Merged -> "merged"
        AgentTaskPrStatus.Closed -> "closed"
        AgentTaskPrStatus.Unknown -> "unknown"
    }

    private fun sourceWireName(source: AgentTaskSource): String = when (source) {
        AgentTaskSource.Website -> "website"
        AgentTaskSource.Editor -> "editor"
        AgentTaskSource.Slack -> "slack"
        AgentTaskSource.Linear -> "linear"
        AgentTaskSource.Ios -> "ios_app"
        AgentTaskSource.Api -> "api"
        AgentTaskSource.GitHub -> "github"
        AgentTaskSource.Cli -> "cli"
        AgentTaskSource.GitHubCi -> "github_ci_autofix"
        AgentTaskSource.GitLab -> "gitlab"
        AgentTaskSource.EnvSetup -> "environment_setup_web"
        AgentTaskSource.Grind -> "grind_web"
        AgentTaskSource.Bugbot -> "bugbot_autofix"
        AgentTaskSource.Automations -> "automations"
        AgentTaskSource.Sdk -> "sdk"
        AgentTaskSource.GrokBot -> "grok_bot"
        AgentTaskSource.Unknown -> "unknown"
    }

    private fun normalize(raw: String?): String? =
        raw?.trim()?.takeIf { it.isNotBlank() }?.lowercase(Locale.US)

    private fun JSONObject.nullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).takeIf { it.isNotBlank() }
    }

    private fun JSONObject.nullableLong(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        return optLong(key, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }
    }

    private const val STATUS_PREFIX = "background_composer_status_"
    private const val PR_PREFIX = "pr_status_"
    private const val SOURCE_PREFIX = "background_composer_source_"
}
