import SwiftUI

struct ContentView: View {
    @StateObject private var store = MemoStore()
    @State private var selectedMemo: Memo?
    @State private var detailDraft = ""

    var body: some View {
        NavigationSplitView {
            List(selection: $store.selectedTab) {
                Label("Current", systemImage: "tray.full").tag(0)
                Label("Search", systemImage: "magnifyingglass").tag(1)
                Label("History", systemImage: "archivebox").tag(2)
                Label("Settings", systemImage: "gearshape").tag(3)
            }
            .navigationTitle("DreamCue")
        } detail: {
            switch store.selectedTab {
            case 0:
                currentView
            case 1:
                searchView
            case 2:
                historyView
            default:
                settingsView
            }
        }
        .sheet(item: $selectedMemo) { memo in
            MemoDetailView(
                memo: memo,
                draft: $detailDraft,
                onSave: {
                    store.updateMemo(memo, content: detailDraft)
                    selectedMemo = nil
                },
                onClearOrReopen: {
                    if memo.isActive {
                        store.clearMemo(memo)
                    } else {
                        store.reopenMemo(memo)
                    }
                    selectedMemo = nil
                },
                onDelete: {
                    store.deleteMemo(memo)
                    selectedMemo = nil
                }
            )
        }
    }

    private var currentView: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Current Memos").font(.largeTitle.bold())
            HStack {
                TextField("Memo", text: $store.draft, axis: .vertical)
                    .textFieldStyle(.roundedBorder)
                    .accessibilityIdentifier("memoInput")
                Button("Save") {
                    store.addMemo()
                }
                .keyboardShortcut(.return, modifiers: .command)
            }
            memoList(store.currentMemos)
        }
        .padding()
    }

    private var searchView: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Search Memos").font(.largeTitle.bold())
            TextField("Search", text: $store.searchQuery)
                .textFieldStyle(.roundedBorder)
                .accessibilityIdentifier("searchInput")
            memoList(store.searchResults)
        }
        .padding()
    }

    private var historyView: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("History").font(.largeTitle.bold())
            memoList(store.historyMemos)
        }
        .padding()
    }

    private var settingsView: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text("Settings").font(.largeTitle.bold())

            VStack(alignment: .leading, spacing: 10) {
                Text("Sync Account")
                    .font(.headline)
                Text(store.syncStatus)
                    .foregroundStyle(.secondary)

                Grid(alignment: .leadingFirstTextBaseline, horizontalSpacing: 12, verticalSpacing: 8) {
                    GridRow {
                        Text("Email")
                        TextField("Email", text: $store.syncEmail)
                            .textContentType(.emailAddress)
                            .frame(width: 360)
                    }
                    GridRow {
                        Text("Password")
                        SecureField("Password", text: $store.syncPassword)
                            .frame(width: 360)
                    }
                }

                HStack(spacing: 8) {
                    Button("Sign In") {
                        store.signIn()
                    }
                    Button("Create Account") {
                        store.createAccount()
                    }
                    Button("Sign Out") {
                        store.signOut()
                    }
                }
            }
            .padding(16)
            .background(Color(nsColor: .controlBackgroundColor))
            .clipShape(RoundedRectangle(cornerRadius: 10))

            Spacer()
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }

    private func memoList(_ memos: [Memo]) -> some View {
        List(memos) { memo in
            Button {
                detailDraft = memo.content
                selectedMemo = memo
            } label: {
                VStack(alignment: .leading) {
                    Text(memo.content)
                        .font(.headline)
                    Text(memo.isActive ? "Active" : "Cleared")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .buttonStyle(.plain)
            .accessibilityLabel(memo.content)
            .accessibilityValue(memo.isActive ? "Active" : "Cleared")
        }
        .overlay {
            if memos.isEmpty {
                ContentUnavailableView("No memos", systemImage: "note.text")
            }
        }
    }
}

private struct MemoDetailView: View {
    let memo: Memo
    @Binding var draft: String
    let onSave: () -> Void
    let onClearOrReopen: () -> Void
    let onDelete: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(memo.isActive ? "Edit Memo" : "Historical Memo")
                .font(.title.bold())
            TextField("Memo", text: $draft, axis: .vertical)
                .textFieldStyle(.roundedBorder)
                .frame(minHeight: 120)
            HStack {
                Button("Delete", role: .destructive, action: onDelete)
                Spacer()
                Button(memo.isActive ? "Clear" : "Reopen", action: onClearOrReopen)
                Button("Save", action: onSave)
                    .keyboardShortcut(.return, modifiers: .command)
            }
        }
        .padding()
        .frame(width: 420)
    }
}
