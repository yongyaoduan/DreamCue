import AppKit
import SwiftUI

struct ContentView: View {
    @StateObject private var store = MemoStore()
    @State private var selectedMemo: Memo?
    @State private var detailDraft = ""
    @State private var isNewCuePresented = false
    @State private var isTimePickerPresented = false
    @State private var selectedHour = 21
    @State private var selectedMinute = 0

    var body: some View {
        ZStack {
            HStack(spacing: 0) {
                sidebar
                Divider().overlay(DreamCueStyle.border)
                detail
            }
            .background(DreamCueStyle.canvas)

            if isNewCuePresented {
                ModalOverlay(onDismiss: {
                    store.draft = ""
                    isNewCuePresented = false
                }) {
                    NewCueSheet(
                        draft: $store.draft,
                        onCancel: {
                            store.draft = ""
                            isNewCuePresented = false
                        },
                        onSave: {
                            store.addMemo()
                            isNewCuePresented = false
                        }
                    )
                }
            }

            if let memo = selectedMemo {
                ModalOverlay(onDismiss: dismissDetailSavingDraft) {
                    CueDetailSheet(
                        memo: memo,
                        draft: $detailDraft,
                        onSave: {
                            saveDetailAndDismiss(memo)
                        },
                        onClear: {
                            store.clearMemo(memo)
                            selectedMemo = nil
                        },
                        onReopen: {
                            store.reopenMemo(memo)
                            selectedMemo = nil
                        },
                        onSetPinned: { pinned in
                            store.setMemoPinned(memo, pinned: pinned)
                            selectedMemo = nil
                        },
                        onDelete: {
                            store.deleteMemo(memo)
                            selectedMemo = nil
                        }
                    )
                }
            }

            if isTimePickerPresented {
                ModalOverlay(onDismiss: {
                    isTimePickerPresented = false
                }) {
                    ReminderTimePickerSheet(
                        hour: $selectedHour,
                        minute: $selectedMinute,
                        onCancel: {
                            isTimePickerPresented = false
                        },
                        onTimeChanged: { hour, minute in
                            store.setReminderTime(hour: hour, minute: minute)
                        }
                    )
                }
            }
        }
        .frame(minWidth: 980, minHeight: 650)
        .background(AppFocusRestorer())
    }

    private var sidebar: some View {
        VStack(spacing: 18) {
            VStack(spacing: 6) {
                SidebarButton(title: "Today", systemImage: "house", isSelected: store.selectedTab == 0) {
                    store.selectedTab = 0
                }
                SidebarButton(title: "Archive", systemImage: "archivebox", isSelected: store.selectedTab == 1) {
                    store.selectedTab = 1
                }
                SidebarButton(title: "Rhythm", systemImage: "clock", isSelected: store.selectedTab == 2) {
                    store.selectedTab = 2
                }
                SidebarButton(title: "Account", systemImage: "person.crop.circle", isSelected: store.selectedTab == 3) {
                    store.selectedTab = 3
                }
            }
            .padding(.top, 18)
            .frame(maxWidth: .infinity, alignment: .topLeading)

            Spacer()
        }
        .frame(width: 140)
        .background(DreamCueStyle.sidebar)
    }

    @ViewBuilder
    private var detail: some View {
        switch store.selectedTab {
        case 0:
            TodayView(
                currentMemos: store.currentMemos,
                reminderTime: store.reminderTimeText,
                onNewCue: { isNewCuePresented = true },
                onOpen: openMemo,
                onMove: store.moveCurrentMemo
            )
        case 1:
            ArchiveView(
                query: $store.searchQuery,
                memos: store.searchResults,
                onOpen: openMemo
            )
        case 2:
            RhythmView(
                dailyReminderEnabled: $store.dailyReminderEnabled,
                reminderTime: store.reminderTimeText,
                onChangeTime: {
                    selectedHour = store.reminderHour
                    selectedMinute = store.reminderMinute
                    isTimePickerPresented = true
                }
            )
        default:
            AccountView(
                email: $store.syncEmail,
                password: $store.syncPassword,
                syncStatus: store.syncStatus,
                signedInEmail: store.syncDisplayEmail,
                isSyncActive: store.isSyncActive,
                onSignIn: store.signIn,
                onCreate: store.createAccount,
                onSignOut: store.signOut
            )
        }
    }

    private func openMemo(_ memo: Memo) {
        detailDraft = memo.content
        selectedHour = store.reminderHour
        selectedMinute = store.reminderMinute
        selectedMemo = memo
    }

    private func saveDetailAndDismiss(_ memo: Memo) {
        store.updateMemo(memo, content: detailDraft)
        selectedMemo = nil
    }

    private func dismissDetailSavingDraft() {
        guard let memo = selectedMemo else { return }
        let trimmed = detailDraft.trimmingCharacters(in: .whitespacesAndNewlines)
        if !trimmed.isEmpty, trimmed != memo.content {
            store.updateMemo(memo, content: detailDraft)
        }
        selectedMemo = nil
    }
}

