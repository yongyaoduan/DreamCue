import CoreGraphics
import Foundation

@main
enum CueDragPresentationTests {
    static func main() {
        let active = CueDragPresentation(translationY: 112, isDragging: true)
        check(active.offsetY == 112, "Dragged cue must follow the pointer translation.")
        check(active.scale > 1, "Dragged cue must lift above the list.")
        check(active.shadowRadius > 0, "Dragged cue must show lift while moving.")

        let resting = CueDragPresentation(translationY: 112, isDragging: false)
        check(resting.offsetY == 0, "Resting cue must return to its row.")
        check(resting.scale == 1, "Resting cue must keep the default scale.")

        check(
            CueDragPresentation.targetIndex(index: 0, rowCount: 3, translationY: 160) == 2,
            "Downward drag must target a lower row."
        )
        check(
            CueDragPresentation.targetIndex(index: 2, rowCount: 3, translationY: -160) == 0,
            "Upward drag must target a higher row."
        )
    }

    private static func check(_ condition: @autoclosure () -> Bool, _ message: String) {
        if !condition() {
            FileHandle.standardError.write((message + "\n").data(using: .utf8)!)
            exit(1)
        }
    }
}
