package app.dreamcue.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import app.dreamcue.DreamCueRepository
import app.dreamcue.model.Memo
import app.dreamcue.model.ReminderTime
import app.dreamcue.model.SearchResult
import app.dreamcue.sync.FirebaseSyncCoordinator
import app.dreamcue.sync.MemoSyncCoordinator
import app.dreamcue.worker.NotificationHelper
import app.dreamcue.worker.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MemoScreen(
    val title: String,
    val subtitle: String,
) {
    CURRENT(
        title = "Current Memos",
        subtitle = "Tap a memo to view details",
    ),
    HISTORY(
        title = "History",
        subtitle = "Cleared memos stay here",
    ),
    REMINDER(
        title = "Reminder Rhythm",
        subtitle = "A quiet daily return point",
    ),
    ACCOUNT(
        title = "Account",
        subtitle = "Private sync across devices",
    ),
}

data class MainUiState(
    val draft: String = "",
    val searchQuery: String = "",
    val submittedSearchQuery: String = "",
    val selectedScreen: MemoScreen = MemoScreen.CURRENT,
    val currentMemos: List<Memo> = emptyList(),
    val historyMemos: List<Memo> = emptyList(),
    val searchResults: List<SearchResult> = emptyList(),
    val reminderTime: ReminderTime = ReminderTime.Default,
    val reminderEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val nativeReady: Boolean = false,
    val nativeError: String? = null,
    val errorMessage: String? = null,
    val syncEmail: String = "",
    val syncPassword: String = "",
    val syncStatus: String = "Sign in to sync across devices.",
    val selectedMemo: Memo? = null,
    val detailDraft: String = "",
    val pendingDeleteMemo: Memo? = null,
)

private data class DashboardPayload(
    val currentMemos: List<Memo>,
    val historyMemos: List<Memo>,
    val searchResults: List<SearchResult>,
)

