package app.dreamcue.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.view.WindowCompat
import app.dreamcue.R
import app.dreamcue.model.Memo
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val Ink = Color(0xFF15201D)
private val InkSoft = Color(0xFF58635E)
private val Ivory = Color(0xFFF6F3EC)
private val Porcelain = Color(0xFFFFFCF6)
private val Stone = Color(0xFFE9E3D8)
private val Forest = Color(0xFF0F4B3A)
private val ForestSoft = Color(0xFFE0EEE8)
private val ForestPale = Color(0xFFD3E9DF)
private val Brass = Color(0xFFC9A46A)
private val Clay = Color(0xFFC4614B)
private val Line = Color(0xFFD9D1C4)
private val DeepForest = Color(0xFF0F3D2E)
private val PinnedPanel = Color(0xFFF0EFEB)
private val ScreenPadding = PaddingValues(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 18.dp)
private const val CueLongPressMillis = 500L
val CueDragOffsetYKey = SemanticsPropertyKey<Float>("CueDragOffsetY")
var SemanticsPropertyReceiver.cueDragOffsetY by CueDragOffsetYKey
val CueAvoidanceOffsetYKey = SemanticsPropertyKey<Float>("CueAvoidanceOffsetY")
var SemanticsPropertyReceiver.cueAvoidanceOffsetY by CueAvoidanceOffsetYKey
val CuePinnedKey = SemanticsPropertyKey<Boolean>("CuePinned")
var SemanticsPropertyReceiver.cuePinned by CuePinnedKey
val CueDraggingKey = SemanticsPropertyKey<Boolean>("CueDragging")
var SemanticsPropertyReceiver.cueDragging by CueDraggingKey
val TimeWheelDragOffsetYKey = SemanticsPropertyKey<Float>("TimeWheelDragOffsetY")
var SemanticsPropertyReceiver.timeWheelDragOffsetY by TimeWheelDragOffsetYKey
val TimeWheelSelectedValueKey = SemanticsPropertyKey<Int>("TimeWheelSelectedValue")
var SemanticsPropertyReceiver.timeWheelSelectedValue by TimeWheelSelectedValueKey

private val DreamCueColorScheme = lightColorScheme(
    primary = Forest,
    onPrimary = Porcelain,
    secondary = Brass,
    background = Ivory,
    surface = Porcelain,
    onSurface = Ink,
    outline = Line,
    error = Clay,
)

internal fun timeOfDayGreeting(calendar: Calendar = Calendar.getInstance()): String {
    return when (calendar.get(Calendar.HOUR_OF_DAY)) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Good night"
    }
}

@Composable
fun DreamCueApp(
    state: MainUiState,
    onDraftChange: (String) -> Unit,
    onAddMemo: () -> Unit,
    onSelectScreen: (MemoScreen) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onRunSearch: () -> Unit,
    onClearMemo: (String) -> Unit,
    onDetailDraftChange: (String) -> Unit,
    onSaveReminderTime: (Int, Int) -> Unit,
    onOpenMemoDetail: (Memo) -> Unit,
    onDismissMemoDetail: () -> Unit,
    onReopenMemo: (String) -> Unit,
    onRequestDelete: (Memo) -> Unit,
    onDismissDeleteRequest: () -> Unit,
    onConfirmDelete: () -> Unit,
    onSyncEmailChange: (String) -> Unit,
    onSyncPasswordChange: (String) -> Unit,
    onSignInSync: () -> Unit,
    onCreateSyncAccount: () -> Unit,
    onSignOutSync: () -> Unit,
    onReminderEnabledChange: (Boolean) -> Unit,
    onReorderCurrentMemos: (Int, Int) -> Unit = { _, _ -> },
    onSetMemoPinned: (String, Boolean) -> Unit = { _, _ -> },
) {
    var showCaptureSheet by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    var startupAccountPromptSkipped by rememberSaveable { mutableStateOf(false) }
    val showArchiveSearchResults =
        state.submittedSearchQuery.isNotBlank() && state.submittedSearchQuery == state.searchQuery
    val showStartupAccountPrompt = !startupAccountPromptSkipped && state.needsStartupAccountPrompt()

    MaterialTheme(
        colorScheme = DreamCueColorScheme,
        typography = MaterialTheme.typography,
    ) {
        DreamCueSystemBars()
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Ivory,
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    DreamCueNavigation(
                        selectedScreen = state.selectedScreen,
                        onSelectScreen = onSelectScreen,
                    )
                },
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Porcelain, Ivory, Color(0xFFF1EBDD)),
                            ),
                        )
                        .padding(padding),
                ) {
                    SubtlePaperTexture(Modifier.fillMaxSize())

                    if (state.isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter),
                            color = Brass,
                            trackColor = Stone,
                        )
                    }

                    when (state.selectedScreen) {
                        MemoScreen.CURRENT -> TodayScreen(
                            state = state,
                            showAccountReminder = showStartupAccountPrompt,
                            onOpenCapture = { showCaptureSheet = true },
                            onOpenMemo = onOpenMemoDetail,
                            onReorderCurrentMemos = onReorderCurrentMemos,
                            onOpenAccount = {
                                startupAccountPromptSkipped = true
                                onSelectScreen(MemoScreen.ACCOUNT)
                            },
                            onSkipAccountReminder = {
                                startupAccountPromptSkipped = true
                            },
                        )

                        MemoScreen.HISTORY -> ArchiveScreen(
                            state = state,
                            showSearchResults = showArchiveSearchResults,
                            onSearchQueryChange = onSearchQueryChange,
                            onRunSearch = onRunSearch,
                            onOpenMemo = onOpenMemoDetail,
                        )

                        MemoScreen.REMINDER -> RhythmScreen(
                            state = state,
                            onOpenTimePicker = { showTimePicker = true },
                            onReminderEnabledChange = onReminderEnabledChange,
                        )

                        MemoScreen.ACCOUNT -> AccountScreen(
                            state = state,
                            onSyncEmailChange = onSyncEmailChange,
                            onSyncPasswordChange = onSyncPasswordChange,
                            onSignInSync = onSignInSync,
                            onCreateSyncAccount = onCreateSyncAccount,
                            onSignOutSync = onSignOutSync,
                        )
                    }
                }
            }

            if (showCaptureSheet) {
                CaptureSheet(
                    draft = state.draft,
                    onDraftChange = onDraftChange,
                    onDismiss = { showCaptureSheet = false },
                    onSave = {
                        onAddMemo()
                        if (state.draft.isNotBlank()) {
                            showCaptureSheet = false
                        }
                    },
                )
            }

            if (showTimePicker) {
                ReminderTimePickerSheet(
                    reminderHour = state.reminderTime.hour,
                    reminderMinute = state.reminderTime.minute,
                    onDismiss = { showTimePicker = false },
                    onSave = { hour, minute ->
                        onSaveReminderTime(hour, minute)
                        showTimePicker = false
                    },
                )
            }

            state.selectedMemo?.let { memo ->
                MemoDetailDialog(
                    memo = memo,
                    draft = state.detailDraft,
                    showPinAction = state.selectedScreen == MemoScreen.CURRENT && memo.isActive,
                    onDraftChange = onDetailDraftChange,
                    onDismiss = onDismissMemoDetail,
                    onPrimaryAction = {
                        if (memo.isActive) {
                            onClearMemo(memo.id)
                        } else {
                            onReopenMemo(memo.id)
                        }
                    },
                    onSetPinned = { onSetMemoPinned(memo.id, it) },
                    onDelete = { onRequestDelete(memo) },
                )
            }

            state.pendingDeleteMemo?.let { memo ->
                DeleteConfirmSheet(
                    memo = memo,
                    onDismiss = onDismissDeleteRequest,
                    onConfirmDelete = onConfirmDelete,
                )
            }
        }
    }
}

