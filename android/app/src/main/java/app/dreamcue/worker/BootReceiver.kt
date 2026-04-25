package app.dreamcue.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.dreamcue.DreamCueRepository

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val appContext = context.applicationContext
        val repository = DreamCueRepository(appContext)
        if (repository.reminderEnabled()) {
            ReminderScheduler.schedule(appContext)
        } else {
            ReminderScheduler.cancel(appContext)
        }
    }
}
