import Foundation

@main
enum TimeOfDayGreetingTests {
    static func main() {
        checkGreeting(hour: 4, minute: 59, timeZoneId: "Asia/Shanghai", expected: "Good night")
        checkGreeting(hour: 5, minute: 0, timeZoneId: "Asia/Shanghai", expected: "Good morning")
        checkGreeting(hour: 11, minute: 57, timeZoneId: "Asia/Shanghai", expected: "Good morning")
        checkGreeting(hour: 12, minute: 0, timeZoneId: "Asia/Shanghai", expected: "Good afternoon")
        checkGreeting(hour: 17, minute: 0, timeZoneId: "Asia/Shanghai", expected: "Good evening")
        checkGreeting(hour: 22, minute: 0, timeZoneId: "Asia/Shanghai", expected: "Good night")

        let instant = Date(timeIntervalSince1970: 1_777_175_820)
        check(
            TimeOfDayGreeting.text(for: instant, calendar: calendar(timeZoneId: "Asia/Shanghai")) == "Good morning",
            "Greeting must use the provided local time zone."
        )
        check(
            TimeOfDayGreeting.text(for: instant, calendar: calendar(timeZoneId: "America/Los_Angeles")) == "Good evening",
            "Greeting must change when the same instant is evaluated in another time zone."
        )
    }

    private static func checkGreeting(hour: Int, minute: Int, timeZoneId: String, expected: String) {
        let calendar = calendar(timeZoneId: timeZoneId)
        let date = calendar.date(from: DateComponents(
            timeZone: calendar.timeZone,
            year: 2026,
            month: 4,
            day: 26,
            hour: hour,
            minute: minute
        ))!
        check(
            TimeOfDayGreeting.text(for: date, calendar: calendar) == expected,
            "\(String(format: "%02d:%02d", hour, minute)) in \(timeZoneId) should show \(expected)."
        )
    }

    private static func calendar(timeZoneId: String) -> Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.locale = Locale(identifier: "en_US_POSIX")
        calendar.timeZone = TimeZone(identifier: timeZoneId)!
        return calendar
    }

    private static func check(_ condition: @autoclosure () -> Bool, _ message: String) {
        if !condition() {
            FileHandle.standardError.write((message + "\n").data(using: .utf8)!)
            exit(1)
        }
    }
}
