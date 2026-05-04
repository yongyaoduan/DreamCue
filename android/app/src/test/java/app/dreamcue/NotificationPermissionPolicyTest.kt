package app.dreamcue

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPermissionPolicyTest {
    @Test
    fun androidThirteenAndNewerRequestsMissingNotificationPermission() {
        assertTrue(
            NotificationPermissionPolicy.shouldRequestNotificationPermission(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                permissionState = PackageManager.PERMISSION_DENIED,
            ),
        )
    }

    @Test
    fun androidThirteenAndNewerDoesNotRequestGrantedNotificationPermission() {
        assertFalse(
            NotificationPermissionPolicy.shouldRequestNotificationPermission(
                sdkInt = Build.VERSION_CODES.TIRAMISU,
                permissionState = PackageManager.PERMISSION_GRANTED,
            ),
        )
    }

    @Test
    fun olderAndroidDoesNotNeedRuntimeNotificationPermission() {
        assertFalse(
            NotificationPermissionPolicy.shouldRequestNotificationPermission(
                sdkInt = Build.VERSION_CODES.S_V2,
                permissionState = PackageManager.PERMISSION_DENIED,
            ),
        )
    }

    @Test
    fun permissionNameMatchesAndroidRuntimePermission() {
        assertTrue(NotificationPermissionPolicy.permission == Manifest.permission.POST_NOTIFICATIONS)
    }
}
