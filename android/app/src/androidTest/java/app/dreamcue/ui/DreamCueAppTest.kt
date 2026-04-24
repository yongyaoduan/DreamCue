package app.dreamcue.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class DreamCueAppTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsShowsTenantScopedSyncControls() {
        composeRule.setContent {
            DreamCueApp(
                state = MainUiState(selectedScreen = MemoScreen.REMINDER),
                onDraftChange = {},
                onAddMemo = {},
                onSelectScreen = {},
                onSearchQueryChange = {},
                onRunSearch = {},
                onClearMemo = {},
                onDetailDraftChange = {},
                onSaveReminderTime = { _, _ -> },
                onOpenMemoDetail = {},
                onDismissMemoDetail = {},
                onReopenMemo = {},
                onRequestDelete = {},
                onDismissDeleteRequest = {},
                onConfirmDelete = {},
                onSyncEmailChange = {},
                onSyncPasswordChange = {},
                onSignInSync = {},
                onCreateSyncAccount = {},
                onSignOutSync = {},
            )
        }

        composeRule.onNodeWithText("Sync Account").assertIsDisplayed()
        composeRule.onNodeWithText("Email").assertIsDisplayed()
        composeRule.onNodeWithText("Password").assertIsDisplayed()
        composeRule.onNodeWithText("Sign In").assertIsDisplayed()
    }
}
