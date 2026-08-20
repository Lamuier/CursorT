package com.lamuier.cursorT.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddTask
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.lamuier.cursorT.util.AgentTaskPresentation

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun CreateTaskSheet(
    suggestedRepos: List<String>,
    creating: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onClearError: () -> Unit,
    onCreate: (prompt: String, repository: String, ref: String?, autoCreatePr: Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var prompt by remember { mutableStateOf("") }
    var repository by remember { mutableStateOf(suggestedRepos.firstOrNull().orEmpty()) }
    var ref by remember { mutableStateOf("") }
    var autoCreatePr by remember { mutableStateOf(false) }
    val repoValid = AgentTaskPresentation.normalizeRepositoryUrl(repository) != null
    val canSubmit = prompt.trim().isNotEmpty() && repoValid && !creating

    ModalBottomSheet(
        onDismissRequest = { if (!creating) onDismiss() },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    Icons.Outlined.AddTask,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    "新建云端任务",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                "用当前账号在 Cursor 云端启动一个后台智能体。需要填写任务说明和已连接的 Git 仓库。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = prompt,
                onValueChange = {
                    if (it.length <= MAX_PROMPT_CHARS) {
                        prompt = it
                        if (error != null) onClearError()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !creating,
                label = { Text("任务说明") },
                placeholder = { Text("例如：给 README 加上安装说明") },
                minLines = 3,
                maxLines = 8,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next,
                ),
            )
            OutlinedTextField(
                value = repository,
                onValueChange = {
                    if (it.length <= MAX_REPO_CHARS) {
                        repository = it
                        if (error != null) onClearError()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !creating,
                label = { Text("仓库") },
                placeholder = { Text("https://github.com/owner/repo") },
                isError = repository.isNotBlank() && !repoValid,
                supportingText = {
                    Text(
                        if (repository.isNotBlank() && !repoValid) {
                            "仅支持 github.com 或 gitlab.com 的 https 地址"
                        } else {
                            "可从下方近期任务仓库中点选"
                        },
                    )
                },
                singleLine = true,
            )
            if (suggestedRepos.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    suggestedRepos.take(8).forEach { repo ->
                        val selected = AgentTaskPresentation.normalizeRepositoryUrl(repository) == repo
                        FilterChip(
                            selected = selected,
                            onClick = {
                                repository = repo
                                if (error != null) onClearError()
                            },
                            enabled = !creating,
                            label = { Text(repo.removePrefix("https://")) },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = ref,
                onValueChange = { if (it.length <= MAX_REF_CHARS) ref = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = !creating,
                label = { Text("基础分支（可选）") },
                placeholder = { Text("留空则使用默认分支") },
                singleLine = true,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        role = Role.Switch
                        contentDescription = "完成后创建 Pull Request"
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("完成后创建 PR", fontWeight = FontWeight.Medium)
                    Text(
                        "智能体结束时尝试打开 Pull Request",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = autoCreatePr,
                    onCheckedChange = { autoCreatePr = it },
                    enabled = !creating,
                )
            }
            if (error != null) {
                Text(
                    error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    onCreate(
                        prompt.trim(),
                        repository.trim(),
                        ref.trim().takeIf { it.isNotBlank() },
                        autoCreatePr,
                    )
                },
                enabled = canSubmit,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (creating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("创建任务")
                }
            }
        }
    }
}

private const val MAX_PROMPT_CHARS = 8_000
private const val MAX_REPO_CHARS = 400
private const val MAX_REF_CHARS = 200
