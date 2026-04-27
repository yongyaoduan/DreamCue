import Foundation

@main
enum ReturnKeyCommitPolicyTests {
    static func main() {
        check(
            ReturnKeyCommitPolicy.action(
                isReturnKey: true,
                commandModified: false,
                hasMarkedText: true
            ) == .ignore,
            "Return must stay with the input method while marked text is active."
        )
        check(
            ReturnKeyCommitPolicy.action(
                isReturnKey: true,
                commandModified: false,
                hasMarkedText: false
            ) == .commit,
            "Plain Return must save when no input method composition is active."
        )
        check(
            ReturnKeyCommitPolicy.action(
                isReturnKey: true,
                commandModified: true,
                hasMarkedText: false
            ) == .insertNewline,
            "Command Return must insert a line break."
        )
        check(
            ReturnKeyCommitPolicy.action(
                isReturnKey: false,
                commandModified: false,
                hasMarkedText: false
            ) == .ignore,
            "Non-return keys must not save."
        )
    }

    private static func check(_ condition: @autoclosure () -> Bool, _ message: String) {
        if !condition() {
            FileHandle.standardError.write((message + "\n").data(using: .utf8)!)
            exit(1)
        }
    }
}
