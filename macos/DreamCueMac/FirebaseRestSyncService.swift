import Foundation

final class FirebaseRestSyncService {
    private struct Session {
        let userId: String
        let idToken: String
    }

    private let projectId: String
    private let apiKey: String
    private let databaseURL: String
    private var session: Session?

    init() {
        let config = Bundle.main.url(forResource: "FirebaseConfig", withExtension: "plist")
            .flatMap { NSDictionary(contentsOf: $0) as? [String: String] } ?? [:]
        projectId = config["ProjectID"]?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        apiKey = config["APIKey"]?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        databaseURL = config["DatabaseURL"]?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
    }

    func signIn(email: String, password: String) async throws {
        session = try await authenticate(
            endpoint: "accounts:signInWithPassword",
            email: email,
            password: password,
            fallback: "Sync sign-in failed."
        )
    }

    func createAccount(email: String, password: String) async throws {
        session = try await authenticate(
            endpoint: "accounts:signUp",
            email: email,
            password: password,
            fallback: "Sync account creation failed."
        )
    }

    func signOut() {
        session = nil
    }

    func uploadMemos(_ memos: [Memo]) async {
        for memo in memos {
            await uploadMemo(memo)
        }
    }

    func uploadDeletedMemo(id: String, deletedAtMs: Int64? = nil) async {
        guard let session, !databaseURL.isEmpty else { return }
        let now = deletedAtMs ?? Int64(Date().timeIntervalSince1970 * 1000)
        await patchDocument(id: id, fields: firebaseDeletedMemoDocumentFields(deletedAtMs: now), session: session)
    }

    func fetchMemoRecords() async -> [RemoteMemoRecord] {
        guard let session, !databaseURL.isEmpty else { return [] }
        guard var components = URLComponents(string: databaseURL.appending("/users/\(session.userId)/memos.json")) else { return [] }
        components.queryItems = [URLQueryItem(name: "auth", value: session.idToken)]
        guard let url = components.url else { return [] }
        let request = URLRequest(url: url)

        guard let (data, _) = try? await URLSession.shared.data(for: request),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any]
        else {
            return []
        }

        return root.compactMap { memoId, raw in
            guard let fields = raw as? [String: Any] else { return nil }
            return documentToRecord(id: memoId, fields: fields)
        }
    }

    private func authenticate(endpoint: String, email: String, password: String, fallback: String) async throws -> Session {
        guard !apiKey.isEmpty else {
            throw SyncError.missingFirebaseConfig
        }
        let url = URL(string: "https://identitytoolkit.googleapis.com/v1/\(endpoint)?key=\(apiKey)")!
        let body: [String: Any] = [
            "email": email,
            "password": password,
            "returnSecureToken": true,
        ]
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (data, response) = try await URLSession.shared.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse, (200..<300).contains(httpResponse.statusCode) else {
            throw SyncError.authenticationFailed(firebaseAuthFailureMessage(data: data, fallback: fallback))
        }
        guard let root = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let userId = root["localId"] as? String,
              let idToken = root["idToken"] as? String
        else {
            throw SyncError.authenticationFailed(fallback)
        }
        return Session(userId: userId, idToken: idToken)
    }

    private func uploadMemo(_ memo: Memo) async {
        guard let session, !databaseURL.isEmpty else { return }
        await patchDocument(id: memo.id, fields: firebaseMemoDocumentFields(memo), session: session)
    }

    private func patchDocument(id: String, fields: [String: Any], session: Session) async {
        guard var components = URLComponents(string: databaseURL.appending("/users/\(session.userId)/memos/\(id).json")) else { return }
        components.queryItems = [URLQueryItem(name: "auth", value: session.idToken)]
        guard let url = components.url else { return }
        var request = URLRequest(url: url)
        request.httpMethod = "PUT"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try? JSONSerialization.data(withJSONObject: fields)
        _ = try? await URLSession.shared.data(for: request)
    }

    private func documentToRecord(id: String, fields: [String: Any]) -> RemoteMemoRecord? {
        let updatedAtMs = intField(fields["updated_at_ms"]) ?? 0
        if boolField(fields["deleted"]) {
            return RemoteMemoRecord(
                id: id,
                memo: nil,
                deleted: true,
                updatedAtMs: updatedAtMs
            )
        }

        guard let content = fields["content"] as? String,
              let statusRaw = fields["status"] as? String,
              let status = MemoStatus(rawValue: statusRaw),
              let createdAtMs = intField(fields["created_at_ms"]),
              let reminderCount = intField(fields["reminder_count"])
        else {
            return nil
        }

        let memo = Memo(
            id: id,
            content: content,
            status: status,
            createdAtMs: createdAtMs,
            updatedAtMs: updatedAtMs,
            clearedAtMs: intField(fields["cleared_at_ms"]),
            reminderCount: reminderCount,
            lastReviewedAtMs: intField(fields["last_reviewed_at_ms"]),
            displayOrder: intField(fields["display_order"]) ?? updatedAtMs,
            pinned: boolField(fields["pinned"])
        )
        return RemoteMemoRecord(
            id: id,
            memo: memo,
            deleted: false,
            updatedAtMs: updatedAtMs
        )
    }
}

