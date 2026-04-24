package com.example.memolog

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.memolog.ui.MainViewModel
import com.example.memolog.ui.MemoApp
import com.example.memolog.worker.NotificationHelper
import com.example.memolog.worker.ReminderScheduler

class MainActivity : ComponentActivity() {
    private var exactAlarmAccessPrompted = false

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper.ensureChannel(this)

        val repository = MemoRepository(applicationContext)
        ReminderScheduler.schedule(this, repository.reminderTime())
        requestNotificationPermissionIfNeeded()
        requestExactAlarmPermissionIfNeeded()

        setContent {
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModel.factory(repository),
            )

            MemoApp(
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
            )
        }
    }

    override fun onResume() {
        super.onResume()
        val repository = MemoRepository(applicationContext)
        ReminderScheduler.schedule(this, repository.reminderTime())
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun requestExactAlarmPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return
        }
        if (ReminderScheduler.canScheduleExactAlarms(this) || exactAlarmAccessPrompted) {
            return
        }

        exactAlarmAccessPrompted = true
        try {
            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                },
            )
        } catch (_: ActivityNotFoundException) {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$packageName"),
                ),
            )
        }
    }
}
