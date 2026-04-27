import Foundation
import UserNotifications

struct ReminderNotificationPlan {
    let individualMemos: [Memo]
    let summaryCount: Int

    var summaryText: String {
        let noun = summaryCount == 1 ? "cue" : "cues"
        return "\(summaryCount) \(noun), please review."
    }

    static func fromMemos(_ memos: [Memo]) -> ReminderNotificationPlan {
        let activeMemos = memos.filter(\.isActive)
        return ReminderNotificationPlan(
            individualMemos: activeMemos.filter(\.pinned),
            summaryCount: activeMemos.filter { !$0.pinned }.count
        )
    }
}

final class DreamCueReminderNotificationScheduler {
    static let shared = DreamCueReminderNotificationScheduler()

    private let center: UNUserNotificationCenter
    private let prefix = "dreamcue.reminder."
    private let summaryIdentifier = "dreamcue.reminder.summary"

    init(center: UNUserNotificationCenter = .current()) {
        self.center = center
    }

    func sync(enabled: Bool, hour: Int, minute: Int, memos: [Memo], requestAuthorization: Bool) {
        replacePendingRequests {
            guard enabled else { return }
            let plan = ReminderNotificationPlan.fromMemos(memos)
            guard plan.summaryCount > 0 || !plan.individualMemos.isEmpty else { return }
            if requestAuthorization {
                self.center.requestAuthorization(options: [.alert, .sound]) { granted, _ in
                    guard granted else { return }
                    self.schedule(plan: plan, hour: hour, minute: minute)
                }
            } else {
                self.center.getNotificationSettings { settings in
                    guard settings.authorizationStatus == .authorized ||
                          settings.authorizationStatus == .provisional else {
                        return
                    }
                    self.schedule(plan: plan, hour: hour, minute: minute)
                }
            }
        }
    }

    func cancelAll() {
        replacePendingRequests {}
    }

    private func replacePendingRequests(then action: @escaping () -> Void) {
        center.getPendingNotificationRequests { requests in
            let identifiers = requests.map(\.identifier).filter { $0.hasPrefix(self.prefix) }
            self.center.removePendingNotificationRequests(withIdentifiers: identifiers)
            action()
        }
    }

    private func schedule(plan: ReminderNotificationPlan, hour: Int, minute: Int) {
        let trigger = UNCalendarNotificationTrigger(
            dateMatching: DateComponents(hour: hour, minute: minute),
            repeats: true
        )
        for memo in plan.individualMemos {
            let content = UNMutableNotificationContent()
            content.title = "Pinned Cue"
            content.body = memo.content
            content.sound = .default
            center.add(
                UNNotificationRequest(
                    identifier: "\(prefix)memo.\(memo.id)",
                    content: content,
                    trigger: trigger
                )
            )
        }
        if plan.summaryCount > 0 {
            let summary = UNMutableNotificationContent()
            summary.title = "DreamCue"
            summary.body = plan.summaryText
            summary.sound = .default
            center.add(
                UNNotificationRequest(
                    identifier: summaryIdentifier,
                    content: summary,
                    trigger: trigger
                )
            )
        }
    }
}
