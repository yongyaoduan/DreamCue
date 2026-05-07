import Foundation

@main
enum FirebaseSessionPersistenceTests {
    static func main() {
        testKeychainSessionRoundTripAndClear()
        testSignInResponseKeepsRefreshTokenForRestore()
        testRefreshResponseKeepsReplacementRefreshToken()
    }

    private static func testKeychainSessionRoundTripAndClear() {
        let store = FirebaseSyncSessionStore(
            service: "app.dreamcue.mac.tests.\(UUID().uuidString)",
            account: "sync"
        )
        let session = PersistedFirebaseSyncSession(
            email: "owner@example.com",
            userId: "uid-123",
            refreshToken: "refresh-token-123"
        )

        store.clear()
        check(store.load() == nil, "New session store must start empty.")
        check(store.save(session), "Session store must save a session.")
        check(store.load() == session, "Session store must read back the saved session.")
        store.clear()
        check(store.load() == nil, "Session store must clear the saved session.")
    }

    private static func testSignInResponseKeepsRefreshTokenForRestore() {
        let data = jsonData([
            "localId": "uid-123",
            "idToken": "id-token-123",
            "refreshToken": "refresh-token-123",
        ])

        let session = tryValue(
            try firebaseAuthenticatedSession(
                data: data,
                email: "owner@example.com",
                fallback: "Sign in failed."
            )
        )

        check(session.email == "owner@example.com", "Authenticated session must keep the signed-in email.")
        check(session.userId == "uid-123", "Authenticated session must keep the Firebase uid.")
        check(session.idToken == "id-token-123", "Authenticated session must keep the id token.")
        check(session.refreshToken == "refresh-token-123", "Authenticated session must keep the refresh token.")
    }

    private static func testRefreshResponseKeepsReplacementRefreshToken() {
        let data = jsonData([
            "user_id": "uid-123",
            "id_token": "id-token-456",
            "refresh_token": "refresh-token-456",
        ])

        let session = tryValue(
            try firebaseRefreshedSession(
                data: data,
                email: "owner@example.com",
                fallback: "Refresh failed."
            )
        )

        check(session.email == "owner@example.com", "Refreshed session must keep the signed-in email.")
        check(session.userId == "uid-123", "Refreshed session must keep the Firebase uid.")
        check(session.idToken == "id-token-456", "Refreshed session must keep the refreshed id token.")
        check(session.refreshToken == "refresh-token-456", "Refreshed session must keep the replacement refresh token.")
    }

    private static func jsonData(_ object: [String: Any]) -> Data {
        try! JSONSerialization.data(withJSONObject: object)
    }

    private static func tryValue<T>(_ expression: @autoclosure () throws -> T) -> T {
        do {
            return try expression()
        } catch {
            FileHandle.standardError.write(("Unexpected error: \(error)\n").data(using: .utf8)!)
            exit(1)
        }
    }

    private static func check(_ condition: @autoclosure () -> Bool, _ message: String) {
        if !condition() {
            FileHandle.standardError.write((message + "\n").data(using: .utf8)!)
            exit(1)
        }
    }
}
