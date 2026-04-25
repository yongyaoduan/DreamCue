import Foundation

@main
enum FirebasePayloadTests {
    static func main() {
        let memo = Memo(
            id: "memo-sync-order",
            content: "Synced ordered cue",
            status: .active,
            createdAtMs: 1_700_000_000_000,
            updatedAtMs: 1_700_000_010_000,
            clearedAtMs: nil,
            reminderCount: 1,
            lastReviewedAtMs: nil,
            displayOrder: 1_700_000_020_000,
            pinned: true
        )

        let fields = firebaseMemoDocumentFields(memo)
        check(fields["display_order"] as? Int64 == memo.displayOrder, "Memo payload must include display_order.")
        check(fields["pinned"] as? Bool == true, "Memo payload must include pinned state.")
        check(fields["deleted"] as? Bool == false, "Memo payload must stay live unless explicitly deleted.")

        let deletedFields = firebaseDeletedMemoDocumentFields(deletedAtMs: 1_700_000_030_000)
        check(deletedFields["display_order"] as? Int64 == 1_700_000_030_000, "Deleted payload must include a sortable tombstone timestamp.")
        check(deletedFields["pinned"] as? Bool == false, "Deleted payload must clear pinned state.")
        check(deletedFields["deleted"] as? Bool == true, "Deleted payload must be marked deleted.")
    }

    private static func check(_ condition: @autoclosure () -> Bool, _ message: String) {
        if !condition() {
            FileHandle.standardError.write((message + "\n").data(using: .utf8)!)
            exit(1)
        }
    }
}
