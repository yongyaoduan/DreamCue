import CoreGraphics

struct CueDragPresentation {
    let translationY: CGFloat
    let isDragging: Bool

    var offsetY: CGFloat {
        isDragging ? translationY : 0
    }

    var scale: CGFloat {
        isDragging ? 1.012 : 1
    }

    var shadowRadius: CGFloat {
        isDragging ? 18 : 0
    }

    var shadowOffsetY: CGFloat {
        isDragging ? 8 : 0
    }

    static func targetIndex(index: Int, rowCount: Int, translationY: CGFloat, rowHeight: CGFloat = 78) -> Int {
        let rowStep = Int((translationY / rowHeight).rounded())
        return min(max(index + rowStep, 0), rowCount - 1)
    }

    static func displacedOffset(
        rowIndex: Int,
        sourceIndex: Int,
        targetIndex: Int,
        rowHeight: CGFloat = 78
    ) -> CGFloat {
        if sourceIndex < targetIndex, rowIndex > sourceIndex, rowIndex <= targetIndex {
            return -rowHeight
        }
        if sourceIndex > targetIndex, rowIndex >= targetIndex, rowIndex < sourceIndex {
            return rowHeight
        }
        return 0
    }
}
