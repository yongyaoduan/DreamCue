import Foundation

final class FirebaseRestSyncService {
    private struct Session {
        let userId: String
        let idToken: String
    }

    private let projectId: String
    private let apiKey: String
    private var session: Session?

    init() {
        let config = Bundle.main.url(forResource: "FirebaseConfig", withExtension: "plist")
            .flatMap { NSDictionary(contentsOf: $0) as? [String: String] } ?? [:]
        projectId = config["ProjectID"]?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        apiKey = config["APIKey"]?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
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
        guard let session, !projectId.isEmpty else { return }
        let now = Int64(Date().timeIntervalSince1970 * 1000)
        let fields: [String: Any] = [
            "content": stringValue(""),
            "status": stringValue("cleared"),
            "created_at_ms": integerValue(now),
            "updated_at_ms": integerValue(now),
            "cleared_at_ms": integerValue(now),
            "reminder_count": integerValue(0),
            "last_reviewed_at_ms": integerValue(now),
            "deleted": booleanValue(true),
        ]
        await patchDocument(id: id, fields: fields, session: session)
    }

    func fetchMemos() async -> [Memo] {
        guard let session, !projectId.isEmpty else { return [] }
        let path = "users/\(session.userId)/memos"
        guard let url = URL(string: firestoreBaseURL().appending("/\(path)")) else { return [] }
        var request = URLRequest(url: url)
        request.setValue("Bearer \(session.idToken)", forHTTPHeaderField: "Authorization")

        guard let (data, _) = try? await URLSession.shared.data(for: request),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let documents = root["documents"] as? [[String: Any]]
        else {
            return []
        }

        return documents.compactMap(documentToMemo)
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
        guard let session, !projectId.isEmpty else { return }
        let fields: [String: Any] = [
            "content": stringValue(memo.content),
            "status": stringValue(memo.status.rawValue),
            "created_at_ms": integerValue(memo.createdAtMs),
            "updated_at_ms": integerValue(memo.updatedAtMs),
            "cleared_at_ms": optionalIntegerValue(memo.clearedAtMs),
            "reminder_count": integerValue(memo.reminderCount),
            "last_reviewed_at_ms": optionalIntegerValue(memo.lastReviewedAtMs),
            "deleted": booleanValue(false),
        ]
        await patchDocument(id: memo.id, fields: fields, session: session)
    }

    private func patchDocument(id: String, fields: [String: Any], session: Session) async {
        let path = "users/\(session.userId)/memos/\(id)"
        guard let url = URL(string: firestoreBaseURL().appending("/\(path)")) else { return }
        var request = URLRequest(url: url)
        request.httpMethod = "PATCH"
        request.setValue("Bearer \(session.idToken)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try? JSONSerialization.data(withJSONObject: ["fields": fields])
        _ = try? await URLSession.shared.data(for: request)
    }

    private func documentToMemo(_ document: [String: Any]) -> Memo? {
        guard let name = document["name"] as? String,
              let id = name.split(separator: "/").last.map(String.init),
              let fields = document["fields"] as? [String: Any],
              !boolField(fields["deleted"]),
              let content = stringField(fields["content"]),
              let status = stringField(fields["status"]).flatMap(MemoStatus.init(rawValue:)),
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

    private func firestoreBaseURL() -> String {
        "https://firestore.googleapis.com/v1/projects/\(projectId)/databases/(default)/documents"
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

private func stringValue(_ value: String) -> [String: Any] {
    ["stringValue": value]
}

private func integerValue(_ value: Int64) -> [String: Any] {
    ["integerValue": String(value)]
}

private func optionalIntegerValue(_ value: Int64?) -> [String: Any] {
    if let value {
        return integerValue(value)
    }
    return ["nullValue": NSNull()]
}

private func booleanValue(_ value: Bool) -> [String: Any] {
    ["booleanValue": value]
}

private func stringField(_ value: Any?) -> String? {
    (value as? [String: Any])?["stringValue"] as? String
}

private func intField(_ value: Any?) -> Int64? {
    guard let raw = (value as? [String: Any])?["integerValue"] as? String else { return nil }
    return Int64(raw)
}

private func boolField(_ value: Any?) -> Bool {
    ((value as? [String: Any])?["booleanValue"] as? Bool) ?? false
}
