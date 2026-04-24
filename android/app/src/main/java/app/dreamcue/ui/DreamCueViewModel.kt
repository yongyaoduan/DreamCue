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
        title = "当前备忘",
        subtitle = "轻点一条备忘可查看详情",
    ),
    SEARCH(
        title = "搜索备忘",
        subtitle = "输入关键词后点击搜索",
    ),
    HISTORY(
        title = "历史备忘",
        subtitle = "这里保留已经消除的备忘",
    ),
    REMINDER(
        title = "提醒设置",
        subtitle = "设置每天的提醒时间",
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
    val isLoading: Boolean = false,
    val nativeReady: Boolean = false,
    val nativeError: String? = null,
    val errorMessage: String? = null,
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
) : ViewModel() {
    private var detailAutoSaveJob: Job? = null

    var uiState by mutableStateOf(
        MainUiState(
            reminderTime = repository.reminderTime(),
            nativeError = repository.nativeLoadError(),
        ),
    )
        private set

    init {
        refresh()
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
                    val currentMemos = allMemos
                        .asSequence()
                        .filter { it.isActive }
                        .sortedByDescending { it.updatedAtMs }
                        .toList()
                    val historyMemos = allMemos
                        .asSequence()
                        .filter { !it.isActive }
                        .sortedByDescending { it.clearedAtMs ?: it.updatedAtMs }
                        .toList()
                    DashboardPayload(
                        currentMemos = currentMemos,
                        historyMemos = historyMemos,
                        searchResults = if (submittedSearchQuery.isBlank()) {
                            emptyList()
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
                )
            }.onFailure { throwable ->
                uiState = uiState.copy(
                    isLoading = false,
                    nativeReady = false,
                    nativeError = repository.nativeLoadError(),
                    errorMessage = throwable.message ?: "加载失败",
                )
            }
        }
    }

    fun addMemo() {
        val draft = uiState.draft
        if (draft.isBlank()) {
            uiState = uiState.copy(errorMessage = "请输入一条备忘")
            return
        }

        runMutation(
            clearDraft = true,
            action = {
                repository.addMemo(draft)
            },
        )
    }

    fun search() {
        val query = uiState.searchQuery.trim()
        uiState = uiState.copy(
            submittedSearchQuery = query,
            selectedScreen = MemoScreen.SEARCH,
            searchResults = if (query.isBlank()) emptyList() else uiState.searchResults,
        )
        refresh()
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
        detailAutoSaveJob?.cancel()
        runMutation(
            action = {
                repository.deleteMemo(memo.id)
            },
            afterSuccess = {
                NotificationHelper.cancelMemoReminder(repository.appContext, memo.id)
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
        ReminderScheduler.schedule(repository.appContext, reminderTime)
        uiState = uiState.copy(reminderTime = reminderTime, errorMessage = null)
    }

    override fun onCleared() {
        detailAutoSaveJob?.cancel()
        repository.dispose()
        super.onCleared()
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
                    errorMessage = throwable.message ?: "操作失败",
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
            }.onFailure { throwable ->
                uiState = uiState.copy(
                    errorMessage = throwable.message ?: "自动保存失败",
                    nativeError = repository.nativeLoadError(),
                )
            }
        }
    }

    private fun applyUpdatedMemo(updatedMemo: Memo) {
        uiState = uiState.copy(
            currentMemos = uiState.currentMemos
                .map { memo -> if (memo.id == updatedMemo.id) updatedMemo else memo }
                .sortedByDescending { memo -> memo.updatedAtMs },
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
