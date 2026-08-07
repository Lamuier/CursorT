package com.lamuier.cursorusage.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.lamuier.cursorusage.data.NotificationSettings
import com.lamuier.cursorusage.data.PercentDisplayMode
import com.lamuier.cursorusage.data.ThemeSettings
import com.lamuier.cursorusage.model.AppStage
import com.lamuier.cursorusage.model.CursorAccount
import com.lamuier.cursorusage.ui.theme.ColorPalette
import com.lamuier.cursorusage.ui.theme.ThemeMode
import kotlinx.coroutines.delay

private const val FOREGROUND_REFRESH_INTERVAL_MS = 5 * 60 * 1_000L

@Composable
fun CursorUsageApp(
    viewModel: CursorUsageViewModel,
    themeSettings: ThemeSettings,
    notificationSettings: NotificationSettings,
    onThemeModeChange: (ThemeMode) -> Unit,
    onPaletteChange: (ColorPalette) -> Unit,
    onLiveUpdatesToggle: (Boolean) -> Unit,
    onThresholdRemindersToggle: (Boolean) -> Unit,
    percentDisplayMode: PercentDisplayMode,
    onPercentDisplayModeChange: (PercentDisplayMode) -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current
    val account = remember(state.accounts, state.selectedAccountId) {
        state.accounts.firstOrNull { it.id == state.selectedAccountId }
            ?: state.accounts.firstOrNull()
    }

    var manageAccount by remember { mutableStateOf(false) }
    var showTokenHelp by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var hasAutoOpenedEmptyAccount by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<CursorAccount?>(null) }
    val sheetOpen = manageAccount || showTokenHelp || showSettings

    LaunchedEffect(state.stage) {
        if (state.stage != AppStage.Dashboard) {
            manageAccount = false
            showTokenHelp = false
            showSettings = false
            deleteTarget = null
        }
    }

    LaunchedEffect(state.error, sheetOpen) {
        if (sheetOpen) return@LaunchedEffect
        val message = state.error ?: return@LaunchedEffect
        viewModel.clearError()
        snackbarHostState.showSnackbar(message)
    }

    LaunchedEffect(state.stage, state.loadingAccounts, account?.id, state.selectedAccountId) {
        if (
            state.stage == AppStage.Dashboard &&
            !state.loadingAccounts &&
            account != null &&
            state.selectedAccountId != account.id
        ) {
            viewModel.selectAccount(account.id)
        }
    }

    LaunchedEffect(state.stage, state.loadingAccounts, account?.id) {
        if (
            state.stage == AppStage.Dashboard &&
            !state.loadingAccounts &&
            account == null &&
            !hasAutoOpenedEmptyAccount
        ) {
            hasAutoOpenedEmptyAccount = true
            manageAccount = true
        }
    }

    LaunchedEffect(
        lifecycleOwner,
        state.stage,
        account?.id,
        account?.tokenExpired,
        state.submitting,
        sheetOpen,
    ) {
        if (
            state.stage != AppStage.Dashboard ||
            account == null ||
            account.tokenExpired ||
            state.submitting ||
            sheetOpen
        ) {
            return@LaunchedEffect
        }
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.refreshSelected(force = false, silent = true)
            while (true) {
                delay(FOREGROUND_REFRESH_INTERVAL_MS)
                viewModel.refreshSelected(force = false, silent = true)
            }
        }
    }

    when (state.stage) {
        AppStage.Booting -> BootScreen()
        AppStage.Dashboard -> DashboardScreen(
            state = state,
            snackbarHostState = snackbarHostState,
            onRefresh = { viewModel.refreshSelected(force = true, silent = true) },
            onManageAccount = { manageAccount = true },
            onShowSettings = { showSettings = true },
            onShowTokenHelp = { showTokenHelp = true },
        )
    }

    if (manageAccount && state.stage == AppStage.Dashboard) {
        AccountManagementSheet(
            account = account,
            busy = state.submitting || state.loadingAccounts,
            error = state.error,
            onClearError = viewModel::clearError,
            onDismiss = { manageAccount = false },
            onAdd = viewModel::addAccount,
            onUpdate = viewModel::updateAccount,
            onDeleteRequest = { deleteTarget = it },
            onRevealSavedToken = viewModel::revealAccessToken,
        )
    }

    if (showTokenHelp) {
        TokenHelpSheet(onDismiss = { showTokenHelp = false })
    }

    if (showSettings) {
        SettingsSheet(
            settings = themeSettings,
            notificationSettings = notificationSettings,
            onDismiss = { showSettings = false },
            onThemeModeChange = onThemeModeChange,
            onPaletteChange = onPaletteChange,
            onLiveUpdatesToggle = onLiveUpdatesToggle,
            onThresholdRemindersToggle = onThresholdRemindersToggle,
            percentDisplayMode = percentDisplayMode,
            onPercentDisplayModeChange = onPercentDisplayModeChange,
            onManageAccount = { showSettings = false; manageAccount = true },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { if (!state.submitting) deleteTarget = null },
            title = {
                Text(
                    "删除「${target.alias}」？",
                    fontWeight = FontWeight.SemiBold,
                )
            },
            text = {
                Text("本机保存的账号和用量缓存将被删除。之后需要重新录入 Access Token。")
            },
            confirmButton = {
                Button(
                    enabled = !state.submitting,
                    onClick = {
                        viewModel.deleteAccount(target.id) {
                            deleteTarget = null
                            manageAccount = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    if (state.submitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onError,
                        )
                    } else {
                        Text("删除")
                    }
                }
            },
            dismissButton = {
                TextButton(enabled = !state.submitting, onClick = { deleteTarget = null }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun BootScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                "CursorUsage",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "Cursor 用量监控",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(28.dp),
                strokeWidth = 2.5.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}