private struct AppFocusRestorer: NSViewRepresentable {
    func makeNSView(context: Context) -> NSView {
        let view = NSView()
        DispatchQueue.main.async {
            restoreFocus(for: view)
        }
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) {
            restoreFocus(for: view)
        }
        return view
    }

    func updateNSView(_ nsView: NSView, context: Context) {}

    private func restoreFocus(for view: NSView) {
        view.window?.makeKeyAndOrderFront(nil)
        NSApp.setActivationPolicy(.regular)
        NSApp.activate(ignoringOtherApps: true)
    }
}

private struct TodayView: View {
    let currentMemos: [Memo]
    let reminderTime: String
    let onNewCue: () -> Void
    let onOpen: (Memo) -> Void
    let onMove: (Int, Int) -> Void

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Good evening")
                            .font(.system(size: 26, weight: .regular, design: .serif))
                            .foregroundStyle(DreamCueStyle.ink)
                        Text("Capture what matters. We'll keep it in rhythm.")
                            .foregroundStyle(DreamCueStyle.muted)
                    }
                    Spacer()
                    HStack(spacing: 0) {
                        StatBlock(title: "Daily rhythm", value: reminderTime, caption: "Every day", systemImage: "clock")
                        Divider().frame(height: 58)
                        StatBlock(title: "Active cues", value: "\(currentMemos.count)", caption: "Awaiting reminder", systemImage: "bell")
                    }
                    .padding(16)
                    .frame(width: 360)
                    .background(DreamCueStyle.panel, in: RoundedRectangle(cornerRadius: 10))
                    .overlay(RoundedRectangle(cornerRadius: 10).stroke(DreamCueStyle.border, lineWidth: 1))
                }

                Button(action: onNewCue) {
                    HStack(spacing: 16) {
                        DreamCueIcon(size: 54)
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Capture a cue...")
                                .font(.headline)
                            Text("Press Return to save, ⌘+N for a new cue")
                                .font(.caption)
                                .foregroundStyle(.white.opacity(0.74))
                        }
                        Spacer()
                        Image(systemName: "plus")
                            .font(.title3.weight(.semibold))
                            .foregroundStyle(DreamCueStyle.ink)
                            .frame(width: 44, height: 44)
                            .background(DreamCueStyle.gold, in: Circle())
                    }
                    .foregroundStyle(.white)
                    .padding(18)
                    .background(DreamCueStyle.deepGreen, in: RoundedRectangle(cornerRadius: 10))
                    .shadow(color: .black.opacity(0.12), radius: 18, y: 8)
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("New Cue")

                VStack(alignment: .leading, spacing: 12) {
                    HStack {
                        Text("Active Cues")
                            .font(.title2.weight(.semibold))
                        Spacer()
                        Text("Recent")
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(DreamCueStyle.muted)
                    }

                    if currentMemos.isEmpty {
                        EmptyCueCard()
                    } else {
                        VStack(spacing: 10) {
                            ForEach(Array(currentMemos.enumerated()), id: \.element.id) { index, memo in
                                CueRow(
                                    memo: memo,
                                    displayNumber: index + 1,
                                    index: index,
                                    rowCount: currentMemos.count,
                                    onOpen: onOpen,
                                    onMove: onMove
                                )
                            }
                        }
                    }
                }
            }
            .padding(32)
            .frame(maxWidth: .infinity, alignment: .topLeading)
        }
    }
}

private struct ArchiveView: View {
    @Binding var query: String
    let memos: [Memo]
    let onOpen: (Memo) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            HStack(spacing: 16) {
                Text("Archive")
                    .font(.system(size: 28, weight: .regular, design: .serif))
                    .foregroundStyle(DreamCueStyle.ink)
                Spacer()
                SearchField(text: $query, placeholder: "Search cues...")
                    .frame(width: 280, height: 32)
            }

            HStack(spacing: 10) {
                FilterPill(title: "All Status")
                FilterPill(title: "All Time")
                Spacer()
                Text("\(memos.count) cues")
                    .font(.caption)
                    .foregroundStyle(DreamCueStyle.muted)
            }

            VStack(spacing: 0) {
                HStack {
                    Text("Title")
                    Spacer()
                    Text("Updated ↓")
                        .frame(width: 145, alignment: .leading)
                }
                .font(.caption.weight(.semibold))
                .foregroundStyle(DreamCueStyle.muted)
                .padding(.horizontal, 14)
                .padding(.vertical, 10)

                Divider().overlay(DreamCueStyle.border)

                ScrollView {
                    VStack(spacing: 0) {
                        ForEach(Array(memos.enumerated()), id: \.element.id) { index, memo in
                            ArchiveRow(memo: memo, displayNumber: index + 1, onOpen: onOpen)
                            Divider().overlay(DreamCueStyle.border.opacity(0.6))
                        }
                    }
                }
            }
            .background(DreamCueStyle.panel, in: RoundedRectangle(cornerRadius: 10))
            .overlay(RoundedRectangle(cornerRadius: 10).stroke(DreamCueStyle.border, lineWidth: 1))
        }
        .padding(32)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }
}

private struct RhythmView: View {
    @Binding var dailyReminderEnabled: Bool
    let reminderTime: String
    let onChangeTime: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text("Rhythm")
                .font(.system(size: 30, weight: .regular, design: .serif))
                .foregroundStyle(DreamCueStyle.ink)

