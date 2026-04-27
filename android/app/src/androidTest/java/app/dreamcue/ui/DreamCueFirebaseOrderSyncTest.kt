package app.dreamcue.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import app.dreamcue.BuildConfig
import app.dreamcue.MainActivity
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.net.URL
import java.util.concurrent.TimeUnit

class DreamCueFirebaseOrderSyncTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun uploadsDraggedOrderToFirebaseWhenConfigured() {
        assumeTrue(argument("dreamcueOrderSyncMode") == "upload")
        val email = requiredArgument("dreamcueOrderSyncEmail")
        val password = requiredArgument("dreamcueOrderSyncPassword")
        val prefix = "${requiredArgument("dreamcueOrderSyncPrefix")}_${System.currentTimeMillis()}"
        val first = "${prefix}_first"
        val second = "${prefix}_second"
        val third = "${prefix}_third"

        signIn(email, password)
        createCue(first)
        createCue(second)
        createCue(third)

        waitForVisibleOrder(listOf(third, second, first))
        dragCueDown(third)
        waitForVisibleOrder(listOf(second, first, third))
        waitForRemoteOrder(prefix, listOf(second, first, third))
        takeDeviceScreenshotIfRequested()
    }

    @Test
    fun pullsRemoteOrderIntoAndroidUiWhenConfigured() {
        assumeTrue(argument("dreamcueOrderSyncMode") == "pull")
        val email = requiredArgument("dreamcueOrderSyncEmail")
        val password = requiredArgument("dreamcueOrderSyncPassword")
        val expectedOrder = requiredArgument("dreamcueExpectedOrder").split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        signIn(email, password)
        composeRule.onNodeWithContentDescription("Today").performClick()
        waitForVisibleOrder(expectedOrder)
        takeDeviceScreenshotIfRequested()
    }

    private fun signIn(email: String, password: String) {
        composeRule.onNodeWithContentDescription("Account").performClick()
        composeRule.onNodeWithText("Email").performTextInput(email)
        composeRule.onNodeWithText("Password").performTextInput(password)
        composeRule.onNodeWithText("Sign In").performClick()
        composeRule.waitUntil(timeoutMillis = 20_000) {
            composeRule.onAllNodesWithText("Realtime sync is active.").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun createCue(content: String) {
        composeRule.onNodeWithContentDescription("Today").performClick()
        composeRule.onNodeWithContentDescription("New Cue").performClick()
        composeRule.onNodeWithText("Cue").performTextInput(content)
        composeRule.onNodeWithText("Save").performClick()
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(content).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun dragCueDown(content: String) {
        composeRule.onNodeWithText(content)
            .performTouchInput {
                down(center)
                advanceEventTime(800)
                moveBy(Offset(0f, 210f))
                up()
            }
    }

    private fun waitForVisibleOrder(expectedOrder: List<String>) {
        composeRule.waitUntil(timeoutMillis = 30_000) {
            expectedOrder.all { content ->
                composeRule.onAllNodesWithText(content).fetchSemanticsNodes().isNotEmpty()
            } && isVisibleOrder(expectedOrder)
        }
        expectedOrder.forEach { composeRule.onNodeWithText(it).assertIsDisplayed() }
    }

    private fun isVisibleOrder(expectedOrder: List<String>): Boolean {
        val tops = expectedOrder.map { content ->
            composeRule.onAllNodesWithText(content).fetchSemanticsNodes().firstOrNull()?.boundsInRoot?.top
                ?: return false
        }
        return tops.zipWithNext().all { (before, after) -> before < after }
    }

    private fun waitForRemoteOrder(prefix: String, expectedOrder: List<String>) {
        composeRule.waitUntil(timeoutMillis = 30_000) {
            runCatching { remoteOrder(prefix) == expectedOrder }.getOrDefault(false)
        }
    }

    private fun remoteOrder(prefix: String): List<String> {
        val user = FirebaseAuth.getInstance().currentUser ?: return emptyList()
        val token = Tasks.await(
            user.getIdToken(false),
            10,
            TimeUnit.SECONDS,
        ).token ?: return emptyList()
        val databaseUrl = BuildConfig.FIREBASE_DATABASE_URL.trim().removeSuffix("/")
        if (databaseUrl.isEmpty()) return emptyList()

        val raw = URL("$databaseUrl/users/${user.uid}/memos.json?auth=$token").readText()
        if (raw == "null") return emptyList()
        val json = JSONObject(raw)
        return json.keys().asSequence().mapNotNull { key ->
            val values = json.optJSONObject(key) ?: return@mapNotNull null
            val content = values.optString("content", "")
            if (!content.startsWith(prefix)) return@mapNotNull null
            if (values.optBoolean("deleted", false)) return@mapNotNull null
            if (values.optString("status") != "active") return@mapNotNull null
            val order = values.optLong("display_order", 0L)
            val pinned = values.optBoolean("pinned", false)
            RemoteOrder(content, order, pinned)
        }
            .toList()
            .sortedWith(
                compareByDescending<RemoteOrder> { it.pinned }
                    .thenByDescending { it.displayOrder },
            )
            .map { it.content }
    }

    private fun argument(name: String): String? {
        return InstrumentationRegistry.getArguments().getString(name)
    }

    private fun takeDeviceScreenshotIfRequested() {
        val name = argument("dreamcueScreenshotName") ?: return
        composeRule.waitForIdle()
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.waitForIdle()
        Thread.sleep(300)
        val safeName = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val file = File("/sdcard/Download/$safeName.png")
        device.takeScreenshot(file)
    }

    private fun requiredArgument(name: String): String {
        return requireNotNull(argument(name)) { "$name is required." }
    }

    private data class RemoteOrder(
        val content: String,
        val displayOrder: Long,
        val pinned: Boolean,
    )
}
