package com.lamuier.cursorusage.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.lamuier.cursorusage.model.CursorAccount
import com.lamuier.cursorusage.util.DeviceCredentialGate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun AccountManagementSheet(
    account: CursorAccount?,
    busy: Boolean,
    error: String?,
    onClearError: () -> Unit,
    onDismiss: () -> Unit,
    onAdd: (String, String, () -> Unit) -> Unit,
    onUpdate: (Int, String?, String?, () -> Unit) -> Unit,
    onDeleteRequest: (CursorAccount) -> Unit,
    onRevealSavedToken: (Int) -> String,
) {
    var alias by remember(account?.id) { mutableStateOf(account?.alias.orEmpty()) }
    var accessToken by remember(account?.id) { mutableStateOf("") }
    var tokenVisible by remember(account?.id) { mutableStateOf(false) }
    var aliasError by remember(account?.id) { mutableStateOf<String?>(null) }
    var tokenError by remember(account?.id) { mutableStateOf<String?>(null) }
    var formError by remember(account?.id) { mutableStateOf<String?>(null) }
    var showingTokenHelp by remember(account?.id) { mutableStateOf(false) }
    var revealedToken by remember(account?.id) { mutableStateOf<String?>(null) }
    var revealedTokenVisible by remember(account?.id) { mutableStateOf(false) }
    var revealError by remember(account?.id) { mutableStateOf<String?>(null) }
    var revealBusy by remember(account?.id) { mutableStateOf(false) }
    var tokenCopied by remember(account?.id) { mutableStateOf(false) }

    val activity = LocalContext.current.findFragmentActivity()
    val clipboard = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val aliasBringIntoView = remember { BringIntoViewRequester() }
    val tokenBringIntoView = remember { BringIntoViewRequester() }
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)

    fun clearRevealedToken() {
        revealedToken = null
        revealedTokenVisible = false
        tokenCopied = false
        revealError = null
        revealBusy = false
    }

    fun requestRevealSavedToken(target: CursorAccount) {
        val host = activity
        if (host == null) {
            revealError = "当前界面无法发起设备验证"
            return
        }
        revealError = null
        revealBusy = true
        DeviceCredentialGate.authenticate(
            activity = host,
            title = "验证以查看 Token",
            subtitle = "使用指纹、面部或锁屏密码/图案确认是你本人",
            onSuccess = {
                try {
                    revealedToken = onRevealSavedToken(target.id)
                    revealedTokenVisible = false
                    tokenCopied = false
                    revealError = null
                } catch (error: Exception) {
                    revealedToken = null
                    revealError = error.message?.takeIf { it.isNotBlank() }
                        ?: "无法读取已保存的 Access Token"
                } finally {
                    revealBusy = false
                }
            },
            onError = { message ->
                revealBusy = false
                revealError = message
            },
            onCanceled = {
                revealBusy = false
            },
        )
    }

    LaunchedEffect(imeBottom) {
        if (imeBottom > 0) {
            delay(60)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    fun submit() {
        onClearError()
        val trimmedAlias = alias.trim()
        val trimmedToken = accessToken.trim()
        aliasError = when {
            trimmedAlias.isEmpty() -> "请输入账号别名"
            trimmedAlias.length > 64 -> "别名不能超过 64 个字符"
            else -> null
        }
        tokenError = when {
            account == null && trimmedToken.isEmpty() -> "请输入 Cursor Access Token"
            else -> null
        }
        formError = null
        if (aliasError != null || tokenError != null) return

        if (account == null) {
            onAdd(trimmedAlias, trimmedToken) {
                accessToken = ""
                tokenVisible = false
                onDismiss()
            }
            return
        }

        val nextAlias = trimmedAlias.takeIf { it != account.alias }
        val nextToken = trimmedToken.ifBlank { null }
        if (nextAlias == null && nextToken == null) {
            formError = "没有需要保存的更改"
            return
        }
        onUpdate(account.id, nextAlias, nextToken) {
            accessToken = ""
            tokenVisible = false
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            when {
                showingTokenHelp -> showingTokenHelp = false
                busy -> Unit
                else -> {
                    clearRevealedToken()
                    onDismiss()
                }
            }
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
    ) {
        if (showingTokenHelp) {
            TokenHelpContent(
                onDismiss = { showingTokenHelp = false },
                actionLabel = "返回账号设置",
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = if (account == null) "添加 Cursor 账号" else "编辑 Cursor 账号",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "此设备仅保存一个 Cursor 账号。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        enabled = !busy,
                        onClick = {
                            clearRevealedToken()
                            onDismiss()
                        },
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "关闭账号设置")
                    }
                }

                AccountHeroCard(account = account)

                if (account != null) {
                    SavedTokenRevealSection(
                        revealedToken = revealedToken,
                        revealedTokenVisible = revealedTokenVisible,
                        revealBusy = revealBusy || busy,
                        revealError = revealError,
                        tokenCopied = tokenCopied,
                        onReveal = { requestRevealSavedToken(account) },
                        onHide = { clearRevealedToken() },
                        onToggleVisibility = { revealedTokenVisible = !revealedTokenVisible },
                        onCopy = {
                            val token = revealedToken ?: return@SavedTokenRevealSection
                            clipboard.setText(AnnotatedString(token))
                            tokenCopied = true
                        },
                    )
                }

                TextButton(
                    enabled = !busy,
                    onClick = { showingTokenHelp = true },
                    modifier = Modifier.align(Alignment.Start),
                ) {
                    Text("如何获取 Access Token？")
                }

                error?.let { SheetError(it, onClearError) }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        OutlinedTextField(
                            value = alias,
                            onValueChange = {
                                alias = it
                                onClearError()
                                aliasError = null
                                formError = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewRequester(aliasBringIntoView)
                                .onFocusChanged { focus ->
                                    if (focus.isFocused) {
                                        scope.launch {
                                            delay(120)
                                            aliasBringIntoView.bringIntoView()
                                        }
                                    }
                                },
                            label = { Text("账号别名") },
                            supportingText = {
                                Text(aliasError ?: "例如：个人账号；最多 64 个字符")
                            },
                            isError = aliasError != null,
                            enabled = !busy,
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        )

                        OutlinedTextField(
                            value = accessToken,
                            onValueChange = {
                                accessToken = it
                                onClearError()
                                tokenError = null
                                formError = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .bringIntoViewRequester(tokenBringIntoView)
                                .onFocusChanged { focus ->
                                    if (focus.isFocused) {
                                        scope.launch {
                                            delay(120)
                                            tokenBringIntoView.bringIntoView()
                                            scrollState.animateScrollTo(scrollState.maxValue)
                                        }
                                    }
                                },
                            label = {
                                Text(
                                    if (account == null) {
                                        "Cursor Access Token"
                                    } else {
                                        "新 Access Token（可选）"
                                    },
                                )
                            },
                            supportingText = {
                                Text(
                                    tokenError ?: if (account == null) {
                                        "仅加密保存在本机，不会发送给第三方服务"
                                    } else {
                                        "留空会保留当前 Token"
                                    },
                                )
                            },
                            isError = tokenError != null,
                            enabled = !busy,
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            visualTransformation = if (tokenVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { if (!busy) submit() },
                            ),
                            trailingIcon = {
                                TextButton(
                                    enabled = !busy,
                                    onClick = { tokenVisible = !tokenVisible },
                                ) {
                                    Text(if (tokenVisible) "隐藏" else "显示")
                                }
                            },
                        )

                        formError?.let {
                            Text(
                                it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        Button(
                            onClick = ::submit,
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            if (busy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text(if (account == null) "保存账号" else "保存更改")
                            }
                        }

                        if (account != null) {
                            OutlinedButton(
                                onClick = {
                                    onDismiss()
                                    onDeleteRequest(account)
                                },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                            ) {
                                Text("删除此设备上的账号")
                            }
                        }
                    }
                }

                Text(
                    "Token 只会发送到 Cursor 官方 HTTPS 接口。更换账号前，请先删除当前账号。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Extra room so the focused field clears the IME when scrolled to the end.
                Box(Modifier.windowInsetsBottomHeight(WindowInsets.ime))
            }
        }
    }
}

@Composable
private fun SavedTokenRevealSection(
    revealedToken: String?,
    revealedTokenVisible: Boolean,
    revealBusy: Boolean,
    revealError: String?,
    tokenCopied: Boolean,
    onReveal: () -> Unit,
    onHide: () -> Unit,
    onToggleVisibility: () -> Unit,
    onCopy: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "已保存的 Access Token",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "查看前需通过指纹、面部或锁屏密码/图案验证。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (revealedToken == null) {
                FilledTonalButton(
                    onClick = onReveal,
                    enabled = !revealBusy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (revealBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            Icons.Outlined.Fingerprint,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(
                        if (revealBusy) "验证中…" else "验证并查看 Token",
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            } else {
                SelectionContainer {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        Text(
                            text = if (revealedTokenVisible) {
                                revealedToken
                            } else {
                                "•".repeat(revealedToken.length.coerceIn(12, 48))
                            },
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = onToggleVisibility) {
                        Icon(
                            imageVector = if (revealedTokenVisible) {
                                Icons.Outlined.VisibilityOff
                            } else {
                                Icons.Outlined.Visibility
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            if (revealedTokenVisible) "隐藏" else "显示",
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                    TextButton(onClick = onCopy) {
                        Text(if (tokenCopied) "已复制" else "复制")
                    }
                    TextButton(onClick = onHide) {
                        Text("收起")
                    }
                }
            }

            revealError?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun AccountHeroCard(account: CursorAccount?) {
    val expired = account?.tokenExpired == true
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (expired) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f))
        } else {
            null
        },
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = CircleShape,
                color = if (expired) {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                contentColor = if (expired) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                },
                border = BorderStroke(
                    width = 2.dp,
                    color = if (expired) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    },
                ),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = (account?.alias?.firstOrNull()?.uppercaseChar() ?: 'C').toString(),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    account?.alias ?: "尚未添加账号",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    when {
                        account == null -> "保存后即可在首页查看 Cursor 用量"
                        expired -> "当前 Token 已过期，请在下方填写新 Token"
                        else -> "账号和 Token 使用 Android Keystore 加密保存在本机"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (expired) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
private fun SheetError(message: String, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 6.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    }
}

internal fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is FragmentActivity) return current
        current = current.baseContext
    }
    return current as? FragmentActivity
}
