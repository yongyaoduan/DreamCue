package app.dreamcue.ui

import android.app.TimePickerDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.dreamcue.model.Memo
import app.dreamcue.model.SearchResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

private val DreamCueColorScheme = lightColorScheme(
    primary = Color(0xFF214E4D),
    onPrimary = Color(0xFFFDF8F1),
    secondary = Color(0xFFC4694E),
    background = Color(0xFFF6F1E8),
    surface = Color(0xFFFFFBF5),
    onSurface = Color(0xFF1D2523),
    outline = Color(0xFFB6AFA1),
)

@OptIn(ExperimentalMaterial3Api::class)
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
    val context = LocalContext.current
    val latestSelectedScreen = rememberUpdatedState(state.selectedScreen)
    val pagerState = rememberPagerState(
        initialPage = state.selectedScreen.ordinal,
        pageCount = { MemoScreen.entries.size },
    )
    val showSearchResults =
        state.submittedSearchQuery.isNotBlank() && state.submittedSearchQuery == state.searchQuery

    LaunchedEffect(state.selectedScreen) {
        val targetPage = state.selectedScreen.ordinal
        if (pagerState.currentPage != targetPage && pagerState.targetPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .filter { it in MemoScreen.entries.indices }
            .distinctUntilChanged()
            .map { MemoScreen.entries[it] }
            .collectLatest { screen ->
                if (screen != latestSelectedScreen.value) {
                    onSelectScreen(screen)
                }
            }
    }

    MaterialTheme(colorScheme = DreamCueColorScheme) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TopAppBar(
                        title = {
                            Text("DreamCue", fontWeight = FontWeight.Bold)
                        },
                    )
                },
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    ScreenSelector(
                        selectedScreen = state.selectedScreen,
                        onSelectScreen = onSelectScreen,
                    )

                    if (state.isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }

                    state.nativeError?.let { nativeError ->
                        MessageCard(
                            title = "Rust core is not loaded",
                            content = nativeError,
                            tone = MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f),
                        )
                    }

                    state.errorMessage?.let { errorMessage ->
                        MessageCard(
                            title = "Notice",
                            content = errorMessage,
                            tone = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        )
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    ) { page ->
                        when (MemoScreen.entries[page]) {
                            MemoScreen.CURRENT -> CurrentScreen(
                                state = state,
                                onDraftChange = onDraftChange,
                                onAddMemo = onAddMemo,
                                onOpenMemo = onOpenMemoDetail,
                            )

                            MemoScreen.SEARCH -> SearchScreen(
                                state = state,
                                showSearchResults = showSearchResults,
                                onSearchQueryChange = onSearchQueryChange,
                                onRunSearch = onRunSearch,
                                onOpenMemo = onOpenMemoDetail,
                            )

                            MemoScreen.HISTORY -> HistoryScreen(
                                state = state,
                                onOpenMemo = onOpenMemoDetail,
                            )

                            MemoScreen.REMINDER -> ReminderScreen(
                                state = state,
                                onSyncEmailChange = onSyncEmailChange,
                                onSyncPasswordChange = onSyncPasswordChange,
                                onSignInSync = onSignInSync,
                                onCreateSyncAccount = onCreateSyncAccount,
                                onSignOutSync = onSignOutSync,
                                showTimePicker = {
                                    TimePickerDialog(
                                        context,
                                        { _, hour, minute -> onSaveReminderTime(hour, minute) },
                                        state.reminderTime.hour,
                                        state.reminderTime.minute,
                                        true,
                                    ).show()
                                },
                            )
                        }
                    }
                }
            }
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
                onDelete = {
                    onRequestDelete(memo)
                },
            )
        }

        state.pendingDeleteMemo?.let { memo ->
            AlertDialog(
                onDismissRequest = onDismissDeleteRequest,
                title = { Text("Delete this memo?") },
                text = {
                    Text("\"${memo.content}\" and its history will be permanently removed.")
                },
                confirmButton = {
                    Button(onClick = onConfirmDelete) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismissDeleteRequest) {
                        Text("Cancel")
                    }
                },
            )
        }
    }
}

@Composable
private fun CurrentScreen(
    state: MainUiState,
    onDraftChange: (String) -> Unit,
    onAddMemo: () -> Unit,
    onOpenMemo: (Memo) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionHeader(
                title = MemoScreen.CURRENT.title,
                subtitle = MemoScreen.CURRENT.subtitle,
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Write a New Memo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Keep the original wording. Short notes work best.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    )
                    OutlinedTextField(
                        value = state.draft,
                        onValueChange = onDraftChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Memo") },
                        placeholder = { Text("Example: Pick up the package before Tuesday evening") },
                        minLines = 3,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Button(onClick = onAddMemo) {
                            Text("Save")
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Active Memos",
                subtitle = "${state.currentMemos.size} items, sorted by recent updates",
            )
        }

        if (state.currentMemos.isEmpty()) {
            item {
                EmptyCard("No active memos yet")
            }
        } else {
            items(items = state.currentMemos, key = { "current-${it.id}" }) { memo ->
                MemoCard(
                    memo = memo,
                    accent = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                    onOpen = { onOpenMemo(memo) },
                )
            }
        }
    }
}

