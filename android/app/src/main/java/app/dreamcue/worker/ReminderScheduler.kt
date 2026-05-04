package app.dreamcue.worker

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import app.dreamcue.DreamCueRepository
import app.dreamcue.model.ReminderTime

object ReminderScheduler {
    private const val REQUEST_CODE_DAILY_REVIEW = 3401

    fun schedule(context: Context, reminderTime: ReminderTime? = null) {
        val appContext = context.applicationContext
        val time = reminderTime ?: DreamCueRepository(appContext).reminderTime()
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = reviewPendingIntent(appContext)
        val triggerAtMillis = ReminderAlarmPolicy.nextTriggerAtMillis(time)

        when (ReminderAlarmPolicy.deliveryModeForSdk(Build.VERSION.SDK_INT)) {
            ReminderAlarmDeliveryMode.SET_AND_ALLOW_WHILE_IDLE -> alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
            ReminderAlarmDeliveryMode.SET -> alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        }
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

}
