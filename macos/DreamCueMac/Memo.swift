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
    var displayOrder: Int64
    var pinned: Bool

    var isActive: Bool {
        status == .active
    }

    enum CodingKeys: String, CodingKey {
        case id
        case content
        case status
        case createdAtMs
        case updatedAtMs
        case clearedAtMs
        case reminderCount
        case lastReviewedAtMs
        case displayOrder
        case pinned
    }

    init(
        id: String,
        content: String,
        status: MemoStatus,
        createdAtMs: Int64,
        updatedAtMs: Int64,
        clearedAtMs: Int64?,
        reminderCount: Int64,
        lastReviewedAtMs: Int64?,
        displayOrder: Int64,
        pinned: Bool
    ) {
        self.id = id
        self.content = content
        self.status = status
        self.createdAtMs = createdAtMs
        self.updatedAtMs = updatedAtMs
        self.clearedAtMs = clearedAtMs
        self.reminderCount = reminderCount
        self.lastReviewedAtMs = lastReviewedAtMs
        self.displayOrder = displayOrder
        self.pinned = pinned
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(String.self, forKey: .id)
        content = try container.decode(String.self, forKey: .content)
        status = try container.decode(MemoStatus.self, forKey: .status)
        createdAtMs = try container.decode(Int64.self, forKey: .createdAtMs)
        updatedAtMs = try container.decode(Int64.self, forKey: .updatedAtMs)
        clearedAtMs = try container.decodeIfPresent(Int64.self, forKey: .clearedAtMs)
        reminderCount = try container.decode(Int64.self, forKey: .reminderCount)
        lastReviewedAtMs = try container.decodeIfPresent(Int64.self, forKey: .lastReviewedAtMs)
        displayOrder = try container.decodeIfPresent(Int64.self, forKey: .displayOrder) ?? updatedAtMs
        pinned = try container.decodeIfPresent(Bool.self, forKey: .pinned) ?? false
    }
}
