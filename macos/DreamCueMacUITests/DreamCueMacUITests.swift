import AppKit
import XCTest

final class DreamCueMacUITests: XCTestCase {
    @objc
    func testSidebarSelectionFillsFullRows() {
        let app = launchIsolatedApp()

        XCTAssertTrue(app.buttons["Today"].waitForExistence(timeout: 5))
        assertSidebarButtonFillsRow(app.buttons["Today"].firstMatch)

        app.buttons["Archive"].firstMatch.click()
        assertSidebarButtonFillsRow(app.buttons["Archive"].firstMatch)

        app.buttons["Rhythm"].firstMatch.click()
        assertSidebarButtonFillsRow(app.buttons["Rhythm"].firstMatch)
        attachScreenshot("mac-sidebar-selection-redesign", app: app)

        app.buttons["Account"].firstMatch.click()
        assertSidebarButtonFillsRow(app.buttons["Account"].firstMatch)
    }

    @objc
    func testPrimaryScreensMatchDesktopFlow() {
        let app = launchIsolatedApp()

        XCTAssertTrue(app.staticTexts["DreamCue"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Capture a cue"].exists || app.buttons["New Cue"].exists)
        XCTAssertTrue(app.staticTexts["Active Cues"].exists)
        XCTAssertFalse(app.staticTexts["Private cues."].exists)
        XCTAssertFalse(app.staticTexts["Syncing"].exists)
        attachScreenshot("mac-today-redesign", app: app)

        clickTrailingSideOf(app.buttons["Archive"].firstMatch)
        XCTAssertTrue(app.staticTexts["Archive"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.searchFields["Search cues..."].exists)
        XCTAssertFalse(app.buttons["Run Search"].exists)
        attachScreenshot("mac-archive-redesign", app: app)

        app.buttons["Rhythm"].firstMatch.click()
        XCTAssertTrue(app.staticTexts["Daily Reminder"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.descendants(matching: .any)["Daily Reminder Toggle"].exists)
        XCTAssertFalse(app.descendants(matching: .any)["Quiet Hours Toggle"].exists)
        XCTAssertFalse(app.staticTexts["Quiet Hours"].exists)
        XCTAssertTrue(app.buttons["Change Time"].exists)
        attachScreenshot("mac-rhythm-redesign", app: app)

        app.buttons["Account"].firstMatch.click()
        XCTAssertTrue(app.staticTexts["Sync Account"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["Private sync"].exists)
        XCTAssertFalse(app.staticTexts["Private sync across devices."].exists)
        XCTAssertFalse(app.staticTexts["Remote path"].exists)
        XCTAssertFalse(app.staticTexts["users/{uid}/memos"].exists)
        attachScreenshot("mac-account-redesign", app: app)
    }

    @objc
    func testNavigationDismissesOpenModalBeforeChangingSurface() {
        let app = launchIsolatedApp()

        XCTAssertTrue(app.staticTexts["DreamCue"].waitForExistence(timeout: 5))
        app.buttons["Rhythm"].firstMatch.click()
        app.buttons["Change Time"].click()
        XCTAssertTrue(app.staticTexts["Select reminder time"].waitForExistence(timeout: 5))

        app.buttons["Account"].firstMatch.click()
        XCTAssertFalse(app.staticTexts["Select reminder time"].exists)
        XCTAssertTrue(app.staticTexts["Sync Account"].waitForExistence(timeout: 5))

        app.typeKey("n", modifierFlags: .command)
        XCTAssertTrue(app.staticTexts["New Cue"].waitForExistence(timeout: 5))
        XCTAssertFalse(app.staticTexts["Select reminder time"].exists)
        attachScreenshot("mac-modal-navigation-redesign", app: app)
    }

    @objc
    func testCommandNFocusesNewCueEditor() {
        let app = launchIsolatedApp()
        let cueText = "cmdn_focus_\(UUID().uuidString.prefix(6))"

        XCTAssertTrue(app.staticTexts["DreamCue"].waitForExistence(timeout: 5))
        app.typeKey("n", modifierFlags: .command)
        XCTAssertTrue(app.staticTexts["New Cue"].waitForExistence(timeout: 5))
        paste(cueText, app: app)
        app.typeKey(.return, modifierFlags: [])

        XCTAssertTrue(app.buttons[cueText].waitForExistence(timeout: 5))
        attachScreenshot("mac-command-n-focus", app: app)
    }

    @objc
    func testCueEditorUsesIconActionsAndReminderPicker() {
        let app = launchIsolatedApp()
        let cueText = "mac_design_\(UUID().uuidString.prefix(8))"

        XCTAssertTrue(app.staticTexts["DreamCue"].waitForExistence(timeout: 5))
        app.buttons["New Cue"].click()
        XCTAssertTrue(app.staticTexts["New Cue"].waitForExistence(timeout: 5))
        assertNewCueSheetLayout(app)
        attachScreenshot("mac-new-cue-redesign", app: app)
        focusCueEditor(app)
        paste(cueText, app: app)
        app.typeKey(.return, modifierFlags: .command)
        XCTAssertTrue(app.staticTexts["New Cue"].exists)
        app.typeKey(.return, modifierFlags: [])
        dismissSystemSettingsIfOpen(app: app)

        XCTAssertTrue(app.buttons[cueText].waitForExistence(timeout: 5))
        app.buttons[cueText].coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).click()
        XCTAssertTrue(app.staticTexts[cueText].waitForExistence(timeout: 5))
        XCTAssertFalse(app.buttons["Favorite"].exists)
        XCTAssertFalse(app.buttons["More"].exists)
        XCTAssertFalse(app.staticTexts["Add detail"].exists)
        XCTAssertTrue(app.buttons["Complete Cue"].exists)
        XCTAssertTrue(app.buttons["Delete Cue"].exists)
        XCTAssertFalse(app.buttons["Archive Cue"].exists)
        XCTAssertFalse(app.buttons["Remind"].exists)
        XCTAssertFalse(app.buttons["Change Time"].exists)
        attachScreenshot("mac-cue-detail-redesign", app: app)

        app.typeKey(.return, modifierFlags: [])
        app.buttons["Rhythm"].firstMatch.click()
        app.buttons["Change Time"].click()
        XCTAssertTrue(app.staticTexts["Select reminder time"].waitForExistence(timeout: 5))
        XCTAssertFalse(app.staticTexts["Wed"].exists)
        XCTAssertFalse(app.staticTexts["Apr 23"].exists)
        XCTAssertFalse(app.buttons["Save Time"].exists)
        XCTAssertTrue(app.textFields["timePicker.hourField"].exists)
        XCTAssertTrue(app.textFields["timePicker.minuteField"].exists)
        XCTAssertTrue(app.buttons["timePicker.hourIncrement"].exists)
        XCTAssertTrue(app.buttons["timePicker.hourDecrement"].exists)
        XCTAssertTrue(app.buttons["timePicker.minuteIncrement"].exists)
        XCTAssertTrue(app.buttons["timePicker.minuteDecrement"].exists)
        XCTAssertTrue(app.buttons["timePicker.doneButton"].exists)
        setTimeToNineThirty(app: app)
        waitForTimePickerValue("09:30", app: app)
        attachScreenshot("mac-time-picker-redesign", app: app)
        app.buttons["timePicker.doneButton"].click()
        XCTAssertTrue(app.staticTexts["09:30"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.staticTexts["You'll be reminded daily at 09:30."].exists)
    }

    @objc
    func testPinnedCueStaysAboveNewCue() {
        let app = launchIsolatedApp()
        let pinnedCue = "pinned_order_\(UUID().uuidString.prefix(6))"
        let newerCue = "new_order_\(UUID().uuidString.prefix(6))"

        XCTAssertTrue(app.staticTexts["DreamCue"].waitForExistence(timeout: 5))
        createCue(pinnedCue, app: app)
        XCTAssertTrue(app.buttons[pinnedCue].waitForExistence(timeout: 5))
        app.buttons[pinnedCue].click()
        XCTAssertTrue(app.buttons["Pin Cue"].waitForExistence(timeout: 5))
        app.buttons["Pin Cue"].click()

        createCue(newerCue, app: app)
        XCTAssertTrue(app.buttons[pinnedCue].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons[newerCue].waitForExistence(timeout: 5))
        XCTAssertLessThan(app.buttons[pinnedCue].frame.minY, app.buttons[newerCue].frame.minY)
        attachScreenshot("mac-pinned-order-redesign", app: app)
    }

    @objc
    func testDraggingCueReordersNumbersAndPersistsOrder() {
        let app = launchIsolatedApp()
        let firstCue = "first_drag_\(UUID().uuidString.prefix(6))"
        let secondCue = "second_drag_\(UUID().uuidString.prefix(6))"
        let thirdCue = "third_drag_\(UUID().uuidString.prefix(6))"

        XCTAssertTrue(app.staticTexts["DreamCue"].waitForExistence(timeout: 5))
        createCue(firstCue, app: app)
        createCue(secondCue, app: app)
        createCue(thirdCue, app: app)

        XCTAssertEqual(app.buttons[thirdCue].value as? String, "#01")
        XCTAssertEqual(app.buttons[secondCue].value as? String, "#02")
        XCTAssertEqual(app.buttons[firstCue].value as? String, "#03")
        dragCue(app.buttons[thirdCue], below: app.buttons[firstCue])

        XCTAssertFalse(app.buttons["Save"].exists)
        XCTAssertTrue(app.buttons[secondCue].waitForExistence(timeout: 5))
        XCTAssertLessThan(app.buttons[secondCue].frame.minY, app.buttons[thirdCue].frame.minY)
        XCTAssertEqual(app.buttons[secondCue].value as? String, "#01")
        XCTAssertEqual(app.buttons[firstCue].value as? String, "#02")
        XCTAssertEqual(app.buttons[thirdCue].value as? String, "#03")
        attachScreenshot("mac-drag-order-redesign", app: app)

        app.terminate()
        app.launch()
        XCTAssertTrue(app.buttons[secondCue].waitForExistence(timeout: 5))
        XCTAssertEqual(app.buttons[secondCue].value as? String, "#01")
        XCTAssertEqual(app.buttons[firstCue].value as? String, "#02")
        XCTAssertEqual(app.buttons[thirdCue].value as? String, "#03")
    }

    @objc
    func testImmediateCueSwipeDoesNotReorderWithoutLongPress() {
        let app = launchIsolatedApp()
        let firstCue = "first_swipe_\(UUID().uuidString.prefix(6))"
        let secondCue = "second_swipe_\(UUID().uuidString.prefix(6))"
        let thirdCue = "third_swipe_\(UUID().uuidString.prefix(6))"

        XCTAssertTrue(app.staticTexts["DreamCue"].waitForExistence(timeout: 5))
        createCue(firstCue, app: app)
        createCue(secondCue, app: app)
        createCue(thirdCue, app: app)

        XCTAssertEqual(app.buttons[thirdCue].value as? String, "#01")
        XCTAssertEqual(app.buttons[secondCue].value as? String, "#02")
        XCTAssertEqual(app.buttons[firstCue].value as? String, "#03")
        app.buttons[thirdCue].press(forDuration: 0.1, thenDragTo: app.buttons[firstCue])

        XCTAssertFalse(app.buttons["Save"].exists)
        XCTAssertEqual(app.buttons[thirdCue].value as? String, "#01")
        XCTAssertEqual(app.buttons[secondCue].value as? String, "#02")
        XCTAssertEqual(app.buttons[firstCue].value as? String, "#03")
        usleep(500_000)
        attachScreenshot("mac-immediate-swipe-no-drag", app: app)
    }

    @objc
    func testDraggingCueFollowsPointerWhileHeld() {
        let app = launchIsolatedApp()
        let firstCue = "first_hold_\(UUID().uuidString.prefix(6))"
        let secondCue = "second_hold_\(UUID().uuidString.prefix(6))"
        let thirdCue = "third_hold_\(UUID().uuidString.prefix(6))"

        XCTAssertTrue(app.staticTexts["DreamCue"].waitForExistence(timeout: 5))
        createCue(firstCue, app: app)
        createCue(secondCue, app: app)
        createCue(thirdCue, app: app)
        XCTAssertTrue(app.buttons[thirdCue].waitForExistence(timeout: 5))
        app.buttons[thirdCue].press(
            forDuration: 0.7,
            thenDragTo: app.buttons[firstCue],
            withVelocity: .slow,
            thenHoldForDuration: 3.0
        )
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
        XCTAssertTrue(app.staticTexts["Private sync"].exists)
        XCTAssertFalse(app.staticTexts["Tenant-scoped"].exists)
        XCTAssertFalse(app.staticTexts["Private sync is active."].exists)
        XCTAssertFalse(app.staticTexts["Private sync is active on this account."].exists)
        XCTAssertFalse(app.staticTexts["Remote path"].exists)
        XCTAssertFalse(app.staticTexts["Realtime Database"].exists)
        attachScreenshot("mac-account-signed-in-compact", app: app)
    }

    @objc
    func testArchiveDetailUsesStatusSpecificActions() {
        let app = launchIsolatedApp()
        let activeCue = "active_archive_\(UUID().uuidString.prefix(6))"
        let clearedCue = "cleared_archive_\(UUID().uuidString.prefix(6))"

        XCTAssertTrue(app.staticTexts["Daily rhythm"].waitForExistence(timeout: 5))
        createCue(activeCue, app: app)
        createCue(clearedCue, app: app)
        app.buttons[clearedCue].click()
        app.buttons["Complete Cue"].click()

        app.buttons["Archive"].firstMatch.click()
        clickTrailingSideOf(app.buttons[activeCue].firstMatch)
        XCTAssertTrue(app.buttons["Delete Cue"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["Complete Cue"].exists)
        XCTAssertFalse(app.buttons["Remind"].exists)
        XCTAssertFalse(app.buttons["Return to Today"].exists)
        app.buttons["Save"].click()

        clickTrailingSideOf(app.buttons[clearedCue].firstMatch)
        XCTAssertTrue(app.buttons["Delete Cue"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["Return to Today"].exists)
        XCTAssertFalse(app.buttons["Complete Cue"].exists)
        XCTAssertFalse(app.buttons["Remind"].exists)
        app.buttons["Return to Today"].click()

        app.buttons["Today"].firstMatch.click()
        XCTAssertTrue(app.buttons[clearedCue].waitForExistence(timeout: 5))
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

    @objc
    func testFirebaseOrderSyncPullsAndroidOrderWhenConfigured() throws {
        let syncMarkerPath = "/tmp/dreamcue-run-firebase-order-pull-test"
        let isSyncEnabled = ProcessInfo.processInfo.environment["DREAMCUE_RUN_FIREBASE_ORDER_PULL_TEST"] == "1"
            || FileManager.default.fileExists(atPath: syncMarkerPath)
        guard isSyncEnabled else {
            throw XCTSkip("Firebase order pull test is disabled.")
        }
        let email = try syncEnvironment("DREAMCUE_SYNC_EMAIL", fallbackPath: "/tmp/dreamcue-order-sync-a2m-email.txt")
        let password = try syncEnvironment("DREAMCUE_SYNC_PASSWORD", fallbackPath: "/tmp/dreamcue-order-sync-a2m-password.txt")
        let expectedOrder = try syncEnvironment(
            "DREAMCUE_EXPECTED_ORDER",
            fallbackPath: "/tmp/dreamcue-order-sync-a2m-expected-order.txt"
        )
            .split(separator: ",")
            .map { String($0).trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        let app = launchIsolatedApp(function: #function)

        signInSync(email: email, password: password, app: app)
        app.buttons["Today"].firstMatch.click()
        assertVisibleOrder(expectedOrder, app: app)
        attachScreenshot("mac-sync-pulled-android-order", app: app)
    }

    @objc
    func testFirebaseOrderSyncUploadsMacOrderWhenConfigured() throws {
        let syncMarkerPath = "/tmp/dreamcue-run-firebase-order-upload-test"
        let isSyncEnabled = ProcessInfo.processInfo.environment["DREAMCUE_RUN_FIREBASE_ORDER_UPLOAD_TEST"] == "1"
            || FileManager.default.fileExists(atPath: syncMarkerPath)
        guard isSyncEnabled else {
            throw XCTSkip("Firebase order upload test is disabled.")
        }
        let email = try syncEnvironment("DREAMCUE_SYNC_EMAIL", fallbackPath: "/tmp/dreamcue-order-sync-m2a-email.txt")
        let password = try syncEnvironment("DREAMCUE_SYNC_PASSWORD", fallbackPath: "/tmp/dreamcue-order-sync-m2a-password.txt")
        let prefix = try syncEnvironment("DREAMCUE_MAC_ORDER_PREFIX", fallbackPath: "/tmp/dreamcue-order-sync-m2a-prefix.txt")
        let firstCue = "\(prefix)_first"
        let secondCue = "\(prefix)_second"
        let thirdCue = "\(prefix)_third"
        let app = launchIsolatedApp(function: #function)

        signInSync(email: email, password: password, app: app)
        app.buttons["Today"].firstMatch.click()
        createCue(firstCue, app: app)
        createCue(secondCue, app: app)
        createCue(thirdCue, app: app)
        assertVisibleOrder([thirdCue, secondCue, firstCue], app: app)

        dragCue(app.buttons[thirdCue], below: app.buttons[firstCue])
        assertVisibleOrder([secondCue, firstCue, thirdCue], app: app)
        attachScreenshot("mac-sync-uploaded-mac-order", app: app)
    }

    private func createCue(_ text: String, app: XCUIApplication) {
        app.activate()
        let newCue = app.buttons["New Cue"].firstMatch
        XCTAssertTrue(newCue.waitForExistence(timeout: 5))
        newCue.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5)).click()
        XCTAssertTrue(app.staticTexts["New Cue"].waitForExistence(timeout: 5))
        focusCueEditor(app)
        paste(text, app: app)
        app.buttons["Save"].click()
    }

    private func dragCue(_ source: XCUIElement, below destination: XCUIElement) {
        XCTAssertTrue(source.waitForExistence(timeout: 5))
        XCTAssertTrue(destination.waitForExistence(timeout: 5))
        let sourceFrame = source.frame
        let destinationFrame = destination.frame
        let startpoint = source.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.5))
        let endpoint = startpoint.withOffset(CGVector(dx: 0, dy: destinationFrame.maxY + 20 - sourceFrame.midY))
        startpoint.press(forDuration: 0.7, thenDragTo: endpoint)
    }

    private func focusCueEditor(_ app: XCUIApplication) {
        let editor = app.scrollViews["Cue Text"].firstMatch
        XCTAssertTrue(editor.waitForExistence(timeout: 5))
        XCTAssertTrue(editor.isHittable)
        editor.click()
    }

    private func dismissSystemSettingsIfOpen(app: XCUIApplication) {
        NSRunningApplication.runningApplications(withBundleIdentifier: "com.apple.systempreferences")
            .forEach { $0.forceTerminate() }
        app.activate()
    }

    private func clickTrailingSideOf(_ element: XCUIElement) {
        XCTAssertTrue(element.waitForExistence(timeout: 5))
        element.coordinate(withNormalizedOffset: CGVector(dx: 0.92, dy: 0.5)).click()
    }

    private func assertNewCueSheetLayout(_ app: XCUIApplication) {
        let editor = app.scrollViews["Cue Text"].firstMatch
        let save = app.buttons["Save"].firstMatch
        let cancel = app.buttons["Cancel"].firstMatch
        XCTAssertTrue(editor.waitForExistence(timeout: 5))
        XCTAssertTrue(save.exists)
        XCTAssertTrue(cancel.exists)
        XCTAssertGreaterThanOrEqual(editor.frame.width, 510)
        XCTAssertLessThan(editor.frame.height, 290)
        XCTAssertLessThan(save.frame.maxY, editor.frame.minY)
        XCTAssertLessThan(cancel.frame.maxY, editor.frame.minY)
        XCTAssertLessThanOrEqual(abs(save.frame.maxX - editor.frame.maxX), 2)
        XCTAssertGreaterThanOrEqual(editor.frame.minX, 0)
        XCTAssertGreaterThan(cancel.frame.minX, editor.frame.minX)
    }

    private func signInSync(email: String, password: String, app: XCUIApplication) {
        app.buttons["Account"].firstMatch.click()
        XCTAssertTrue(app.staticTexts["Sync Account"].waitForExistence(timeout: 5))
        app.textFields["Email"].click()
        paste(email, app: app)
        app.secureTextFields["Password"].click()
        paste(password, app: app)
        app.buttons["Sign In"].click()
        XCTAssertTrue(app.staticTexts["Sync Health"].waitForExistence(timeout: 25))
    }

    private func assertVisibleOrder(_ expectedOrder: [String], app: XCUIApplication) {
        var previousNumber = 0
        for cue in expectedOrder {
            XCTAssertTrue(app.buttons[cue].waitForExistence(timeout: 30), "Expected \(cue) to exist.")
            let displayNumber = app.buttons[cue].value as? String
            XCTAssertNotNil(displayNumber, "Expected \(cue) to expose a display number.")
            let number = Int(displayNumber?.dropFirst() ?? "") ?? 0
            XCTAssertGreaterThan(number, previousNumber, "Expected \(cue) to keep a relative display order.")
            previousNumber = number
        }
        for (before, after) in zip(expectedOrder, expectedOrder.dropFirst()) {
            XCTAssertLessThan(app.buttons[before].frame.minY, app.buttons[after].frame.minY)
        }
    }

    private func assertSidebarButtonFillsRow(_ element: XCUIElement) {
        XCTAssertTrue(element.waitForExistence(timeout: 5))
        XCTAssertGreaterThanOrEqual(element.frame.width, 132)
        XCTAssertLessThanOrEqual(element.frame.height, 44)
    }

    private func launchIsolatedApp(
        function: String = #function,
        extraEnvironment: [String: String] = [:]
    ) -> XCUIApplication {
        closeSystemOverlays()
        let app = XCUIApplication()
        let safeName = function.replacingOccurrences(of: "()", with: "")
        let storageURL = FileManager.default.temporaryDirectory
            .appendingPathComponent("DreamCueMacUITests", isDirectory: true)
            .appendingPathComponent(safeName, isDirectory: true)
            .appendingPathComponent(UUID().uuidString, isDirectory: true)
        try? FileManager.default.removeItem(at: storageURL)
        try? FileManager.default.createDirectory(at: storageURL, withIntermediateDirectories: true)
        app.launchEnvironment["DREAMCUE_STORAGE_DIR"] = storageURL.path
        app.launchEnvironment["DREAMCUE_NOTIFICATIONS_DISABLED"] = "1"
        for (key, value) in extraEnvironment {
            app.launchEnvironment[key] = value
        }
        app.launch()
        return app
    }

    private func closeSystemOverlays() {
        let bundleIdentifiers = [
            "com.apple.systempreferences",
            "com.apple.notificationcenterui",
            "com.apple.tips",
            "net.hearthsim.hstracker",
            "app.livenotes.mac",
        ]
        for bundleIdentifier in bundleIdentifiers {
            let app = XCUIApplication(bundleIdentifier: bundleIdentifier)
            if app.exists {
                app.terminate()
            }
        }
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
        app.typeKey("v", modifierFlags: [.command])
    }

    private func setTimeToNineThirty(app: XCUIApplication) {
        let hourDecrement = app.buttons["timePicker.hourDecrement"]
        let hourIncrement = app.buttons["timePicker.hourIncrement"]
        let minuteDecrement = app.buttons["timePicker.minuteDecrement"]
        let minuteIncrement = app.buttons["timePicker.minuteIncrement"]
        XCTAssertTrue(hourDecrement.waitForExistence(timeout: 5))
        XCTAssertTrue(hourIncrement.exists)
        XCTAssertTrue(minuteDecrement.exists)
        XCTAssertTrue(minuteIncrement.exists)
        for _ in 0..<24 {
            hourDecrement.click()
        }
        for _ in 0..<9 {
            hourIncrement.click()
        }
        for _ in 0..<12 {
            minuteDecrement.click()
        }
        for _ in 0..<6 {
            minuteIncrement.click()
        }
    }

    private func waitForTimePickerValue(_ value: String, app: XCUIApplication) {
        let element = app.staticTexts["timePicker.value"]
        XCTAssertTrue(element.waitForExistence(timeout: 3))
        let deadline = Date().addingTimeInterval(3)
        while Date() < deadline {
            if element.value as? String == value {
                return
            }
            RunLoop.current.run(until: Date().addingTimeInterval(0.1))
        }
        XCTFail("Expected time picker value \(value), got \(String(describing: element.value))")
    }

    private func attachScreenshot(_ name: String, app: XCUIApplication) {
        let attachment = XCTAttachment(screenshot: app.screenshot())
        attachment.name = name
        attachment.lifetime = .keepAlways
        add(attachment)
    }

}
