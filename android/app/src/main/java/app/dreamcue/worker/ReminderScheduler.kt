package app.dreamcue.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import app.dreamcue.DreamCueRepository
import app.dreamcue.model.ReminderTime
import java.util.Calendar

object ReminderScheduler {
    private const val REQUEST_CODE_DAILY_REVIEW = 3401

    fun schedule(context: Context, reminderTime: ReminderTime? = null) {
        val appContext = context.applicationContext
        val time = reminderTime ?: DreamCueRepository(appContext).reminderTime()
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = reviewPendingIntent(appContext)
        val triggerAtMillis = nextTriggerAtMillis(time)

        alarmManager.set(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent,
        )
    }

    fun cancel(context: Context) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(reviewPendingIntent(appContext))
    }

    private fun reviewPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, DailyReviewReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_DAILY_REVIEW,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun nextTriggerAtMillis(reminderTime: ReminderTime): Long {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, reminderTime.hour)
            set(Calendar.MINUTE, reminderTime.minute)
        }
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }
}
