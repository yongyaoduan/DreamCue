import Foundation

enum MemoStatus: String, Codable {
    case active
    case cleared
}

struct Memo: Identifiable, Codable, Equatable {
    let id: String
    var content: String
    var status: MemoStatus
    var createdAtMs: Int64
    var updatedAtMs: Int64
    var clearedAtMs: Int64?
    var reminderCount: Int64
    var lastReviewedAtMs: Int64?

    var isActive: Bool {
        status == .active
    }
}