            RhythmCard {
                HStack {
                    VStack(alignment: .leading, spacing: 6) {
                        Toggle("Daily Reminder", isOn: $dailyReminderEnabled)
                            .toggleStyle(.switch)
                            .accessibilityIdentifier("Daily Reminder Toggle")
                        Text("Get a gentle reminder every day.")
                            .font(.caption)
                            .foregroundStyle(DreamCueStyle.muted)
                    }
                    Spacer()
                    HStack(spacing: 10) {
                        Text(reminderTime)
                            .font(.title2.monospacedDigit())
                        Button("Change Time", action: onChangeTime)
                    }
                }
            }

            RhythmCard {
                HStack(spacing: 16) {
                    VStack(alignment: .leading, spacing: 6) {
                        Text("Preview")
                            .font(.headline)
                        Text(dailyReminderEnabled ? "You'll be reminded daily at \(reminderTime)." : "Daily reminders are off.")
                            .foregroundStyle(DreamCueStyle.muted)
                    }
                    Spacer()
                    DreamCueIcon(size: 68)
                }
            }

            Spacer()
        }
        .padding(32)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }
}

private struct AccountView: View {
    @Binding var email: String
    @Binding var password: String
    let syncStatus: String
    let signedInEmail: String
    let isSyncActive: Bool
    let onSignIn: () -> Void
    let onCreate: () -> Void
    let onSignOut: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text("Account")
                .font(.system(size: 30, weight: .regular, design: .serif))
                .foregroundStyle(DreamCueStyle.ink)

            if isSyncActive {
                signedInCard
                InfoCard(
                    title: "Sync Health",
                    content: "Cue changes stay private to this account and sync automatically.",
                    iconName: "checkmark"
                )
            } else {
                signInCard
                InfoCard(
                    title: "Tenant-scoped privacy",
                    content: "Cue sync stays private to the signed-in account.",
                    iconName: nil
                )
            }

            Spacer()
        }
        .padding(32)
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }

    private var signInCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            Text("Sync Account")
                .font(.title2.weight(.semibold))
            Text(syncStatus)
                .foregroundStyle(DreamCueStyle.muted)
            TextField("Email", text: $email)
                .textFieldStyle(.roundedBorder)
                .frame(maxWidth: 420)
            SecureField("Password", text: $password)
                .textFieldStyle(.roundedBorder)
                .frame(maxWidth: 420)
            HStack(spacing: 10) {
                Button("Create", action: onCreate)
                    .buttonStyle(SecondaryButtonStyle())
                Button("Sign In", action: onSignIn)
                    .buttonStyle(PrimaryButtonStyle())
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(DreamCueStyle.panel, in: RoundedRectangle(cornerRadius: 12))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(DreamCueStyle.border, lineWidth: 1))
        .shadow(color: .black.opacity(0.08), radius: 14, y: 6)
    }

    private var signedInCard: some View {
        VStack(alignment: .leading, spacing: 18) {
            HStack {
                Text("Tenant-scoped")
                    .font(.caption.weight(.bold))
                    .padding(.horizontal, 12)
                    .padding(.vertical, 7)
                    .background(.white.opacity(0.16), in: Capsule())
                Spacer()
                Image("IconRemind")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 34, height: 34)
                    .padding(14)
                    .background(.white.opacity(0.14), in: Circle())
            }
            Text(compactAccountEmail(signedInEmail))
                    .font(.system(size: 24, weight: .bold))
                    .lineLimit(1)
                    .minimumScaleFactor(0.75)
            HStack(alignment: .bottom) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Last sync")
                        .foregroundStyle(.white.opacity(0.66))
                    Text("just now")
                        .font(.headline)
                }
                Spacer()
                Button("Sign Out", action: onSignOut)
                    .buttonStyle(DarkOutlineButtonStyle())
            }
        }
        .foregroundStyle(.white)
        .padding(22)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(DreamCueStyle.deepGreen, in: RoundedRectangle(cornerRadius: 12))
        .shadow(color: .black.opacity(0.14), radius: 18, y: 8)
    }
}

private struct ModalOverlay<Content: View>: View {
    let onDismiss: () -> Void
    @ViewBuilder let content: Content

    var body: some View {
        ZStack {
            Color.black.opacity(0.22)
                .ignoresSafeArea()
                .onTapGesture(perform: onDismiss)
            content
        }
        .transition(.opacity)
    }
}

private struct CommittingTextEditor: NSViewRepresentable {
    @Binding var text: String
    let accessibilityLabel: String
    let onCommit: () -> Void

    func makeNSView(context: Context) -> NSScrollView {
        let textView = CommitTextView()
        textView.onCommit = onCommit
        textView.delegate = context.coordinator
        textView.font = .systemFont(ofSize: NSFont.systemFontSize)
        textView.textColor = NSColor(DreamCueStyle.ink)
        textView.backgroundColor = .clear
        textView.drawsBackground = false
        textView.isRichText = false
        textView.allowsUndo = true
        textView.textContainerInset = NSSize(width: 12, height: 12)
        textView.textContainer?.widthTracksTextView = true
        textView.setAccessibilityLabel(accessibilityLabel)
        textView.setAccessibilityIdentifier(accessibilityLabel)

        let scrollView = NSScrollView()
        scrollView.drawsBackground = false
        scrollView.hasVerticalScroller = true
        scrollView.autohidesScrollers = true
        scrollView.scrollerStyle = .overlay
        scrollView.documentView = textView
        return scrollView
    }

