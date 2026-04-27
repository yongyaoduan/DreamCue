enum ReturnKeyCommitAction {
    case ignore
    case commit
    case insertNewline
}

enum ReturnKeyCommitPolicy {
    static func action(isReturnKey: Bool, commandModified: Bool, hasMarkedText: Bool) -> ReturnKeyCommitAction {
        guard isReturnKey, !hasMarkedText else { return .ignore }
        return commandModified ? .insertNewline : .commit
    }
}
