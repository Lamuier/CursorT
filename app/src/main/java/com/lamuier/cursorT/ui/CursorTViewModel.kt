package com.lamuier.cursorT.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.annotation.StringRes
import com.lamuier.cursorT.R
import com.lamuier.cursorT.data.CursorRepository
import com.lamuier.cursorT.data.CursorStatusRepository
import com.lamuier.cursorT.model.AppStage
import com.lamuier.cursorT.model.AppUiState
import com.lamuier.cursorT.model.CursorAccount
import com.lamuier.cursorT.model.CursorServiceStatus
import com.lamuier.cursorT.model.CursorTasks
import com.lamuier.cursorT.model.CursorTOverview
import com.lamuier.cursorT.network.ApiException
import com.lamuier.cursorT.notification.CursorTNotificationCoordinator
import com.lamuier.cursorT.util.AppLocale
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CursorTViewModel(
    private val repository: CursorRepository,
    private val statusRepository: CursorStatusRepository,
    private val appContext: Context,
) : ViewModel() {
    private val activeUsageRequests = mutableSetOf<UsageRequestKey>()
    private var statusRequestInFlight = false
    private var tasksRequestInFlight = false

    private val _state = MutableStateFlow(
        AppUiState(
            stage = AppStage.Booting,
            loadingAccounts = true,
        ),
    )
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        bootstrap()
    }

    fun selectAccount(accountId: Int) {
        val snapshot = _state.value
        if (snapshot.submitting || snapshot.selectedAccountId == accountId) return
        if (snapshot.accounts.none { it.id == accountId }) {
            _state.update { it.copy(error = str(R.string.error_account_missing)) }
            return
        }

        val selectionError = saveSelection(accountId)
        val cached = repository.cachedUsage(accountId)?.takeIf { it.accountId == accountId }
        val cachedTasks = repository.cachedTasks(accountId)?.takeIf { it.accountId == accountId }
        _state.update {
            it.copy(
                selectedAccountId = accountId,
                usage = cached,
                tasks = cachedTasks,
                loadingUsage = cached == null,
                loadingTasks = cachedTasks == null,
                refreshing = false,
                refreshingTasks = false,
                error = selectionError,
                tasksError = null,
                extraHistory = emptyMap(),
                loadingHistoryKey = null,
            )
        }
        refreshSelected(force = false, silent = cached != null)
    }

    fun loadHistoryWindow(
        key: String,
        startMs: Long,
        endMs: Long,
        yearMonth: String? = null,
    ) {
        val snapshot = _state.value
        if (snapshot.stage != AppStage.Dashboard || snapshot.submitting) return
        val accountId = snapshot.selectedAccountId ?: return
        if (key.isBlank() || endMs < startMs) return
        if (snapshot.extraHistory.containsKey(key)) return
        if (yearMonth != null && snapshot.usage?.history?.calendarMonth?.yearMonth == yearMonth) return
        if (snapshot.loadingHistoryKey == key) return
        _state.update { it.copy(loadingHistoryKey = key) }
        viewModelScope.launch {
            try {
                val window = withContext(Dispatchers.IO) {
                    repository.fetchHistoryWindow(accountId, startMs, endMs, yearMonth)
                }
                if (_state.value.selectedAccountId == accountId) {
                    _state.update {
                        it.copy(
                            extraHistory = it.extraHistory + (key to window),
                            loadingHistoryKey = if (it.loadingHistoryKey == key) null else it.loadingHistoryKey,
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                if (_state.value.selectedAccountId == accountId) {
                    _state.update {
                        it.copy(
                            loadingHistoryKey = if (it.loadingHistoryKey == key) null else it.loadingHistoryKey,
                        )
                    }
                }
            }
        }
    }

    fun refreshSelected(
        force: Boolean,
        silent: Boolean,
        includeStatus: Boolean = true,
        includeTasks: Boolean = true,
    ) {
        refreshUsage(force = force, silent = silent)
        if (includeTasks) refreshTasks(force = force, silent = silent)
        if (includeStatus) refreshStatus(force = force, silent = silent)
    }

    fun refreshTasks(force: Boolean, silent: Boolean) {
        val snapshot = _state.value
        if (snapshot.stage != AppStage.Dashboard || snapshot.submitting) return
        val accountId = snapshot.selectedAccountId ?: return
        if (tasksRequestInFlight) return
        tasksRequestInFlight = true

        _state.update {
            if (silent || it.tasks != null) {
                it.copy(
                    refreshingTasks = true,
                    loadingTasks = false,
                    tasksError = if (force) null else it.tasksError,
                )
            } else {
                it.copy(loadingTasks = true, refreshingTasks = false, tasksError = null)
            }
        }

        viewModelScope.launch {
            try {
                val tasks = withContext(Dispatchers.IO) {
                    repository.fetchTasks(accountId, force)
                }
                if (_state.value.selectedAccountId == accountId) {
                    _state.update {
                        it.copy(
                            tasks = tasks,
                            tasksError = null,
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (_state.value.selectedAccountId == accountId) {
                    _state.update {
                        it.copy(tasksError = messageFor(error, R.string.error_load_tasks))
                    }
                }
            } finally {
                tasksRequestInFlight = false
                if (_state.value.selectedAccountId == accountId) {
                    _state.update { it.copy(loadingTasks = false, refreshingTasks = false) }
                }
            }
        }
    }

    fun refreshStatus(force: Boolean, silent: Boolean) {
        val snapshot = _state.value
        if (snapshot.stage != AppStage.Dashboard || snapshot.submitting) return
        if (statusRequestInFlight) return
        statusRequestInFlight = true

        _state.update {
            if (silent || it.serviceStatus != null) {
                it.copy(
                    refreshingStatus = true,
                    loadingStatus = false,
                    statusError = if (force) null else it.statusError,
                )
            } else {
                it.copy(loadingStatus = true, refreshingStatus = false, statusError = null)
            }
        }

        viewModelScope.launch {
            try {
                val status = withContext(Dispatchers.IO) {
                    statusRepository.fetch(force)
                }
                _state.update {
                    it.copy(serviceStatus = status, statusError = null)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update {
                    it.copy(statusError = messageFor(error, R.string.error_load_status))
                }
            } finally {
                statusRequestInFlight = false
                _state.update { it.copy(loadingStatus = false, refreshingStatus = false) }
            }
        }
    }

    private fun refreshUsage(force: Boolean, silent: Boolean) {
        val snapshot = _state.value
        if (snapshot.stage != AppStage.Dashboard || snapshot.submitting) return
        val accountId = snapshot.selectedAccountId ?: return
        val revision = try {
            repository.accountRevision(accountId)
        } catch (error: Exception) {
            _state.update { it.copy(error = messageFor(error, R.string.error_read_accounts)) }
            return
        }
        val requestKey = UsageRequestKey(accountId, revision)
        if (!activeUsageRequests.add(requestKey)) return

        _state.update {
            if (it.selectedAccountId != accountId) {
                it
            } else if (silent || it.usage != null) {
                it.copy(
                    refreshing = true,
                    loadingUsage = false,
                    error = if (force) null else it.error,
                )
            } else {
                it.copy(loadingUsage = true, refreshing = false, error = null)
            }
        }

        viewModelScope.launch {
            try {
                val usage = withContext(Dispatchers.IO) {
                    repository.fetchUsage(accountId, force)
                }
                if (!isUsageRequestCurrent(requestKey) || _state.value.selectedAccountId != accountId) {
                    return@launch
                }
                val latestAccounts = withContext(Dispatchers.IO) {
                    latestAccountsOr(_state.value.accounts)
                }
                if (!isUsageRequestCurrent(requestKey) || _state.value.selectedAccountId != accountId) {
                    return@launch
                }
                _state.update {
                    it.copy(
                        accounts = latestAccounts,
                        usage = usage,
                        error = null,
                    )
                }
                // 用量刷新成功后同步推送常驻 Live Update 通知与阈值提醒。
                runCatching {
                    CursorTNotificationCoordinator.get(appContext).refresh(usage)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                if (isUsageRequestCurrent(requestKey) && _state.value.selectedAccountId == accountId) {
                    val latestAccounts = withContext(Dispatchers.IO) {
                        latestAccountsOr(_state.value.accounts)
                    }
                    if (!isUsageRequestCurrent(requestKey) || _state.value.selectedAccountId != accountId) {
                        return@launch
                    }
                    // Keep the last result visible; failures only update status and account flags.
                    _state.update {
                        it.copy(
                            accounts = latestAccounts,
                            error = messageFor(error, R.string.error_load_usage),
                        )
                    }
                }
            } finally {
                activeUsageRequests.remove(requestKey)
                if (isUsageRequestCurrent(requestKey) && _state.value.selectedAccountId == accountId) {
                    _state.update { it.copy(loadingUsage = false, refreshing = false) }
                }
            }
        }
    }

    fun addAccount(alias: String, accessToken: String, onSuccess: () -> Unit = {}) {
        if (_state.value.submitting) return
        if (alias.isBlank() || accessToken.isBlank()) {
            _state.update { it.copy(error = str(R.string.error_alias_token_required)) }
            return
        }

        // Set synchronously so rapid taps cannot start duplicate writes.
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val account = repository.addAccount(alias, accessToken)
                    AccountSaveResult(
                        account = account,
                        cachedUsage = repository.cachedUsage(account.id)?.takeIf { it.accountId == account.id },
                        cachedTasks = repository.cachedTasks(account.id)?.takeIf { it.accountId == account.id },
                        selectionError = saveSelection(account.id),
                    )
                }
                val accounts = mergeAccount(_state.value.accounts, result.account)
                _state.update {
                    it.copy(
                        accounts = accounts,
                        selectedAccountId = result.account.id,
                        usage = result.cachedUsage,
                        tasks = result.cachedTasks,
                        loadingUsage = result.cachedUsage == null,
                        loadingTasks = result.cachedTasks == null,
                        refreshing = false,
                        refreshingTasks = false,
                        submitting = false,
                        error = result.selectionError,
                    )
                }
                runCatching(onSuccess)
                refreshSelected(force = true, silent = result.cachedUsage != null)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val latestAccounts = withContext(Dispatchers.IO) {
                    latestAccountsOr(_state.value.accounts)
                }
                _state.update {
                    it.copy(
                        accounts = latestAccounts,
                        submitting = false,
                        error = messageFor(error, R.string.error_add_account),
                    )
                }
            }
        }
    }

    fun updateAccount(
        accountId: Int,
        alias: String?,
        accessToken: String?,
        onSuccess: () -> Unit = {},
    ) {
        if (_state.value.submitting) return
        if (alias.isNullOrBlank() && accessToken.isNullOrBlank()) {
            _state.update { it.copy(error = str(R.string.error_alias_or_token)) }
            return
        }

        // Set synchronously so account revision changes are serialized.
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            try {
                val selected = _state.value.selectedAccountId == accountId
                val previousUsage = _state.value.usage?.takeIf { it.accountId == accountId }
                val previousTasks = _state.value.tasks?.takeIf { it.accountId == accountId }
                val result = withContext(Dispatchers.IO) {
                    val updated = repository.updateAccount(accountId, alias, accessToken)
                    AccountSaveResult(
                        account = updated,
                        cachedUsage = if (selected) {
                            repository.cachedUsage(accountId)?.takeIf { it.accountId == accountId }
                        } else {
                            null
                        },
                        cachedTasks = if (selected) {
                            repository.cachedTasks(accountId)?.takeIf { it.accountId == accountId }
                        } else {
                            null
                        },
                    )
                }
                val cached = if (selected) {
                    result.cachedUsage ?: previousUsage.takeIf { accessToken.isNullOrBlank() }
                } else {
                    null
                }
                val cachedTasks = if (selected) {
                    result.cachedTasks ?: previousTasks.takeIf { accessToken.isNullOrBlank() }
                } else {
                    null
                }
                _state.update {
                    it.copy(
                        accounts = mergeAccount(it.accounts, result.account),
                        usage = if (selected) cached else it.usage,
                        tasks = if (selected) cachedTasks else it.tasks,
                        loadingUsage = selected && cached == null,
                        loadingTasks = selected && cachedTasks == null,
                        refreshing = false,
                        refreshingTasks = false,
                        submitting = false,
                        error = null,
                    )
                }
                runCatching(onSuccess)
                if (selected) refreshSelected(force = true, silent = cached != null && cachedTasks != null)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val latestAccounts = withContext(Dispatchers.IO) {
                    latestAccountsOr(_state.value.accounts)
                }
                _state.update {
                    it.copy(
                        accounts = latestAccounts,
                        submitting = false,
                        error = messageFor(error, R.string.error_update_account),
                    )
                }
            }
        }
    }

    fun deleteAccount(accountId: Int, onSuccess: () -> Unit = {}) {
        if (_state.value.submitting) return

        // Set synchronously so delete cannot race another local mutation.
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            try {
                val previousSelected = _state.value.selectedAccountId
                val result = withContext(Dispatchers.IO) {
                    repository.deleteAccount(accountId)
                    val accounts = repository.listAccounts()
                    val selected = previousSelected
                        ?.takeIf { id -> accounts.any { it.id == id } }
                        ?: accounts.firstOrNull()?.id
                    DeleteAccountResult(
                        accounts = accounts,
                        selectedAccountId = selected,
                        cachedUsage = selected?.let(repository::cachedUsage)?.takeIf { it.accountId == selected },
                        cachedTasks = selected?.let(repository::cachedTasks)?.takeIf { it.accountId == selected },
                        selectionError = saveSelection(selected),
                    )
                }
                val selectionChanged = result.selectedAccountId != previousSelected
                val cached = if (selectionChanged) result.cachedUsage else _state.value.usage
                val cachedTasks = if (selectionChanged) result.cachedTasks else _state.value.tasks
                _state.update {
                    it.copy(
                        accounts = result.accounts,
                        selectedAccountId = result.selectedAccountId,
                        usage = cached,
                        tasks = cachedTasks,
                        loadingUsage = selectionChanged && result.selectedAccountId != null && cached == null,
                        loadingTasks = selectionChanged && result.selectedAccountId != null && cachedTasks == null,
                        refreshing = false,
                        refreshingTasks = false,
                        submitting = false,
                        error = result.selectionError,
                    )
                }
                runCatching(onSuccess)
                if (selectionChanged && result.selectedAccountId != null) {
                    refreshSelected(force = false, silent = cached != null && cachedTasks != null)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                val latestAccounts = withContext(Dispatchers.IO) {
                    latestAccountsOr(_state.value.accounts)
                }
                _state.update {
                    it.copy(
                        accounts = latestAccounts,
                        submitting = false,
                        error = messageFor(error, R.string.error_delete_account),
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun revealAccessToken(accountId: Int): String = repository.revealAccessToken(accountId)

    private fun bootstrap() {
        viewModelScope.launch {
            try {
                val initial = withContext(Dispatchers.IO) {
                    val accounts = repository.listAccounts()
                    val saved = repository.selectedAccountId()
                    val selected = saved
                        ?.takeIf { id -> accounts.any { it.id == id } }
                        ?: accounts.firstOrNull()?.id
                    BootstrapResult(
                        accounts = accounts,
                        selectedAccountId = selected,
                        usage = selected?.let(repository::cachedUsage)?.takeIf { it.accountId == selected },
                        tasks = selected?.let(repository::cachedTasks)?.takeIf { it.accountId == selected },
                        serviceStatus = statusRepository.cached(),
                        selectionError = saveSelection(selected),
                    )
                }
                _state.value = AppUiState(
                    stage = AppStage.Dashboard,
                    accounts = initial.accounts,
                    selectedAccountId = initial.selectedAccountId,
                    usage = initial.usage,
                    tasks = initial.tasks,
                    serviceStatus = initial.serviceStatus,
                    loadingAccounts = false,
                    loadingUsage = initial.selectedAccountId != null && initial.usage == null,
                    loadingTasks = initial.selectedAccountId != null && initial.tasks == null,
                    loadingStatus = initial.serviceStatus == null,
                    error = initial.selectionError,
                )
                refreshSelected(
                    force = false,
                    silent = initial.usage != null || initial.serviceStatus != null,
                )
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.value = AppUiState(
                    stage = AppStage.Dashboard,
                    loadingAccounts = false,
                    serviceStatus = runCatching { statusRepository.cached() }.getOrNull(),
                    error = messageFor(error, R.string.error_load_local_accounts),
                )
                refreshStatus(force = false, silent = true)
            }
        }
    }

    private fun saveSelection(accountId: Int?): String? = try {
        repository.saveSelectedAccountId(accountId)
        null
    } catch (error: Exception) {
        messageFor(error, R.string.error_save_selection)
    }

    private fun latestAccountsOr(fallback: List<CursorAccount>): List<CursorAccount> =
        try {
            repository.listAccounts()
        } catch (_: Exception) {
            fallback
        }

    private fun mergeAccount(accounts: List<CursorAccount>, updated: CursorAccount): List<CursorAccount> =
        (accounts.filterNot { it.id == updated.id } + updated).sortedBy { it.id }

    private fun isUsageRequestCurrent(key: UsageRequestKey): Boolean =
        try {
            repository.accountRevision(key.accountId) == key.revision
        } catch (_: Exception) {
            false
        }

    private fun str(@StringRes id: Int) = AppLocale.string(appContext, id)

    private fun messageFor(error: Throwable, @StringRes fallback: Int): String = when (error) {
        is ApiException -> error.message
        is IllegalArgumentException -> error.message ?: str(fallback)
        is IllegalStateException -> error.message ?: str(fallback)
        is IOException -> str(R.string.error_network)
        else -> error.message?.takeIf { it.isNotBlank() } ?: str(fallback)
    }

    private data class UsageRequestKey(
        val accountId: Int,
        val revision: Long,
    )

    private data class BootstrapResult(
        val accounts: List<CursorAccount>,
        val selectedAccountId: Int?,
        val usage: CursorTOverview?,
        val tasks: CursorTasks?,
        val serviceStatus: CursorServiceStatus?,
        val selectionError: String?,
    )

    private data class AccountSaveResult(
        val account: CursorAccount,
        val cachedUsage: CursorTOverview?,
        val cachedTasks: CursorTasks? = null,
        val selectionError: String? = null,
    )

    private data class DeleteAccountResult(
        val accounts: List<CursorAccount>,
        val selectedAccountId: Int?,
        val cachedUsage: CursorTOverview?,
        val cachedTasks: CursorTasks? = null,
        val selectionError: String?,
    )

    class Factory(context: Context) : ViewModelProvider.Factory {
        private val appContext = context.applicationContext

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(CursorTViewModel::class.java))
            return CursorTViewModel(
                CursorRepository(appContext),
                CursorStatusRepository(appContext),
                appContext,
            ) as T
        }
    }
}