    func updateNSView(_ scrollView: NSScrollView, context: Context) {
        context.coordinator.update(text: $text, onCommit: onCommit)
        guard let textView = scrollView.documentView as? CommitTextView else { return }
        textView.onCommit = onCommit
        guard textView.string != text else { return }
        textView.string = text
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(text: $text, onCommit: onCommit)
    }

    final class Coordinator: NSObject, NSTextViewDelegate {
        private var text: Binding<String>
        private var onCommit: () -> Void

        init(text: Binding<String>, onCommit: @escaping () -> Void) {
            self.text = text
            self.onCommit = onCommit
        }

        func update(text: Binding<String>, onCommit: @escaping () -> Void) {
            self.text = text
            self.onCommit = onCommit
        }

        func textDidChange(_ notification: Notification) {
            guard let textView = notification.object as? NSTextView else { return }
            text.wrappedValue = textView.string
        }

        func textView(_ textView: NSTextView, doCommandBy commandSelector: Selector) -> Bool {
            if commandSelector == #selector(NSResponder.insertNewline(_:)) {
                onCommit()
                return true
            }
            return false
        }
    }
}

private final class CommitTextView: NSTextView {
    var onCommit: (() -> Void)?

    override func keyDown(with event: NSEvent) {
        let isReturn = event.keyCode == 36 || event.keyCode == 76
        guard isReturn else {
            super.keyDown(with: event)
            return
        }

        if event.modifierFlags.contains(.command) {
            insertText("\n", replacementRange: selectedRange())
        } else {
            onCommit?()
        }
    }
}

private struct NewCueSheet: View {
    @Binding var draft: String
    let onCancel: () -> Void
    let onSave: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 10) {
                Text("New Cue")
                    .font(.headline)
                Spacer()
                Button("Cancel", action: onCancel)
                    .buttonStyle(SecondaryButtonStyle())
                Button("Save", action: onSave)
                    .buttonStyle(PrimaryButtonStyle())
                    .keyboardShortcut(.return, modifiers: [])
            }
            CommittingTextEditor(text: $draft, accessibilityLabel: "Cue Text", onCommit: onSave)
                .frame(maxWidth: .infinity)
                .frame(height: 245)
                .background(DreamCueStyle.panel, in: RoundedRectangle(cornerRadius: 8))
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(DreamCueStyle.border, lineWidth: 1))
        }
        .padding(22)
        .frame(width: 560, alignment: .leading)
        .background(DreamCueStyle.canvas)
        .clipShape(RoundedRectangle(cornerRadius: 22))
        .shadow(color: .black.opacity(0.22), radius: 24, y: 12)
    }
}

private struct CueDetailSheet: View {
    let memo: Memo
    @Binding var draft: String
    let onSave: () -> Void
    let onClear: () -> Void
    let onReopen: () -> Void
    let onSetPinned: (Bool) -> Void
    let onDelete: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 10) {
                DreamCueIcon(size: 24)
                VStack(alignment: .leading, spacing: 1) {
                    Text(memo.content)
                        .font(.headline)
                        .lineLimit(1)
                    Text("Edited just now")
                        .font(.caption)
                        .foregroundStyle(DreamCueStyle.muted)
                }
                Spacer()
                Button("Save", action: onSave)
                    .buttonStyle(PrimaryButtonStyle())
                    .keyboardShortcut(.return, modifiers: [])
            }
            .padding(16)

            CommittingTextEditor(text: $draft, accessibilityLabel: "Cue Text", onCommit: onSave)
                .frame(height: 176)
                .background(DreamCueStyle.panel, in: RoundedRectangle(cornerRadius: 8))
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(DreamCueStyle.border, lineWidth: 1))
                .padding(.horizontal, 16)

            HStack(spacing: 12) {
                IconActionButton(accessibility: "Delete Cue", assetName: "IconDelete", tint: DreamCueStyle.deleteRed, action: onDelete)
                if memo.isActive {
                    IconActionButton(
                        accessibility: memo.pinned ? "Unpin Cue" : "Pin Cue",
                        assetName: "IconPin",
                        tint: DreamCueStyle.muted,
                        action: { onSetPinned(!memo.pinned) }
                    )
                    IconActionButton(accessibility: "Complete Cue", assetName: "IconComplete", tint: DreamCueStyle.deepGreen, action: onClear)
                } else {
                    IconActionButton(accessibility: "Return to Today", assetName: "IconReturn", tint: DreamCueStyle.muted, action: onReopen)
                }
            }
            .padding(16)
        }
        .frame(width: 520)
        .background(DreamCueStyle.canvas)
        .clipShape(RoundedRectangle(cornerRadius: 22))
        .shadow(color: .black.opacity(0.22), radius: 24, y: 12)
    }
}

private struct ReminderTimePickerSheet: View {
    @Binding var hour: Int
    @Binding var minute: Int
    @State private var hourText: String
    @State private var minuteText: String
    let onCancel: () -> Void
    let onTimeChanged: (Int, Int) -> Void

