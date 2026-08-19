package com.lamuier.cursorT.network

import com.lamuier.cursorT.model.AgentTask
import com.lamuier.cursorT.model.AgentTaskConversation
import com.lamuier.cursorT.model.AgentTaskMessage
import com.lamuier.cursorT.model.AgentTaskMessageRole
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

/**
 * 解析云端任务对话。网页版与 Connect RPC 字段不完全稳定，
 * 因此按多种常见形状防御式读取，无法识别的条目直接跳过。
 *
 * 详情接口常常同时带有创建时的短 `conversationHistory` 和完整 `bubbles` /
 * `followupMessages`。必须收集全部候选再挑最完整的一份，不能拿到第一个非空数组就停。
 */
object TaskConversationJsonParser {
    fun parse(
        payload: JSONObject,
        fallbackTask: AgentTask,
        accountId: Int,
        fetchedAt: String,
        fromCache: Boolean = false,
    ): AgentTaskConversation {
        val parsedTask = parseTask(payload)?.takeIf { it.id == fallbackTask.id }
        return AgentTaskConversation(
            accountId = accountId,
            task = parsedTask ?: fallbackTask,
            messages = parseMessages(payload).take(MAX_MESSAGES),
            fetchedAt = fetchedAt,
            fromCache = fromCache,
        )
    }

    /**
     * 详情接口优先提供任务元数据；其它对话接口可能更完整。
     * 任务字段始终取自 primary，消息取候选中最完整的一份。
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
        val best = candidates.maxBy(::score)
        return parsedPrimary.copy(messages = best.messages)
    }

    fun parseTask(payload: JSONObject): AgentTask? {
        findComposerObjects(payload).forEach { candidate ->
            TasksJsonParser.parseComposerItem(candidate)?.let { return it }
        }
        return TasksJsonParser.parseComposerItem(payload)
    }

    fun parseMessages(payload: JSONObject): List<AgentTaskMessage> {
        val candidates = mutableListOf<List<AgentTaskMessage>>()
        collectCandidates(payload, depth = 0, into = candidates)
        val best = candidates.maxByOrNull(::score).orEmpty()
        return mergeConsecutiveAssistants(best).take(MAX_MESSAGES)
    }

    internal fun score(conversation: AgentTaskConversation): Int = score(conversation.messages)

    private fun score(messages: List<AgentTaskMessage>): Int {
        if (messages.isEmpty()) return 0
        val hasUser = messages.any { it.role == AgentTaskMessageRole.User }
        val hasAssistant = messages.any { it.role == AgentTaskMessageRole.Assistant }
        var value = messages.size * 10
        if (hasUser && hasAssistant) value += 1_000
        else if (hasAssistant) value += 200
        return value
    }

    private fun findComposerObjects(root: JSONObject): List<JSONObject> = buildList {
        root.optJSONArray("composers")?.let { array ->
            for (index in 0 until array.length()) {
                array.optJSONObject(index)?.let { add(it) }
            }
        }
        root.optJSONObject("composer")?.let { add(it) }
        add(root)
    }

    private fun collectCandidates(
        node: JSONObject,
        depth: Int,
        into: MutableList<List<AgentTaskMessage>>,
    ) {
        if (depth > MAX_DEPTH || into.size >= MAX_CANDIDATES) return
        MESSAGE_ARRAY_KEYS.forEach { key ->
            parseMessageArray(node.optJSONArray(key)).takeIf { it.isNotEmpty() }?.let(into::add)
        }
        concatenatedHistory(node)?.let(into::add)
        parseConversationMap(node)?.takeIf { it.isNotEmpty() }?.let(into::add)

        val keys = node.keys()
        while (keys.hasNext()) {
            if (into.size >= MAX_CANDIDATES) return
            val key = keys.next()
            node.optJSONObject(key)?.let { collectCandidates(it, depth + 1, into) }
            node.optJSONArray(key)?.let { array ->
                if (key !in MESSAGE_ARRAY_KEYS && key !in SKIP_ARRAY_KEYS) {
                    parseMessageArray(array).takeIf { it.isNotEmpty() }?.let(into::add)
                }
                if (key !in SKIP_ARRAY_KEYS || key == "composers") {
                    for (index in 0 until array.length()) {
                        array.optJSONObject(index)?.let { child ->
                            collectCandidates(child, depth + 1, into)
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
        val id = item.optString("id").ifBlank { item.optString("bubbleId") }
            .ifBlank { "msg-$index" }
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
        extractFromContent(item.opt("content"))?.let { return it }
        extractFromContent(item.opt("parts"))?.let { return it }
        val rich = item.opt("richText")
        when (rich) {
            is String -> lexicalPlainText(rich)?.let { return it }
            is JSONObject -> lexicalPlainText(rich.toString())?.let { return it }
        }
        item.nullableString("assistantText")?.let { return it }
        item.nullableString("response")?.let { return it }
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
                    val part = content.optJSONObject(index) ?: continue
                    val piece = part.nullableString("text")
                        ?: part.nullableString("content")
                        ?: part.nullableString("output_text")
                        ?: continue
                    if (isNotEmpty()) append('\n')
                    append(piece)
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
    private const val MAX_DEPTH = 6
    private const val MAX_MESSAGES = 200
    private const val MAX_CANDIDATES = 40
    private const val MAX_MESSAGE_CHARS = 20_000
}