class DreamCueViewModel(
    private val repository: DreamCueRepository,
    private val syncCoordinator: MemoSyncCoordinator = FirebaseSyncCoordinator(repository),
) : ViewModel() {
    private var detailAutoSaveJob: Job? = null

    var uiState by mutableStateOf(
        MainUiState(
            reminderTime = repository.reminderTime(),
            reminderEnabled = repository.reminderEnabled(),
            nativeError = repository.nativeLoadError(),
        ),
    )
        private set

    init {
        refresh()
        startSync()
    }

    fun selectScreen(screen: MemoScreen) {
        uiState = uiState.copy(selectedScreen = screen)
    }

    fun updateDraft(value: String) {
        uiState = uiState.copy(draft = value)
    }

    fun updateSearchQuery(value: String) {
        uiState = if (value.isBlank()) {
            uiState.copy(
                searchQuery = "",
                submittedSearchQuery = "",
                searchResults = emptyList(),
            )
        } else {
            uiState.copy(searchQuery = value)
        }
    }

    fun updateDetailDraft(value: String) {
        uiState = uiState.copy(detailDraft = value)
        scheduleDetailAutoSave()
    }

    fun updateSyncEmail(value: String) {
        uiState = uiState.copy(syncEmail = value)
    }

    fun updateSyncPassword(value: String) {
        uiState = uiState.copy(syncPassword = value)
    }

    fun signInSync() {
        val email = uiState.syncEmail
        val password = uiState.syncPassword
        if (email.isBlank() || password.isBlank()) {
            uiState = uiState.copy(syncStatus = "Enter an email and password.")
            return
        }

        syncCoordinator.signIn(
            email = email,
            password = password,
            onStatus = ::updateSyncStatus,
            onRemoteChange = ::refresh,
        )
    }

    fun createSyncAccount() {
        val email = uiState.syncEmail
        val password = uiState.syncPassword
        if (email.isBlank() || password.isBlank()) {
            uiState = uiState.copy(syncStatus = "Enter an email and password.")
            return
        }

        syncCoordinator.createAccount(
            email = email,
            password = password,
            onStatus = ::updateSyncStatus,
            onRemoteChange = ::refresh,
        )
    }

    fun signOutSync() {
        syncCoordinator.signOut(::updateSyncStatus)
        uiState = uiState.copy(syncPassword = "")
    }

    fun openMemoDetail(memo: Memo) {
        detailAutoSaveJob?.cancel()
        uiState = uiState.copy(
            selectedMemo = memo,
            detailDraft = memo.content,
            errorMessage = null,
        )
    }

    fun dismissMemoDetail() {
        val memo = uiState.selectedMemo
        val draft = uiState.detailDraft
        detailAutoSaveJob?.cancel()
        uiState = uiState.copy(selectedMemo = null, detailDraft = "")

        if (memo != null && draft.isNotBlank() && draft != memo.content) {
            persistMemoDraft(memo.id, draft)
        }
    }

    fun requestDelete(memo: Memo) {
        detailAutoSaveJob?.cancel()
        uiState = uiState.copy(
            selectedMemo = if (uiState.selectedMemo?.id == memo.id) null else uiState.selectedMemo,
            detailDraft = if (uiState.selectedMemo?.id == memo.id) "" else uiState.detailDraft,
            pendingDeleteMemo = memo,
        )
    }

    fun dismissDeleteRequest() {
        uiState = uiState.copy(pendingDeleteMemo = null)
    }

    fun refresh() {
        val submittedSearchQuery = uiState.submittedSearchQuery
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null, nativeError = repository.nativeLoadError())
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    repository.initialize().getOrThrow()
                    val allMemos = repository.listAllMemos()
                    val currentMemos = repository.listActiveMemos()
                    val historyMemos = allMemos
                        .asSequence()
                        .filter { !it.isActive }
                        .sortedByDescending { it.clearedAtMs ?: it.updatedAtMs }
                        .toList()
                    DashboardPayload(
                        currentMemos = currentMemos,
                        historyMemos = historyMemos,
                        searchResults = if (submittedSearchQuery.isBlank()) {
                            (currentMemos + historyMemos).map { memo ->
                                SearchResult(
                                    memo = memo,
                                    score = 0.0,
                                    matchedBy = listOf("recent"),
                                )
                            }
                        } else {
                            repository.searchMemos(submittedSearchQuery, limit = 20)
                        },
                    )
                }
            }

            result.onSuccess { payload ->
                uiState = uiState.copy(
                    currentMemos = payload.currentMemos,
                    historyMemos = payload.historyMemos,
                    searchResults = payload.searchResults,
                    nativeReady = repository.isReady(),
                    nativeError = null,
                    isLoading = false,
                    reminderTime = repository.reminderTime(),
                    reminderEnabled = repository.reminderEnabled(),
                    syncEmail = syncCoordinator.currentEmail().ifBlank { uiState.syncEmail },
                )
                syncCoordinator.uploadAll(payload.currentMemos + payload.historyMemos)
            }.onFailure { throwable ->
                uiState = uiState.copy(
                    isLoading = false,
                    nativeReady = false,
                    nativeError = repository.nativeLoadError(),
                    errorMessage = throwable.message ?: "Load failed",
                )
            }
        }
    }

    fun addMemo() {
        val draft = uiState.draft
        if (draft.isBlank()) {
            uiState = uiState.copy(errorMessage = "Enter a memo")
            return
        }

        runMutation(
            clearDraft = true,
            action = {
                repository.addMemo(draft)
            },
        )
    }

    fun setMemoPinned(memoId: String, pinned: Boolean) {
        val pendingDraft = pendingDetailDraftFor(memoId)
        detailAutoSaveJob?.cancel()
        runMutation(
            action = {
                pendingDraft?.let { repository.updateMemo(memoId, it) }
                repository.setMemoPinned(memoId, pinned)
            },
            afterSuccess = {
                if (uiState.selectedMemo?.id == memoId) {
                    uiState = uiState.copy(selectedMemo = null, detailDraft = "")
                }
            },
        )
    }

    fun search() {
        val query = uiState.searchQuery.trim()
        uiState = uiState.copy(
            submittedSearchQuery = query,
            selectedScreen = MemoScreen.HISTORY,
        )
        refresh()
    }

    fun reorderCurrentMemos(fromIndex: Int, toIndex: Int) {
        val current = uiState.currentMemos.toMutableList()
        if (fromIndex !in current.indices || toIndex !in current.indices || fromIndex == toIndex) {
            return
        }

        val moved = current.removeAt(fromIndex)
        current.add(toIndex, moved)
        uiState = uiState.copy(currentMemos = current)

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    repository.initialize().getOrThrow()
                    repository.reorderActiveMemos(current.map { it.id })
                }
            }
            result.onSuccess { reordered ->
                uiState = uiState.copy(currentMemos = reordered, errorMessage = null)
                syncCoordinator.uploadAll(reordered)
            }.onFailure { throwable ->
                uiState = uiState.copy(
                    errorMessage = throwable.message ?: "Reorder failed",
                    nativeError = repository.nativeLoadError(),
                )
            }
        }
    }

    fun clearMemo(memoId: String) {
        val pendingDraft = pendingDetailDraftFor(memoId)
        detailAutoSaveJob?.cancel()
        runMutation(
            action = {
                pendingDraft?.let { repository.updateMemo(memoId, it) }
                repository.clearMemo(memoId)
            },
            afterSuccess = {
                NotificationHelper.cancelMemoReminder(repository.appContext, memoId)
                if (uiState.selectedMemo?.id == memoId) {
                    uiState = uiState.copy(selectedMemo = null, detailDraft = "")
                }
            },
        )
    }

    fun reopenMemo(memoId: String) {
        val pendingDraft = pendingDetailDraftFor(memoId)
        detailAutoSaveJob?.cancel()
        runMutation(
            action = {
                pendingDraft?.let { repository.updateMemo(memoId, it) }
                repository.reopenMemo(memoId)
            },
            afterSuccess = {
                if (uiState.selectedMemo?.id == memoId) {
                    uiState = uiState.copy(selectedMemo = null, detailDraft = "")
                }
            },
        )
    }

    fun deleteMemo() {
        val memo = uiState.pendingDeleteMemo ?: return
        var deletedAtMs: Long? = null
        detailAutoSaveJob?.cancel()
        runMutation(
            action = {
                deletedAtMs = repository.deleteMemoWithTombstone(memo.id)
            },
            afterSuccess = {
                NotificationHelper.cancelMemoReminder(repository.appContext, memo.id)
                syncCoordinator.uploadDeletedMemo(memo.id, deletedAtMs ?: System.currentTimeMillis())
                uiState = uiState.copy(
                    pendingDeleteMemo = null,
                    selectedMemo = if (uiState.selectedMemo?.id == memo.id) null else uiState.selectedMemo,
                    detailDraft = if (uiState.selectedMemo?.id == memo.id) "" else uiState.detailDraft,
                )
            },
        )
    }

    fun saveReminderTime(hour: Int, minute: Int) {
        val reminderTime = ReminderTime(hour, minute)
        repository.saveReminderTime(reminderTime)
        if (uiState.reminderEnabled) {
            ReminderScheduler.schedule(repository.appContext, reminderTime)
        }
        uiState = uiState.copy(reminderTime = reminderTime, errorMessage = null)
    }

    fun setReminderEnabled(enabled: Boolean) {
        repository.saveReminderEnabled(enabled)
        if (enabled) {
            ReminderScheduler.schedule(repository.appContext, repository.reminderTime())
        } else {
            ReminderScheduler.cancel(repository.appContext)
        }
        uiState = uiState.copy(reminderEnabled = enabled, errorMessage = null)
    }

    override fun onCleared() {
        detailAutoSaveJob?.cancel()
        syncCoordinator.stop()
        repository.dispose()
        super.onCleared()
    }

    private fun startSync() {
        syncCoordinator.start(
            onStatus = ::updateSyncStatus,
            onRemoteChange = ::refresh,
        )
    }

    private fun updateSyncStatus(status: String) {
        uiState = uiState.copy(syncStatus = status)
    }

    private fun runMutation(
        clearDraft: Boolean = false,
        action: suspend () -> Unit,
        afterSuccess: (() -> Unit)? = null,
    ) {
        viewModelScope.launch {
            uiState = uiState.copy(isLoading = true, errorMessage = null)
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    repository.initialize().getOrThrow()
                    action()
                }
            }
            result.onSuccess {
                if (clearDraft) {
                    uiState = uiState.copy(draft = "")
                }
                afterSuccess?.invoke()
                refresh()
            }.onFailure { throwable ->
                uiState = uiState.copy(
                    isLoading = false,
                    errorMessage = throwable.message ?: "Action failed",
                    nativeError = repository.nativeLoadError(),
                )
            }
        }
    }

    private fun scheduleDetailAutoSave() {
        val memo = uiState.selectedMemo ?: return
        val draft = uiState.detailDraft

        detailAutoSaveJob?.cancel()
        if (draft.isBlank() || draft == memo.content) {
            return
        }

        detailAutoSaveJob = viewModelScope.launch {
            delay(500)
            persistMemoDraft(memo.id, draft)
        }
    }

    private fun persistMemoDraft(memoId: String, draft: String) {
        if (draft.isBlank()) {
            return
        }

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    repository.initialize().getOrThrow()
                    repository.updateMemo(memoId, draft)
                }
            }

            result.onSuccess { updatedMemo ->
                applyUpdatedMemo(updatedMemo)
                syncCoordinator.uploadAll(listOf(updatedMemo))
            }.onFailure { throwable ->
                uiState = uiState.copy(
                    errorMessage = throwable.message ?: "Autosave failed",
                    nativeError = repository.nativeLoadError(),
                )
            }
        }
    }

    private fun applyUpdatedMemo(updatedMemo: Memo) {
        uiState = uiState.copy(
            currentMemos = uiState.currentMemos
                .map { memo -> if (memo.id == updatedMemo.id) updatedMemo else memo }
                .sortedWith(activeMemoComparator),
            historyMemos = uiState.historyMemos
                .map { memo -> if (memo.id == updatedMemo.id) updatedMemo else memo }
                .sortedByDescending { memo -> memo.clearedAtMs ?: memo.updatedAtMs },
            searchResults = uiState.searchResults.map { result ->
                if (result.memo.id == updatedMemo.id) {
                    result.copy(memo = updatedMemo)
                } else {
                    result
                }
            },
            selectedMemo = if (uiState.selectedMemo?.id == updatedMemo.id) updatedMemo else uiState.selectedMemo,
            errorMessage = null,
        )
    }

    private fun pendingDetailDraftFor(memoId: String): String? {
        val memo = uiState.selectedMemo ?: return null
        val draft = uiState.detailDraft
        if (memo.id != memoId || draft.isBlank() || draft == memo.content) {
            return null
        }
        return draft
    }

    companion object {
        fun factory(repository: DreamCueRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return DreamCueViewModel(repository) as T
                }
            }
        }
    }
}

private val activeMemoComparator = compareByDescending<Memo> { it.pinned }
    .thenByDescending { it.displayOrder }
    .thenByDescending { it.updatedAtMs }
