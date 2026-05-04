package app.dreamcue

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build

object NotificationPermissionPolicy {
    const val requestCode = 3403
    val permission: String = Manifest.permission.POST_NOTIFICATIONS

    fun shouldRequestNotificationPermission(
        sdkInt: Int,
        permissionState: Int,
    ): Boolean {
        return sdkInt >= Build.VERSION_CODES.TIRAMISU &&
            permissionState != PackageManager.PERMISSION_GRANTED
    }

    fun requestIfNeeded(activity: Activity) {
        val permissionState = activity.checkSelfPermission(permission)
        if (shouldRequestNotificationPermission(Build.VERSION.SDK_INT, permissionState)) {
            activity.requestPermissions(arrayOf(permission), requestCode)
        }
    }
}