@Composable
private fun SearchScreen(
    state: MainUiState,
    showSearchResults: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onRunSearch: () -> Unit,
    onOpenMemo: (Memo) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionHeader(
                title = MemoScreen.SEARCH.title,
                subtitle = MemoScreen.SEARCH.subtitle,
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search") },
                        placeholder = { Text("Example: meeting, payment, phone call") },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Button(onClick = onRunSearch) {
                            Text("Search")
                        }
                    }
                }
            }
        }

        when {
            showSearchResults -> {
                item {
                    SectionHeader(
                        title = "Search Results",
                        subtitle = "Sorted by relevance, ${state.searchResults.size} items",
                    )
                }
                if (state.searchResults.isEmpty()) {
                    item {
                        EmptyCard("No matching memos found")
                    }
                } else {
                    items(items = state.searchResults, key = { "search-${it.memo.id}" }) { result ->
                        SearchResultCard(
                            result = result,
                            onOpen = { onOpenMemo(result.memo) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(
    state: MainUiState,
    onOpenMemo: (Memo) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionHeader(
                title = MemoScreen.HISTORY.title,
                subtitle = MemoScreen.HISTORY.subtitle,
            )
        }

        if (state.historyMemos.isEmpty()) {
            item {
                EmptyCard("No cleared memos yet")
            }
        } else {
            items(items = state.historyMemos, key = { "history-${it.id}" }) { memo ->
                HistoryMemoCard(
                    memo = memo,
                    onOpen = { onOpenMemo(memo) },
                )
            }
        }
    }
}

@Composable
private fun ReminderScreen(
    state: MainUiState,
    onSyncEmailChange: (String) -> Unit,
    onSyncPasswordChange: (String) -> Unit,
    onSignInSync: () -> Unit,
    onCreateSyncAccount: () -> Unit,
    onSignOutSync: () -> Unit,
    showTimePicker: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SectionHeader(
                title = MemoScreen.REMINDER.title,
                subtitle = MemoScreen.REMINDER.subtitle,
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Current reminder time: ${state.reminderTime.asText()}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "DreamCue sends one notification for each active memo at that time.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        Button(onClick = showTimePicker) {
                            Text("Choose Time")
                        }
                    }
                }
            }
        }

        item {
            MessageCard(
                title = "Reminder Permissions",
                content = "If reminders do not appear, allow notifications and exact alarms in system settings.",
                tone = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Sync Account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = state.syncStatus,
                        style = MaterialTheme.typography.bodyMedium,
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    ) {
                        TextButton(onClick = onSignOutSync) {
                            Text("Sign Out")
                        }
                        TextButton(onClick = onCreateSyncAccount) {
                            Text("Create Account")
                        }
                        Button(onClick = onSignInSync) {
                            Text("Sign In")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenSelector(
    selectedScreen: MemoScreen,
    onSelectScreen: (MemoScreen) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(28.dp),
            )
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        MemoScreen.entries.forEach { screen ->
            val selected = selectedScreen == screen
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            Color.Transparent
                        },
                        shape = RoundedCornerShape(22.dp),
                    )
                    .clickable { onSelectScreen(screen) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = screen.navLabel(),
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        if (subtitle.isNotBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
            )
        }
    }
}

@Composable
private fun MessageCard(title: String, content: String, tone: Color) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = tone),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(text = title, fontWeight = FontWeight.Bold)
            Text(text = content, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun EmptyCard(message: String) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Text(
                text = message,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
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
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Delete",
                        )
                    }
                }
                OutlinedTextField(
                    value = draft,
                    onValueChange = onDraftChange,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 6,
                    placeholder = { Text("Write your memo") },
                )
                FlowLine("Added", formatTimestamp(memo.createdAtMs))
                FlowLine("Last Modified", formatTimestamp(memo.updatedAtMs))
                if (!memo.isActive) {
                    FlowLine("Cleared", formatTimestamp(memo.clearedAtMs))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    FilledTonalIconButton(onClick = onPrimaryAction) {
                        Icon(
                            imageVector = if (memo.isActive) {
                                Icons.Outlined.TaskAlt
                            } else {
                                Icons.AutoMirrored.Outlined.Undo
                            },
                            contentDescription = if (memo.isActive) "Clear" else "Reopen",
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    result: SearchResult,
    onOpen: () -> Unit,
) {
    Card(
        modifier = Modifier.clickable(onClick = onOpen),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = result.memo.content,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(10.dp))
                StatusPill(if (result.memo.isActive) "Current" else "History")
            }
            Text(
                text = memoSummaryLine(result.memo),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun MemoCard(
    memo: Memo,
    accent: Color,
    onOpen: () -> Unit,
) {
    Card(
        modifier = Modifier.clickable(onClick = onOpen),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(accent, RoundedCornerShape(16.dp))
                    .padding(14.dp),
            ) {
                Text(
                    text = memo.content,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HistoryMemoCard(
    memo: Memo,
    onOpen: () -> Unit,
) {
    Card(
        modifier = Modifier.clickable(onClick = onOpen),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = memo.content,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = memoSummaryLine(memo),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun StatusPill(text: String) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun FlowLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun MemoScreen.navLabel(): String {
    return when (this) {
        MemoScreen.CURRENT -> "Current"
        MemoScreen.SEARCH -> "Search"
        MemoScreen.HISTORY -> "History"
        MemoScreen.REMINDER -> "Settings"
    }
}

private fun memoSummaryLine(memo: Memo): String {
    return if (memo.isActive) {
        val label = if (memo.updatedAtMs > memo.createdAtMs) "Updated" else "Added"
        "$label ${formatTimestamp(memo.updatedAtMs)}"
    } else {
        "Cleared ${formatTimestamp(memo.clearedAtMs)}"
    }
}

private fun formatTimestamp(timestampMs: Long?): String {
    if (timestampMs == null) {
        return "Never"
    }
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return formatter.format(Date(timestampMs))
}
