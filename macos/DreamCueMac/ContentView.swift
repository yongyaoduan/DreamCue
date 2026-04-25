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
        HStack(spacing: 0) {
            sidebar
            Divider().overlay(DreamCueStyle.border)
            detail
        }
        .background(DreamCueStyle.canvas)
        .frame(minWidth: 980, minHeight: 650)
        .sheet(isPresented: $isNewCuePresented) {
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
        .sheet(item: $selectedMemo) { memo in
            CueDetailSheet(
                memo: memo,
                draft: $detailDraft,
                reminderTime: store.reminderTimeText,
                selectedHour: $selectedHour,
                selectedMinute: $selectedMinute,
                onSaveTime: {
                    store.setReminderTime(hour: selectedHour, minute: selectedMinute)
                },
                onSave: {
                    store.updateMemo(memo, content: detailDraft)
                    selectedMemo = nil
                },
                onClear: {
                    store.clearMemo(memo)
                    selectedMemo = nil
                },
                onDelete: {
                    store.deleteMemo(memo)
                    selectedMemo = nil
                }
            )
        }
        .sheet(isPresented: $isTimePickerPresented) {
            ReminderTimePickerSheet(
                hour: $selectedHour,
                minute: $selectedMinute,
                onCancel: {
                    isTimePickerPresented = false
                },
                onSave: {
                    store.setReminderTime(hour: selectedHour, minute: selectedMinute)
                    isTimePickerPresented = false
                }
            )
        }
    }

    private var sidebar: some View {
        VStack(alignment: .leading, spacing: 18) {
            HStack(spacing: 12) {
                DreamCueIcon(size: 46)
                VStack(alignment: .leading, spacing: 2) {
                    Text("DreamCue")
                        .font(.system(size: 24, weight: .regular, design: .serif))
                        .foregroundStyle(DreamCueStyle.ink)
                    Text("Private cues. Gentle rhythm.")
                        .font(.caption)
                        .foregroundStyle(DreamCueStyle.muted)
                }
            }
            .padding(.top, 18)
            .padding(.bottom, 8)

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

            Spacer()

            HStack(spacing: 8) {
                Circle()
                    .fill(store.isSyncActive ? DreamCueStyle.sage : DreamCueStyle.border)
                    .frame(width: 8, height: 8)
                VStack(alignment: .leading, spacing: 2) {
                    Text(store.isSyncActive ? "Syncing" : "Local")
                        .font(.caption.weight(.semibold))
                    Text(store.isSyncActive ? "Up to date" : "Sign in to sync")
                        .font(.caption2)
                        .foregroundStyle(DreamCueStyle.muted)
                }
            }
            .padding(12)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(DreamCueStyle.panel.opacity(0.85), in: RoundedRectangle(cornerRadius: 8))
            .overlay(RoundedRectangle(cornerRadius: 8).stroke(DreamCueStyle.border, lineWidth: 1))
        }
        .padding(.horizontal, 14)
        .frame(width: 215)
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
                onOpen: openMemo
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
                quietHoursEnabled: $store.quietHoursEnabled,
                reminderTime: store.reminderTimeText,
                quietHours: store.quietHoursText,
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
}

private struct TodayView: View {
    let currentMemos: [Memo]
    let reminderTime: String
    let onNewCue: () -> Void
    let onOpen: (Memo) -> Void

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
                            ForEach(currentMemos) { memo in
                                CueRow(memo: memo, onOpen: onOpen)
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
                        ForEach(memos) { memo in
                            ArchiveRow(memo: memo, onOpen: onOpen)
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
    @Binding var quietHoursEnabled: Bool
    let reminderTime: String
    let quietHours: String
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
                HStack {
                    VStack(alignment: .leading, spacing: 6) {
                        Toggle("Quiet Hours", isOn: $quietHoursEnabled)
                            .toggleStyle(.switch)
                            .accessibilityIdentifier("Quiet Hours Toggle")
                        Text("Mute reminders during your rest.")
                            .font(.caption)
                            .foregroundStyle(DreamCueStyle.muted)
                    }
                    Spacer()
                    Text(quietHours)
                        .font(.body.monospacedDigit())
                        .foregroundStyle(DreamCueStyle.ink)
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
            Text(isSyncActive ? "Private sync is active." : "Private sync across devices.")
                .foregroundStyle(DreamCueStyle.muted)

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
        .frame(maxWidth: 560, alignment: .leading)
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
            Text("Private sync is active on this account.")
                .foregroundStyle(.white.opacity(0.72))
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
        .frame(maxWidth: 560, alignment: .leading)
        .background(DreamCueStyle.deepGreen, in: RoundedRectangle(cornerRadius: 12))
        .shadow(color: .black.opacity(0.14), radius: 18, y: 8)
    }
}

private struct NewCueSheet: View {
    @Binding var draft: String
    let onCancel: () -> Void
    let onSave: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack {
                Text("New Cue")
                    .font(.headline)
                Spacer()
            }
            TextEditor(text: $draft)
                .font(.body)
                .scrollContentBackground(.hidden)
                .padding(10)
                .background(DreamCueStyle.panel, in: RoundedRectangle(cornerRadius: 8))
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(DreamCueStyle.border, lineWidth: 1))
                .frame(width: 520, height: 260)
                .accessibilityLabel("Cue Text")
                .accessibilityIdentifier("Cue Text")
            HStack {
                Spacer()
                Button("Cancel", action: onCancel)
                Button("Save", action: onSave)
                    .buttonStyle(PrimaryButtonStyle())
                    .keyboardShortcut(.return, modifiers: .command)
            }
        }
        .padding(20)
        .background(DreamCueStyle.canvas)
    }
}

