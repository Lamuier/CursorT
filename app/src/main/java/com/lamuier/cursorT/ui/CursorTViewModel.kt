package com.lamuier.cursorT.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lamuier.cursorT.data.CursorRepository
import com.lamuier.cursorT.data.CursorStatusRepository
import com.lamuier.cursorT.model.AppStage
import com.lamuier.cursorT.model.AppUiState
import com.lamuier.cursorT.model.CursorAccount
import com.lamuier.cursorT.model.CursorServiceStatus
import com.lamuier.cursorT.model.CursorTOverview
import com.lamuier.cursorT.network.ApiException
import com.lamuier.cursorT.notification.CursorTNotificationCoordinator
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
            _state.update { it.copy(error = "账号不存在或已被删除") }
            return
        }

        val selectionError = saveSelection(accountId)
        val cached = repository.cachedUsage(accountId)?.takeIf { it.accountId == accountId }
        _state.update {
            it.copy(
                selectedAccountId = accountId,
                usage = cached,
                loadingUsage = cached == null,
                refreshing = false,
                error = selectionError,
            )
        }
        refreshSelected(force = false, silent = cached != null)
    }

    fun refreshSelected(force: Boolean, silent: Boolean, includeStatus: Boolean = true) {
        refreshUsage(force = force, silent = silent)
        if (includeStatus) refreshStatus(force = force, silent = silent)
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
                    it.copy(statusError = messageFor(error, "加载 Cursor 状态失败"))
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
            _state.update { it.copy(error = messageFor(error, "无法读取账号状态")) }
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
                            error = messageFor(error, "加载 Cursor 用量失败"),
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
            _state.update { it.copy(error = "请填写别名和 Access Token") }
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
                        selectionError = saveSelection(account.id),
                    )
                }
                val accounts = mergeAccount(_state.value.accounts, result.account)
                _state.update {
                    it.copy(
                        accounts = accounts,
                        selectedAccountId = result.account.id,
                        usage = result.cachedUsage,
                        loadingUsage = result.cachedUsage == null,
                        refreshing = false,
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
                        error = messageFor(error, "添加账号失败"),
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
            _state.update { it.copy(error = "别名或 Access Token 至少填写一项") }
            return
        }

        // Set synchronously so account revision changes are serialized.
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            try {
                val selected = _state.value.selectedAccountId == accountId
                val previousUsage = _state.value.usage?.takeIf { it.accountId == accountId }
                val result = withContext(Dispatchers.IO) {
                    val updated = repository.updateAccount(accountId, alias, accessToken)
                    AccountSaveResult(
                        account = updated,
                        cachedUsage = if (selected) {
                            repository.cachedUsage(accountId)?.takeIf { it.accountId == accountId }
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
                _state.update {
                    it.copy(
                        accounts = mergeAccount(it.accounts, result.account),
                        usage = if (selected) cached else it.usage,
                        loadingUsage = selected && cached == null,
                        refreshing = false,
                        submitting = false,
                        error = null,
                    )
                }
                runCatching(onSuccess)
                if (selected) refreshSelected(force = true, silent = cached != null)
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
                        error = messageFor(error, "更新账号失败"),
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
                        selectionError = saveSelection(selected),
                    )
                }
                val selectionChanged = result.selectedAccountId != previousSelected
                val cached = if (selectionChanged) result.cachedUsage else _state.value.usage
                _state.update {
                    it.copy(
                        accounts = result.accounts,
                        selectedAccountId = result.selectedAccountId,
                        usage = cached,
                        loadingUsage = selectionChanged && result.selectedAccountId != null && cached == null,
                        refreshing = false,
                        submitting = false,
                        error = result.selectionError,
                    )
                }
                runCatching(onSuccess)
                if (selectionChanged && result.selectedAccountId != null) {
                    refreshSelected(force = false, silent = cached != null)
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
                        error = messageFor(error, "删除账号失败"),
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
                        serviceStatus = statusRepository.cached(),
                        selectionError = saveSelection(selected),
                    )
                }
                _state.value = AppUiState(
                    stage = AppStage.Dashboard,
                    accounts = initial.accounts,
                    selectedAccountId = initial.selectedAccountId,
                    usage = initial.usage,
                    serviceStatus = initial.serviceStatus,
                    loadingAccounts = false,
                    loadingUsage = initial.selectedAccountId != null && initial.usage == null,
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
                    error = messageFor(error, "加载本地账号失败"),
                )
                refreshStatus(force = false, silent = true)
            }
        }
    }

    private fun saveSelection(accountId: Int?): String? = try {
        repository.saveSelectedAccountId(accountId)
        null
    } catch (error: Exception) {
        messageFor(error, "无法保存账号选择")
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

    private fun messageFor(error: Throwable, fallback: String): String = when (error) {
        is ApiException -> error.message
        is IllegalArgumentException -> error.message ?: fallback
        is IllegalStateException -> error.message ?: fallback
        is IOException -> "无法连接 Cursor 服务，请检查网络"
        else -> error.message?.takeIf { it.isNotBlank() } ?: fallback
    }

    private data class UsageRequestKey(
        val accountId: Int,
        val revision: Long,
    )

    private data class BootstrapResult(
        val accounts: List<CursorAccount>,
        val selectedAccountId: Int?,
        val usage: CursorTOverview?,
        val serviceStatus: CursorServiceStatus?,
        val selectionError: String?,
    )

    private data class AccountSaveResult(
        val account: CursorAccount,
        val cachedUsage: CursorTOverview?,
        val selectionError: String? = null,
    )

    private data class DeleteAccountResult(
        val accounts: List<CursorAccount>,
        val selectedAccountId: Int?,
        val cachedUsage: CursorTOverview?,
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