    init(
        hour: Binding<Int>,
        minute: Binding<Int>,
        onCancel: @escaping () -> Void,
        onTimeChanged: @escaping (Int, Int) -> Void
    ) {
        _hour = hour
        _minute = minute
        _hourText = State(initialValue: String(format: "%02d", hour.wrappedValue))
        _minuteText = State(initialValue: String(format: "%02d", minute.wrappedValue))
        self.onCancel = onCancel
        self.onTimeChanged = onTimeChanged
    }

    var body: some View {
        VStack(spacing: 18) {
            Text("Select reminder time")
                .font(.headline)

            Text(String(format: "%02d:%02d", hour, minute))
                .font(.title2.monospacedDigit().weight(.semibold))
                .foregroundStyle(DreamCueStyle.deepGreen)
                .accessibilityLabel("timePicker.value")
                .accessibilityValue(String(format: "%02d:%02d", hour, minute))
                .accessibilityIdentifier("timePicker.value")

            HStack(alignment: .center, spacing: 12) {
                TimeNumberControl(
                    title: "Hour",
                    text: $hourText,
                    decrementIdentifier: "timePicker.hourDecrement",
                    fieldIdentifier: "timePicker.hourField",
                    incrementIdentifier: "timePicker.hourIncrement",
                    onCommit: applyHourText,
                    onTextChange: applyHourDraft,
                    onDecrement: { setHour(hour - 1) },
                    onIncrement: { setHour(hour + 1) }
                )
                Text(":")
                    .font(.title.weight(.semibold))
                    .foregroundStyle(DreamCueStyle.deepGreen)
                    .padding(.top, 18)
                TimeNumberControl(
                    title: "Minute",
                    text: $minuteText,
                    decrementIdentifier: "timePicker.minuteDecrement",
                    fieldIdentifier: "timePicker.minuteField",
                    incrementIdentifier: "timePicker.minuteIncrement",
                    onCommit: applyMinuteText,
                    onTextChange: applyMinuteDraft,
                    onDecrement: { setMinute(minute - 5) },
                    onIncrement: { setMinute(minute + 5) }
                )
            }
            Button("Done") {
                applyHourText()
                applyMinuteText()
                onCancel()
            }
                .buttonStyle(PrimaryButtonStyle())
                .accessibilityLabel("timePicker.doneButton")
                .accessibilityIdentifier("timePicker.doneButton")
        }
        .padding(24)
        .frame(width: 390)
        .background(DreamCueStyle.canvas)
        .clipShape(RoundedRectangle(cornerRadius: 22))
        .shadow(color: .black.opacity(0.22), radius: 24, y: 12)
        .accessibilityIdentifier("timePicker")
    }

    private func applyHourText() {
        setHour(Int(hourText) ?? hour)
    }

    private func applyHourDraft() {
        if let value = Int(hourText) {
            hour = min(max(value, 0), 23)
            onTimeChanged(hour, minute)
        }
    }

    private func applyMinuteText() {
        setMinute(Int(minuteText) ?? minute)
    }

    private func applyMinuteDraft() {
        if let value = Int(minuteText) {
            minute = min(max(value, 0), 59)
            onTimeChanged(hour, minute)
        }
    }

    private func setHour(_ value: Int) {
        hour = min(max(value, 0), 23)
        hourText = String(format: "%02d", hour)
        onTimeChanged(hour, minute)
    }

    private func setMinute(_ value: Int) {
        minute = min(max(value, 0), 59)
        minuteText = String(format: "%02d", minute)
        onTimeChanged(hour, minute)
    }
}

private struct TimeNumberControl: View {
    let title: String
    @Binding var text: String
    let decrementIdentifier: String
    let fieldIdentifier: String
    let incrementIdentifier: String
    let onCommit: () -> Void
    let onTextChange: () -> Void
    let onDecrement: () -> Void
    let onIncrement: () -> Void

    var body: some View {
        VStack(spacing: 8) {
            Text(title)
                .font(.caption.weight(.semibold))
                .foregroundStyle(DreamCueStyle.muted)
            HStack(spacing: 8) {
                Button("-", action: onDecrement)
                    .buttonStyle(TimeStepButtonStyle())
                    .accessibilityLabel(decrementIdentifier)
                    .accessibilityIdentifier(decrementIdentifier)
                TimeTextField(
                    text: $text,
                    identifier: fieldIdentifier,
                    onCommit: onCommit,
                    onTextChange: onTextChange
                )
                    .frame(width: 54, height: 38)
                    .background(DreamCueStyle.panel, in: RoundedRectangle(cornerRadius: 8))
                    .overlay(RoundedRectangle(cornerRadius: 8).stroke(DreamCueStyle.border, lineWidth: 1))
                Button("+", action: onIncrement)
                    .buttonStyle(TimeStepButtonStyle())
                    .accessibilityLabel(incrementIdentifier)
                    .accessibilityIdentifier(incrementIdentifier)
            }
        }
    }
}

private struct TimeTextField: NSViewRepresentable {
    @Binding var text: String
    let identifier: String
    let onCommit: () -> Void
    let onTextChange: () -> Void

