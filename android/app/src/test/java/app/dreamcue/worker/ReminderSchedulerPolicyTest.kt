package app.dreamcue.worker

import android.os.Build
import app.dreamcue.model.ReminderTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderSchedulerPolicyTest {
    @Test
    fun modernAndroidUsesIdleAwareAlarmDelivery() {
        assertEquals(
            ReminderAlarmDeliveryMode.SET_AND_ALLOW_WHILE_IDLE,
            ReminderAlarmPolicy.deliveryModeForSdk(Build.VERSION_CODES.M),
        )
    }

    @Test
    fun preMarshmallowAndroidFallsBackToRegularAlarmDelivery() {
        assertEquals(
            ReminderAlarmDeliveryMode.SET,
            ReminderAlarmPolicy.deliveryModeForSdk(Build.VERSION_CODES.LOLLIPOP_MR1),
        )
    }

    @Test
    fun nextTriggerUsesTodayWhenReminderIsStillAhead() {
        val now = localTimeMillis(hour = 8, minute = 30)
        val trigger = ReminderAlarmPolicy.nextTriggerAtMillis(
            reminderTime = ReminderTime(hour = 9, minute = 0),
            nowMillis = now,
        )

        assertEquals(localTimeMillis(hour = 9, minute = 0), trigger)
    }

    @Test
    fun nextTriggerMovesToTomorrowWhenReminderHasPassed() {
        val now = localTimeMillis(hour = 9, minute = 1)
        val trigger = ReminderAlarmPolicy.nextTriggerAtMillis(
            reminderTime = ReminderTime(hour = 9, minute = 0),
            nowMillis = now,
        )

        assertEquals(localTimeMillis(day = 2, hour = 9, minute = 0), trigger)
    }

    private fun localTimeMillis(day: Int = 1, hour: Int, minute: Int): Long {
        return java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, 2026)
            set(java.util.Calendar.MONTH, java.util.Calendar.JANUARY)
            set(java.util.Calendar.DAY_OF_MONTH, day)
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
