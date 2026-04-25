import Foundation
import SwiftUI

@MainActor
final class MemoStore: ObservableObject {
    @Published var draft = ""
    @Published var searchQuery = ""
    @Published var selectedTab = 0
    @Published private(set) var memos: [Memo] = []
    @Published var syncEmail = ""
    @Published var syncPassword = ""
    @Published var syncStatus = "Sign in to sync across devices."
    @Published private(set) var signedInEmail = ""
    @Published var dailyReminderEnabled = true
    @Published var quietHoursEnabled = true
    @Published var reminderHour = 21
    @Published var reminderMinute = 0
    @Published var quietStartHour = 22
    @Published var quietEndHour = 7

    private let storageURL: URL
    private let syncService = FirebaseRestSyncService()
    private var pollTask: Task<Void, Never>?

    init() {
        let appURL: URL
        if let testStorage = ProcessInfo.processInfo.environment["DREAMCUE_STORAGE_DIR"], !testStorage.isEmpty {
            appURL = URL(fileURLWithPath: testStorage, isDirectory: true)
        } else {
            let baseURL = FileManager.default.urls(
                for: .applicationSupportDirectory,
                in: .userDomainMask
            ).first ?? FileManager.default.temporaryDirectory
            appURL = baseURL.appendingPathComponent("DreamCue", isDirectory: true)
        }
        try? FileManager.default.createDirectory(at: appURL, withIntermediateDirectories: true)
        storageURL = appURL.appendingPathComponent("memos.json")
        load()
        if let previewEmail = ProcessInfo.processInfo.environment["DREAMCUE_PREVIEW_SYNC_EMAIL"], !previewEmail.isEmpty {
            signedInEmail = previewEmail
            syncEmail = previewEmail
            syncStatus = "Sync account signed in."
        }
    }

    var currentMemos: [Memo] {
        memos.filter(\.isActive).sorted { $0.updatedAtMs > $1.updatedAtMs }
    }

    var historyMemos: [Memo] {
        memos.filter { !$0.isActive }.sorted {
            ($0.clearedAtMs ?? $0.updatedAtMs) > ($1.clearedAtMs ?? $1.updatedAtMs)
        }
    }

    var searchResults: [Memo] {
        let query = searchQuery.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        if query.isEmpty {
            return archiveMemos
        }
        return memos.filter { $0.content.lowercased().contains(query) }
            .sorted { $0.updatedAtMs > $1.updatedAtMs }
    }

    var archiveMemos: [Memo] {
        memos.sorted { $0.updatedAtMs > $1.updatedAtMs }
    }

    var reminderTimeText: String {
        String(format: "%02d:%02d", reminderHour, reminderMinute)
    }

    var quietHoursText: String {
        String(format: "%02d:00 – %02d:00", quietStartHour, quietEndHour)
    }

    var isSyncActive: Bool {
        !signedInEmail.isEmpty
    }

    var syncDisplayEmail: String {
        signedInEmail.isEmpty ? syncEmail : signedInEmail
    }

    func addMemo() {
        let content = draft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !content.isEmpty else { return }
        let now = currentTimeMs()
        let memo = Memo(
            id: UUID().uuidString,
            content: content,
            status: .active,
            createdAtMs: now,
            updatedAtMs: now,
            clearedAtMs: nil,
            reminderCount: 0,
            lastReviewedAtMs: nil
        )
        memos.append(memo)
        draft = ""
        persistAndUpload()
    }

    func updateMemo(_ memo: Memo, content: String) {
        guard let index = memos.firstIndex(where: { $0.id == memo.id }) else { return }
        let trimmed = content.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else { return }
        memos[index].content = trimmed
        memos[index].updatedAtMs = currentTimeMs()
        persistAndUpload()
    }

    func clearMemo(_ memo: Memo) {
        guard let index = memos.firstIndex(where: { $0.id == memo.id }) else { return }
        let now = currentTimeMs()
        memos[index].status = .cleared
        memos[index].updatedAtMs = now
        memos[index].clearedAtMs = now
        memos[index].lastReviewedAtMs = now
        memos[index].reminderCount += 1
        persistAndUpload()
    }

    func reopenMemo(_ memo: Memo) {
        guard let index = memos.firstIndex(where: { $0.id == memo.id }) else { return }
        memos[index].status = .active
        memos[index].updatedAtMs = currentTimeMs()
        memos[index].clearedAtMs = nil
        memos[index].lastReviewedAtMs = nil
        persistAndUpload()
    }

    func deleteMemo(_ memo: Memo) {
        memos.removeAll { $0.id == memo.id }
        persist()
        Task {
            await syncService.uploadDeletedMemo(id: memo.id)
        }
    }

    func signIn() {
        let email = syncEmail.trimmingCharacters(in: .whitespacesAndNewlines)
        let password = syncPassword
        guard !email.isEmpty, !password.isEmpty else {
            syncStatus = "Enter an email and password."
            return
        }

        Task {
            do {
                try await syncService.signIn(email: email, password: password)
                signedInEmail = email
                syncStatus = "Sync account signed in."
                await pullAndUpload()
                startPolling()
            } catch {
                syncStatus = error.localizedDescription
            }
        }
    }

    func createAccount() {
        let email = syncEmail.trimmingCharacters(in: .whitespacesAndNewlines)
        let password = syncPassword
        guard !email.isEmpty, !password.isEmpty else {
            syncStatus = "Enter an email and password."
            return
        }

        Task {
            do {
                try await syncService.createAccount(email: email, password: password)
                signedInEmail = email
                syncStatus = "Sync account created."
                await pullAndUpload()
                startPolling()
            } catch {
                syncStatus = error.localizedDescription
            }
        }
    }

    func signOut() {
        pollTask?.cancel()
        pollTask = nil
        syncService.signOut()
        syncPassword = ""
        signedInEmail = ""
        syncStatus = "Sync account signed out."
    }

    func setReminderTime(hour: Int, minute: Int) {
        reminderHour = hour
        reminderMinute = minute
    }

    private func load() {
        guard let data = try? Data(contentsOf: storageURL) else { return }
        memos = (try? JSONDecoder().decode([Memo].self, from: data)) ?? []
    }

    private func persist() {
        if let data = try? JSONEncoder().encode(memos) {
            try? data.write(to: storageURL, options: .atomic)
        }
    }

    private func persistAndUpload() {
        persist()
        Task {
            await syncService.uploadMemos(memos)
        }
    }

    private func pullAndUpload() async {
        let remoteMemos = await syncService.fetchMemos()
        merge(remoteMemos)
        persist()
        await syncService.uploadMemos(memos)
    }

    private func merge(_ remoteMemos: [Memo]) {
        for remote in remoteMemos {
            if let index = memos.firstIndex(where: { $0.id == remote.id }) {
                if remote.updatedAtMs >= memos[index].updatedAtMs {
                    memos[index] = remote
                }
            } else {
                memos.append(remote)
            }
        }
    }

    private func startPolling() {
        pollTask?.cancel()
        pollTask = Task { [weak self] in
            while !Task.isCancelled {
                try? await Task.sleep(for: .seconds(8))
                await self?.pullAndUpload()
            }
        }
    }

    private func currentTimeMs() -> Int64 {
        Int64(Date().timeIntervalSince1970 * 1000)
    }
}
