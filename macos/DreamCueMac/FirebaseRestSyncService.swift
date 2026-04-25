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
        session = try await authenticate(endpoint: "accounts:signInWithPassword", email: email, password: password)
    }

    func createAccount(email: String, password: String) async throws {
        session = try await authenticate(endpoint: "accounts:signUp", email: email, password: password)
    }

    func signOut() {
        session = nil
    }

    func uploadMemos(_ memos: [Memo]) async {
        for memo in memos {
            await uploadMemo(memo)
        }
    }

    func uploadDeletedMemo(id: String) async {
        guard let session, !databaseURL.isEmpty else { return }
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        let fields: [String: Any] = [
            "content": "",
            "status": "cleared",
            "created_at_ms": now,
            "updated_at_ms": now,
            "cleared_at_ms": now,
            "reminder_count": 0,
            "last_reviewed_at_ms": now,
            "deleted": true,
        ]
        await patchDocument(id: id, fields: fields, session: session)
    }

    func fetchMemos() async -> [Memo] {
        guard let session, !databaseURL.isEmpty else { return [] }
        guard var components = URLComponents(string: databaseURL.appending("/users/\(session.userId)/memos.json")) else { return [] }
        components.queryItems = [URLQueryItem(name: "auth", value: session.idToken)]
        guard let url = components.url else { return [] }
        var request = URLRequest(url: url)

        guard let (data, _) = try? await URLSession.shared.data(for: request),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              !(root is NSNull)
        else {
            return []
        }

        return root.compactMap { memoId, raw in
            guard let fields = raw as? [String: Any] else { return nil }
            return documentToMemo(id: memoId, fields: fields)
        }
    }

    private func authenticate(endpoint: String, email: String, password: String) async throws -> Session {
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
            throw SyncError.authenticationFailed
        }
        guard let root = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let userId = root["localId"] as? String,
              let idToken = root["idToken"] as? String
        else {
            throw SyncError.authenticationFailed
        }
        return Session(userId: userId, idToken: idToken)
    }

    private func uploadMemo(_ memo: Memo) async {
        guard let session, !databaseURL.isEmpty else { return }
        let fields: [String: Any] = [
            "content": memo.content,
            "status": memo.status.rawValue,
            "created_at_ms": memo.createdAtMs,
            "updated_at_ms": memo.updatedAtMs,
            "cleared_at_ms": jsonValue(memo.clearedAtMs),
            "reminder_count": memo.reminderCount,
            "last_reviewed_at_ms": jsonValue(memo.lastReviewedAtMs),
            "deleted": false,
        ]
        await patchDocument(id: memo.id, fields: fields, session: session)
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

    private func documentToMemo(id: String, fields: [String: Any]) -> Memo? {
        guard !boolField(fields["deleted"]),
              let content = fields["content"] as? String,
              let statusRaw = fields["status"] as? String,
              let status = MemoStatus(rawValue: statusRaw),
              let createdAtMs = intField(fields["created_at_ms"]),
              let updatedAtMs = intField(fields["updated_at_ms"]),
              let reminderCount = intField(fields["reminder_count"])
        else {
            return nil
        }

        return Memo(
            id: id,
            content: content,
            status: status,
            createdAtMs: createdAtMs,
            updatedAtMs: updatedAtMs,
            clearedAtMs: intField(fields["cleared_at_ms"]),
            reminderCount: reminderCount,
            lastReviewedAtMs: intField(fields["last_reviewed_at_ms"])
        )
    }
}

private enum SyncError: LocalizedError {
    case missingFirebaseConfig
    case authenticationFailed

    var errorDescription: String? {
        switch self {
        case .missingFirebaseConfig:
            "Firebase sync is not configured."
        case .authenticationFailed:
            "Sync authentication failed."
        }
    }
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
