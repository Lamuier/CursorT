package com.lamuier.cursorT.network

import com.lamuier.cursorT.model.AgentTask
import com.lamuier.cursorT.model.AgentTaskConversation
import com.lamuier.cursorT.model.AgentTaskMessage
import com.lamuier.cursorT.model.AgentTaskMessageRole
import com.lamuier.cursorT.model.AgentTaskStatus
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * 解析云端任务对话。网页版、Cloud Agents API 与 Connect RPC 字段不完全稳定，
 * 因此按多种常见形状防御式读取，无法识别的条目直接跳过。
 *
 * 详情接口常常同时带有创建时的短 `conversationHistory` 和完整 `messages` /
 * `bubbles` / `followupMessages`。必须收集全部候选再合并，不能拿到第一个非空数组就停。
 * 列表里可能夹着其它任务，消息只从当前 `bcId` 对应的 composer 读取。
 */
object TaskConversationJsonParser {
    fun parse(
        payload: JSONObject,
        fallbackTask: AgentTask,
        accountId: Int,
        fetchedAt: String,
        fromCache: Boolean = false,
    ): AgentTaskConversation {
        val parsedTask = parseTask(payload, expectedId = fallbackTask.id)
        return AgentTaskConversation(
            accountId = accountId,
            task = mergeTask(parsedTask, fallbackTask),
            messages = parseMessages(payload, taskId = fallbackTask.id).take(MAX_MESSAGES),
            fetchedAt = fetchedAt,
            fromCache = fromCache,
        )
    }

    /**
     * 详情接口优先提供任务元数据；其它对话接口（尤其是官方 conversation）可能更完整。
     * 任务字段始终取自 primary，消息合并各候选后取最完整的一份。
     */
    fun parsePreferred(
        primary: JSONObject,
        extras: List<JSONObject>,
        fallbackTask: AgentTask,
        accountId: Int,
        fetchedAt: String,
    ): AgentTaskConversation {
        val parsedPrimary = parse(
            payload = primary,
            fallbackTask = fallbackTask,
            accountId = accountId,
            fetchedAt = fetchedAt,
        )
        if (extras.isEmpty()) return parsedPrimary
        val candidates = buildList {
            add(parsedPrimary)
            extras.forEach { extra ->
                add(
                    parse(
                        payload = extra,
                        fallbackTask = parsedPrimary.task,
                        accountId = accountId,
                        fetchedAt = fetchedAt,
                    ),
                )
            }
        }
        return parsedPrimary.copy(messages = mergeMessageLists(candidates.map { it.messages }))
    }

    private fun mergeTask(parsed: AgentTask?, fallback: AgentTask): AgentTask {
        if (parsed == null || parsed.id != fallback.id) return fallback
        return fallback.copy(
            name = parsed.name.takeIf { it.isNotBlank() && it != "未命名任务" } ?: fallback.name,
            status = parsed.status.takeUnless { it == AgentTaskStatus.Unknown } ?: fallback.status,
            repoUrl = parsed.repoUrl ?: fallback.repoUrl,
            branchName = parsed.branchName ?: fallback.branchName,
            prUrl = parsed.prUrl ?: fallback.prUrl,
            prStatus = parsed.prStatus ?: fallback.prStatus,
            linesAdded = parsed.linesAdded.takeIf { it > 0 } ?: fallback.linesAdded,
            linesRemoved = parsed.linesRemoved.takeIf { it > 0 } ?: fallback.linesRemoved,
            filesChanged = parsed.filesChanged.takeIf { it > 0 } ?: fallback.filesChanged,
            modelName = parsed.modelName ?: fallback.modelName,
            maxMode = parsed.maxMode || fallback.maxMode,
            createdAtMs = parsed.createdAtMs.takeIf { it > 0L } ?: fallback.createdAtMs,
            updatedAtMs = parsed.updatedAtMs.takeIf { it > 0L } ?: fallback.updatedAtMs,
            lastActivityMs = parsed.lastActivityMs ?: fallback.lastActivityMs,
        )
    }

    fun parseTask(payload: JSONObject, expectedId: String? = null): AgentTask? {
        val parsed = findComposerObjects(payload).mapNotNull(TasksJsonParser::parseComposerItem)
        if (expectedId != null) {
            parsed.firstOrNull { it.id == expectedId }?.let { return it }
        }
        return parsed.firstOrNull() ?: TasksJsonParser.parseComposerItem(payload)
    }

