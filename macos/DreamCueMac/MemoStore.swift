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
    @Published var reminderHour = 21
    @Published var reminderMinute = 0

    private let storageURL: URL
    private let settingsURL: URL
    private let deletedMemoTombstonesURL: URL
    private let syncService = FirebaseRestSyncService()
    private let notificationScheduler = DreamCueReminderNotificationScheduler.shared
    private var pollTask: Task<Void, Never>?
    private var deletedMemoTombstones: [String: Int64] = [:]

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
        settingsURL = appURL.appendingPathComponent("settings.json")
        deletedMemoTombstonesURL = appURL.appendingPathComponent("deleted-memos.json")
        loadSettings()
        load()
        if let previewEmail = ProcessInfo.processInfo.environment["DREAMCUE_PREVIEW_SYNC_EMAIL"], !previewEmail.isEmpty {
            signedInEmail = previewEmail
            syncEmail = previewEmail
            syncStatus = "Sync account signed in."
        }
        syncReminderNotifications(requestAuthorization: false)
    }

    var currentMemos: [Memo] {
        memos.filter(\.isActive).sorted(by: activeMemoSort)
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
            lastReviewedAtMs: nil,
            displayOrder: now,
            pinned: false
        )
        memos.append(memo)
        draft = ""
        persistAndUpload(requestAuthorization: true)
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
        let now = currentTimeMs()
        memos[index].updatedAtMs = now
        memos[index].clearedAtMs = nil
        memos[index].lastReviewedAtMs = nil
        memos[index].displayOrder = now
        persistAndUpload()
    }

    func setMemoPinned(_ memo: Memo, pinned: Bool) {
        guard let index = memos.firstIndex(where: { $0.id == memo.id }) else { return }
        let now = currentTimeMs()
        memos[index].pinned = pinned
        memos[index].displayOrder = now
        memos[index].updatedAtMs = now
        persistAndUpload(requestAuthorization: true)
    }

    func moveCurrentMemo(from sourceIndex: Int, to destinationIndex: Int) {
        var current = currentMemos
        guard current.indices.contains(sourceIndex),
              current.indices.contains(destinationIndex),
              sourceIndex != destinationIndex
        else {
            return
        }

        let moved = current.remove(at: sourceIndex)
        current.insert(moved, at: destinationIndex)
        applyActiveOrder(current.map(\.id))
    }

    func deleteMemo(_ memo: Memo) {
        let deletedAtMs = currentTimeMs()
        memos.removeAll { $0.id == memo.id }
        deletedMemoTombstones[memo.id] = deletedAtMs
        persist()
        syncReminderNotifications(requestAuthorization: false)
        persistDeletedMemoTombstones()
        Task {
            await syncService.uploadDeletedMemo(id: memo.id, deletedAtMs: deletedAtMs)
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
        if let blockedMessage = syncCreateAccountBlockedMessage(enteredEmail: email, signedInEmail: signedInEmail) {
            syncStatus = blockedMessage
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
        persistSettings()
        syncReminderNotifications(requestAuthorization: dailyReminderEnabled)
    }

    func setDailyReminderEnabled(_ enabled: Bool) {
        dailyReminderEnabled = enabled
        persistSettings()
        syncReminderNotifications(requestAuthorization: enabled)
    }

    private func load() {
        if let data = try? Data(contentsOf: storageURL) {
            memos = (try? JSONDecoder().decode([Memo].self, from: data)) ?? []
        }
        if let deletedData = try? Data(contentsOf: deletedMemoTombstonesURL),
           let decodedTombstones = try? JSONDecoder().decode([String: Int64].self, from: deletedData) {
            deletedMemoTombstones = decodedTombstones
        }
    }

    private func loadSettings() {
        guard let data = try? Data(contentsOf: settingsURL),
              let settings = try? JSONDecoder().decode(ReminderSettings.self, from: data)
        else {
            return
        }
        dailyReminderEnabled = settings.dailyReminderEnabled
        reminderHour = settings.reminderHour
        reminderMinute = settings.reminderMinute
    }

    private func persist() {
        if let data = try? JSONEncoder().encode(memos) {
            try? data.write(to: storageURL, options: .atomic)
        }
    }

    private func persistSettings() {
        let settings = ReminderSettings(
            dailyReminderEnabled: dailyReminderEnabled,
            reminderHour: reminderHour,
            reminderMinute: reminderMinute
        )
        if let data = try? JSONEncoder().encode(settings) {
            try? data.write(to: settingsURL, options: .atomic)
        }
    }

    private func persistDeletedMemoTombstones() {
        if let data = try? JSONEncoder().encode(deletedMemoTombstones) {
            try? data.write(to: deletedMemoTombstonesURL, options: .atomic)
        }
    }

    private func persistAndUpload(requestAuthorization: Bool = false) {
        persist()
        syncReminderNotifications(requestAuthorization: requestAuthorization)
        Task {
            await syncService.uploadMemos(memos)
        }
    }

    private func pullAndUpload() async {
        let remoteRecords = await syncService.fetchMemoRecords()
        let outcome = mergeRemoteRecordsWithTombstones(
            remoteRecords,
            into: memos,
            deletedMemoTombstones: deletedMemoTombstones
        )
        memos = outcome.memos
        deletedMemoTombstones = outcome.deletedMemoTombstones
        persist()
        syncReminderNotifications(requestAuthorization: false)
        persistDeletedMemoTombstones()
        for deletedMemo in outcome.deletedMemosToUpload {
            await syncService.uploadDeletedMemo(
                id: deletedMemo.id,
                deletedAtMs: deletedMemo.deletedAtMs
            )
        }
        await syncService.uploadMemos(memos)
    }

    private func applyActiveOrder(_ orderedIds: [String]) {
        let now = currentTimeMs()
        for (offset, memoId) in orderedIds.enumerated() {
            if let index = memos.firstIndex(where: { $0.id == memoId && $0.isActive }) {
                memos[index].displayOrder = now - Int64(offset)
            }
        }
        persistAndUpload()
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

    private func syncReminderNotifications(requestAuthorization: Bool) {
        if ProcessInfo.processInfo.environment["DREAMCUE_NOTIFICATIONS_DISABLED"] == "1" {
            notificationScheduler.cancelAll()
            return
        }
        notificationScheduler.sync(
            enabled: dailyReminderEnabled,
            hour: reminderHour,
            minute: reminderMinute,
            memos: memos,
            requestAuthorization: requestAuthorization
        )
    }
}

private struct ReminderSettings: Codable {
    let dailyReminderEnabled: Bool
    let reminderHour: Int
    let reminderMinute: Int
}

private func activeMemoSort(_ lhs: Memo, _ rhs: Memo) -> Bool {
    if lhs.pinned != rhs.pinned {
        return lhs.pinned && !rhs.pinned
    }
    if lhs.displayOrder != rhs.displayOrder {
        return lhs.displayOrder > rhs.displayOrder
    }
    return lhs.updatedAtMs > rhs.updatedAtMs
}
