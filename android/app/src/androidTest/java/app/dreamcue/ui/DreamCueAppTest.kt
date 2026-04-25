package app.dreamcue.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.dreamcue.model.ReminderTime
import org.junit.Rule
import org.junit.Test

class DreamCueAppTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun todayUsesBrandedVisualDetails() {
        var state by mutableStateOf(MainUiState())
        composeRule.setContent {
            DreamCueApp(
                state = state,
                onDraftChange = {},
                onAddMemo = {},
                onSelectScreen = { state = state.copy(selectedScreen = it) },
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

        composeRule.onNodeWithContentDescription("DreamCue menu").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Reminder notification").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("DreamCue leaf mark").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Capture cue leaf").assertIsDisplayed()
    }

    @Test
    fun bottomNavigationSwitchesToAccountSyncControls() {
        var state by mutableStateOf(MainUiState())
        composeRule.setContent {
            DreamCueApp(
                state = state,
                onDraftChange = {},
                onAddMemo = {},
                onSelectScreen = { state = state.copy(selectedScreen = it) },
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

        composeRule.onNodeWithText("Today").assertIsDisplayed()
        composeRule.onNodeWithText("Recall").assertIsDisplayed()
        composeRule.onNodeWithText("Archive").assertIsDisplayed()
        composeRule.onNodeWithText("Rhythm").assertIsDisplayed()
        composeRule.onNodeWithText("Account").assertIsDisplayed()

        composeRule.onNodeWithText("Account").performClick()
        composeRule.onNodeWithText("Sync Account").assertIsDisplayed()
        composeRule.onNodeWithText("Email").assertIsDisplayed()
        composeRule.onNodeWithText("Password").assertIsDisplayed()
        composeRule.onNodeWithText("Sign In").assertIsDisplayed()
    }

    @Test
    fun todayCaptureFlowSavesDraftFromFocusedSheet() {
        var state by mutableStateOf(MainUiState())
        var saved = false
        composeRule.setContent {
            DreamCueApp(
                state = state,
                onDraftChange = { state = state.copy(draft = it) },
                onAddMemo = {
                    saved = true
                    state = state.copy(draft = "")
                },
                onSelectScreen = { state = state.copy(selectedScreen = it) },
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

        composeRule.onNodeWithText("Capture a cue...").performClick()
        composeRule.onNodeWithText("New Cue").assertIsDisplayed()
        composeRule.onNodeWithText("Short memo").performTextInput("Design sync with Sarah")
        composeRule.onNodeWithText("Save Cue").performClick()

        assert(saved)
    }

    @Test
    fun rhythmUsesWheelStyleReminderTimePicker() {
        var state by mutableStateOf(MainUiState(selectedScreen = MemoScreen.REMINDER))
        var savedTime: ReminderTime? = null
        composeRule.setContent {
            DreamCueApp(
                state = state,
                onDraftChange = {},
                onAddMemo = {},
                onSelectScreen = { state = state.copy(selectedScreen = it) },
                onSearchQueryChange = {},
                onRunSearch = {},
                onClearMemo = {},
                onDetailDraftChange = {},
                onSaveReminderTime = { hour, minute ->
                    savedTime = ReminderTime(hour, minute)
                    state = state.copy(reminderTime = ReminderTime(hour, minute))
                },
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

        composeRule.onNodeWithText("Rhythm").assertIsDisplayed()
        composeRule.onNodeWithText("Change Time").performClick()
        composeRule.onNodeWithText("Choose Reminder Time").assertIsDisplayed()
        composeRule.onNodeWithText("21").assertIsDisplayed()
        composeRule.onNodeWithText("00").assertIsDisplayed()
        composeRule.onAllNodes(hasText("22"))[0].performClick()
        composeRule.onNodeWithText("Save Time").performClick()

        assert(savedTime == ReminderTime(hour = 22, minute = 0))
    }
}