private struct CueDetailSheet: View {
    let memo: Memo
    @Binding var draft: String
    let reminderTime: String
    @Binding var selectedHour: Int
    @Binding var selectedMinute: Int
    let onSaveTime: () -> Void
    let onSave: () -> Void
    let onClear: () -> Void
    let onDelete: () -> Void
    @State private var isTimePickerPresented = false

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
            }
            .padding(18)

            TextEditor(text: $draft)
                .font(.body)
                .scrollContentBackground(.hidden)
                .padding(12)
                .frame(height: 230)
                .background(DreamCueStyle.panel, in: RoundedRectangle(cornerRadius: 8))
                .overlay(RoundedRectangle(cornerRadius: 8).stroke(DreamCueStyle.border, lineWidth: 1))
                .padding(.horizontal, 18)
                .accessibilityLabel("Cue Text")
                .accessibilityIdentifier("Cue Text")

            HStack {
                Image("IconRemind")
                    .resizable()
                    .scaledToFit()
                    .frame(width: 22, height: 22)
                Text("Reminder scheduled")
                    .font(.caption.weight(.semibold))
                Text("Today, \(reminderTime)")
                    .font(.caption)
                    .foregroundStyle(DreamCueStyle.muted)
                Spacer()
                Button("Change Time") {
                    isTimePickerPresented = true
                }
            }
            .padding(12)
            .background(DreamCueStyle.sage.opacity(0.12))
            .padding(.horizontal, 18)
            .padding(.top, 10)

            HStack(spacing: 12) {
                IconActionButton(title: "Delete", accessibility: "Delete Cue", assetName: "IconDelete", action: onDelete)
                IconActionButton(title: "Remind", accessibility: "Remind", assetName: "IconRemind") {
                    isTimePickerPresented = true
                }
                IconActionButton(title: "Complete", accessibility: "Complete Cue", assetName: "IconComplete", action: onClear)
            }
            .padding(18)
        }
        .frame(width: 590)
        .background(DreamCueStyle.canvas)
        .sheet(isPresented: $isTimePickerPresented) {
            ReminderTimePickerSheet(
                hour: $selectedHour,
                minute: $selectedMinute,
                onCancel: {
                    isTimePickerPresented = false
                },
                onSave: {
                    onSaveTime()
                    isTimePickerPresented = false
                }
            )
        }
    }
}

private struct ReminderTimePickerSheet: View {
    @Binding var hour: Int
    @Binding var minute: Int
    let onCancel: () -> Void
    let onSave: () -> Void

    private let minutes = [0, 10, 15, 30, 45, 50]

    var body: some View {
        VStack(spacing: 18) {
            Text("Select reminder time")
                .font(.headline)
            HStack(spacing: 18) {
                DateChip(day: "Wed", date: "Apr 23", isSelected: false)
                DateChip(day: "Thu", date: "Apr 24", isSelected: false)
                DateChip(day: "Fri", date: "Apr 25", isSelected: true)
                DateChip(day: "Sat", date: "Apr 26", isSelected: false)
                DateChip(day: "Sun", date: "Apr 27", isSelected: false)
            }
            HStack(spacing: 24) {
                RollingNumberColumn(
                    title: "Hour",
                    values: Array(0..<24),
                    selection: $hour
                )

                Text(":")
                    .font(.title.weight(.bold))
                    .foregroundStyle(DreamCueStyle.deepGreen)

                RollingNumberColumn(
                    title: "Minute",
                    values: minutes,
                    selection: $minute
                )
            }
            .padding(.vertical, 8)
            .background(DreamCueStyle.panel, in: RoundedRectangle(cornerRadius: 12))
            HStack(spacing: 12) {
                Button("Cancel", action: onCancel)
                    .buttonStyle(SecondaryButtonStyle())
                Button("Save Time", action: onSave)
                    .buttonStyle(PrimaryButtonStyle())
            }
        }
        .padding(24)
        .background(DreamCueStyle.canvas)
    }
}

private struct RollingNumberColumn: View {
    let title: String
    let values: [Int]
    @Binding var selection: Int

