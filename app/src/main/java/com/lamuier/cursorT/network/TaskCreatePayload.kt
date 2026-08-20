package com.lamuier.cursorT.network

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** 组装网页版「从快照启动后台智能体」与 Cloud Agents API 的创建请求体。 */
object TaskCreatePayload {
    fun newBcId(): String = "bc-${UUID.randomUUID()}"

    fun webSnapshot(
        bcId: String,
        prompt: String,
        snapshotRepo: String,
        httpsRepo: String,
        ref: String?,
        modelName: String?,
        autoCreatePr: Boolean,
    ): JSONObject {
        val branch = ref?.trim().orEmpty().ifBlank { "main" }
        val model = JSONObject()
            .put("maxMode", false)
        modelName?.trim()?.takeIf { it.isNotBlank() }?.let { model.put("modelName", it) }
        return JSONObject()
            .put("snapshotNameOrId", snapshotRepo)
            .put(
                "devcontainerStartingPoint",
                JSONObject().put("url", httpsRepo).put("ref", branch),
            )
            .put("modelDetails", model)
            .put("repositoryInfo", JSONObject())
            .put("snapshotWorkspaceRootPath", "/workspace")
            .put("autoBranch", true)
            .put("autoCreatePr", autoCreatePr)
            .put("returnImmediately", true)
            .put("repoUrl", snapshotRepo)
            .put("conversationHistory", JSONArray().put(humanMessage(prompt)))
            .put("source", "BACKGROUND_COMPOSER_SOURCE_WEBSITE")
            .put("bcId", bcId)
            .put("addInitialMessageToResponses", true)
    }

    fun cloudAgent(
        prompt: String,
        httpsRepo: String,
        ref: String?,
        autoCreatePr: Boolean,
    ): JSONObject {
        val source = JSONObject().put("repository", httpsRepo)
        ref?.trim()?.takeIf { it.isNotBlank() }?.let { source.put("ref", it) }
        return JSONObject()
            .put("prompt", JSONObject().put("text", prompt))
            .put("source", source)
            .put("target", JSONObject().put("autoCreatePr", autoCreatePr))
    }

    fun cloudFollowup(text: String): JSONObject =
        JSONObject().put("prompt", JSONObject().put("text", text))

    fun createdBcId(payload: JSONObject, requestedId: String): String {
        val nested = payload.optJSONObject("composer")
        val fromResponse = payload.optString("bcId")
            .ifBlank { payload.optString("id") }
            .ifBlank { nested?.optString("bcId").orEmpty() }
            .ifBlank { nested?.optString("id").orEmpty() }
        return fromResponse.takeIf { it.isNotBlank() } ?: requestedId
    }

    private fun humanMessage(prompt: String): JSONObject {
        val lexical = JSONObject()
            .put(
                "root",
                JSONObject()
                    .put(
                        "children",
                        JSONArray().put(
                            JSONObject()
                                .put(
                                    "children",
                                    JSONArray().put(
                                        JSONObject()
                                            .put("detail", 0)
                                            .put("format", 0)
                                            .put("mode", "normal")
                                            .put("style", "")
                                            .put("text", prompt)
                                            .put("type", "text")
                                            .put("version", 1),
                                    ),
                                )
                                .put("direction", "ltr")
                                .put("format", "")
                                .put("indent", 0)
                                .put("type", "paragraph")
                                .put("version", 1)
                                .put("textFormat", 0)
                                .put("textStyle", ""),
                        ),
                    )
                    .put("direction", "ltr")
                    .put("format", "")
                    .put("indent", 0)
                    .put("type", "root")
                    .put("version", 1),
            )
        return JSONObject()
            .put("text", prompt)
            .put("type", "MESSAGE_TYPE_HUMAN")
            .put("richText", lexical.toString())
    }
}