private enum SyncError: LocalizedError {
    case missingFirebaseConfig
    case authenticationFailed(String)

    var errorDescription: String? {
        switch self {
        case .missingFirebaseConfig:
            "Firebase sync is not configured."
        case .authenticationFailed(let message):
            message
        }
    }
}

func syncCreateAccountBlockedMessage(enteredEmail: String, signedInEmail: String) -> String? {
    let entered = enteredEmail.trimmingCharacters(in: .whitespacesAndNewlines)
    let signedIn = signedInEmail.trimmingCharacters(in: .whitespacesAndNewlines)
    if !entered.isEmpty, !signedIn.isEmpty, entered.compare(signedIn, options: [.caseInsensitive]) == .orderedSame {
        return "An account already exists for this email."
    }
    return nil
}

func firebaseAuthFailureMessage(data: Data, fallback: String) -> String {
    guard let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
          let error = root["error"] as? [String: Any],
          let message = error["message"] as? String
    else {
        return fallback
    }
    if message.contains("EMAIL_EXISTS") || message.localizedCaseInsensitiveContains("already in use") {
        return "An account already exists for this email."
    }
    if message.contains("INVALID_EMAIL") {
        return "Enter a valid email address."
    }
    if message.contains("WEAK_PASSWORD") {
        return "Use a password with at least 6 characters."
    }
    if message.contains("INVALID_LOGIN_CREDENTIALS") ||
        message.contains("INVALID_PASSWORD") ||
        message.contains("EMAIL_NOT_FOUND") {
        return "Email or password is incorrect."
    }
    return fallback
}

private func intField(_ value: Any?) -> Int64? {
    if let value = value as? Int64 {
        return value
    }
    if let value = value as? Int {
        return Int64(value)
    }
    if let value = value as? Double {
        return Int64(value)
    }
    if let value = value as? String {
        return Int64(value)
    }
    return nil
}

private func boolField(_ value: Any?) -> Bool {
    (value as? Bool) ?? false
}

private func jsonValue(_ value: Int64?) -> Any {
    value ?? NSNull()
}

func firebaseMemoDocumentFields(_ memo: Memo) -> [String: Any] {
    [
        "content": memo.content,
        "status": memo.status.rawValue,
        "created_at_ms": memo.createdAtMs,
        "updated_at_ms": memo.updatedAtMs,
        "cleared_at_ms": jsonValue(memo.clearedAtMs),
        "reminder_count": memo.reminderCount,
        "last_reviewed_at_ms": jsonValue(memo.lastReviewedAtMs),
        "display_order": memo.displayOrder,
        "pinned": memo.pinned,
        "deleted": false,
    ]
}

func firebaseDeletedMemoDocumentFields(deletedAtMs: Int64) -> [String: Any] {
    [
        "content": "",
        "status": "cleared",
        "created_at_ms": deletedAtMs,
        "updated_at_ms": deletedAtMs,
        "cleared_at_ms": deletedAtMs,
        "reminder_count": 0,
        "last_reviewed_at_ms": deletedAtMs,
        "display_order": deletedAtMs,
        "pinned": false,
        "deleted": true,
    ]
}
