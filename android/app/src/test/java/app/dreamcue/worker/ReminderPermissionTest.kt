package app.dreamcue.worker

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Test

class ReminderPermissionTest {
    @Test
    fun manifestDoesNotRequestExactAlarmPermission() {
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertFalse(manifest.contains("SCHEDULE_EXACT_ALARM"))
    }
}
