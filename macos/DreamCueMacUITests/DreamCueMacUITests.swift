import AppKit
import XCTest

final class DreamCueMacUITests: XCTestCase {
    @objc
    func testPrimaryScreensMatchDesktopFlow() {
        let app = launchIsolatedApp()

        XCTAssertTrue(app.staticTexts["DreamCue"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Daily rhythm"].exists)
        XCTAssertTrue(app.staticTexts["Capture a cue..."].exists || app.buttons["New Cue"].exists)
        XCTAssertTrue(app.staticTexts["Active Cues"].exists)
        attachScreenshot("mac-today-redesign", app: app)

        app.buttons["Archive"].firstMatch.click()
        XCTAssertTrue(app.staticTexts["Archive"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.searchFields["Search cues..."].exists)
        XCTAssertFalse(app.buttons["Run Search"].exists)
        attachScreenshot("mac-archive-redesign", app: app)

        app.buttons["Rhythm"].firstMatch.click()
        XCTAssertTrue(app.staticTexts["Daily Reminder"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.descendants(matching: .any)["Daily Reminder Toggle"].exists)
        XCTAssertTrue(app.descendants(matching: .any)["Quiet Hours Toggle"].exists)
        XCTAssertTrue(app.buttons["Change Time"].exists)
        attachScreenshot("mac-rhythm-redesign", app: app)

        app.buttons["Account"].firstMatch.click()
        XCTAssertTrue(app.staticTexts["Sync Account"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Tenant-scoped privacy"].exists)
        XCTAssertFalse(app.staticTexts["Remote path"].exists)
        XCTAssertFalse(app.staticTexts["users/{uid}/memos"].exists)
        attachScreenshot("mac-account-redesign", app: app)
    }

    @objc
    func testCueEditorUsesIconActionsAndReminderPicker() {
        let app = launchIsolatedApp()
        let cueText = "mac_design_\(UUID().uuidString.prefix(8))"

        XCTAssertTrue(app.staticTexts["DreamCue"].waitForExistence(timeout: 5))
        app.buttons["New Cue"].click()
        XCTAssertTrue(app.staticTexts["New Cue"].waitForExistence(timeout: 5))
        app.textViews["Cue Text"].click()
        paste(cueText, app: app)
        app.buttons["Save"].click()

        XCTAssertTrue(app.buttons[cueText].waitForExistence(timeout: 5))
        app.buttons[cueText].click()
        XCTAssertTrue(app.staticTexts[cueText].waitForExistence(timeout: 5))
        XCTAssertFalse(app.buttons["Favorite"].exists)
        XCTAssertFalse(app.buttons["More"].exists)
        XCTAssertFalse(app.staticTexts["Add detail"].exists)
        XCTAssertTrue(app.buttons["Complete Cue"].exists)
        XCTAssertTrue(app.buttons["Delete Cue"].exists)
        XCTAssertFalse(app.buttons["Archive Cue"].exists)
        XCTAssertTrue(app.buttons["Remind"].exists)
        attachScreenshot("mac-cue-detail-redesign", app: app)

        app.buttons["Change Time"].click()
        XCTAssertTrue(app.staticTexts["Select reminder time"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["Save Time"].exists)
        XCTAssertTrue(app.buttons["Cancel"].exists)
        attachScreenshot("mac-time-picker-redesign", app: app)
    }

    @objc
    func testArchiveSearchShowsAllWhenEmptyAndFiltersByText() {
        let app = launchIsolatedApp()
        let firstCue = "alpha_archive_\(UUID().uuidString.prefix(6))"
        let secondCue = "beta_archive_\(UUID().uuidString.prefix(6))"

        XCTAssertTrue(app.staticTexts["DreamCue"].waitForExistence(timeout: 5))
        createCue(firstCue, app: app)
        createCue(secondCue, app: app)

        app.buttons["Archive"].firstMatch.click()
        XCTAssertTrue(app.buttons[secondCue].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons[firstCue].exists)
        app.searchFields["Search cues..."].click()
        paste("alpha", app: app)
        XCTAssertTrue(app.buttons[firstCue].waitForExistence(timeout: 5))
        XCTAssertFalse(app.buttons[secondCue].exists)
        attachScreenshot("mac-archive-filtered", app: app)
    }

    @objc
    func testAccountShowsCompactSignedInEmailWithoutBackendPath() {
        let email = "syncpeer1777086916@example.com"
        let app = launchIsolatedApp(extraEnvironment: ["DREAMCUE_PREVIEW_SYNC_EMAIL": email])

        app.buttons["Account"].firstMatch.click()
        XCTAssertTrue(app.staticTexts["syncpeer1777...@example.com"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Sync Health"].exists)
        XCTAssertFalse(app.staticTexts["Remote path"].exists)
        XCTAssertFalse(app.staticTexts["Realtime Database"].exists)
        attachScreenshot("mac-account-signed-in-compact", app: app)
    }

    @objc
    func testFirebaseSyncPullsRemoteAndUploadsLocalWhenConfigured() throws {
        let syncMarkerPath = "/tmp/dreamcue-run-firebase-sync-test"
        let isSyncEnabled = ProcessInfo.processInfo.environment["DREAMCUE_RUN_FIREBASE_SYNC_TEST"] == "1"
            || FileManager.default.fileExists(atPath: syncMarkerPath)
        guard isSyncEnabled else {
            throw XCTSkip("Firebase sync test is disabled.")
        }
        let email = try syncEnvironment("DREAMCUE_SYNC_EMAIL", fallbackPath: "/tmp/dreamcue-sync-email.txt")
        let password = try syncEnvironment("DREAMCUE_SYNC_PASSWORD", fallbackPath: "/tmp/dreamcue-sync-password.txt")
        let remoteMemo = try syncEnvironment("DREAMCUE_EXPECTED_REMOTE_MEMO", fallbackPath: "/tmp/dreamcue-android-memo.txt")
        let localMemo = try syncEnvironment("DREAMCUE_MAC_CREATE_MEMO", fallbackPath: "/tmp/dreamcue-mac-memo.txt")
        let app = launchIsolatedApp(function: #function)

        app.buttons["Account"].firstMatch.click()
        XCTAssertTrue(app.staticTexts["Sync Account"].waitForExistence(timeout: 5))
        app.textFields["Email"].click()
        paste(email, app: app)
        app.secureTextFields["Password"].click()
        paste(password, app: app)
        app.buttons["Sign In"].click()

        XCTAssertTrue(app.staticTexts["Sync Health"].waitForExistence(timeout: 20))
        attachScreenshot("mac-sync-signed-in", app: app)

        app.buttons["Today"].firstMatch.click()
        XCTAssertTrue(app.buttons[remoteMemo].waitForExistence(timeout: 30))
        attachScreenshot("mac-sync-pulled-android-memo", app: app)

        createCue(localMemo, app: app)
        XCTAssertTrue(app.buttons[localMemo].waitForExistence(timeout: 10))
        attachScreenshot("mac-sync-created-local-memo", app: app)
    }

    private func createCue(_ text: String, app: XCUIApplication) {
        app.buttons["New Cue"].click()
        XCTAssertTrue(app.staticTexts["New Cue"].waitForExistence(timeout: 5))
        app.textViews["Cue Text"].click()
        paste(text, app: app)
        app.buttons["Save"].click()
    }

    private func launchIsolatedApp(
        function: String = #function,
        extraEnvironment: [String: String] = [:]
    ) -> XCUIApplication {
        let app = XCUIApplication()
        let safeName = function.replacingOccurrences(of: "()", with: "")
        let storageURL = FileManager.default.temporaryDirectory
            .appendingPathComponent("DreamCueMacUITests", isDirectory: true)
            .appendingPathComponent(safeName, isDirectory: true)
            .appendingPathComponent(UUID().uuidString, isDirectory: true)
        try? FileManager.default.removeItem(at: storageURL)
        try? FileManager.default.createDirectory(at: storageURL, withIntermediateDirectories: true)
        app.launchEnvironment["DREAMCUE_STORAGE_DIR"] = storageURL.path
        for (key, value) in extraEnvironment {
            app.launchEnvironment[key] = value
        }
        app.launch()
        return app
    }

    private func syncEnvironment(_ key: String, fallbackPath: String) throws -> String {
        let value = ProcessInfo.processInfo.environment[key, default: ""]
            .trimmingCharacters(in: .whitespacesAndNewlines)
        if !value.isEmpty {
            return value
        }
        let fileValue = (try? String(contentsOfFile: fallbackPath, encoding: .utf8))?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        if !fileValue.isEmpty {
            return fileValue
        }
        throw XCTSkip("Firebase sync credentials are not configured.")
    }

    private func paste(_ text: String, app: XCUIApplication) {
        NSPasteboard.general.clearContents()
        NSPasteboard.general.setString(text, forType: .string)
        app.menuBars.menuBarItems["Edit"].click()
        app.menuItems["Paste"].click()
    }

    private func attachScreenshot(_ name: String, app: XCUIApplication) {
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

}