    func makeCoordinator() -> Coordinator {
        Coordinator(text: $text, onCommit: onCommit, onTextChange: onTextChange)
    }

    func makeNSView(context: Context) -> NSTextField {
        let field = TimeEntryField(string: text)
        field.delegate = context.coordinator
        field.target = context.coordinator
        field.action = #selector(Coordinator.commitAction(_:))
        field.isBordered = false
        field.drawsBackground = false
        field.alignment = .center
        field.font = NSFont.monospacedDigitSystemFont(ofSize: 17, weight: .semibold)
        field.focusRingType = .none
        field.usesSingleLineMode = true
        field.lineBreakMode = .byClipping
        field.setAccessibilityIdentifier(identifier)
        field.setAccessibilityLabel(identifier)
        field.setAccessibilityValue(text)
        return field
    }

    func updateNSView(_ field: NSTextField, context: Context) {
        context.coordinator.text = $text
        context.coordinator.onCommit = onCommit
        context.coordinator.onTextChange = onTextChange
        if field.stringValue != text {
            field.stringValue = text
        }
        field.setAccessibilityIdentifier(identifier)
        field.setAccessibilityLabel(identifier)
        field.setAccessibilityValue(text)
    }

    final class Coordinator: NSObject, NSTextFieldDelegate {
        var text: Binding<String>
        var onCommit: () -> Void
        var onTextChange: () -> Void

        init(text: Binding<String>, onCommit: @escaping () -> Void, onTextChange: @escaping () -> Void) {
            self.text = text
            self.onCommit = onCommit
            self.onTextChange = onTextChange
        }

        func controlTextDidChange(_ notification: Notification) {
            guard let field = notification.object as? NSTextField else { return }
            text.wrappedValue = field.stringValue
            field.setAccessibilityValue(field.stringValue)
            onTextChange()
        }

        func controlTextDidEndEditing(_ notification: Notification) {
            guard let field = notification.object as? NSTextField else { return }
            text.wrappedValue = field.stringValue
            field.setAccessibilityValue(field.stringValue)
            onCommit()
        }

        @objc func commitAction(_ sender: NSTextField) {
            text.wrappedValue = sender.stringValue
            sender.setAccessibilityValue(sender.stringValue)
            onCommit()
        }

        func control(_ control: NSControl, textView: NSTextView, doCommandBy commandSelector: Selector) -> Bool {
            if commandSelector == #selector(NSResponder.insertNewline(_:)) {
                text.wrappedValue = textView.string
                control.setAccessibilityValue(textView.string)
                onCommit()
                control.window?.makeFirstResponder(nil)
                return true
            }
            return false
        }
    }
}

private final class TimeEntryField: NSTextField {
    override func mouseDown(with event: NSEvent) {
        window?.makeFirstResponder(self)
        super.mouseDown(with: event)
    }

    override func becomeFirstResponder() -> Bool {
        let accepted = super.becomeFirstResponder()
        if accepted {
            currentEditor()?.selectAll(nil)
        }
        return accepted
    }
}

private struct SidebarButton: View {
    let title: String
    let systemImage: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 10) {
                Image(systemName: systemImage)
                    .font(.system(size: 15, weight: .medium))
                    .frame(width: 22)
                Text(title)
                    .font(.system(size: 14, weight: .medium))
                    .lineLimit(1)
                Spacer(minLength: 0)
            }
            .foregroundStyle(isSelected ? DreamCueStyle.deepGreen : DreamCueStyle.ink)
            .padding(.leading, 24)
            .padding(.trailing, 12)
            .frame(height: 38)
            .frame(maxWidth: .infinity, alignment: .leading)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(isSelected ? DreamCueStyle.selected : .clear)
        .contentShape(Rectangle())
        .accessibilityLabel(title)
    }
}

private struct CueRow: View {
    let memo: Memo
    let displayNumber: Int
    let index: Int
    let rowCount: Int
    let onOpen: (Memo) -> Void
    let onMove: (Int, Int) -> Void
    @State private var dragOffset: CGFloat = 0

