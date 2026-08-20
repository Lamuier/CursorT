package com.lamuier.cursorT.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lamuier.cursorT.model.AgentTask
import com.lamuier.cursorT.model.AgentTaskConversation
import com.lamuier.cursorT.model.AgentTaskMessage
import com.lamuier.cursorT.model.AgentTaskMessageRole
import com.lamuier.cursorT.util.AgentTaskPresentation

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun TaskDetailSheet(
    task: AgentTask,
    conversation: AgentTaskConversation?,
    loading: Boolean,
    refreshing: Boolean,
    sending: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onRefresh: () -> Unit,
    onSend: (String) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draft by remember(task.id) { mutableStateOf("") }
    val canSend = AgentTaskPresentation.canSendFollowup(task.status)
    val sendBlockedReason = AgentTaskPresentation.sendDisabledReason(task.status)
    val messages = conversation?.messages.orEmpty()
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, messages.lastOrNull()?.id) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.94f)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        task.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        AgentTaskPresentation.statusLabel(task.status),
                        style = MaterialTheme.typography.labelSmall,
                        color = taskStatusColor(task.status),
                    )
                }
                IconButton(
                    onClick = onRefresh,
                    enabled = !loading && !refreshing && !sending,
                    modifier = Modifier.semantics { contentDescription = "刷新对话" },
                ) {
                    if (refreshing || loading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                    }
                }
            }

            FlowRow(
                modifier = Modifier.padding(top = 8.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TaskChip(AgentTaskPresentation.statusLabel(task.status), taskStatusColor(task.status))
                task.prStatus?.let { TaskChip(AgentTaskPresentation.prStatusLabel(it), prStatusColor(it)) }
                AgentTaskPresentation.displayModel(task.modelName, task.maxMode)?.let { model ->
                    TaskChip(model, MaterialTheme.colorScheme.onSurfaceVariant)
                }
                task.branchName?.trim()?.takeIf { it.isNotBlank() }?.let { branch ->
                    TaskChip(branch, MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            when {
                conversation == null && loading -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                        Text(
                            "正在加载对话…",
                            modifier = Modifier.padding(top = 12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        if (error != null) {
                            item {
                                Text(
                                    error,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        if (messages.isEmpty() && !loading) {
                            item {
                                EmptyConversationHint(task, uriHandler)
                            }
                        } else {
                            itemsIndexed(messages, key = { index, message -> "${index}-${message.id}" }) { _, message ->
                                ConversationBubble(message)
                            }
                        }
                    }
                }
            }

            val webUrl = AgentTaskPresentation.agentConversationUrl(task.id)
            if (webUrl != null) {
                TextButton(
                    onClick = { openSafeCursorUrl(uriHandler, webUrl) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.size(6.dp))
                    Text("在网页打开此任务")
                }
            }

            if (!canSend) {
                Text(
                    sendBlockedReason ?: "当前无法发送消息",
                    modifier = Modifier.padding(bottom = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { if (it.length <= MAX_DRAFT_CHARS) draft = it },
                    modifier = Modifier.weight(1f),
                    enabled = canSend && !sending,
                    placeholder = { Text(if (canSend) "发送跟进消息…" else "无法发送") },
                    minLines = 1,
                    maxLines = 5,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send,
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            val text = draft.trim()
                            if (text.isNotEmpty() && canSend && !sending) {
                                onSend(text)
                                draft = ""
                            }
                        },
                    ),
                )
                FilledIconButton(
                    onClick = {
                        val text = draft.trim()
                        if (text.isNotEmpty()) {
                            onSend(text)
                            draft = ""
                        }
                    },
                    enabled = canSend && !sending && draft.trim().isNotEmpty(),
                    modifier = Modifier.semantics {
                        role = Role.Button
                        contentDescription = "发送"
                    },
                ) {
                    if (sending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationBubble(message: AgentTaskMessage) {
    val fromUser = message.role == AgentTaskMessageRole.User
    val bubbleColor = when {
        fromUser -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = when {
        fromUser -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (fromUser) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (fromUser) 16.dp else 4.dp,
                bottomEnd = if (fromUser) 4.dp else 16.dp,
            ),
            color = bubbleColor,
            contentColor = contentColor,
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    if (fromUser) "你" else "智能体",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor.copy(alpha = 0.72f),
                )
                SelectionContainer {
                    Text(
                        message.text,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (message.pending) {
                    Text(
                        "发送中…",
                        style = MaterialTheme.typography.labelSmall,
                        color = contentColor.copy(alpha = 0.64f),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyConversationHint(task: AgentTask, uriHandler: UriHandler) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Outlined.SmartToy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "还没有解析到对话内容。接口字段可能变化，可在网页查看完整记录。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AgentTaskPresentation.agentConversationUrl(task.id)?.let { url ->
                TextButton(onClick = { openSafeCursorUrl(uriHandler, url) }) {
                    Text("在网页打开")
                }
            }
        }
    }
}

private fun openSafeCursorUrl(uriHandler: UriHandler, url: String) {
    if (AgentTaskPresentation.isSafeCursorUrl(url)) {
        runCatching { uriHandler.openUri(url) }
    }
}

private const val MAX_DRAFT_CHARS = 8_000
