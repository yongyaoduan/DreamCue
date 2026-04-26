import Foundation

@main
enum SyncAccountPolicyTests {
    static func main() {
        check(
            syncCreateAccountBlockedMessage(
                enteredEmail: " Owner@Example.com ",
                signedInEmail: "owner@example.com"
            ) == "An account already exists for this email.",
            "Creating the currently signed-in account must show an existing-account message."
        )
        check(
            syncCreateAccountBlockedMessage(
                enteredEmail: "new@example.com",
                signedInEmail: "owner@example.com"
            ) == nil,
            "Creating a different email must remain available."
        )
        check(
            firebaseAuthFailureMessage(
                data: Data("""
                {"error":{"message":"EMAIL_EXISTS"}}
                """.utf8),
                fallback: "Sync account creation failed."
            ) == "An account already exists for this email.",
            "Firebase EMAIL_EXISTS must use a clear existing-account message."
        )
    }

    private static func check(_ condition: @autoclosure () -> Bool, _ message: String) {
        if !condition() {
            FileHandle.standardError.write((message + "\n").data(using: .utf8)!)
            exit(1)
        }
    }
}
