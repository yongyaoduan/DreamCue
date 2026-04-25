package app.dreamcue.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.dreamcue.R
import app.dreamcue.model.Memo
import app.dreamcue.model.SearchResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF15201D)
private val InkSoft = Color(0xFF58635E)
private val Ivory = Color(0xFFF6F3EC)
private val Porcelain = Color(0xFFFFFCF6)
private val Stone = Color(0xFFE9E3D8)
private val Forest = Color(0xFF0F4B3A)
private val ForestSoft = Color(0xFFE0EEE8)
private val Brass = Color(0xFFC9A46A)
private val Clay = Color(0xFFC4614B)
private val Line = Color(0xFFD9D1C4)
private val DeepForest = Color(0xFF0F3D2E)
private val ScreenPadding = PaddingValues(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 18.dp)

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
) {
    var showCaptureSheet by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    val showSearchResults =
        state.submittedSearchQuery.isNotBlank() && state.submittedSearchQuery == state.searchQuery

    MaterialTheme(
        colorScheme = DreamCueColorScheme,
        typography = MaterialTheme.typography,
    ) {
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
                            onOpenCapture = { showCaptureSheet = true },
                            onOpenMemo = onOpenMemoDetail,
                        )

                        MemoScreen.SEARCH -> RecallScreen(
                            state = state,
                            showSearchResults = showSearchResults,
                            onSearchQueryChange = onSearchQueryChange,
                            onRunSearch = onRunSearch,
                            onOpenMemo = onOpenMemoDetail,
                        )

                        MemoScreen.HISTORY -> ArchiveScreen(
                            state = state,
                            onOpenMemo = onOpenMemoDetail,
                        )

                        MemoScreen.REMINDER -> RhythmScreen(
                            state = state,
                            onOpenTimePicker = { showTimePicker = true },
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
                    onDraftChange = onDetailDraftChange,
                    onDismiss = onDismissMemoDetail,
                    onPrimaryAction = {
                        if (memo.isActive) {
                            onClearMemo(memo.id)
                        } else {
                            onReopenMemo(memo.id)
                        }
                    },
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
    onOpenCapture: () -> Unit,
    onOpenMemo: (Memo) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = ScreenPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            HomeTopBar()
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
            items(items = state.currentMemos, key = { "today-${it.id}" }) { memo ->
                CueCard(
                    memo = memo,
                    statusLabel = "Current",
                    accent = Forest,
                    onOpen = { onOpenMemo(memo) },
                )
            }
        }
    }
}

@Composable
private fun RecallScreen(
    state: MainUiState,
    showSearchResults: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onRunSearch: () -> Unit,
    onOpenMemo: (Memo) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = ScreenPadding,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            AppHeader(
                title = "Recall",
                subtitle = "Ask or search anything.",
                trailing = { IconBadge(Icons.Outlined.Search, "Search") },
            )
        }

        item {
            ElevatedPanel {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Ask or search anything") },
                    placeholder = { Text("Example: Firebase billing, dentist, release notes") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                        )
                    },
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    PrimaryButton(
                        text = "Run Search",
                        onClick = onRunSearch,
                    )
                }
            }
        }

        if (showSearchResults) {
            item {
                SectionTitle(
                    title = "Top Matches",
                    subtitle = "${state.searchResults.size} relevance-ranked cues",
                )
            }
            if (state.searchResults.isEmpty()) {
                item {
                    EmptyPanel("No matching cues found")
                }
            } else {
                items(items = state.searchResults, key = { "recall-${it.memo.id}" }) { result ->
                    SearchResultCueCard(
                        result = result,
                        onOpen = { onOpenMemo(result.memo) },
                    )
                }
            }
        } else {
            item {
                NoticePanel(
                    title = "Search memory, not folders",
                    content = "Recall looks across active cues and archive entries.",
                    tone = Forest.copy(alpha = 0.08f),
                )
            }
        }
    }
}

