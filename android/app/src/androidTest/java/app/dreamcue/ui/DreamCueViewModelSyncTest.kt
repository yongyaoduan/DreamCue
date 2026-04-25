package app.dreamcue.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.dreamcue.DreamCueRepository
import app.dreamcue.model.Memo
import app.dreamcue.sync.MemoSyncCoordinator
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class DreamCueViewModelSyncTest {
    @Test
    fun detailAutosaveUploadsUpdatedMemo() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        File(context.filesDir, "dreamcue.sqlite3").delete()
        val repository = DreamCueRepository(context)
        val syncCoordinator = RecordingSyncCoordinator()
        val viewModel = DreamCueViewModel(repository, syncCoordinator)

        waitUntil { !viewModel.uiState.isLoading && viewModel.uiState.nativeReady }
        viewModel.updateDraft("initial cue")
        viewModel.addMemo()
        waitUntil { viewModel.uiState.currentMemos.any { it.content == "initial cue" } }

        val memo = viewModel.uiState.currentMemos.first { it.content == "initial cue" }
        syncCoordinator.uploadedMemos.clear()
        viewModel.openMemoDetail(memo)
        viewModel.updateDetailDraft("updated cue")

        waitUntil {
            syncCoordinator.uploadedMemos.any { uploaded ->
                uploaded.id == memo.id && uploaded.content == "updated cue"
            }
        }
        assertTrue(
            syncCoordinator.uploadedMemos.any { uploaded ->
                uploaded.id == memo.id && uploaded.content == "updated cue"
            },
        )
        repository.dispose()
    }

    @Test
    fun reorderingCurrentMemosUploadsDisplayOrderForSync() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        File(context.filesDir, "dreamcue.sqlite3").delete()
        val repository = DreamCueRepository(context)
        val syncCoordinator = RecordingSyncCoordinator()
        val viewModel = DreamCueViewModel(repository, syncCoordinator)

        waitUntil { !viewModel.uiState.isLoading && viewModel.uiState.nativeReady }
        viewModel.updateDraft("first ordered cue")
        viewModel.addMemo()
        viewModel.updateDraft("second ordered cue")
        viewModel.addMemo()
        viewModel.updateDraft("third ordered cue")
        viewModel.addMemo()
        waitUntil { viewModel.uiState.currentMemos.size >= 3 }

        val movedMemoId = viewModel.uiState.currentMemos.first().id
        syncCoordinator.uploadedMemos.clear()
        viewModel.reorderCurrentMemos(0, 2)

        waitUntil {
            syncCoordinator.uploadedMemos.size >= 3 &&
                syncCoordinator.uploadedMemos.last().id == movedMemoId
        }
        val uploaded = syncCoordinator.uploadedMemos.takeLast(3)
        assertTrue(uploaded[0].displayOrder > uploaded[1].displayOrder)
        assertTrue(uploaded[1].displayOrder > uploaded[2].displayOrder)
        assertTrue(uploaded[2].id == movedMemoId)
        repository.dispose()
    }

    private fun waitUntil(timeoutMs: Long = 5_000L, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) {
                return
            }
            Thread.sleep(50L)
        }
        check(condition()) { "Condition was not met within ${timeoutMs}ms." }
    }
}

private class RecordingSyncCoordinator : MemoSyncCoordinator {
    val uploadedMemos = mutableListOf<Memo>()

    override fun currentEmail(): String = ""

    override fun start(
        onStatus: (String) -> Unit,
        onRemoteChange: () -> Unit,
    ) = Unit

    override fun signIn(
        email: String,
        password: String,
        onStatus: (String) -> Unit,
        onRemoteChange: () -> Unit,
    ) = Unit

    override fun createAccount(
        email: String,
        password: String,
        onStatus: (String) -> Unit,
        onRemoteChange: () -> Unit,
    ) = Unit

    override fun signOut(onStatus: (String) -> Unit) = Unit

    override fun uploadAll(memos: List<Memo>) {
        uploadedMemos += memos
    }

    override fun uploadDeletedMemo(memoId: String, deletedAtMs: Long) = Unit

    override fun stop() = Unit
}