    var body: some View {
        Button {
            onOpen(memo)
        } label: {
            HStack(spacing: 14) {
                Text("#\(String(format: "%02d", displayNumber))")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(memo.isActive ? DreamCueStyle.deepGreen : DreamCueStyle.gold)
                    .frame(width: 38, height: 26)
                    .background(memo.pinned ? .white.opacity(0.62) : DreamCueStyle.selected, in: RoundedRectangle(cornerRadius: 8))
                VStack(alignment: .leading, spacing: 5) {
                    Text(memo.content)
                        .font(.headline)
                        .foregroundStyle(DreamCueStyle.ink)
                        .lineLimit(1)
                    Text(memo.isActive ? "Added \(formattedDate(memo.createdAtMs))" : "Updated \(formattedDate(memo.updatedAtMs))")
                        .font(.caption)
                        .foregroundStyle(DreamCueStyle.muted)
                }
                Spacer()
                if memo.pinned {
                    Image(systemName: "pin.fill")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(DreamCueStyle.muted)
                }
                Text(memo.isActive ? "Current" : "Cleared")
                    .font(.caption.weight(.semibold))
                    .padding(.horizontal, 10)
                    .padding(.vertical, 5)
                    .background(DreamCueStyle.selected, in: Capsule())
                Image(systemName: "chevron.right")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(DreamCueStyle.muted)
            }
            .padding(14)
            .background(memo.pinned ? DreamCueStyle.border.opacity(0.28) : DreamCueStyle.panel, in: RoundedRectangle(cornerRadius: 10))
            .overlay(RoundedRectangle(cornerRadius: 10).stroke(DreamCueStyle.border, lineWidth: 1))
            .contentShape(RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(memo.content)
        .accessibilityValue("#\(String(format: "%02d", displayNumber))")
        .offset(y: dragOffset)
        .gesture(
            DragGesture(minimumDistance: 8)
                .onChanged { value in
                    dragOffset = value.translation.height
                }
                .onEnded { value in
                    let rowStep = Int((value.translation.height / 78).rounded())
                    let target = min(max(index + rowStep, 0), rowCount - 1)
                    dragOffset = 0
                    if target != index {
                        onMove(index, target)
                    }
                }
        )
    }
}

private struct ArchiveRow: View {
    let memo: Memo
    let displayNumber: Int
    let onOpen: (Memo) -> Void

    var body: some View {
        Button {
            onOpen(memo)
        } label: {
            HStack(spacing: 12) {
                Text("#\(String(format: "%02d", displayNumber))")
                    .font(.caption.weight(.bold))
                    .foregroundStyle(memo.isActive ? DreamCueStyle.deepGreen : DreamCueStyle.gold)
                    .frame(width: 38, height: 24)
                    .background(memo.pinned ? DreamCueStyle.border.opacity(0.28) : DreamCueStyle.selected, in: RoundedRectangle(cornerRadius: 8))
                VStack(alignment: .leading, spacing: 4) {
                    Text(memo.content)
                        .font(.body.weight(.semibold))
                        .foregroundStyle(DreamCueStyle.ink)
                        .lineLimit(1)
                    Text(memo.isActive ? "Active cue" : "Completed cue")
                        .font(.caption)
                        .foregroundStyle(DreamCueStyle.muted)
                }
                Spacer()
                if memo.pinned {
                    Image(systemName: "pin.fill")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(DreamCueStyle.muted)
                }
                Text(memo.isActive ? "Current" : "Cleared")
                    .font(.caption)
                    .foregroundStyle(DreamCueStyle.deepGreen)
                    .padding(.horizontal, 9)
                    .padding(.vertical, 4)
                    .background(DreamCueStyle.selected, in: Capsule())
            Text(formattedDate(memo.updatedAtMs))
                .font(.caption)
                .foregroundStyle(DreamCueStyle.muted)
                .frame(width: 145, alignment: .leading)
            Image(systemName: "chevron.right")
                .foregroundStyle(DreamCueStyle.muted)
        }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(memo.content)
    }
}

private struct StatBlock: View {
    let title: String
    let value: String
    let caption: String
    let systemImage: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: systemImage)
                .font(.title3)
                .foregroundStyle(DreamCueStyle.deepGreen)
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(DreamCueStyle.muted)
                Text(value)
                    .font(.title2.monospacedDigit().weight(.semibold))
                    .foregroundStyle(DreamCueStyle.ink)
                Text(caption)
                    .font(.caption2)
                    .foregroundStyle(DreamCueStyle.muted)
            }
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }
}

private struct EmptyCueCard: View {
    var body: some View {
        VStack(spacing: 14) {
            DreamCueIcon(size: 82)
            Text("A quiet place for what matters.")
                .font(.system(size: 24, weight: .regular, design: .serif))
                .foregroundStyle(DreamCueStyle.ink)
            Text("Capture a short cue and DreamCue will bring it back at the right time.")
                .foregroundStyle(DreamCueStyle.muted)
        }
        .padding(36)
        .frame(maxWidth: .infinity)
        .background(DreamCueStyle.panel, in: RoundedRectangle(cornerRadius: 12))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(DreamCueStyle.border, lineWidth: 1))
    }
}

private struct InfoCard: View {
    let title: String
    let content: String
    let iconName: String?

    var body: some View {
        HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 3)
                .fill(DreamCueStyle.gold.opacity(0.24))
                .frame(width: 4, height: 30)
            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(.callout.weight(.semibold))
                    .foregroundStyle(DreamCueStyle.ink)
                Text(content)
                    .font(.caption)
                    .foregroundStyle(DreamCueStyle.muted)
            }
            Spacer()
            if let iconName {
                Image(systemName: iconName)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(DreamCueStyle.deepGreen)
                    .frame(width: 30, height: 30)
                    .background(DreamCueStyle.selected, in: Circle())
            }
        }
        .padding(10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(DreamCueStyle.panel, in: RoundedRectangle(cornerRadius: 12))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(DreamCueStyle.border, lineWidth: 1))
    }
}

private struct RhythmCard<Content: View>: View {
    @ViewBuilder let content: Content

    var body: some View {
        content
            .padding(18)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(DreamCueStyle.panel, in: RoundedRectangle(cornerRadius: 10))
            .overlay(RoundedRectangle(cornerRadius: 10).stroke(DreamCueStyle.border, lineWidth: 1))
    }
}

