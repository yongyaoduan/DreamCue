import Foundation

@main
enum ReminderNotificationPlanTests {
    static func main() {
        let pinned = memo(id: "pinned", content: "Pinned cue", status: .active, pinned: true)
        let regular = memo(id: "regular", content: "Regular cue", status: .active, pinned: false)
        let cleared = memo(id: "cleared", content: "Cleared cue", status: .cleared, pinned: true)

        let mixedPlan = ReminderNotificationPlan.fromMemos([pinned, regular, cleared])
        check(
            mixedPlan.individualMemos.map(\.id) == ["pinned"],
            "Pinned active cue must receive an individual notification."
        )
        check(
            mixedPlan.summaryCount == 2,
            "Summary count must include all active cues and exclude cleared cues."
        )
        check(
            mixedPlan.summaryText == "2 cues, please review.",
            "Summary text must describe all active cues."
        )

        let regularOnlyPlan = ReminderNotificationPlan.fromMemos([regular])
        check(
            regularOnlyPlan.individualMemos.isEmpty,
            "Regular cues must not receive individual notifications."
        )
        check(
            regularOnlyPlan.summaryCount == 1,
            "Regular-only reminders must use one summary count."
        )
        check(
            regularOnlyPlan.summaryText == "1 cue, please review.",
            "Singular summary text must be correct."
        )
    }

    private static func memo(id: String, content: String, status: MemoStatus, pinned: Bool) -> Memo {
        Memo(
            id: id,
            content: content,
            status: status,
            createdAtMs: 1_000,
            updatedAtMs: 1_000,
            clearedAtMs: status == .cleared ? 1_100 : nil,
            reminderCount: 0,
            lastReviewedAtMs: nil,
            displayOrder: 1_000,
            pinned: pinned
        )
    }

    private static func check(_ condition: @autoclosure () -> Bool, _ message: String) {
        if !condition() {
            FileHandle.standardError.write((message + "\n").data(using: .utf8)!)
            exit(1)
        }
    }
}
