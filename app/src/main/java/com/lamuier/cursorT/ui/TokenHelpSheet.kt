package com.lamuier.cursorT.ui

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import com.lamuier.cursorT.R

private val CodeSurface = Color(0xFF0F1419)
private val CodeOnSurface = Color(0xFFD7E0EA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TokenHelpSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        TokenHelpContent(onDismiss = onDismiss, actionLabel = stringResource(R.string.token_help_got_it))
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
                    stringResource(R.string.token_help_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(R.string.token_help_intro),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.token_help_close))
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
                    title = stringResource(R.string.token_help_step1_title),
                    body = stringResource(R.string.token_help_step1_body),
                    isLast = false,
                )
                HelpTimelineStep(
                    number = 2,
                    title = stringResource(R.string.token_help_step2_title),
                    body = stringResource(R.string.token_help_step2_body),
                    isLast = false,
                )
                HelpTimelineStep(
                    number = 3,
                    title = stringResource(R.string.token_help_step3_title),
                    body = stringResource(R.string.token_help_step3_body),
                    isLast = true,
                )
            }
        }

        HelpCommand(
            title = stringResource(R.string.token_help_powershell_title),
            description = stringResource(R.string.token_help_powershell_body),
            command = POWERSHELL_COMMAND,
        )

        HelpCommand(
            title = stringResource(R.string.token_help_python_title),
            description = stringResource(R.string.token_help_python_body),
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
                    stringResource(R.string.token_help_protect_title),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(R.string.token_help_protect_body),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                )
                SelectionContainer {
                    Text(
                        stringResource(R.string.token_help_clear_clipboard),
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
                            .setPrimaryClip(ClipData.newPlainText(context.getString(R.string.token_help_command_clip_label), command))
                        copied = true
                    },
                ) {
                    Text(if (copied) stringResource(R.string.action_copied) else stringResource(R.string.token_help_copy_command))
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
