package com.example.memolog.worker

import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.AlarmManagerCompat
import com.example.memolog.MainActivity
import com.example.memolog.MemoRepository
import com.example.memolog.model.ReminderTime
import java.util.Calendar

object ReminderScheduler {
    private const val REQUEST_CODE_DAILY_REVIEW = 3401

    fun schedule(context: Context, reminderTime: ReminderTime? = null) {
        val appContext = context.applicationContext
        val time = reminderTime ?: MemoRepository(appContext).reminderTime()
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = reviewPendingIntent(appContext)
        val triggerAtMillis = nextTriggerAtMillis(time)

        if (canScheduleExactAlarms(appContext)) {
            val showIntent = PendingIntent.getActivity(
                appContext,
                REQUEST_CODE_DAILY_REVIEW + 100,
                Intent(appContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            alarmManager.setAlarmClock(
                AlarmClockInfo(triggerAtMillis, showIntent),
                pendingIntent,
            )
        } else {
            AlarmManagerCompat.setAndAllowWhileIdle(
                alarmManager,
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

    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
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