private struct IconActionButton: View {
    let accessibility: String
    let assetName: String
    let tint: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(assetName)
                .resizable()
                .renderingMode(.template)
                .scaledToFit()
                .frame(width: 21, height: 21)
                .foregroundStyle(tint)
            .frame(maxWidth: .infinity, minHeight: 48)
            .background(DreamCueStyle.panel, in: RoundedRectangle(cornerRadius: 8))
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(DreamCueStyle.border, lineWidth: 1))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(accessibility)
    }
}

private struct FilterPill: View {
    let title: String

    var body: some View {
        Text(title)
            .font(.caption.weight(.medium))
            .foregroundStyle(DreamCueStyle.muted)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(DreamCueStyle.panel, in: Capsule())
            .overlay(Capsule().stroke(DreamCueStyle.border, lineWidth: 1))
    }
}

private struct DreamCueIcon: View {
    let size: CGFloat

    var body: some View {
        Image("DreamCueIcon")
            .resizable()
            .scaledToFit()
            .frame(width: size, height: size)
            .accessibilityLabel("DreamCue leaf mark")
    }
}

private struct SearchField: NSViewRepresentable {
    @Binding var text: String
    let placeholder: String

    func makeNSView(context: Context) -> NSSearchField {
        let field = NSSearchField()
        field.placeholderString = placeholder
        field.delegate = context.coordinator
        field.controlSize = .regular
        field.bezelStyle = .roundedBezel
        field.setAccessibilityLabel(placeholder)
        return field
    }

    func updateNSView(_ nsView: NSSearchField, context: Context) {
        if nsView.stringValue != text {
            nsView.stringValue = text
        }
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(text: $text)
    }

    final class Coordinator: NSObject, NSSearchFieldDelegate {
        @Binding var text: String

        init(text: Binding<String>) {
            _text = text
        }

        func controlTextDidChange(_ notification: Notification) {
            guard let field = notification.object as? NSSearchField else { return }
            text = field.stringValue
        }
    }
}

private struct PrimaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.body.weight(.semibold))
            .foregroundStyle(.white)
            .padding(.horizontal, 22)
            .padding(.vertical, 10)
            .background(configuration.isPressed ? DreamCueStyle.deepGreen.opacity(0.86) : DreamCueStyle.deepGreen, in: RoundedRectangle(cornerRadius: 8))
    }
}

private struct SecondaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.body.weight(.semibold))
            .foregroundStyle(DreamCueStyle.deepGreen)
            .padding(.horizontal, 22)
            .padding(.vertical, 10)
            .background(configuration.isPressed ? DreamCueStyle.selected.opacity(0.7) : DreamCueStyle.selected, in: RoundedRectangle(cornerRadius: 8))
    }
}

private struct TimeStepButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.title3.weight(.semibold))
            .foregroundStyle(DreamCueStyle.deepGreen)
            .frame(width: 34, height: 34)
            .background(configuration.isPressed ? DreamCueStyle.selected.opacity(0.7) : DreamCueStyle.selected, in: RoundedRectangle(cornerRadius: 8))
    }
}

private struct DarkOutlineButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.body.weight(.semibold))
            .foregroundStyle(.white)
            .padding(.horizontal, 20)
            .padding(.vertical, 9)
            .background(configuration.isPressed ? .white.opacity(0.15) : .clear, in: RoundedRectangle(cornerRadius: 8))
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(.white.opacity(0.42), lineWidth: 1))
    }
}

private enum DreamCueStyle {
    static let canvas = Color(red: 0.965, green: 0.953, blue: 0.925)
    static let sidebar = Color(red: 0.94, green: 0.935, blue: 0.905)
    static let panel = Color(red: 0.988, green: 0.982, blue: 0.958)
    static let selected = Color(red: 0.86, green: 0.92, blue: 0.89)
    static let deepGreen = Color(red: 0.047, green: 0.240, blue: 0.180)
    static let sage = Color(red: 0.180, green: 0.460, blue: 0.330)
    static let gold = Color(red: 0.790, green: 0.640, blue: 0.420)
    static let deleteRed = Color(red: 0.770, green: 0.380, blue: 0.290)
    static let ink = Color(red: 0.080, green: 0.120, blue: 0.105)
    static let muted = Color(red: 0.360, green: 0.400, blue: 0.380)
    static let border = Color(red: 0.825, green: 0.800, blue: 0.735)
}

private func compactAccountEmail(_ email: String) -> String {
    let trimmed = email.trimmingCharacters(in: .whitespacesAndNewlines)
    guard let atIndex = trimmed.firstIndex(of: "@") else { return trimmed }
    let local = String(trimmed[..<atIndex])
    let domain = String(trimmed[atIndex...])
    if local.count <= 12 {
        return trimmed
    }
    return "\(local.prefix(12))...\(domain)"
}

private func formattedDate(_ milliseconds: Int64) -> String {
    let date = Date(timeIntervalSince1970: TimeInterval(milliseconds) / 1000)
    let formatter = DateFormatter()
    formatter.dateFormat = "MMM d, HH:mm"
    return formatter.string(from: date)
}
