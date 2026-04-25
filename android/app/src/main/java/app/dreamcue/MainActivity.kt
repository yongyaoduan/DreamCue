package app.dreamcue

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import app.dreamcue.ui.DreamCueViewModel
import app.dreamcue.ui.DreamCueApp
import app.dreamcue.worker.NotificationHelper
import app.dreamcue.worker.ReminderScheduler

class MainActivity : ComponentActivity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = true

        NotificationHelper.ensureChannel(this)

        val repository = DreamCueRepository(applicationContext)
        ReminderScheduler.schedule(this, repository.reminderTime())

        setContent {
            val viewModel: DreamCueViewModel = viewModel(
                factory = DreamCueViewModel.factory(repository),
            )

            DreamCueApp(
                state = viewModel.uiState,
                onDraftChange = viewModel::updateDraft,
                onAddMemo = viewModel::addMemo,
                onSelectScreen = viewModel::selectScreen,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onRunSearch = viewModel::search,
                onClearMemo = viewModel::clearMemo,
                onDetailDraftChange = viewModel::updateDetailDraft,
                onSaveReminderTime = viewModel::saveReminderTime,
                onOpenMemoDetail = viewModel::openMemoDetail,
                onDismissMemoDetail = viewModel::dismissMemoDetail,
                onReopenMemo = viewModel::reopenMemo,
                onRequestDelete = viewModel::requestDelete,
                onDismissDeleteRequest = viewModel::dismissDeleteRequest,
                onConfirmDelete = viewModel::deleteMemo,
                onSyncEmailChange = viewModel::updateSyncEmail,
                onSyncPasswordChange = viewModel::updateSyncPassword,
                onSignInSync = viewModel::signInSync,
                onCreateSyncAccount = viewModel::createSyncAccount,
                onSignOutSync = viewModel::signOutSync,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        val repository = DreamCueRepository(applicationContext)
        ReminderScheduler.schedule(this, repository.reminderTime())
    }
}
