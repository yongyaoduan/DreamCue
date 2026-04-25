package app.dreamcue.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.swipeUp
import app.dreamcue.model.Memo
import app.dreamcue.model.MemoStatus
import app.dreamcue.model.ReminderTime
import app.dreamcue.model.SearchResult
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
                onReminderEnabledChange = {},
            )
        }

        assert(composeRule.onAllNodesWithText("DreamCue").fetchSemanticsNodes().isNotEmpty())
        assert(composeRule.onAllNodesWithText("Recall").fetchSemanticsNodes().isEmpty())
        assert(composeRule.onAllNodesWithContentDescription("DreamCue menu").fetchSemanticsNodes().isEmpty())
        assert(composeRule.onAllNodesWithContentDescription("Reminder notification").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithContentDescription("DreamCue leaf mark").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Capture cue leaf").assertIsDisplayed()
    }

    @Test
    fun everyPrimaryScreenShowsExpectedControls() {
        var state by mutableStateOf(
            MainUiState(
                currentMemos = listOf(activeMemo),
                historyMemos = listOf(archivedMemo),
            ),
        )
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
                onReminderEnabledChange = {},
            )
        }

        composeRule.onNodeWithText("Daily rhythm").assertIsDisplayed()
        composeRule.onNodeWithText("Active cues").assertIsDisplayed()
        composeRule.onNodeWithText("Active Cues").assertIsDisplayed()
        composeRule.onNodeWithText(activeMemo.content).assertIsDisplayed()

        composeRule.onNodeWithText("Archive").performClick()
        composeRule.onNodeWithText("Search archive").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Run archive search").assertIsDisplayed()
        assert(composeRule.onAllNodesWithText("Run Search").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText("All History").assertIsDisplayed()
        composeRule.onNodeWithText(archivedMemo.content).assertIsDisplayed()
        assert(composeRule.onAllNodesWithText("Restore").fetchSemanticsNodes().isEmpty())

        composeRule.onNodeWithText("Rhythm").performClick()
        composeRule.onNodeWithText("Daily reminder").assertIsDisplayed()
        composeRule.onNodeWithText("On").assertIsDisplayed()
        composeRule.onNodeWithText("21:00").assertIsDisplayed()
        composeRule.onNodeWithText("Change Time").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Permission health"))
        assert(composeRule.onAllNodesWithText("Quiet hours").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText("Permission health").assertIsDisplayed()
        composeRule.onNodeWithText("Exact alarms").assertIsDisplayed()
        composeRule.onNodeWithText("Notifications").assertIsDisplayed()

        composeRule.onNodeWithText("Account").performClick()
        composeRule.onNodeWithText("Sync Account").assertIsDisplayed()
        composeRule.onNodeWithText("Email").assertIsDisplayed()
        composeRule.onNodeWithText("Password").assertIsDisplayed()
        composeRule.onNodeWithText("Create").assertIsDisplayed()
        composeRule.onNodeWithText("Sign In").assertIsDisplayed()
        composeRule.onNodeWithText("Tenant-scoped privacy").assertIsDisplayed()
        composeRule.onNodeWithText("Cue sync stays private to the signed-in account.")
            .assertIsDisplayed()
        assert(composeRule.onAllNodesWithText("users/{uid}/memos").fetchSemanticsNodes().isEmpty())
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
                onReminderEnabledChange = {},
            )
        }

        composeRule.onNodeWithText("Today").assertIsDisplayed()
        assert(composeRule.onAllNodesWithText("Recall").fetchSemanticsNodes().isEmpty())
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
    fun connectedAccountPanelCompactsLongEmail() {
        val longEmail = "syncpeer1777086916@example.com"
        var state by mutableStateOf(
            MainUiState(
                selectedScreen = MemoScreen.ACCOUNT,
                syncEmail = longEmail,
                syncStatus = "Syncing as $longEmail.",
            ),
        )
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
                onReminderEnabledChange = {},
            )
        }

        composeRule.onNodeWithText("syncpeer1777...@example.com").assertIsDisplayed()
        composeRule.onNodeWithText("Realtime sync is active.").assertIsDisplayed()
        composeRule.onNodeWithText("Tenant-scoped").assertIsDisplayed()
        assert(composeRule.onAllNodesWithText(longEmail).fetchSemanticsNodes().isEmpty())
        assert(composeRule.onAllNodesWithText("Syncing as $longEmail.").fetchSemanticsNodes().isEmpty())
        assert(composeRule.onAllNodesWithText("Remote path").fetchSemanticsNodes().isEmpty())
        assert(composeRule.onAllNodesWithText("users/{uid}/memos").fetchSemanticsNodes().isEmpty())
        assert(composeRule.onAllNodesWithText("Pending uploads").fetchSemanticsNodes().isEmpty())
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
                onReminderEnabledChange = {},
            )
        }

        composeRule.onNodeWithText("Capture a cue...").performClick()
        composeRule.onNodeWithText("New Cue").assertIsDisplayed()
        composeRule.onNodeWithText("Short memo").performTextInput("Design sync with Sarah")
        composeRule.onNodeWithText("Save Cue").performClick()

        assert(saved)
    }

    @Test
    fun captureSheetShowsAllEntryControlsAndDismisses() {
        var state by mutableStateOf(MainUiState())
        var dismissed = false
        composeRule.setContent {
            DreamCueApp(
                state = state,
                onDraftChange = { state = state.copy(draft = it) },
                onAddMemo = {},
                onSelectScreen = { state = state.copy(selectedScreen = it) },
                onSearchQueryChange = {},
                onRunSearch = {},
                onClearMemo = {},
                onDetailDraftChange = {},
                onSaveReminderTime = { _, _ -> },
                onOpenMemoDetail = {},
                onDismissMemoDetail = { dismissed = true },
                onReopenMemo = {},
                onRequestDelete = {},
                onDismissDeleteRequest = {},
                onConfirmDelete = {},
                onSyncEmailChange = {},
                onSyncPasswordChange = {},
                onSignInSync = {},
                onCreateSyncAccount = {},
                onSignOutSync = {},
                onReminderEnabledChange = {},
            )
        }

        composeRule.onNodeWithText("Capture a cue...").performClick()
        composeRule.onNodeWithText("New Cue").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Close").assertIsDisplayed()
        composeRule.onNodeWithText("Short memo").assertIsDisplayed()
        composeRule.onNodeWithText("Private").assertIsDisplayed()
        composeRule.onNodeWithText("Stored locally").assertIsDisplayed()
        composeRule.onNodeWithText("300/300").assertIsDisplayed()
        assert(composeRule.onAllNodesWithText("Add details").fetchSemanticsNodes().isEmpty())
        assert(composeRule.onAllNodesWithText("Cue details").fetchSemanticsNodes().isEmpty())
        assert(composeRule.onAllNodesWithText("Save with context").fetchSemanticsNodes().isEmpty())
        assert(composeRule.onAllNodesWithText("Context and timing can stay optional.").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText("Save Cue").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Close").performClick()
        assert(composeRule.onAllNodesWithText("New Cue").fetchSemanticsNodes().isEmpty())
        assert(!dismissed)
    }

    @Test
    fun archiveSearchFlowShowsResultsAndDetailControls() {
        var state by mutableStateOf(
            MainUiState(
                selectedScreen = MemoScreen.HISTORY,
                historyMemos = listOf(olderArchivedMemo, archivedMemo),
            ),
        )
        composeRule.setContent {
            DreamCueApp(
                state = state,
                onDraftChange = {},
                onAddMemo = {},
                onSelectScreen = { state = state.copy(selectedScreen = it) },
                onSearchQueryChange = { state = state.copy(searchQuery = it) },
                onRunSearch = {
                    state = state.copy(
                        submittedSearchQuery = state.searchQuery,
                        searchResults = listOf(searchResult),
                    )
                },
                onClearMemo = {},
                onDetailDraftChange = { state = state.copy(detailDraft = it) },
                onSaveReminderTime = { _, _ -> },
                onOpenMemoDetail = {
                    state = state.copy(selectedMemo = it, detailDraft = it.content)
                },
                onDismissMemoDetail = {
                    state = state.copy(selectedMemo = null, detailDraft = "")
                },
                onReopenMemo = {},
                onRequestDelete = {},
                onDismissDeleteRequest = {},
                onConfirmDelete = {},
                onSyncEmailChange = {},
                onSyncPasswordChange = {},
                onSignInSync = {},
                onCreateSyncAccount = {},
                onSignOutSync = {},
                onReminderEnabledChange = {},
            )
        }

        composeRule.onNodeWithText("All History").assertIsDisplayed()
        composeRule.onNodeWithText(archivedMemo.content).assertIsDisplayed()
        composeRule.onNodeWithText(olderArchivedMemo.content).assertIsDisplayed()
        composeRule.onNodeWithText("Search archive").performTextInput("release")
        composeRule.onNodeWithContentDescription("Run archive search").performClick()
        composeRule.onNodeWithText("Search Results").assertIsDisplayed()
        composeRule.onNodeWithText("1 archived cue").assertIsDisplayed()
        assert(composeRule.onAllNodesWithText(archivedMemo.content).fetchSemanticsNodes().isNotEmpty())
        assert(composeRule.onAllNodesWithText("title").fetchSemanticsNodes().isEmpty())

        composeRule.onAllNodesWithText(archivedMemo.content)[0].performClick()
        composeRule.onNodeWithText("Archived Cue").assertIsDisplayed()
        composeRule.onNodeWithText("Cue").assertIsDisplayed()
        assert(composeRule.onAllNodesWithText("Favorite").fetchSemanticsNodes().isEmpty())
        assert(composeRule.onAllNodesWithText("More").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText("Created").assertIsDisplayed()
        composeRule.onNodeWithText("Last updated").assertIsDisplayed()
        composeRule.onNodeWithText("Last reminded").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Delete cue").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Keep cue").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Restore cue").assertIsDisplayed()
    }

    @Test
    fun archiveSearchRunsFromKeyboardSearchAction() {
        var state by mutableStateOf(
            MainUiState(
                selectedScreen = MemoScreen.HISTORY,
                historyMemos = listOf(archivedMemo),
            ),
        )
        var searchSubmitted = false
        composeRule.setContent {
            DreamCueApp(
                state = state,
                onDraftChange = {},
                onAddMemo = {},
                onSelectScreen = { state = state.copy(selectedScreen = it) },
                onSearchQueryChange = { state = state.copy(searchQuery = it) },
                onRunSearch = {
                    searchSubmitted = true
                    state = state.copy(
                        submittedSearchQuery = state.searchQuery,
                        searchResults = listOf(searchResult),
                    )
                },
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
                onReminderEnabledChange = {},
            )
        }

        composeRule.onNodeWithText("Search archive").performTextInput("release")
        composeRule.onNodeWithText("Search archive").performImeAction()

        assert(searchSubmitted)
        composeRule.onNodeWithText("Search Results").assertIsDisplayed()
    }

    @Test
    fun archiveDetailRestoresHistoricalMemo() {
        var state by mutableStateOf(
            MainUiState(
                selectedScreen = MemoScreen.HISTORY,
                historyMemos = listOf(archivedMemo),
            ),
        )
        var restoredMemoId = ""
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
                onOpenMemoDetail = {
                    state = state.copy(selectedMemo = it, detailDraft = it.content)
                },
                onDismissMemoDetail = {},
                onReopenMemo = { restoredMemoId = it },
                onRequestDelete = {},
                onDismissDeleteRequest = {},
                onConfirmDelete = {},
                onSyncEmailChange = {},
                onSyncPasswordChange = {},
                onSignInSync = {},
                onCreateSyncAccount = {},
                onSignOutSync = {},
                onReminderEnabledChange = {},
            )
        }

        composeRule.onNodeWithText(archivedMemo.content).performClick()
        composeRule.onNodeWithText("Archived Cue").assertIsDisplayed()
        composeRule.onNodeWithText("Completed").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Keep cue").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Restore cue").performClick()

        assert(restoredMemoId == archivedMemo.id)
    }

    @Test
    fun deleteConfirmationSupportsCancelAndConfirm() {
        var state by mutableStateOf(
            MainUiState(
                currentMemos = listOf(activeMemo),
                selectedMemo = activeMemo,
                detailDraft = activeMemo.content,
            ),
        )
        var confirmed = false
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
                onRequestDelete = {
                    state = state.copy(selectedMemo = null, pendingDeleteMemo = it)
                },
                onDismissDeleteRequest = {
                    state = state.copy(pendingDeleteMemo = null, selectedMemo = activeMemo)
                },
                onConfirmDelete = {
                    confirmed = true
                    state = state.copy(pendingDeleteMemo = null)
                },
                onSyncEmailChange = {},
                onSyncPasswordChange = {},
                onSignInSync = {},
                onCreateSyncAccount = {},
                onSignOutSync = {},
                onReminderEnabledChange = {},
            )
        }

        composeRule.onNodeWithContentDescription("Delete cue").performClick()
        composeRule.onNodeWithText("Delete this memo?").assertIsDisplayed()
        composeRule.onNodeWithText("This cue and its event history will be permanently removed from local storage and remote sync.")
            .assertIsDisplayed()
        assert(composeRule.onAllNodesWithText(activeMemo.content).fetchSemanticsNodes().isNotEmpty())
        composeRule.onNodeWithText("Cancel").performClick()
        assert(composeRule.onAllNodesWithText("Delete this memo?").fetchSemanticsNodes().isEmpty())
        assert(!confirmed)

        composeRule.onNodeWithContentDescription("Delete cue").performClick()
        composeRule.onNodeWithText("Delete this memo?").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").performClick()
        assert(confirmed)
    }

    @Test
    fun accountButtonsSubmitTypedCredentials() {
        var state by mutableStateOf(MainUiState(selectedScreen = MemoScreen.ACCOUNT))
        var created = false
        var signedIn = false
        composeRule.setContent {
            DreamCueApp(
                state = state,
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
                onSyncEmailChange = { state = state.copy(syncEmail = it) },
                onSyncPasswordChange = { state = state.copy(syncPassword = it) },
                onSignInSync = { signedIn = true },
                onCreateSyncAccount = { created = true },
                onSignOutSync = {},
                onReminderEnabledChange = {},
            )
        }

        composeRule.onNodeWithText("Email").performTextInput("qa@example.com")
        composeRule.onNodeWithText("Password").performTextInput("password123")
        composeRule.onNodeWithText("Create").performClick()
        composeRule.onNodeWithText("Sign In").performClick()

        assert(created)
        assert(signedIn)
    }

    @Test
    fun signedInAccountShowsTenantScopedSyncElements() {
        var signedOut = false
        val email = "qa@example.com"
        composeRule.setContent {
            DreamCueApp(
                state = MainUiState(
                    selectedScreen = MemoScreen.ACCOUNT,
                    syncEmail = email,
                    syncStatus = "Syncing as $email.",
                ),
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
                onSignOutSync = { signedOut = true },
                onReminderEnabledChange = {},
            )
        }

        composeRule.onNodeWithText("Private sync is active.").assertIsDisplayed()
        composeRule.onNodeWithText("Tenant-scoped").assertIsDisplayed()
        composeRule.onNodeWithText(email).assertIsDisplayed()
        composeRule.onNodeWithText("Last sync").assertIsDisplayed()
        composeRule.onNodeWithText("Cue changes stay private to this account and sync automatically.")
            .assertIsDisplayed()
        assert(composeRule.onAllNodesWithText("Remote path").fetchSemanticsNodes().isEmpty())
        assert(composeRule.onAllNodesWithText("users/{uid}/memos").fetchSemanticsNodes().isEmpty())
        assert(composeRule.onAllNodesWithText("Pending uploads").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText("Sign Out").performClick()

        assert(signedOut)
    }

    @Test
    fun rhythmUsesWheelStyleReminderTimePicker() {
        var state by mutableStateOf(MainUiState(selectedScreen = MemoScreen.REMINDER))
        var savedTime: ReminderTime? = null
        var reminderEnabled = true
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
                onReminderEnabledChange = { enabled ->
                    reminderEnabled = enabled
                    state = state.copy(reminderEnabled = enabled)
                },
            )
        }

        composeRule.onNodeWithText("Rhythm").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Daily reminder switch").performClick()
        assert(!reminderEnabled)
        composeRule.onNodeWithContentDescription("Daily reminder switch").performClick()
        assert(reminderEnabled)
        composeRule.onNodeWithText("Change Time").performClick()
        composeRule.onNodeWithText("Choose Reminder Time").assertIsDisplayed()
        composeRule.onNodeWithText("21").assertIsDisplayed()
        composeRule.onNodeWithText("00").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Hour picker").performTouchInput {
            swipeUp()
        }
        composeRule.onNodeWithText("Save Time").performClick()

        assert(savedTime == ReminderTime(hour = 22, minute = 0))
    }

    private companion object {
        val activeMemo = Memo(
            id = "active-1",
            content = "Plan Android sync QA",
            status = MemoStatus.ACTIVE,
            createdAtMs = 1_700_000_000_000,
            updatedAtMs = 1_700_000_060_000,
            clearedAtMs = null,
            reminderCount = 1,
            lastReviewedAtMs = null,
        )

        val archivedMemo = Memo(
            id = "archived-1",
            content = "Ship release notes",
            status = MemoStatus.CLEARED,
            createdAtMs = 1_700_000_000_000,
            updatedAtMs = 1_700_000_120_000,
            clearedAtMs = 1_700_000_120_000,
            reminderCount = 2,
            lastReviewedAtMs = 1_700_000_120_000,
        )

        val olderArchivedMemo = Memo(
            id = "archived-0",
            content = "Book dentist appointment",
            status = MemoStatus.CLEARED,
            createdAtMs = 1_700_000_000_000,
            updatedAtMs = 1_700_000_010_000,
            clearedAtMs = 1_700_000_010_000,
            reminderCount = 1,
            lastReviewedAtMs = 1_700_000_010_000,
        )

        val searchResult = SearchResult(
            memo = archivedMemo,
            score = 0.91,
            matchedBy = listOf("title"),
        )
    }
}