@Composable
private fun TodayScreen(
    state: MainUiState,
    showAccountReminder: Boolean,
    onOpenCapture: () -> Unit,
    onOpenMemo: (Memo) -> Unit,
    onReorderCurrentMemos: (Int, Int) -> Unit,
    onOpenAccount: () -> Unit,
    onSkipAccountReminder: () -> Unit,
) {
    var dragPreview by remember { mutableStateOf<CueDragPreview?>(null) }
    val cuePreviewStepPx = with(LocalDensity.current) { 104.dp.toPx() }
    val baseViewConfiguration = LocalViewConfiguration.current
    val cueViewConfiguration = remember(baseViewConfiguration) {
        object : ViewConfiguration by baseViewConfiguration {
            override val longPressTimeoutMillis: Long = CueLongPressMillis
        }
    }

    CompositionLocalProvider(LocalViewConfiguration provides cueViewConfiguration) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = ScreenPadding,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                HomeTopBar()
            }

            if (showAccountReminder) {
                item {
                    StartupAccountReminderPanel(
                        onOpenAccount = onOpenAccount,
                        onSkip = onSkipAccountReminder,
                    )
                }
            }

            if (state.nativeError != null) {
                item {
                    NoticePanel(
                        title = "Core unavailable",
                        content = state.nativeError,
                        tone = Clay.copy(alpha = 0.12f),
                    )
                }
            }

            if (state.errorMessage != null) {
                item {
                    NoticePanel(
                        title = "Notice",
                        content = state.errorMessage,
                        tone = Brass.copy(alpha = 0.16f),
                    )
                }
            }

            item {
                DailySummary(
                    activeCount = state.currentMemos.size,
                    reminderTime = state.reminderTime.asText(),
                )
            }

            item {
                CapturePrompt(onOpenCapture)
            }

            item {
                SectionTitle(
                    title = "Active Cues",
                    subtitle = "${state.currentMemos.size} cues awaiting reminder",
                    action = "Recent",
                )
            }

            if (state.currentMemos.isEmpty()) {
                item {
                    FirstCuePanel(onOpenCapture = onOpenCapture)
                }
            } else {
                itemsIndexed(
                    items = state.currentMemos,
                    key = { _, memo -> "today-${memo.id}" },
                ) { index, memo ->
                    val avoidanceOffset = dragPreview?.let {
                        cueDragDisplacedOffset(
                            rowIndex = index,
                            sourceIndex = it.sourceIndex,
                            targetIndex = it.targetIndex,
                            rowHeight = cuePreviewStepPx,
                        )
                    } ?: 0f
                    ReorderableCueCard(
                        memo = memo,
                        displayNumber = index + 1,
                        statusLabel = "Current",
                        accent = Forest,
                        onOpen = { onOpenMemo(memo) },
                        index = index,
                        count = state.currentMemos.size,
                        avoidanceOffset = avoidanceOffset,
                        onDragTargetChange = { targetIndex ->
                            dragPreview = targetIndex?.let {
                                CueDragPreview(sourceIndex = index, targetIndex = it)
                            }
                        },
                        onMove = onReorderCurrentMemos,
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchiveScreen(
    state: MainUiState,
    showSearchResults: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onRunSearch: () -> Unit,
    onOpenMemo: (Memo) -> Unit,
) {
    val archiveMemos = (state.currentMemos + state.historyMemos).sortedByDescending { it.updatedAtMs }
    val archiveSearchResults = state.searchResults
        .map { it.memo }
        .distinctBy { it.id }
    val resultLabel = "${archiveSearchResults.size} ${if (archiveSearchResults.size == 1) "cue" else "cues"}"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = ScreenPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            AppHeader(
                title = "Archive",
                subtitle = "All cue history stays searchable.",
            )
        }

        item {
            ElevatedPanel {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Search archive") },
                    placeholder = { Text("Release notes, dentist, Firebase") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onRunSearch() }),
                    trailingIcon = {
                        IconButton(onClick = onRunSearch) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = "Run archive search",
                                tint = Forest,
                            )
                        }
                    },
                )
            }
        }

        if (showSearchResults) {
            item {
                SectionTitle(
                    title = "Search Results",
                    subtitle = resultLabel,
                )
            }
            if (archiveSearchResults.isEmpty()) {
                item {
                    EmptyPanel("No archived cues found")
                }
            } else {
                itemsIndexed(
                    items = archiveSearchResults,
                    key = { _, memo -> "archive-search-${memo.id}" },
                ) { index, memo ->
                    SearchResultCueCard(
                        memo = memo,
                        displayNumber = index + 1,
                        onOpen = { onOpenMemo(memo) },
                    )
                }
            }
        } else if (archiveMemos.isEmpty()) {
            item {
                SectionTitle(
                    title = "All History",
                    subtitle = "0 cues",
                )
            }
            item {
                EmptyPanel("No cues yet")
            }
        } else {
            item {
                SectionTitle(
                    title = "All History",
                    subtitle = "${archiveMemos.size} ${if (archiveMemos.size == 1) "cue" else "cues"}",
                )
            }
            itemsIndexed(items = archiveMemos, key = { _, memo -> "archive-${memo.id}" }) { index, memo ->
                TimelineCue(
                    memo = memo,
                    displayNumber = index + 1,
                    onOpen = { onOpenMemo(memo) },
                )
            }
        }
    }
}

