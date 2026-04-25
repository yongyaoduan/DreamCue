package app.dreamcue.worker

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.dreamcue.MainActivity
import app.dreamcue.model.Memo
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotificationHelper {
    private const val CHANNEL_ID = "daily_memo_notification_v1"
    private const val CHANNEL_NAME = "Memo Reminder"
    private const val SUMMARY_NOTIFICATION_ID = 3402
    private const val TAG = "NotificationHelper"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Daily reminder for each memo that still needs attention"
            enableLights(true)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            setSound(
                sound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
        manager.createNotificationChannel(channel)
    }

    fun showMemoReminders(context: Context, memos: List<Memo>) {
        ensureChannel(context)
        Log.d(TAG, "showMemoReminders count=${memos.size}")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Skipping notifications because POST_NOTIFICATIONS is not granted")
            return
        }

        val plan = ReminderNotificationPlan.fromMemos(memos)
        plan.individualMemos.forEach { memo ->
            Log.d(TAG, "Posting reminder for memo=${memo.id}")
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("memo_id", memo.id)
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationIdForMemo(memo.id),
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Pinned Cue")
                .setContentText(memo.content)
                .setSubText("Added ${formatTimestamp(memo.createdAtMs)}")
                .setStyle(NotificationCompat.BigTextStyle().bigText(memo.content))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(false)
                .setOnlyAlertOnce(false)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(context).notify(notificationIdForMemo(memo.id), notification)
        }
        if (plan.summaryCount > 0) {
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                SUMMARY_NOTIFICATION_ID,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("DreamCue")
                .setContentText(plan.summaryText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(false)
                .setOnlyAlertOnce(false)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(context).notify(SUMMARY_NOTIFICATION_ID, notification)
        }
    }

    fun cancelMemoReminder(context: Context, memoId: String) {
        NotificationManagerCompat.from(context).cancel(notificationIdForMemo(memoId))
    }

    private fun notificationIdForMemo(memoId: String): Int {
        return memoId.hashCode().let { if (it == Int.MIN_VALUE) 1 else kotlin.math.abs(it) }
    }

    private fun formatTimestamp(timestampMs: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return formatter.format(Date(timestampMs))
    }
}