@Composable
private fun ArchiveScreen(
    state: MainUiState,
    onOpenMemo: (Memo) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
        contentPadding = ScreenPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            AppHeader(
                title = "Archive",
                subtitle = "Completed cues stay searchable.",
                trailing = { IconBadge(Icons.Outlined.Archive, "Archive") },
            )
        }

        item {
            SectionTitle(
                title = "Completed Cues",
                subtitle = "${state.historyMemos.size} archived items",
            )
        }

        if (state.historyMemos.isEmpty()) {
            item {
                EmptyPanel("No completed cues yet")
            }
        } else {
            items(items = state.historyMemos, key = { "archive-${it.id}" }) { memo ->
                TimelineCue(
                    memo = memo,
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
                trailing = { IconBadge(Icons.Outlined.NotificationsNone, "Reminder") },
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
                    StatusChip("Enabled", Forest)
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
            QuietHoursPanel()
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
                subtitle = if (signedIn) "Private sync is active." else "Private sync across devices.",
                trailing = { IconBadge(Icons.Outlined.Lock, "Private") },
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
                NoticePanel(
                    title = "Sync Health",
                    content = "Firestore listener is receiving account-owned cue changes only.",
                    tone = Forest.copy(alpha = 0.08f),
                )
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
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                    ) {
                        TextButton(onClick = onCreateSyncAccount) {
                            Text("Create")
                        }
                        PrimaryButton(
                            text = "Sign In",
                            onClick = onSignInSync,
                        )
                    }
                }
            }

            item {
                NoticePanel(
                    title = "Tenant-scoped privacy",
                    content = "Remote cues are isolated under users/{uid}/memos for the authenticated account.",
                    tone = Brass.copy(alpha = 0.15f),
                )
            }
        }
    }
}

@Composable
private fun DreamCueNavigation(
    selectedScreen: MemoScreen,
    onSelectScreen: (MemoScreen) -> Unit,
) {
    Surface(
        color = Porcelain.copy(alpha = 0.96f),
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 10.dp, vertical = 8.dp),
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
    val color = if (selected) Forest else InkSoft
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .background(if (selected) ForestSoft else Color.Transparent)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            imageVector = screen.icon(),
            contentDescription = screen.navLabel(),
            tint = color,
            modifier = Modifier.size(19.dp),
        )
        Text(
            text = screen.navLabel(),
            color = color,
            fontSize = 10.sp,
            lineHeight = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun AppHeader(
    title: String,
    subtitle: String,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = FontFamily.Serif,
                fontSize = 24.sp,
                lineHeight = 28.sp,
                color = Ink,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                color = InkSoft,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        trailing()
    }
}