@Composable
private fun RhythmScreen(
    state: MainUiState,
    onOpenTimePicker: () -> Unit,
    onReminderEnabledChange: (Boolean) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = ScreenPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            AppHeader(
                title = "Reminder Rhythm",
                subtitle = "A quiet daily return point.",
            )
        }

        item {
            ElevatedPanel {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column {
                        Text(
                            text = "Daily reminder",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Every day",
                            color = InkSoft,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (state.reminderEnabled) "On" else "Off",
                            color = InkSoft,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Switch(
                            checked = state.reminderEnabled,
                            onCheckedChange = onReminderEnabledChange,
                            modifier = Modifier.semantics {
                                contentDescription = "Daily reminder switch"
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Porcelain,
                                checkedTrackColor = Forest,
                                uncheckedThumbColor = Porcelain,
                                uncheckedTrackColor = Stone,
                            ),
                        )
                    }
                }
                Text(
                    text = state.reminderTime.asText(),
                    fontFamily = FontFamily.Serif,
                    fontSize = 48.sp,
                    lineHeight = 52.sp,
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "DreamCue returns active cues once a day at this time.",
                    color = InkSoft,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    SecondaryButton(
                        text = "Change Time",
                        onClick = onOpenTimePicker,
                    )
                }
            }
        }

        item {
            PermissionHealthPanel()
        }
    }
}

