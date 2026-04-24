import XCTest

final class DreamCueMacUITests: XCTestCase {
    @objc
    func testCreateSearchAndOpenMemo() {
        let app = XCUIApplication()
        let memoText = "mac_note_\(UUID().uuidString.prefix(8))"
        app.launch()

        XCTAssertTrue(app.staticTexts["DreamCue"].waitForExistence(timeout: 5))
        app.textFields["Memo"].click()
        app.typeText(memoText)
        app.buttons["Save"].click()

        XCTAssertTrue(app.buttons[memoText].waitForExistence(timeout: 5))
        app.staticTexts["Search"].firstMatch.click()
        app.textFields["Search"].click()
        app.typeText("mac")
        XCTAssertTrue(app.buttons[memoText].waitForExistence(timeout: 5))
    }

    @objc
    func testSettingsExposeTenantScopedSync() {
        let app = XCUIApplication()
        app.launch()

        app.staticTexts["Settings"].firstMatch.click()
        XCTAssertTrue(app.staticTexts["Sync Account"].waitForExistence(timeout: 5))
        let emailField = app.textFields["Email"]
        XCTAssertTrue(emailField.exists)
        XCTAssertTrue(app.secureTextFields["Password"].exists)
        XCTAssertTrue(app.buttons["Sign In"].exists)
        XCTAssertLessThan(
            app.staticTexts["Sync Account"].frame.minY - app.windows.firstMatch.frame.minY,
            160
        )
    }
}