@Composable
private fun HomeTopBar() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
    ) {
        IconButton(
            onClick = {},
            modifier = Modifier
                .size(38.dp)
                .align(Alignment.CenterStart),
        ) {
            Icon(
                imageVector = Icons.Outlined.Menu,
                contentDescription = "DreamCue menu",
                tint = InkSoft,
                modifier = Modifier.size(19.dp),
            )
        }
        Text(
            text = "DreamCue",
            modifier = Modifier.align(Alignment.Center),
            color = DeepForest,
            fontFamily = FontFamily.Serif,
            fontSize = 19.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.Medium,
        )
        IconButton(
            onClick = {},
            modifier = Modifier
                .size(38.dp)
                .align(Alignment.CenterEnd),
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsNone,
                contentDescription = "Reminder notification",
                tint = InkSoft,
                modifier = Modifier.size(19.dp),
            )
        }
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
                    text = "Capture a cue...",
                    color = Porcelain,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "Press to write a short memo",
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
    statusLabel: String,
    accent: Color,
    onOpen: () -> Unit,
) {
    ElevatedPanel(
        modifier = Modifier.clickable(onClick = onOpen),
        contentPadding = PaddingValues(15.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            SmallMarker(accent)
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
                    StatusChip(statusLabel, accent)
                }
                Text(
                    text = memoSummaryLine(memo),
                    color = InkSoft,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 7.dp),
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
private fun SearchResultCueCard(
    result: SearchResult,
    onOpen: () -> Unit,
) {
    val label = if (result.memo.isActive) "Current" else "History"
    val score = result.score.coerceIn(0.0, 1.0).toFloat().coerceAtLeast(0.18f)

    ElevatedPanel(
        modifier = Modifier.clickable(onClick = onOpen),
        contentPadding = PaddingValues(15.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null,
                tint = Forest,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        text = result.memo.content,
                        color = Ink,
                        fontSize = 15.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    StatusChip(label, if (result.memo.isActive) Forest else Brass)
                }
                Text(
                    text = result.matchedBy.joinToString().ifBlank { memoSummaryLine(result.memo) },
                    color = InkSoft,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 7.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .fillMaxWidth()
                        .height(5.dp)
                        .background(Stone, RoundedCornerShape(999.dp)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(score)
                            .fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(listOf(Forest, Brass)),
                                RoundedCornerShape(999.dp),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun TimelineCue(
    memo: Memo,
    onOpen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SmallMarker(Forest)
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(68.dp)
                    .background(Line),
            )
        }
        ElevatedPanel(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(15.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = memo.content,
                        color = Ink,
                        fontSize = 15.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = memoSummaryLine(memo),
                        color = InkSoft,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 7.dp),
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Undo,
                    contentDescription = "Restore",
                    tint = Forest,
                    modifier = Modifier.size(20.dp),
                )
            }
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
private fun QuietHoursPanel() {
    ElevatedPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Quiet hours",
                    color = Ink,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "23:00 - 07:00",
                    color = InkSoft,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            SoftSwitch(enabled = true)
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
        PermissionLine("Exact alarms", "Recommended")
        PermissionLine("Notifications", "Required")
        Text(
            text = "Reminders use exact alarms to show up reliably even on silent mode.",
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Forest,
        shape = RoundedCornerShape(28.dp),
        shadowElevation = 10.dp,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    StatusChip("Tenant scoped", Porcelain, dark = true)
                    Text(
                        text = email,
                        color = Porcelain,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                    Text(
                        text = syncStatus,
                        color = Porcelain.copy(alpha = 0.72f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 5.dp),
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.Sync,
                    contentDescription = null,
                    tint = Porcelain,
                    modifier = Modifier
                        .size(50.dp)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape)
                        .padding(12.dp),
                )
            }
            AccountMetaLine("Remote path", "users/{uid}/memos")
            AccountMetaLine("Last sync", "just now")
            AccountMetaLine("Pending uploads", "0")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
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

@Composable
private fun AccountMetaLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, color = Porcelain.copy(alpha = 0.68f), fontSize = 12.sp)
        Text(text = value, color = Porcelain, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
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
                .padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 42.dp),
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
                TextButton(onClick = onSave) {
                    Text("Save")
                }
            }
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(142.dp),
                label = { Text("Short memo") },
                placeholder = { Text("Design sync with Sarah") },
                minLines = 6,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MetadataChip(Icons.Outlined.Lock, "Private")
                MetadataChip(Icons.Outlined.Archive, "Stored locally")
                MetadataChip(Icons.Outlined.NotificationsNone, "${300 - draft.length}/300")
            }
            Surface(
                color = Ivory,
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(1.dp, Line.copy(alpha = 0.72f)),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(text = "Add details", color = Ink, fontWeight = FontWeight.Bold)
                    Text(text = "Context and timing can stay optional.", color = InkSoft, fontSize = 13.sp)
                }
            }
            PrimaryButton(
                text = "Save Cue",
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
                .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
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
                WheelColumn(
                    values = hourWheelValues(selectedHour),
                    selected = selectedHour,
                    label = { it.toString().padStart(2, '0') },
                    onSelect = { selectedHour = it },
                    modifier = Modifier.weight(1f),
                )
                WheelColumn(
                    values = minuteWheelValues(selectedMinute),
                    selected = selectedMinute,
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
private fun WheelColumn(
    values: List<Int>,
    selected: Int,
    label: (Int) -> String,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .height(168.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFFF1EADF))
            .border(1.dp, Line, RoundedCornerShape(24.dp)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        values.forEach { value ->
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isSelected) 48.dp else 30.dp)
                    .background(if (isSelected) Stone else Color.Transparent)
                    .clickable { onSelect(value) },
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

@Composable
private fun MemoDetailDialog(
    memo: Memo,
    draft: String,
    onDraftChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onPrimaryAction: () -> Unit,
    onDelete: () -> Unit,
) {
    BottomSheetDialog(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Outlined.Close, contentDescription = "Close")
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.StarBorder, contentDescription = "Favorite")
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "More")
                    }
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
            )
            ElevatedPanel(
                color = Ivory,
                contentPadding = PaddingValues(12.dp),
            ) {
                FlowLine("Created", formatTimestamp(memo.createdAtMs))
                FlowLine("Last updated", formatTimestamp(memo.updatedAtMs))
                FlowLine("Last reminded", formatTimestamp(memo.lastReviewedAtMs))
                if (!memo.isActive) {
                    FlowLine("Completed", formatTimestamp(memo.clearedAtMs))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                SecondaryButton(
                    text = "Delete",
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    color = Clay,
                )
                SecondaryButton(
                    text = if (memo.isActive) "Remind Later" else "Keep",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = if (memo.isActive) "Complete" else "Restore",
                    onClick = onPrimaryAction,
                    modifier = Modifier.weight(1f),
                    trailingIcon = if (memo.isActive) Icons.Outlined.TaskAlt else Icons.AutoMirrored.Outlined.Undo,
                )
            }
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Delete this memo?",
                        color = Ink,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "This cue and its event history will be permanently removed from local storage and remote sync.",
                        color = InkSoft,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = null,
                    tint = Clay,
                    modifier = Modifier
                        .size(42.dp)
                        .background(Clay.copy(alpha = 0.10f), CircleShape)
                        .padding(10.dp),
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 36.dp)
                    .navigationBarsPadding(),
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
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, Line.copy(alpha = 0.58f)),
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
        color = tone,
        contentPadding = PaddingValues(15.dp),
    ) {
        Text(text = title, color = Ink, fontWeight = FontWeight.Bold)
        Text(text = content, color = InkSoft, fontSize = 14.sp, lineHeight = 20.sp)
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
private fun IconBadge(icon: ImageVector, description: String) {
    Surface(
        modifier = Modifier.size(38.dp),
        shape = CircleShape,
        color = ForestSoft,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = DeepForest,
            modifier = Modifier.padding(9.dp),
        )
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
private fun SoftSwitch(enabled: Boolean) {
    Surface(
        color = if (enabled) Forest else Stone,
        shape = RoundedCornerShape(999.dp),
        modifier = Modifier.size(width = 46.dp, height = 28.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = if (enabled) Alignment.CenterEnd else Alignment.CenterStart,
        ) {
            Box(
                modifier = Modifier
                    .padding(4.dp)
                    .size(20.dp)
                    .background(Porcelain, CircleShape),
            )
        }
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
        MemoScreen.SEARCH -> "Recall"
        MemoScreen.HISTORY -> "Archive"
        MemoScreen.REMINDER -> "Rhythm"
        MemoScreen.ACCOUNT -> "Account"
    }
}

private fun MemoScreen.icon(): ImageVector {
    return when (this) {
        MemoScreen.CURRENT -> Icons.Outlined.Home
        MemoScreen.SEARCH -> Icons.Outlined.Search
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

private fun hourWheelValues(selectedHour: Int): List<Int> {
    return listOf(-2, -1, 0, 1, 2).map { offset ->
        (selectedHour + offset + 24) % 24
    }
}

private fun minuteWheelValues(selectedMinute: Int): List<Int> {
    return listOf(-15, -10, 0, 10, 15).map { offset ->
        (selectedMinute + offset + 60) % 60
    }
}