@Composable
private fun AccountScreen(
    state: MainUiState,
    onSyncEmailChange: (String) -> Unit,
    onSyncPasswordChange: (String) -> Unit,
    onSignInSync: () -> Unit,
    onCreateSyncAccount: () -> Unit,
    onSignOutSync: () -> Unit,
) {
    val signedIn = state.syncStatus.startsWith("Syncing as ") ||
        state.syncStatus == "Sync account signed in." ||
        state.syncStatus == "Sync account created."

    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = ScreenPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            AppHeader(
                title = "Account",
                subtitle = "",
            )
        }

        if (signedIn) {
            item {
                ConnectedAccountPanel(
                    email = state.syncEmail.ifBlank { "Signed in" },
                    syncStatus = state.syncStatus,
                    onSignOutSync = onSignOutSync,
                )
            }
            item {
                SyncHealthPanel()
            }
        } else {
            item {
                ElevatedPanel {
                    Text(
                        text = "Sync Account",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = state.syncStatus,
                        color = InkSoft,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                    )
                    OutlinedTextField(
                        value = state.syncEmail,
                        onValueChange = onSyncEmailChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email") },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = state.syncPassword,
                        onValueChange = onSyncPasswordChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SecondaryButton(
                            text = "Create",
                            onClick = onCreateSyncAccount,
                            modifier = Modifier.weight(1f),
                        )
                        PrimaryButton(
                            text = "Sign In",
                            onClick = onSignInSync,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            item {
                NoticePanel(
                    title = "Private sync",
                    content = "Only your signed-in account can access synced cues.",
                    tone = Brass.copy(alpha = 0.15f),
                )
            }
        }
    }
}

@Composable
@Suppress("DEPRECATION")
private fun DreamCueSystemBars() {
    val view = LocalView.current
    val context = LocalContext.current
    if (view.isInEditMode) {
        return
    }
    SideEffect {
        val window = context.findActivity()?.window ?: return@SideEffect
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun DreamCueNavigation(
    selectedScreen: MemoScreen,
    onSelectScreen: (MemoScreen) -> Unit,
) {
    Surface(
        color = Porcelain,
        shadowElevation = 16.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MemoScreen.entries.forEach { screen ->
                NavItem(
                    screen = screen,
                    selected = selectedScreen == screen,
                    onClick = { onSelectScreen(screen) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    screen: MemoScreen,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (selected) Forest else InkSoft.copy(alpha = 0.9f)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .background(if (selected) ForestPale else Color.Transparent)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = screen.icon(),
            contentDescription = screen.navLabel(),
            tint = color,
            modifier = Modifier.size(21.dp),
        )
        Text(
            text = screen.navLabel(),
            color = color,
            fontSize = 11.sp,
            lineHeight = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun AppHeader(
    title: String,
    subtitle: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            fontFamily = FontFamily.Serif,
            fontSize = 24.sp,
            lineHeight = 28.sp,
            color = Ink,
            fontWeight = FontWeight.Medium,
        )
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                color = InkSoft,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
private fun HomeTopBar() {
    val greeting = remember { timeOfDayGreeting(Calendar.getInstance()) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(66.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = greeting,
                color = Ink,
                fontFamily = FontFamily.Serif,
                fontSize = 26.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = "Keep what matters in rhythm.",
                color = InkSoft,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Text(
            text = "DreamCue",
            color = DeepForest,
            fontFamily = FontFamily.Serif,
            fontSize = 17.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 12.dp, top = 2.dp),
        )
    }
}

@Composable
private fun SubtlePaperTexture(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val lineColor = Line.copy(alpha = 0.13f)
        for (index in 1..7) {
            val y = size.height * index / 8f
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(size.width, y + 8f),
                strokeWidth = 0.8f,
            )
        }
        val dots = listOf(
            Offset(size.width * 0.10f, size.height * 0.16f),
            Offset(size.width * 0.84f, size.height * 0.19f),
            Offset(size.width * 0.18f, size.height * 0.52f),
            Offset(size.width * 0.74f, size.height * 0.60f),
            Offset(size.width * 0.46f, size.height * 0.84f),
        )
        dots.forEachIndexed { index, offset ->
            drawCircle(
                color = if (index % 2 == 0) Brass.copy(alpha = 0.14f) else Forest.copy(alpha = 0.08f),
                radius = 1.7f,
                center = offset,
            )
        }
    }
}

@Composable
private fun BrandLeafMark(
    contentDescription: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = DeepForest,
) {
    Box(
        modifier = modifier
            .background(backgroundColor, CircleShape)
            .semantics {
                this.contentDescription = contentDescription
            },
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun EmptyStateIllustration(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier.semantics {
            this.contentDescription = "DreamCue leaf mark"
        },
    ) {
        val width = size.width
        val height = size.height
        drawCircle(
            color = Stone.copy(alpha = 0.62f),
            radius = minOf(width, height) * 0.48f,
            center = Offset(width / 2f, height / 2f),
        )

        val hill = Path().apply {
            moveTo(0f, height * 0.72f)
            cubicTo(width * 0.22f, height * 0.60f, width * 0.42f, height * 0.77f, width * 0.62f, height * 0.65f)
            cubicTo(width * 0.82f, height * 0.54f, width, height * 0.66f, width, height * 0.66f)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }
        drawPath(path = hill, color = Brass.copy(alpha = 0.22f))

        val stem = Path().apply {
            moveTo(width * 0.50f, height * 0.76f)
            cubicTo(width * 0.48f, height * 0.58f, width * 0.53f, height * 0.44f, width * 0.62f, height * 0.32f)
        }
        drawPath(
            path = stem,
            color = DeepForest,
            style = Stroke(width = width * 0.035f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        listOf(
            listOf(Offset(width * 0.53f, height * 0.60f), Offset(width * 0.32f, height * 0.47f), Offset(width * 0.46f, height * 0.38f)),
            listOf(Offset(width * 0.57f, height * 0.48f), Offset(width * 0.74f, height * 0.34f), Offset(width * 0.66f, height * 0.26f)),
        ).forEach { points ->
            val leaf = Path().apply {
                moveTo(points[0].x, points[0].y)
                cubicTo(points[1].x, points[1].y, points[2].x, points[2].y, points[2].x, points[2].y)
            }
            drawPath(
                path = leaf,
                color = DeepForest,
                style = Stroke(width = width * 0.032f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }

        listOf(
            Offset(width * 0.25f, height * 0.26f),
            Offset(width * 0.78f, height * 0.32f),
            Offset(width * 0.70f, height * 0.18f),
        ).forEach { center ->
            drawLine(
                color = Brass,
                start = Offset(center.x - 4f, center.y),
                end = Offset(center.x + 4f, center.y),
                strokeWidth = 1.2f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Brass,
                start = Offset(center.x, center.y - 4f),
                end = Offset(center.x, center.y + 4f),
                strokeWidth = 1.2f,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun DailySummary(
    activeCount: Int,
    reminderTime: String,
) {
    ElevatedPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SummaryMetric(
                icon = Icons.Outlined.AccessTime,
                label = "Daily rhythm",
                value = reminderTime,
                footnote = "Every day",
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(64.dp)
                    .background(Line),
            )
            SummaryMetric(
                icon = Icons.Outlined.NotificationsNone,
                label = "Active cues",
                value = activeCount.toString(),
                footnote = "Awaiting reminder",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SummaryMetric(
    icon: ImageVector,
    label: String,
    value: String,
    footnote: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Forest,
            modifier = Modifier.size(25.dp),
        )
        Column {
            Text(text = label, color = InkSoft, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(text = value, color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(text = footnote, color = InkSoft, fontSize = 11.sp)
        }
    }
}

@Composable
private fun CapturePrompt(onOpenCapture: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onOpenCapture),
        color = DeepForest,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BrandLeafMark(
                contentDescription = "Capture cue leaf",
                modifier = Modifier.size(42.dp),
                backgroundColor = Color.White.copy(alpha = 0.08f),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Capture a cue",
                    color = Porcelain,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "Tap to write a short cue",
                    color = Porcelain.copy(alpha = 0.78f),
                    fontSize = 10.sp,
                )
            }
            Surface(
                modifier = Modifier.size(34.dp),
                color = Brass,
                shape = CircleShape,
                onClick = onOpenCapture,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = "New Cue",
                        tint = DeepForest,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String,
    action: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom,
    ) {
        Column {
            Text(text = title, color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                text = subtitle,
                color = InkSoft,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (action != null) {
            Text(text = action, color = InkSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CueCard(
    memo: Memo,
    displayNumber: Int,
    statusLabel: String,
    accent: Color,
    onOpen: () -> Unit,
    numberTag: String? = null,
    isDragging: Boolean = false,
) {
    val cardModifier = if (isDragging) Modifier else Modifier.clickable(onClick = onOpen)

    ElevatedPanel(
        modifier = cardModifier,
        color = when {
            isDragging -> Color(0xFFE8E8E4)
            memo.pinned -> PinnedPanel
            else -> Porcelain
        },
        contentPadding = PaddingValues(15.dp),
        border = BorderStroke(
            1.dp,
            if (memo.pinned) Line.copy(alpha = 0.70f) else Line.copy(alpha = 0.58f),
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            NumberMarker(
                number = displayNumber,
                pinned = memo.pinned,
                color = accent,
                textModifier = numberTag?.let { Modifier.testTag(it) } ?: Modifier,
            )
            if (isDragging) {
                DragGrip()
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = memo.content,
                        color = Ink,
                        fontSize = 15.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StatusChip(if (isDragging) "Moving" else statusLabel, if (isDragging) InkSoft else accent)
                    }
                }
                Text(
                    text = memoSummaryLine(memo),
                    color = InkSoft,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 7.dp),
                )
            }
            if (!isDragging) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = InkSoft,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun DragGrip() {
    Column(
        modifier = Modifier
            .padding(top = 6.dp)
            .width(18.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        repeat(3) {
            Box(
                modifier = Modifier
                    .size(width = 15.dp, height = 2.dp)
                    .background(InkSoft.copy(alpha = 0.44f), RoundedCornerShape(999.dp)),
            )
        }
    }
}

@Composable
private fun ReorderableCueCard(
    memo: Memo,
    displayNumber: Int,
    statusLabel: String,
    accent: Color,
    onOpen: () -> Unit,
    index: Int,
    count: Int,
    avoidanceOffset: Float,
    onDragTargetChange: (Int?) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val rowStepPx = 88f
    val visibleDragOffset by animateFloatAsState(
        targetValue = if (isDragging) dragOffset else 0f,
        animationSpec = spring(stiffness = 700f, dampingRatio = 0.82f),
        label = "cueDragOffset",
    )
    val dragLift by animateFloatAsState(
        targetValue = if (isDragging) 1f else 0f,
        animationSpec = spring(stiffness = 650f, dampingRatio = 0.86f),
        label = "cueDragLift",
    )

    Box(
        modifier = Modifier
            .testTag("currentCue.${memo.id}")
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                translationY = visibleDragOffset + avoidanceOffset
                scaleX = 1f + dragLift * 0.012f
                scaleY = 1f + dragLift * 0.012f
                shadowElevation = dragLift * 18f
            }
            .semantics {
                cueDragOffsetY = visibleDragOffset
                cueAvoidanceOffsetY = avoidanceOffset
                cuePinned = memo.pinned
                cueDragging = isDragging
            }
            .pointerInput(index, count) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        isDragging = true
                        dragOffset = 0f
                        onDragTargetChange(index)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount.y
                        val rowStep = (dragOffset / rowStepPx).roundToInt()
                        val target = (index + rowStep).coerceIn(0, count - 1)
                        onDragTargetChange(target)
                    },
                    onDragEnd = {
                        val rowStep = (dragOffset / rowStepPx).roundToInt()
                        val target = (index + rowStep).coerceIn(0, count - 1)
                        isDragging = false
                        dragOffset = 0f
                        onDragTargetChange(null)
                        if (target != index) {
                            onMove(index, target)
                        }
                    },
                    onDragCancel = {
                        isDragging = false
                        dragOffset = 0f
                        onDragTargetChange(null)
                    },
                )
            },
    ) {
        CueCard(
            memo = memo,
            displayNumber = displayNumber,
            statusLabel = statusLabel,
            accent = accent,
            onOpen = onOpen,
            numberTag = "currentCueNumber.${memo.id}",
            isDragging = isDragging,
        )
    }
}

private data class CueDragPreview(
    val sourceIndex: Int,
    val targetIndex: Int,
)

private fun cueDragDisplacedOffset(
    rowIndex: Int,
    sourceIndex: Int,
    targetIndex: Int,
    rowHeight: Float = 88f,
): Float {
    if (sourceIndex < targetIndex && rowIndex > sourceIndex && rowIndex <= targetIndex) {
        return -rowHeight
    }
    if (sourceIndex > targetIndex && rowIndex >= targetIndex && rowIndex < sourceIndex) {
        return rowHeight
    }
    return 0f
}

@Composable
private fun SearchResultCueCard(
    memo: Memo,
    displayNumber: Int,
    onOpen: () -> Unit,
) {
    val label = if (memo.isActive) "Current" else "Cleared"

    ElevatedPanel(
        modifier = Modifier.clickable(onClick = onOpen),
        color = Porcelain,
        contentPadding = PaddingValues(15.dp),
        border = BorderStroke(
            1.dp,
            if (memo.pinned) Line.copy(alpha = 0.70f) else Line.copy(alpha = 0.58f),
        ),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            NumberMarker(displayNumber, memo.pinned, Forest)
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = memo.content,
                        color = Ink,
                        fontSize = 15.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        StatusChip(label, if (memo.isActive) Forest else Brass)
                    }
                }
                Text(
                    text = memoSummaryLine(memo),
                    color = InkSoft,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 7.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun TimelineCue(
    memo: Memo,
    displayNumber: Int,
    onOpen: () -> Unit,
) {
    ElevatedPanel(
        modifier = Modifier.clickable(onClick = onOpen),
        color = Porcelain,
        contentPadding = PaddingValues(15.dp),
        border = BorderStroke(
            1.dp,
            if (memo.pinned) Line.copy(alpha = 0.70f) else Line.copy(alpha = 0.58f),
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            NumberMarker(displayNumber, memo.pinned, if (memo.isActive) Forest else Brass)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = memo.content,
                    color = Ink,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = memoSummaryLine(memo),
                    color = InkSoft,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 7.dp),
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusChip(
                    text = if (memo.isActive) "Current" else "Cleared",
                    color = if (memo.isActive) Forest else Brass,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = InkSoft,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun FirstCuePanel(onOpenCapture: () -> Unit) {
    ElevatedPanel {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            EmptyStateIllustration(
                modifier = Modifier.size(104.dp),
            )
            Text(
                text = "A quiet place for what matters.",
                color = Ink,
                fontFamily = FontFamily.Serif,
                fontSize = 23.sp,
                lineHeight = 28.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Capture a short cue and DreamCue will bring it back at the right time.",
                color = InkSoft,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
            )
            PrimaryButton(text = "Write your first cue", onClick = onOpenCapture)
        }
    }
}

@Composable
private fun StartupAccountReminderPanel(
    onOpenAccount: () -> Unit,
    onSkip: () -> Unit,
) {
    ElevatedPanel(
        color = ForestSoft,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 13.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = Forest,
                    modifier = Modifier.size(24.dp),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = "Sign in to sync",
                        color = Ink,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Keep cues private across devices with your DreamCue account.",
                        color = InkSoft,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            ) {
                SecondaryButton(text = "Skip", onClick = onSkip)
                PrimaryButton(text = "Sign In", onClick = onOpenAccount)
            }
        }
    }
}

@Composable
private fun SyncHealthPanel() {
    ElevatedPanel(
        color = Porcelain,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(31.dp)
                    .background(Forest.copy(alpha = 0.10f), RoundedCornerShape(999.dp)),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sync Health",
                    color = Ink,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Private sync is up to date.",
                    color = InkSoft,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = Forest,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@Composable
private fun PermissionHealthPanel() {
    ElevatedPanel {
        Text(
            text = "Permission health",
            color = Ink,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
        PermissionLine("Notifications", "Required")
        Text(
            text = "Daily reminders use standard notifications.",
            color = InkSoft,
            fontSize = 12.sp,
            lineHeight = 17.sp,
        )
    }
}

@Composable
private fun PermissionLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Row(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = value, color = InkSoft, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = Forest,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun ConnectedAccountPanel(
    email: String,
    syncStatus: String,
    onSignOutSync: () -> Unit,
) {
    val displayEmail = compactAccountEmail(email)
    val displayStatus = accountSyncStatusLabel(syncStatus)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Forest,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 7.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                StatusChip("Private sync", Porcelain, dark = true)
                Text(
                    text = displayEmail,
                    color = Porcelain,
                    fontSize = 17.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 10.dp),
                )
                Text(
                    text = displayStatus,
                    color = Porcelain.copy(alpha = 0.72f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Last sync",
                        color = Porcelain.copy(alpha = 0.68f),
                        fontSize = 12.sp,
                        maxLines = 1,
                    )
                    Text(
                        text = "just now",
                        color = Porcelain,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                OutlinedButton(
                    onClick = onSignOutSync,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Porcelain),
                    border = BorderStroke(1.dp, Porcelain.copy(alpha = 0.32f)),
                ) {
                    Text("Sign Out")
                }
            }
        }
    }
}

private fun compactAccountEmail(email: String): String {
    val trimmed = email.trim()
    if (trimmed.length <= 28) {
        return trimmed
    }

    val parts = trimmed.split("@", limit = 2)
    if (parts.size != 2) {
        return "${trimmed.take(20)}...${trimmed.takeLast(6)}"
    }

    val local = parts[0]
    val domain = parts[1]
    val compactLocal = if (local.length > 12) "${local.take(12)}..." else local
    val compactDomain = if (domain.length > 18) "${domain.take(7)}...${domain.takeLast(8)}" else domain
    return "$compactLocal@$compactDomain"
}

private fun accountSyncStatusLabel(syncStatus: String): String {
    return if (
        syncStatus.startsWith("Syncing as ") ||
        syncStatus == "Sync account signed in." ||
        syncStatus == "Sync account created."
    ) {
        "Realtime sync is active."
    } else {
        syncStatus
    }
}

private fun MainUiState.needsStartupAccountPrompt(): Boolean {
    return syncEmail.isBlank() &&
        !syncStatus.startsWith("Syncing as ") &&
        syncStatus != "Sync account signed in." &&
        syncStatus != "Sync account created."
}

@Composable
private fun CaptureSheet(
    draft: String,
    onDraftChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    BottomSheetDialog(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 58.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close")
                }
                Text(
                    text = "New Cue",
                    color = Ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.size(48.dp))
            }
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(142.dp),
                label = { Text("Cue") },
                placeholder = { Text("Write a short cue") },
                minLines = 6,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSave() }),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MetadataChip(Icons.Outlined.Lock, "Private")
                MetadataChip(Icons.Outlined.Sync, "Local first")
                CounterChip("${300 - draft.length} left")
            }
            PrimaryButton(
                text = "Save",
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = Icons.Outlined.Check,
            )
        }
    }
}

@Composable
private fun ReminderTimePickerSheet(
    reminderHour: Int,
    reminderMinute: Int,
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit,
) {
    var selectedHour by remember(reminderHour) { mutableIntStateOf(reminderHour) }
    var selectedMinute by remember(reminderMinute) { mutableIntStateOf(reminderMinute) }

    BottomSheetDialog(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 58.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SheetHandle()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Choose Reminder Time",
                    color = Ink,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                )
                StatusChip("24-hour", Forest)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                WheelGroup(
                    title = "Hour",
                    values = (0..23).toList(),
                    selected = selectedHour,
                    contentDescription = "Hour picker",
                    label = { it.toString().padStart(2, '0') },
                    onSelect = { selectedHour = it },
                    modifier = Modifier.weight(1f),
                )
                WheelGroup(
                    title = "Minute",
                    values = (0..59 step 5).toList(),
                    selected = selectedMinute,
                    contentDescription = "Minute picker",
                    label = { it.toString().padStart(2, '0') },
                    onSelect = { selectedMinute = it },
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            ) {
                SecondaryButton(text = "Cancel", onClick = onDismiss)
                PrimaryButton(text = "Save Time", onClick = { onSave(selectedHour, selectedMinute) })
            }
        }
    }
}

@Composable
private fun WheelGroup(
    title: String,
    values: List<Int>,
    selected: Int,
    contentDescription: String,
    label: (Int) -> String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Text(
            text = title,
            color = InkSoft,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 6.dp),
        )
        WheelColumn(
            values = values,
            selected = selected,
            contentDescription = contentDescription,
            label = label,
            onSelect = onSelect,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun WheelColumn(
    values: List<Int>,
    selected: Int,
    contentDescription: String,
    label: (Int) -> String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedState by rememberUpdatedState(selected)
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    val stepPx = with(LocalDensity.current) { 44.dp.toPx() }
    val selectedIndex = values.indexOf(selected).coerceAtLeast(0)
    fun applyDrag(deltaY: Float) {
        if (values.isEmpty()) {
            return
        }
        dragOffsetPx += deltaY
        val steps = (-dragOffsetPx / stepPx).toInt()
        if (steps != 0) {
            val currentIndex = values.indexOf(selectedState).coerceAtLeast(0)
            val targetIndex = wrappedIndex(currentIndex + steps, values.size)
            if (targetIndex != currentIndex) {
                onSelect(values[targetIndex])
                dragOffsetPx += steps * stepPx
            } else {
                dragOffsetPx = 0f
            }
        }
    }
    val dragState = rememberDraggableState { deltaY ->
        applyDrag(deltaY)
    }
    fun visibleValue(offset: Int): Int {
        return values[wrappedIndex(selectedIndex + offset, values.size)]
    }

    Column(
        modifier = modifier
            .height(220.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFF1EADF))
            .border(1.dp, Line, RoundedCornerShape(24.dp))
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                onDragStopped = {
                    dragOffsetPx = 0f
                },
            )
            .semantics {
                this.contentDescription = contentDescription
                this.timeWheelDragOffsetY = dragOffsetPx
                this.timeWheelSelectedValue = selected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .align(Alignment.Center)
                    .background(Stone.copy(alpha = 0.84f)),
            )
            (-2..2).map(::visibleValue).forEach { value ->
                val offset = wrappedDistance(selectedIndex, values.indexOf(value).coerceAtLeast(0), values.size)
                val isSelected = offset == 0
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .align(Alignment.Center)
                        .graphicsLayer {
                            translationY = offset * stepPx + dragOffsetPx
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = label(value),
                        color = if (isSelected) Ink else InkSoft.copy(alpha = 0.78f),
                        fontSize = if (isSelected) 34.sp else 20.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = if (isSelected) 40.sp else 22.sp,
                    )
                }
            }
        }
    }
}

private fun wrappedIndex(index: Int, size: Int): Int {
    return ((index % size) + size) % size
}

private fun wrappedDistance(centerIndex: Int, valueIndex: Int, size: Int): Int {
    var distance = valueIndex - centerIndex
    if (distance > size / 2) {
        distance -= size
    }
    if (distance < -size / 2) {
        distance += size
    }
    return distance
}

@Composable
private fun SheetHandle() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 4.dp)
                .background(Line.copy(alpha = 0.86f), RoundedCornerShape(999.dp)),
        )
    }
}

@Composable
private fun MemoDetailDialog(
    memo: Memo,
    draft: String,
    showPinAction: Boolean,
    onDraftChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onPrimaryAction: () -> Unit,
    onSetPinned: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    BottomSheetDialog(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 58.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close")
                }
            }
            Text(
                text = if (memo.isActive) "Active Cue" else "Archived Cue",
                color = Forest,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                minLines = 5,
                label = { Text("Cue") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onDismiss() }),
            )
            ElevatedPanel(
                color = Ivory,
                contentPadding = PaddingValues(12.dp),
            ) {
                FlowLine("Created", formatTimestamp(memo.createdAtMs))
                FlowLine("Last updated", formatTimestamp(memo.updatedAtMs))
                if (memo.isActive) {
                    FlowLine("Last reminded", formatTimestamp(memo.lastReviewedAtMs))
                } else {
                    FlowLine("Completed", formatTimestamp(memo.clearedAtMs))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DetailActionButton(
                    contentDescription = "Delete cue",
                    icon = Icons.Outlined.DeleteOutline,
                    onClick = onDelete,
                    color = Clay,
                    modifier = Modifier.weight(1f),
                )
                if (showPinAction) {
                    DetailActionButton(
                        contentDescription = if (memo.pinned) "Unpin cue" else "Pin cue",
                        icon = Icons.Outlined.PushPin,
                        onClick = { onSetPinned(!memo.pinned) },
                        color = InkSoft,
                        modifier = Modifier.weight(1f),
                    )
                }
                DetailActionButton(
                    contentDescription = if (memo.isActive) "Complete cue" else "Return to Today",
                    icon = if (memo.isActive) Icons.Outlined.TaskAlt else Icons.AutoMirrored.Outlined.Undo,
                    onClick = onPrimaryAction,
                    modifier = Modifier.weight(1f),
                    filled = true,
                )
            }
        }
    }
}

@Composable
private fun DetailActionButton(
    contentDescription: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Forest,
    filled: Boolean = false,
) {
    Surface(
        modifier = modifier
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp)),
        color = if (filled) color else color.copy(alpha = 0.10f),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (filled) Porcelain else color,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun DeleteConfirmSheet(
    memo: Memo,
    onDismiss: () -> Unit,
    onConfirmDelete: () -> Unit,
) {
    BottomSheetDialog(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 58.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Delete this cue?",
                    color = Ink,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "This cue and its event history will be permanently removed.",
                    color = InkSoft,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            ElevatedPanel(
                color = Ivory,
                contentPadding = PaddingValues(14.dp),
            ) {
                Text(
                    text = memo.content,
                    color = Ink,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = memoSummaryLine(memo),
                    color = InkSoft,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 7.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
            ) {
                SecondaryButton(text = "Cancel", onClick = onDismiss)
                PrimaryButton(text = "Delete", onClick = onConfirmDelete, color = Clay)
            }
        }
    }
}

@Composable
private fun BottomSheetDialog(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val surfaceInteraction = remember { MutableInteractionSource() }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .clickable(onClick = onDismiss),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 36.dp)
                    .navigationBarsPadding()
                    .clickable(
                        interactionSource = surfaceInteraction,
                        indication = null,
                        onClick = {},
                    ),
                color = Porcelain,
                shape = RoundedCornerShape(24.dp),
                shadowElevation = 18.dp,
            ) {
                content()
            }
        }
    }
}