    fun parseMessages(payload: JSONObject, taskId: String? = null): List<AgentTaskMessage> {
        val candidates = mutableListOf<List<AgentTaskMessage>>()
        val matching = taskId?.let { findMatchingComposer(payload, it) }
        if (matching != null) {
            collectCandidates(
                node = matching,
                depth = 0,
                into = candidates,
                skipComposerSiblings = true,
            )
            collectTopLevelArrays(payload, candidates)
        } else {
            collectCandidates(
                node = payload,
                depth = 0,
                into = candidates,
                skipComposerSiblings = false,
            )
        }
        val best = candidates.maxByOrNull(::score).orEmpty()
        return mergeConsecutiveAssistants(best).take(MAX_MESSAGES)
    }

    internal fun score(conversation: AgentTaskConversation): Int = score(conversation.messages)

    internal fun mergeMessageLists(lists: List<List<AgentTaskMessage>>): List<AgentTaskMessage> {
        if (lists.isEmpty()) return emptyList()
        val ranked = lists.filter { it.isNotEmpty() }.sortedByDescending(::score)
        if (ranked.isEmpty()) return emptyList()
        val seen = linkedSetOf<String>()
        val merged = mutableListOf<AgentTaskMessage>()
        fun take(message: AgentTaskMessage): Boolean {
            val key = dedupeKey(message)
            if (key in seen) return false
            seen += key
            merged += message
            return true
        }
        ranked.first().forEach { take(it) }
        if (merged.firstOrNull()?.role != AgentTaskMessageRole.User) {
            ranked.drop(1).mapNotNull { list ->
                list.firstOrNull()?.takeIf { it.role == AgentTaskMessageRole.User && dedupeKey(it) !in seen }
            }.asReversed().forEach { opener ->
                val key = dedupeKey(opener)
                seen += key
                merged.add(0, opener)
            }
        }
        ranked.drop(1).forEach { list ->
            val isSeedOnly = list.size == 1 && list.first().role == AgentTaskMessageRole.User
            if (isSeedOnly && merged.any { it.role == AgentTaskMessageRole.User }) return@forEach
            list.forEach { take(it) }
        }
        return if (merged.count { it.createdAtMs != null } >= (merged.size + 1) / 2 &&
            merged.any { it.createdAtMs != null }
        ) {
            merged.sortedBy { it.createdAtMs ?: Long.MAX_VALUE }
        } else {
            merged
        }
    }

    private fun score(messages: List<AgentTaskMessage>): Int {
        if (messages.isEmpty()) return 0
        val hasUser = messages.any { it.role == AgentTaskMessageRole.User }
        val hasAssistant = messages.any { it.role == AgentTaskMessageRole.Assistant }
        var value = messages.size * 10
        if (hasUser && hasAssistant) value += 1_000
        else if (hasAssistant) value += 200
        return value
    }

    private fun dedupeKey(message: AgentTaskMessage): String =
        "${message.role}:${message.text.trim()}"

    private fun findComposerObjects(root: JSONObject): List<JSONObject> = buildList {
        root.optJSONArray("composers")?.let { array ->
            for (index in 0 until array.length()) {
                array.optJSONObject(index)?.let { add(it) }
            }
        }
        root.optJSONObject("composer")?.let { add(it) }
        add(root)
    }

