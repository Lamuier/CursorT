package com.lamuier.cursorusage.ui

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val CodeSurface = Color(0xFF0F1419)
private val CodeOnSurface = Color(0xFFD7E0EA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TokenHelpSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        TokenHelpContent(onDismiss = onDismiss, actionLabel = "我知道了")
    }
}

@Composable
internal fun TokenHelpContent(onDismiss: () -> Unit, actionLabel: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "如何获取 Access Token",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "以下步骤只在已登录 Cursor 的 Windows 电脑上执行。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "关闭帮助")
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)) {
                HelpTimelineStep(
                    number = 1,
                    title = "确认 Cursor 已登录",
                    body = "打开 Windows 版 Cursor，确认右上角账号已登录。读取数据库前不需要退出账号。",
                    isLast = false,
                )
                HelpTimelineStep(
                    number = 2,
                    title = "找到本地数据库",
                    body = "数据库路径：%APPDATA%\\Cursor\\User\\globalStorage\\state.vscdb",
                    isLast = false,
                )
                HelpTimelineStep(
                    number = 3,
                    title = "读取指定键值",
                    body = "Token 对应的 key 是 cursorAuth/accessToken。建议直接复制到剪贴板，不要重定向到文件。",
                    isLast = true,
                )
            }
        }

        HelpCommand(
            title = "PowerShell（已安装 sqlite3）",
            description = "以只读方式查询并直接复制到 Windows 剪贴板：",
            command = POWERSHELL_COMMAND,
        )

        HelpCommand(
            title = "Python（在 PowerShell 运行）",
            description = "无需 sqlite3 命令行；Python 标准库只读数据库，并通过 clip.exe 直接复制：",
            command = PYTHON_COMMAND,
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.errorContainer,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    "保护你的 Token",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Token 等同于登录凭据。不要发送到聊天、工单、邮件或代码仓库；粘贴到本应用后，及时清空电脑和手机剪贴板。",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                )
                SelectionContainer {
                    Text(
                        "PowerShell 清空剪贴板：Set-Clipboard -Value \"\"",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(actionLabel)
        }
    }
}

@Composable
private fun HelpTimelineStep(
    number: Int,
    title: String,
    body: String,
    isLast: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(28.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        number.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 4.dp)
                        .width(2.dp)
                        .height(36.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 8.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HelpCommand(title: String, description: String, command: String) {
    val context = LocalContext.current
    var copied by remember(command) { mutableStateOf(false) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                )
                FilledTonalButton(
                    onClick = {
                        context.getSystemService(ClipboardManager::class.java)
                            .setPrimaryClip(ClipData.newPlainText("Cursor Token 获取命令", command))
                        copied = true
                    },
                ) {
                    Text(if (copied) "已复制" else "复制命令")
                }
            }
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            SelectionContainer {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = CodeSurface,
                    contentColor = CodeOnSurface,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        command,
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = CodeOnSurface,
                    )
                }
            }
        }
    }
}

private val POWERSHELL_COMMAND = """
sqlite3 -readonly "${'$'}env:APPDATA\Cursor\User\globalStorage\state.vscdb" "SELECT value FROM ItemTable WHERE key = 'cursorAuth/accessToken';" | Set-Clipboard
""".trimIndent()

private val PYTHON_COMMAND = """
python -c 'import os,sqlite3,subprocess,json; from pathlib import Path; p=Path(os.environ["APPDATA"])/"Cursor"/"User"/"globalStorage"/"state.vscdb"; c=sqlite3.connect(p.as_uri()+"?mode=ro",uri=True); v=c.execute("SELECT value FROM ItemTable WHERE key = ?",("cursorAuth/accessToken",)).fetchone()[0]; c.close(); v=json.loads(v) if v.startswith("\"") else v; subprocess.run(["clip.exe"],input=v,text=True,check=True)'
""".trimIndent()