@Composable
private fun ElevatedPanel(
    modifier: Modifier = Modifier,
    color: Color = Porcelain,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    border: BorderStroke = BorderStroke(1.dp, Line.copy(alpha = 0.58f)),
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = border,
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun NoticePanel(
    title: String,
    content: String,
    tone: Color,
) {
    ElevatedPanel(
        color = Porcelain,
        contentPadding = PaddingValues(15.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(42.dp)
                    .background(tone, RoundedCornerShape(999.dp)),
            )
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(text = title, color = Ink, fontWeight = FontWeight.Bold)
                Text(text = content, color = InkSoft, fontSize = 14.sp, lineHeight = 20.sp)
            }
        }
    }
}

@Composable
private fun EmptyPanel(message: String) {
    ElevatedPanel {
        Text(
            text = message,
            color = InkSoft,
            fontSize = 15.sp,
            modifier = Modifier.padding(vertical = 8.dp),
        )
    }
}

@Composable
private fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Forest,
    trailingIcon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Porcelain,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        Text(text = text, fontWeight = FontWeight.Bold)
        if (trailingIcon != null) {
            Spacer(Modifier.width(8.dp))
            Icon(trailingIcon, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Forest,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color.copy(alpha = 0.10f),
            contentColor = color,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = PaddingValues(horizontal = 18.dp),
    ) {
        Text(text = text, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatusChip(
    text: String,
    color: Color,
    dark: Boolean = false,
) {
    Surface(
        color = if (dark) Color.White.copy(alpha = 0.14f) else color.copy(alpha = 0.11f),
        shape = RoundedCornerShape(999.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(if (dark) Porcelain else color, CircleShape),
            )
            Text(
                text = text,
                color = if (dark) Porcelain else color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SyncStatusChip(syncStatus: String) {
    val active = syncStatus.startsWith("Syncing as ") || syncStatus.contains("signed in")
    StatusChip(
        text = if (active) "Synced" else "Local",
        color = if (active) Forest else Brass,
    )
}

@Composable
private fun MetadataChip(icon: ImageVector, text: String) {
    Surface(
        color = ForestSoft,
        shape = RoundedCornerShape(999.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = Forest, modifier = Modifier.size(16.dp))
            Text(text = text, color = Forest, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CounterChip(text: String) {
    Surface(
        color = Stone.copy(alpha = 0.52f),
        shape = RoundedCornerShape(999.dp),
    ) {
        Text(
            text = text,
            color = InkSoft,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun NumberMarker(
    number: Int,
    pinned: Boolean,
    color: Color,
    textModifier: Modifier = Modifier,
) {
    Surface(
        modifier = Modifier
            .padding(top = 1.dp)
            .size(width = 34.dp, height = 28.dp),
        color = if (pinned) Line.copy(alpha = 0.42f) else color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, if (pinned) Line.copy(alpha = 0.72f) else color.copy(alpha = 0.18f)),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "#${number.toString().padStart(2, '0')}",
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = textModifier,
            )
        }
    }
}

@Composable
private fun SmallMarker(color: Color) {
    Box(
        modifier = Modifier
            .padding(top = 5.dp)
            .size(10.dp)
            .background(color.copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .background(color, CircleShape),
        )
    }
}

@Composable
private fun FlowLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            color = InkSoft,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = value,
            color = Ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
        )
    }
}

private fun MemoScreen.navLabel(): String {
    return when (this) {
        MemoScreen.CURRENT -> "Today"
        MemoScreen.HISTORY -> "Archive"
        MemoScreen.REMINDER -> "Rhythm"
        MemoScreen.ACCOUNT -> "Account"
    }
}

private fun MemoScreen.icon(): ImageVector {
    return when (this) {
        MemoScreen.CURRENT -> Icons.Outlined.Home
        MemoScreen.HISTORY -> Icons.Outlined.Archive
        MemoScreen.REMINDER -> Icons.Outlined.Schedule
        MemoScreen.ACCOUNT -> Icons.Outlined.AccountCircle
    }
}

private fun memoSummaryLine(memo: Memo): String {
    return if (memo.isActive) {
        val label = if (memo.updatedAtMs > memo.createdAtMs) "Updated" else "Added"
        "$label ${formatTimestamp(memo.updatedAtMs)}"
    } else {
        "Completed ${formatTimestamp(memo.clearedAtMs)}"
    }
}

private fun formatTimestamp(timestampMs: Long?): String {
    if (timestampMs == null) {
        return "Never"
    }
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(timestampMs))
}
