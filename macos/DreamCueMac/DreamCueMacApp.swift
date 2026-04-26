import AppKit
import SwiftUI

@main
struct DreamCueMacApp: App {
    @NSApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
        .defaultSize(width: 980, height: 682)
        .commands {
            CommandGroup(replacing: .newItem) {
                Button("New Cue") {
                    NotificationCenter.default.post(name: .dreamCueNewCueRequested, object: nil)
                }
                .keyboardShortcut("n", modifiers: .command)
            }
        }
    }
}

extension Notification.Name {
    static let dreamCueNewCueRequested = Notification.Name("dreamCueNewCueRequested")
}

final class AppDelegate: NSObject, NSApplicationDelegate {
    func applicationDidFinishLaunching(_ notification: Notification) {
        UserDefaults.standard.set(false, forKey: "NSQuitAlwaysKeepsWindows")
        NSApp.setActivationPolicy(.regular)
        bringMainWindowForward()
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            self.bringMainWindowForward()
        }
    }

    func applicationSupportsSecureRestorableState(_ app: NSApplication) -> Bool {
        false
    }

    func applicationShouldHandleReopen(_ sender: NSApplication, hasVisibleWindows flag: Bool) -> Bool {
        bringMainWindowForward()
        return true
    }

    private func bringMainWindowForward() {
        DispatchQueue.main.async {
            NSApp.windows.first { $0.canBecomeMain && !$0.isSheet }?.makeKeyAndOrderFront(nil)
            NSApp.activate(ignoringOtherApps: true)
        }
    }
}