    var body: some View {
        ScrollViewReader { proxy in
            ScrollView(.vertical) {
                VStack(spacing: 0) {
                    ForEach(values, id: \.self) { value in
                        Button {
                            selection = value
                            withAnimation(.snappy(duration: 0.18)) {
                                proxy.scrollTo(value, anchor: .center)
                            }
                        } label: {
                            Text(String(format: "%02d", value))
                                .font(.title3.monospacedDigit().weight(selection == value ? .bold : .regular))
                                .foregroundStyle(selection == value ? DreamCueStyle.deepGreen : DreamCueStyle.muted)
                                .frame(width: 84, height: 34)
                                .background(selection == value ? DreamCueStyle.selected : .clear, in: RoundedRectangle(cornerRadius: 7))
                        }
                        .buttonStyle(.plain)
                        .id(value)
                    }
                }
                .padding(.vertical, 58)
            }
            .frame(width: 94, height: 150)
            .background(
                LinearGradient(
                    colors: [DreamCueStyle.panel.opacity(0.15), DreamCueStyle.panel, DreamCueStyle.panel.opacity(0.15)],
                    startPoint: .top,
                    endPoint: .bottom
                ),
                in: RoundedRectangle(cornerRadius: 12)
            )
            .overlay(RoundedRectangle(cornerRadius: 12).stroke(DreamCueStyle.border.opacity(0.7), lineWidth: 1))
            .accessibilityLabel(title)
            .onAppear {
                proxy.scrollTo(selection, anchor: .center)
            }
        }
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
                Spacer()
            }
            .foregroundStyle(isSelected ? DreamCueStyle.deepGreen : DreamCueStyle.ink)
            .padding(.horizontal, 12)
            .padding(.vertical, 10)
            .background(isSelected ? DreamCueStyle.selected : .clear, in: RoundedRectangle(cornerRadius: 8))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(title)
    }
}

private struct CueRow: View {
    let memo: Memo
    let onOpen: (Memo) -> Void

    var body: some View {
        Button {
            onOpen(memo)
        } label: {
            HStack(spacing: 14) {
                Circle()
                    .fill(memo.isActive ? DreamCueStyle.deepGreen : DreamCueStyle.gold)
                    .frame(width: 7, height: 7)
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
            .background(DreamCueStyle.panel, in: RoundedRectangle(cornerRadius: 10))
            .overlay(RoundedRectangle(cornerRadius: 10).stroke(DreamCueStyle.border, lineWidth: 1))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(memo.content)
    }
}

private struct ArchiveRow: View {
    let memo: Memo
    let onOpen: (Memo) -> Void

    var body: some View {
        Button {
            onOpen(memo)
        } label: {
            HStack(spacing: 12) {
                Image(systemName: memo.isActive ? "doc.text" : "archivebox")
                    .foregroundStyle(DreamCueStyle.muted)
                    .frame(width: 22)
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
            .padding(.horizontal, 14)
            .padding(.vertical, 12)
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
        HStack(spacing: 16) {
            RoundedRectangle(cornerRadius: 3)
                .fill(DreamCueStyle.gold.opacity(0.24))
                .frame(width: 5)
            VStack(alignment: .leading, spacing: 8) {
                Text(title)
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(DreamCueStyle.ink)
                Text(content)
                    .foregroundStyle(DreamCueStyle.muted)
            }
            Spacer()
            if let iconName {
                Image(systemName: iconName)
                    .font(.title3.weight(.semibold))
                    .foregroundStyle(DreamCueStyle.deepGreen)
                    .frame(width: 46, height: 46)
                    .background(DreamCueStyle.selected, in: Circle())
            }
        }
        .padding(18)
        .frame(maxWidth: 560, alignment: .leading)
        .background(DreamCueStyle.panel, in: RoundedRectangle(cornerRadius: 12))
        .overlay(RoundedRectangle(cornerRadius: 12).stroke(DreamCueStyle.border, lineWidth: 1))
    }
}

private struct RhythmCard<Content: View>: View {
    @ViewBuilder let content: Content

    var body: some View {
        content
            .padding(18)
            .frame(maxWidth: 560, alignment: .leading)
            .background(DreamCueStyle.panel, in: RoundedRectangle(cornerRadius: 10))
            .overlay(RoundedRectangle(cornerRadius: 10).stroke(DreamCueStyle.border, lineWidth: 1))
    }
}

private struct IconActionButton: View {
    let title: String
    let accessibility: String
    let assetName: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 4) {
                Image(assetName)
                    .resizable()
                    .scaledToFit()
                    .frame(width: 24, height: 24)
                Text(title)
                    .font(.caption)
                    .foregroundStyle(DreamCueStyle.ink)
            }
            .frame(maxWidth: .infinity, minHeight: 58)
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

private struct DateChip: View {
    let day: String
    let date: String
    let isSelected: Bool

    var body: some View {
        VStack(spacing: 3) {
            Text(day)
                .font(.caption.weight(.semibold))
            Text(date)
                .font(.caption2)
        }
        .foregroundStyle(isSelected ? .white : DreamCueStyle.muted)
        .padding(.horizontal, 12)
        .padding(.vertical, 8)
        .background(isSelected ? DreamCueStyle.deepGreen : DreamCueStyle.panel, in: RoundedRectangle(cornerRadius: 8))
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(DreamCueStyle.border, lineWidth: 1))
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
