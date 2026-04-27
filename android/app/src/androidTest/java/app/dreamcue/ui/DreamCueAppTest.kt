package app.dreamcue.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.geometry.Offset
import app.dreamcue.model.Memo
import app.dreamcue.model.MemoStatus
import app.dreamcue.model.ReminderTime
import app.dreamcue.model.SearchResult
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.Calendar
import kotlin.math.roundToInt

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

        composeRule.onNodeWithText(timeOfDayGreeting(Calendar.getInstance())).assertIsDisplayed()
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
                searchResults = listOf(SearchResult(activeMemo, 0.0, listOf("recent"))),
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
        composeRule.onNodeWithText("#01").assertIsDisplayed()
        composeRule.onNodeWithText(activeMemo.content).assertIsDisplayed()

        composeRule.onNodeWithText("Archive").performClick()
        composeRule.onNodeWithText("Search archive").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Run archive search").assertIsDisplayed()
        assert(composeRule.onAllNodesWithText("Run Search").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText("All History").assertIsDisplayed()
        composeRule.onNodeWithText(archivedMemo.content).assertIsDisplayed()
        composeRule.onNodeWithText(activeMemo.content).assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Current"))
        composeRule.onNodeWithText("Current").assertIsDisplayed()
        assert(composeRule.onAllNodesWithText("Restore").fetchSemanticsNodes().isEmpty())

        composeRule.onNodeWithText("Rhythm").performClick()
        composeRule.onNodeWithText("Daily reminder").assertIsDisplayed()
        composeRule.onNodeWithText("On").assertIsDisplayed()
        composeRule.onNodeWithText("21:00").assertIsDisplayed()
        composeRule.onNodeWithText("Change Time").assertIsDisplayed()
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText("Permission health"))
        assert(composeRule.onAllNodesWithText("Quiet hours").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText("Permission health").assertIsDisplayed()
        composeRule.onNodeWithText("Notifications").assertIsDisplayed()
        assert(composeRule.onAllNodesWithText("Exact alarms").fetchSemanticsNodes().isEmpty())

        composeRule.onNodeWithText("Account").performClick()
        composeRule.onNodeWithText("Sync Account").assertIsDisplayed()
        composeRule.onNodeWithText("Email").assertIsDisplayed()
        composeRule.onNodeWithText("Password").assertIsDisplayed()
        composeRule.onNodeWithText("Create").assertIsDisplayed()
        composeRule.onNodeWithText("Sign In").assertIsDisplayed()
        composeRule.onNodeWithText("Private sync").assertIsDisplayed()
        composeRule.onNodeWithText("Only your signed-in account can access synced cues.")
            .assertIsDisplayed()
        assert(composeRule.onAllNodesWithText("users/{uid}/memos").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun draggingTodayCueReordersRowsAndRenumbersThem() {
        var reordered: Pair<Int, Int>? = null
        var state by mutableStateOf(
            MainUiState(
                currentMemos = listOf(activeMemo, secondActiveMemo, thirdActiveMemo),
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
                onReorderCurrentMemos = { from, to ->
                    reordered = from to to
                    val current = state.currentMemos.toMutableList()
                    val moved = current.removeAt(from)
                    current.add(to, moved)
                    state = state.copy(currentMemos = current)
                },
            )
        }

        composeRule.onNodeWithTag("currentCueNumber.${activeMemo.id}", useUnmergedTree = true).assertTextEquals("#01")
        composeRule.onNodeWithTag("currentCueNumber.${secondActiveMemo.id}", useUnmergedTree = true).assertTextEquals("#02")
        composeRule.onNodeWithTag("currentCueNumber.${thirdActiveMemo.id}", useUnmergedTree = true).assertTextEquals("#03")

        composeRule.onNodeWithTag("currentCue.${activeMemo.id}")
            .performTouchInput {
                down(center)
                advanceEventTime(700)
                moveBy(Offset(0f, 190f))
                up()
            }
        composeRule.waitForIdle()

        assert(reordered == (0 to 2))
        composeRule.onNodeWithTag("currentCueNumber.${secondActiveMemo.id}", useUnmergedTree = true).assertTextEquals("#01")
        composeRule.onNodeWithTag("currentCueNumber.${thirdActiveMemo.id}", useUnmergedTree = true).assertTextEquals("#02")
        composeRule.onNodeWithTag("currentCueNumber.${activeMemo.id}", useUnmergedTree = true).assertTextEquals("#03")
        val secondTop = composeRule.onNodeWithText(secondActiveMemo.content).fetchSemanticsNode().boundsInRoot.top
        val activeTop = composeRule.onNodeWithText(activeMemo.content).fetchSemanticsNode().boundsInRoot.top
        assert(secondTop < activeTop)
    }

    @Test
    fun draggingTodayCueFollowsFingerBeforeRelease() {
        var state by mutableStateOf(
            MainUiState(
                currentMemos = listOf(activeMemo, secondActiveMemo, thirdActiveMemo),
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
                onReorderCurrentMemos = { _, _ -> },
            )
        }

        val cue = composeRule.onNodeWithTag("currentCue.${activeMemo.id}")
        cue.performTouchInput {
            down(center)
            advanceEventTime(700)
            moveBy(Offset(0f, 140f))
        }
        composeRule.waitForIdle()

        val draggingOffset = cue.fetchSemanticsNode().config[CueDragOffsetYKey]
        val isDragging = cue.fetchSemanticsNode().config[CueDraggingKey]
        assert(draggingOffset > 90f)
        assert(isDragging)
        takeDeviceScreenshotIfRequested()

        cue.performTouchInput {
            up()
        }
    }

    @Test
    fun draggingTodayCueDisplacesOtherRowsBeforeReleaseWithoutCommittingOrder() {
        var reordered: Pair<Int, Int>? = null
        var state by mutableStateOf(
            MainUiState(
                currentMemos = listOf(activeMemo, secondActiveMemo, thirdActiveMemo),
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
                onReorderCurrentMemos = { from, to ->
                    reordered = from to to
                    val current = state.currentMemos.toMutableList()
                    val moved = current.removeAt(from)
                    current.add(to, moved)
                    state = state.copy(currentMemos = current)
                },
            )
        }

        val cue = composeRule.onNodeWithTag("currentCue.${activeMemo.id}")
        cue.performTouchInput {
            down(center)
            advanceEventTime(700)
            moveBy(Offset(0f, 280f))
        }
        composeRule.waitForIdle()

        assert(reordered == null)
        val secondAvoidanceOffset = composeRule
            .onNodeWithTag("currentCue.${secondActiveMemo.id}")
            .fetchSemanticsNode()
            .config[CueAvoidanceOffsetYKey]
        assert(secondAvoidanceOffset < -40f)
        composeRule.onNodeWithTag("currentCueNumber.${activeMemo.id}", useUnmergedTree = true).assertTextEquals("#01")
        composeRule.onNodeWithTag("currentCueNumber.${secondActiveMemo.id}", useUnmergedTree = true).assertTextEquals("#02")
        composeRule.onNodeWithTag("currentCueNumber.${thirdActiveMemo.id}", useUnmergedTree = true).assertTextEquals("#03")
        takeDeviceScreenshotIfRequested()

        cue.performTouchInput {
            up()
        }
        composeRule.waitForIdle()
        assert(reordered == (0 to 2))
    }

    @Test
    fun immediateSwipeOnTodayCueDoesNotStartDragReorder() {
        var reordered: Pair<Int, Int>? = null
        var state by mutableStateOf(
            MainUiState(
                currentMemos = listOf(activeMemo, secondActiveMemo, thirdActiveMemo),
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
                onReorderCurrentMemos = { from, to ->
                    reordered = from to to
                },
            )
        }

        composeRule.onNodeWithTag("currentCue.${activeMemo.id}")
            .performTouchInput {
                down(center)
                advanceEventTime(120)
                moveBy(Offset(0f, 190f))
                up()
            }
        composeRule.waitForIdle()

        assert(reordered == null)
        composeRule.onNodeWithTag("currentCueNumber.${activeMemo.id}", useUnmergedTree = true).assertTextEquals("#01")
        composeRule.onNodeWithTag("currentCueNumber.${secondActiveMemo.id}", useUnmergedTree = true).assertTextEquals("#02")
        composeRule.onNodeWithTag("currentCueNumber.${thirdActiveMemo.id}", useUnmergedTree = true).assertTextEquals("#03")
    }

    @Test
    fun draggingTodayCueDoesNotOpenDetailOnRelease() {
        var openedMemo: Memo? = null
        var reordered: Pair<Int, Int>? = null
        var state by mutableStateOf(
            MainUiState(
                currentMemos = listOf(activeMemo, secondActiveMemo, thirdActiveMemo),
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
                onOpenMemoDetail = {
                    openedMemo = it
                    state = state.copy(selectedMemo = it, detailDraft = it.content)
                },
                onDismissMemoDetail = { state = state.copy(selectedMemo = null, detailDraft = "") },
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
                onReorderCurrentMemos = { from, to ->
                    reordered = from to to
                },
            )
        }

        composeRule.onNodeWithTag("currentCue.${activeMemo.id}")
            .performTouchInput {
                down(center)
                advanceEventTime(700)
                moveBy(Offset(0f, 190f))
                up()
            }
        composeRule.waitForIdle()

        assert(reordered != null)
        assert(openedMemo == null)
        assert(composeRule.onAllNodesWithText("Active Cue").fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun pinnedTodayCueUsesSubtleRowTreatment() {
        val pinnedMemo = activeMemo.copy(pinned = true)
        var state by mutableStateOf(
            MainUiState(
                currentMemos = listOf(pinnedMemo, secondActiveMemo),
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

        composeRule.onNodeWithTag("currentCueNumber.${pinnedMemo.id}", useUnmergedTree = true).assertTextEquals("#01")
        composeRule.onNodeWithTag("currentCueNumber.${secondActiveMemo.id}", useUnmergedTree = true).assertTextEquals("#02")
        assert(
            composeRule.onNodeWithTag("currentCue.${pinnedMemo.id}")
                .fetchSemanticsNode()
                .config[CuePinnedKey],
        )
        assert(composeRule.onAllNodesWithText("Pinned").fetchSemanticsNodes().isEmpty())
        takeDeviceScreenshotIfRequested()
    }

    @Test
    fun unpinnedTodayCueReturnsToPlainRowTreatment() {
        var state by mutableStateOf(
            MainUiState(
                currentMemos = listOf(activeMemo.copy(pinned = true), secondActiveMemo),
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

        assert(
            composeRule.onNodeWithTag("currentCue.${activeMemo.id}")
                .fetchSemanticsNode()
                .config[CuePinnedKey],
        )

        state = state.copy(currentMemos = listOf(activeMemo.copy(pinned = false), secondActiveMemo))
        composeRule.waitForIdle()

        assert(
            !composeRule.onNodeWithTag("currentCue.${activeMemo.id}")
                .fetchSemanticsNode()
                .config[CuePinnedKey],
        )
        takeDeviceScreenshotIfRequested()
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
        composeRule.onNodeWithText("Private sync").assertIsDisplayed()
        composeRule.onNodeWithText("Private sync is up to date.").assertIsDisplayed()
        assert(composeRule.onAllNodesWithText(longEmail).fetchSemanticsNodes().isEmpty())
        assert(composeRule.onAllNodesWithText("Syncing as $longEmail.").fetchSemanticsNodes().isEmpty())
        assert(composeRule.onAllNodesWithText("Cue changes stay private to this account and sync automatically.").fetchSemanticsNodes().isEmpty())
        assert(composeRule.onAllNodesWithText("Remote path").fetchSemanticsNodes().isEmpty())
        assert(composeRule.onAllNodesWithText("users/{uid}/memos").fetchSemanticsNodes().isEmpty())
        assert(composeRule.onAllNodesWithText("Pending uploads").fetchSemanticsNodes().isEmpty())
        takeDeviceScreenshotIfRequested()
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

        composeRule.onNodeWithText("Capture a cue").performClick()
        composeRule.onNodeWithText("New Cue").assertIsDisplayed()
        composeRule.onNodeWithText("Cue").performTextInput("Design sync with Sarah")
        composeRule.onNodeWithText("Save").performClick()

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

        composeRule.onNodeWithText("Capture a cue").performClick()
        composeRule.onNodeWithText("New Cue").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Close").assertIsDisplayed()
        composeRule.onNodeWithText("Cue").assertIsDisplayed()
        composeRule.onNodeWithText("Private").assertIsDisplayed()
        composeRule.onNodeWithText("Local first").assertIsDisplayed()
        composeRule.onNodeWithText("300 left").assertIsDisplayed()
        assert(composeRule.onAllNodesWithText("Add details").fetchSemanticsNodes().isEmpty())
        assert(composeRule.onAllNodesWithText("Cue details").fetchSemanticsNodes().isEmpty())
        assert(composeRule.onAllNodesWithText("Save with context").fetchSemanticsNodes().isEmpty())
        assert(composeRule.onAllNodesWithText("Context and timing can stay optional.").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText("Save").assertIsDisplayed()
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
        composeRule.onNodeWithText("1 cue").assertIsDisplayed()
        assert(composeRule.onAllNodesWithText(archivedMemo.content).fetchSemanticsNodes().isNotEmpty())
        assert(composeRule.onAllNodesWithText("title").fetchSemanticsNodes().isEmpty())

        composeRule.onAllNodesWithText(archivedMemo.content)[0].performClick()
        composeRule.onNodeWithText("Archived Cue").assertIsDisplayed()
        composeRule.onNodeWithText("Cue").assertIsDisplayed()
        assert(composeRule.onAllNodesWithText("Favorite").fetchSemanticsNodes().isEmpty())
        assert(composeRule.onAllNodesWithText("More").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText("Created").assertIsDisplayed()
        composeRule.onNodeWithText("Last updated").assertIsDisplayed()
        assert(composeRule.onAllNodesWithText("Last reminded").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithContentDescription("Delete cue").assertIsDisplayed()
        assert(composeRule.onAllNodesWithContentDescription("Keep cue").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithContentDescription("Return to Today").assertIsDisplayed()
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
        assert(composeRule.onAllNodesWithContentDescription("Keep cue").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithContentDescription("Return to Today").performClick()

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
        composeRule.onNodeWithText("Delete this cue?").assertIsDisplayed()
        composeRule.onNodeWithText("This cue and its event history will be permanently removed.")
            .assertIsDisplayed()
        assert(composeRule.onAllNodesWithText(activeMemo.content).fetchSemanticsNodes().isNotEmpty())
        composeRule.onNodeWithText("Cancel").performClick()
        assert(composeRule.onAllNodesWithText("Delete this cue?").fetchSemanticsNodes().isEmpty())
        assert(!confirmed)

        composeRule.onNodeWithContentDescription("Delete cue").performClick()
        composeRule.onNodeWithText("Delete this cue?").assertIsDisplayed()
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
    fun signedInAccountShowsPrivateSyncElements() {
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

        assert(composeRule.onAllNodesWithText("Private sync is active.").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithText("Private sync").assertIsDisplayed()
        composeRule.onNodeWithText(email).assertIsDisplayed()
        composeRule.onNodeWithText("Last sync").assertIsDisplayed()
        composeRule.onNodeWithText("Private sync is up to date.").assertIsDisplayed()
        assert(composeRule.onAllNodesWithText("Cue changes stay private to this account and sync automatically.").fetchSemanticsNodes().isEmpty())
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
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.swipe(
            (device.displayWidth * 0.28f).roundToInt(),
            (device.displayHeight * 0.88f).roundToInt(),
            (device.displayWidth * 0.28f).roundToInt(),
            (device.displayHeight * 0.67f).roundToInt(),
            36,
        )
        composeRule.waitUntil {
            composeRule.onAllNodesWithText("19").fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithText("Save Time").performClick()

        assert(savedTime != null && savedTime != ReminderTime(hour = 21, minute = 0))
    }

    @Test
    fun reminderTimeWheelFollowsFingerBeforeRelease() {
        var state by mutableStateOf(MainUiState(selectedScreen = MemoScreen.REMINDER))
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
                onReminderEnabledChange = { state = state.copy(reminderEnabled = it) },
            )
        }

        composeRule.onNodeWithText("Change Time").performClick()
        composeRule.onNodeWithText("Choose Reminder Time").assertIsDisplayed()
        composeRule.waitForIdle()
        val hourPicker = composeRule.onNodeWithContentDescription("Hour picker")
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.waitForIdle()
        device.swipe(
            (device.displayWidth * 0.28f).roundToInt(),
            (device.displayHeight * 0.86f).roundToInt(),
            (device.displayWidth * 0.28f).roundToInt(),
            (device.displayHeight * 0.68f).roundToInt(),
            36,
        )
        composeRule.waitUntil {
            hourPicker.fetchSemanticsNode().config[TimeWheelSelectedValueKey] != 21
        }
        takeDeviceScreenshotIfRequested()

        val node = hourPicker.fetchSemanticsNode()
        assert(node.config[TimeWheelSelectedValueKey] != 21)
    }

    @Test
    fun todayActiveDetailKeepsPinAction() {
        var pinnedMemo: Pair<String, Boolean>? = null
        var state by mutableStateOf(
            MainUiState(
                selectedScreen = MemoScreen.CURRENT,
                currentMemos = listOf(activeMemo),
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
                onOpenMemoDetail = {
                    state = state.copy(selectedMemo = it, detailDraft = it.content)
                },
                onDismissMemoDetail = { state = state.copy(selectedMemo = null, detailDraft = "") },
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
                onSetMemoPinned = { memoId, pinned -> pinnedMemo = memoId to pinned },
            )
        }

        composeRule.onNodeWithText(activeMemo.content).performClick()
        composeRule.onNodeWithContentDescription("Pin cue").performClick()
        assert(pinnedMemo == Pair(activeMemo.id, true))
    }

    @Test
    fun archiveOpensActiveCueWithCompleteAndArchivedCueWithRestore() {
        var completedMemoId = ""
        var restoredMemoId = ""
        var pinnedMemo: Pair<String, Boolean>? = null
        var state by mutableStateOf(
            MainUiState(
                selectedScreen = MemoScreen.HISTORY,
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
                onClearMemo = { completedMemoId = it },
                onDetailDraftChange = {},
                onSaveReminderTime = { _, _ -> },
                onOpenMemoDetail = {
                    state = state.copy(selectedMemo = it, detailDraft = it.content)
                },
                onDismissMemoDetail = { state = state.copy(selectedMemo = null, detailDraft = "") },
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
                onSetMemoPinned = { memoId, pinned -> pinnedMemo = memoId to pinned },
            )
        }

        composeRule.onNodeWithText(activeMemo.content).assertIsDisplayed()
        composeRule.onNodeWithText(activeMemo.content).performClick()
        composeRule.onNodeWithText("Active Cue").assertIsDisplayed()
        assert(composeRule.onAllNodesWithContentDescription("Pin cue").fetchSemanticsNodes().isEmpty())
        assert(composeRule.onAllNodesWithContentDescription("Unpin cue").fetchSemanticsNodes().isEmpty())
        composeRule.onNodeWithContentDescription("Complete cue").assertIsDisplayed()
        assert(pinnedMemo == null)

        state = state.copy(selectedMemo = activeMemo)
        composeRule.onNodeWithContentDescription("Complete cue").performClick()
        assert(completedMemoId == activeMemo.id)

        state = state.copy(selectedMemo = null)
        composeRule.onNodeWithText(archivedMemo.content).performClick()
        composeRule.onNodeWithText("Archived Cue").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Return to Today").performClick()
        assert(restoredMemoId == archivedMemo.id)
        assert(composeRule.onAllNodesWithContentDescription("Keep cue").fetchSemanticsNodes().isEmpty())
    }

    private fun takeDeviceScreenshotIfRequested() {
        val name = InstrumentationRegistry.getArguments().getString("dreamcueScreenshotName") ?: return
        composeRule.waitForIdle()
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.waitForIdle()
        Thread.sleep(300)
        val safeName = name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val file = File("/sdcard/Download/$safeName.png")
        device.takeScreenshot(file)
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
            displayOrder = 1_700_000_060_000,
            pinned = false,
        )

        val secondActiveMemo = Memo(
            id = "active-2",
            content = "Review ordered cue sync",
            status = MemoStatus.ACTIVE,
            createdAtMs = 1_700_000_001_000,
            updatedAtMs = 1_700_000_050_000,
            clearedAtMs = null,
            reminderCount = 0,
            lastReviewedAtMs = null,
            displayOrder = 1_700_000_050_000,
            pinned = false,
        )

        val thirdActiveMemo = Memo(
            id = "active-3",
            content = "Confirm drag numbering",
            status = MemoStatus.ACTIVE,
            createdAtMs = 1_700_000_002_000,
            updatedAtMs = 1_700_000_040_000,
            clearedAtMs = null,
            reminderCount = 0,
            lastReviewedAtMs = null,
            displayOrder = 1_700_000_040_000,
            pinned = false,
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
            displayOrder = 1_700_000_120_000,
            pinned = false,
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
            displayOrder = 1_700_000_010_000,
            pinned = false,
        )

        val searchResult = SearchResult(
            memo = archivedMemo,
            score = 0.91,
            matchedBy = listOf("title"),
        )
    }
}
