package app.dreamcue.worker

import android.os.Build
import app.dreamcue.model.ReminderTime
import java.util.Calendar

enum class ReminderAlarmDeliveryMode {
    SET,
    SET_AND_ALLOW_WHILE_IDLE,
}

object ReminderAlarmPolicy {
    fun deliveryModeForSdk(sdkInt: Int): ReminderAlarmDeliveryMode {
        return if (sdkInt >= Build.VERSION_CODES.M) {
            ReminderAlarmDeliveryMode.SET_AND_ALLOW_WHILE_IDLE
        } else {
            ReminderAlarmDeliveryMode.SET
        }
    }

    fun nextTriggerAtMillis(
        reminderTime: ReminderTime,
        nowMillis: Long = System.currentTimeMillis(),
    ): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, reminderTime.hour)
            set(Calendar.MINUTE, reminderTime.minute)
        }
        if (calendar.timeInMillis <= nowMillis) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        return calendar.timeInMillis
    }
}
