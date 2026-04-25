package app.dreamcue.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import app.dreamcue.DreamCueRepository

class DailyReviewReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "DailyReviewReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val appContext = context.applicationContext
        val pendingResult = goAsync()

        Thread {
            val repository = DreamCueRepository(appContext)
            val reminderEnabled = repository.reminderEnabled()
            try {
                if (!reminderEnabled) {
                    ReminderScheduler.cancel(appContext)
                    return@Thread
                }
                Log.d(TAG, "Received reminder broadcast: action=${intent?.action}")
                val initialized = repository.initialize()
                if (initialized.isSuccess) {
                    val snapshot = runCatching { repository.reviewSnapshot() }.getOrNull()
                    Log.d(TAG, "Snapshot dueCount=${snapshot?.dueCount ?: -1}")
                    if (snapshot != null && snapshot.dueCount > 0) {
                        NotificationHelper.showMemoReminders(appContext, snapshot.due)
                    }
                } else {
                    Log.w(TAG, "Repository init failed", initialized.exceptionOrNull())
                }
            } finally {
                val reminderTime = repository.reminderTime()
                repository.dispose()
                if (reminderEnabled) {
                    ReminderScheduler.schedule(appContext, reminderTime)
                } else {
                    ReminderScheduler.cancel(appContext)
                }
                pendingResult.finish()
            }
        }.start()
    }
}