    private fun findMatchingComposer(payload: JSONObject, taskId: String): JSONObject? {
        payload.optJSONArray("composers")?.let { array ->
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                if (composerId(item) == taskId) return item
            }
        }
        payload.optJSONObject("composer")?.let { nested ->
            if (composerId(nested) == taskId || composerId(payload) == taskId) return payload
        }
        if (composerId(payload) == taskId) return payload
        return null
    }

    private fun composerId(item: JSONObject): String {
        val nested = item.optJSONObject("composer")
        return nested?.optString("bcId")?.ifBlank { nested.optString("id") }
            ?.ifBlank { item.optString("bcId") }
            ?.ifBlank { item.optString("id") }
            .orEmpty()
            .ifBlank { item.optString("bcId") }
            .ifBlank { item.optString("id") }
    }

    private fun collectTopLevelArrays(
        payload: JSONObject,
        into: MutableList<List<AgentTaskMessage>>,
    ) {
        MESSAGE_ARRAY_KEYS.forEach { key ->
            parseMessageArray(payload.optJSONArray(key)).takeIf { it.isNotEmpty() }?.let(into::add)
        }
        parseConversationMap(payload)?.takeIf { it.isNotEmpty() }?.let(into::add)
    }

    private fun collectCandidates(
        node: JSONObject,
        depth: Int,
        into: MutableList<List<AgentTaskMessage>>,
        skipComposerSiblings: Boolean,
    ) {
        if (depth > MAX_DEPTH || into.size >= MAX_CANDIDATES) return
        MESSAGE_ARRAY_KEYS.forEach { key ->
            parseMessageArray(node.optJSONArray(key)).takeIf { it.isNotEmpty() }?.let(into::add)
            parseEncodedArray(node.opt(key)).takeIf { it.isNotEmpty() }?.let(into::add)
        }
        concatenatedHistory(node)?.let(into::add)
        parseConversationMap(node)?.takeIf { it.isNotEmpty() }?.let(into::add)
        syntheticComposerMessages(node)?.let(into::add)

        val keys = node.keys()
        while (keys.hasNext()) {
            if (into.size >= MAX_CANDIDATES) return
            val key = keys.next()
            node.optJSONObject(key)?.let { child ->
                collectCandidates(child, depth + 1, into, skipComposerSiblings)
            }
            node.optJSONArray(key)?.let { array ->
                if (key !in MESSAGE_ARRAY_KEYS && key !in SKIP_ARRAY_KEYS) {
                    parseMessageArray(array).takeIf { it.isNotEmpty() }?.let(into::add)
                }
                val walkChildren = when {
                    key == "composers" -> !skipComposerSiblings
                    key in SKIP_ARRAY_KEYS -> false
                    else -> true
                }
                if (walkChildren) {
                    for (index in 0 until array.length()) {
                        array.optJSONObject(index)?.let { child ->
                            collectCandidates(child, depth + 1, into, skipComposerSiblings)
                        }
                    }
                }
            }
        }
    }

    private fun concatenatedHistory(node: JSONObject): List<AgentTaskMessage>? {
        val history = parseMessageArray(node.optJSONArray("conversationHistory"))
        val followups = FOLLOWUP_ARRAY_KEYS.flatMap { key -> parseMessageArray(node.optJSONArray(key)) }
        if (history.isEmpty() || followups.isEmpty()) return null
        val seed = history.first()
        val followupStart = followups.first()
        if (followupStart.role == seed.role && followupStart.text == seed.text) return null
        return history + followups
    }

    private fun syntheticComposerMessages(node: JSONObject): List<AgentTaskMessage>? {
        val prompt = node.optJSONObject("prompt")?.nullableString("text")
            ?: node.nullableString("promptText")
            ?: node.nullableString("initialPrompt")
        val summary = node.nullableString("summary")
            ?: node.nullableString("resultText")
            ?: node.nullableString("assistantSummary")
        if (prompt.isNullOrBlank() && summary.isNullOrBlank()) return null
        return listOfNotNull(
            prompt?.let {
                AgentTaskMessage(
                    id = "prompt-seed",
                    role = AgentTaskMessageRole.User,
                    text = it.trim().take(MAX_MESSAGE_CHARS),
                )
            },
            summary?.let {
                AgentTaskMessage(
                    id = "summary-seed",
                    role = AgentTaskMessageRole.Assistant,
                    text = it.trim().take(MAX_MESSAGE_CHARS),
                )
            },
        ).takeIf { it.isNotEmpty() }
    }

    private fun parseEncodedArray(value: Any?): List<AgentTaskMessage> {
        val raw = value as? String ?: return emptyList()
        val trimmed = raw.trim()
        if (!trimmed.startsWith("[")) return emptyList()
        return parseMessageArray(runCatching { JSONArray(trimmed) }.getOrNull())
    }

    private fun parseMessageArray(array: JSONArray?): List<AgentTaskMessage> {
        if (array == null) return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                parseMessage(item, index)?.let { add(it); continue }
                addAll(parseTurn(item, index))
            }
        }
    }

    private fun parseTurn(item: JSONObject, index: Int): List<AgentTaskMessage> {
        val user = item.optJSONObject("user") ?: item.optJSONObject("human")
        val assistant = item.optJSONObject("assistant") ?: item.optJSONObject("ai")
        if (user == null && assistant == null) return emptyList()
        return listOfNotNull(
            user?.let { source ->
                if (!source.has("role") && !source.has("type")) source.put("role", "user")
                parseMessage(source, index * 2)
            },
            assistant?.let { source ->
                if (!source.has("role") && !source.has("type")) source.put("role", "assistant")
                parseMessage(source, index * 2 + 1)
            },
        )
    }

    private fun parseConversationMap(root: JSONObject): List<AgentTaskMessage>? {
        val map = root.optJSONObject("conversationMap") ?: return null
        val headers = root.optJSONArray("fullConversationHeadersOnly")
            ?: root.optJSONArray("conversationHeaders")
        val orderedIds = buildList {
            if (headers != null) {
                for (index in 0 until headers.length()) {
                    val header = headers.optJSONObject(index) ?: continue
                    header.optString("bubbleId").ifBlank { header.optString("id") }
                        .takeIf { it.isNotBlank() }
                        ?.let { add(it) }
                }
            }
        }
        val keys = orderedIds.ifEmpty {
            buildList {
                val iterator = map.keys()
                while (iterator.hasNext()) add(iterator.next())
            }
        }
        return keys.mapIndexedNotNull { index, id ->
            val item = map.optJSONObject(id) ?: return@mapIndexedNotNull null
            if (!item.has("bubbleId")) item.put("bubbleId", id)
            parseMessage(item, index)
        }
    }

    private fun parseMessage(item: JSONObject, index: Int): AgentTaskMessage? {
        if (!looksLikeMessage(item)) return null
        val text = extractText(item)?.trim()?.take(MAX_MESSAGE_CHARS) ?: return null
        if (text.isBlank()) return null
        val rawId = item.optString("id").ifBlank { item.optString("bubbleId") }
        val id = if (rawId.isBlank()) "msg-$index" else "$rawId#$index"
        return AgentTaskMessage(
            id = id,
            role = parseRole(item),
            text = text,
            createdAtMs = item.nullableLong("createdAtMs")
                ?: item.nullableLong("timestamp")
                ?: item.nullableLong("createdAt"),
        )
    }

    private fun looksLikeMessage(item: JSONObject): Boolean {
        if (item.has("userId") && item.has("displayName") && !item.has("text") && !item.has("type")) {
            return false
        }
        extractText(item)?.let { return true }
        val type = item.opt("type")
        if (type is Number && type.toInt() in 1..2) return true
        val typeName = item.optString("type")
        if (typeName.isNotBlank() && MESSAGE_TYPE_HINTS.any { typeName.contains(it, ignoreCase = true) }) {
            return true
        }
        val role = item.optString("role").ifBlank { item.optString("sender") }
        return role.equals("user", true) ||
            role.equals("assistant", true) ||
            role.equals("human", true) ||
            role.equals("ai", true)
    }

    private fun parseRole(item: JSONObject): AgentTaskMessageRole {
        val type = item.opt("type")
        if (type is Number) {
            return when (type.toInt()) {
                1 -> AgentTaskMessageRole.User
                2 -> AgentTaskMessageRole.Assistant
                else -> AgentTaskMessageRole.Unknown
            }
        }
        val tokens = listOf(
            item.optString("type"),
            item.optString("role"),
            item.optString("sender"),
            item.optString("author"),
        ).joinToString(" ").lowercase(Locale.US)
        return when {
            tokens.contains("user") || tokens.contains("human") -> AgentTaskMessageRole.User
            tokens.contains("assistant") || tokens.contains("ai") ||
                tokens.contains("bot") || tokens.contains("model") -> AgentTaskMessageRole.Assistant
            else -> AgentTaskMessageRole.Unknown
        }
    }

    private fun extractText(item: JSONObject): String? {
        item.nullableString("text")?.let { return it }
        item.nullableString("message")?.let { return it }
        item.nullableString("promptText")?.let { return it }
        item.nullableString("responseText")?.let { return it }
        item.optJSONObject("prompt")?.nullableString("text")?.let { return it }
        extractFromContent(item.opt("content"))?.let { return it }
        extractFromContent(item.opt("parts"))?.let { return it }
        val rich = item.opt("richText")
        when (rich) {
            is String -> {
                lexicalPlainText(rich)?.let { return it }
                htmlOrPlain(rich)?.let { return it }
            }
            is JSONObject -> lexicalPlainText(rich.toString())?.let { return it }
        }
        item.nullableString("assistantText")?.let { return it }
        item.nullableString("response")?.let { return it }
        item.nullableString("markdown")?.let { return it }
        item.nullableString("output")?.let { return it }
        item.nullableString("finalText")?.let { return it }
        val thinking = item.opt("thinking")
        when (thinking) {
            is String -> thinking.takeIf { it.isNotBlank() }?.let { return it }
            is JSONObject -> thinking.nullableString("text")?.let { return it }
        }
        return null
    }

    private fun extractFromContent(content: Any?): String? = when (content) {
        is String -> content.takeIf { it.isNotBlank() }
        is JSONObject -> extractText(content)
        is JSONArray -> {
            val joined = buildString {
                for (index in 0 until content.length()) {
                    val part = when (val value = content.opt(index)) {
                        is String -> value
                        is JSONObject -> value.nullableString("text")
                            ?: value.nullableString("content")
                            ?: value.nullableString("output_text")
                        else -> null
                    } ?: continue
                    if (isNotEmpty()) append('\n')
                    append(part)
                }
            }
            joined.takeIf { it.isNotBlank() }
        }
        else -> null
    }

    private fun lexicalPlainText(raw: String): String? {
        val root = runCatching { JSONObject(raw) }.getOrNull() ?: return null
        val chunks = mutableListOf<String>()
        collectLexicalText(root.optJSONObject("root") ?: root, chunks)
        return chunks.joinToString("").trim().takeIf { it.isNotBlank() }
    }

    private fun htmlOrPlain(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) return null
        val stripped = HTML_TAG.replace(trimmed, " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
        return WHITESPACE.replace(stripped, " ").trim().takeIf { it.isNotBlank() }
    }

    private fun collectLexicalText(node: JSONObject, into: MutableList<String>) {
        val type = node.optString("type")
        val text = node.optString("text")
        if (type == "text" && text.isNotBlank()) into.add(text)
        val children = node.optJSONArray("children") ?: return
        for (index in 0 until children.length()) {
            val child = children.optJSONObject(index) ?: continue
            if (child.optString("type") == "paragraph" && into.isNotEmpty()) {
                into.add("\n")
            }
            collectLexicalText(child, into)
        }
    }

    /**
     * 连续的助手气泡（思考 / 工具步骤后的正文）合并为一条，避免详情页过碎。
     * 用户消息保持独立。
     */
    private fun mergeConsecutiveAssistants(messages: List<AgentTaskMessage>): List<AgentTaskMessage> {
        if (messages.size <= 1) return messages
        val merged = mutableListOf<AgentTaskMessage>()
        for (message in messages) {
            val last = merged.lastOrNull()
            if (
                message.role == AgentTaskMessageRole.Assistant &&
                last?.role == AgentTaskMessageRole.Assistant &&
                !last.pending &&
                !message.pending
            ) {
                merged[merged.lastIndex] = last.copy(
                    text = last.text.trimEnd() + "\n\n" + message.text.trimStart(),
                    createdAtMs = message.createdAtMs ?: last.createdAtMs,
                )
            } else {
                merged.add(message)
            }
        }
        return merged
    }

    private fun JSONObject.nullableString(key: String): String? {
        if (!has(key) || isNull(key)) return null
        return optString(key).takeIf { it.isNotBlank() }
    }

    private fun JSONObject.nullableLong(key: String): Long? {
        if (!has(key) || isNull(key)) return null
        return optLong(key, Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }
    }

    private val MESSAGE_ARRAY_KEYS = setOf(
        "messages",
        "conversation",
        "conversationHistory",
        "bubbles",
        "turns",
        "followupMessages",
        "followUps",
        "followups",
        "composerMessages",
        "conversationTurns",
    )
    private val FOLLOWUP_ARRAY_KEYS = setOf(
        "followupMessages",
        "followUps",
        "followups",
    )
    private val SKIP_ARRAY_KEYS = setOf(
        "participants",
        "participantUserIds",
        "repoUrls",
        "composers",
        "diffs",
        "files",
        "changes",
        "fileDiffs",
        "collaborators",
        "events",
        "commits",
    )
    private val MESSAGE_TYPE_HINTS = listOf(
        "message",
        "human",
        "assistant",
        "user",
        "ai",
        "bubble",
    )
    private val HTML_TAG = Regex("<[^>]+>")
    private val WHITESPACE = Regex("\\s+")
    private const val MAX_DEPTH = 6
    private const val MAX_MESSAGES = 200
    private const val MAX_CANDIDATES = 40
    private const val MAX_MESSAGE_CHARS = 20_000
}
