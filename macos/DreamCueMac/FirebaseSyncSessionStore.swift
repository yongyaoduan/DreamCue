import Foundation
import Security

struct PersistedFirebaseSyncSession: Codable, Equatable {
    let email: String
    let userId: String
    let refreshToken: String
}

final class FirebaseSyncSessionStore {
    static let shared = FirebaseSyncSessionStore()

    private let service: String
    private let account: String

    init(
        service: String = "app.dreamcue.mac.firebase-sync",
        account: String = "default"
    ) {
        self.service = service
        self.account = account
    }

    func load() -> PersistedFirebaseSyncSession? {
        var query = baseQuery()
        query[kSecReturnData as String] = true
        query[kSecMatchLimit as String] = kSecMatchLimitOne

        var item: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &item)
        guard status == errSecSuccess,
              let data = item as? Data
        else {
            return nil
        }
        return try? JSONDecoder().decode(PersistedFirebaseSyncSession.self, from: data)
    }

    @discardableResult
    func save(_ session: PersistedFirebaseSyncSession) -> Bool {
        clear()
        guard let data = try? JSONEncoder().encode(session) else { return false }
        var query = baseQuery()
        query[kSecValueData as String] = data
        query[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlock
        return SecItemAdd(query as CFDictionary, nil) == errSecSuccess
    }

    func clear() {
        SecItemDelete(baseQuery() as CFDictionary)
    }

    private func baseQuery() -> [String: Any] {
        [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
        ]
    }
}
