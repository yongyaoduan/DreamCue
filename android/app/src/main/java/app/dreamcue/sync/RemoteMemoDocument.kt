package app.dreamcue.sync

import app.dreamcue.model.Memo
import app.dreamcue.model.MemoStatus

data class RemoteMemoDocument(
    val id: String,
    val content: String,
    val status: MemoStatus,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val clearedAtMs: Long?,
    val reminderCount: Long,
    val lastReviewedAtMs: Long?,
    val displayOrder: Long,
    val pinned: Boolean,
    val deleted: Boolean,
) {
    fun toMemo(): Memo {
        return Memo(
            id = id,
            content = content,
            status = status,
            createdAtMs = createdAtMs,
            updatedAtMs = updatedAtMs,
            clearedAtMs = clearedAtMs,
            reminderCount = reminderCount,
            lastReviewedAtMs = lastReviewedAtMs,
            displayOrder = displayOrder,
            pinned = pinned,
        )
    }

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "content" to content,
            "status" to status.toWire(),
            "created_at_ms" to createdAtMs,
            "updated_at_ms" to updatedAtMs,
            "cleared_at_ms" to clearedAtMs,
            "reminder_count" to reminderCount,
            "last_reviewed_at_ms" to lastReviewedAtMs,
            "display_order" to displayOrder,
            "pinned" to pinned,
            "deleted" to deleted,
        )
    }

    companion object {
        fun fromMemo(memo: Memo): RemoteMemoDocument {
            return RemoteMemoDocument(
                id = memo.id,
                content = memo.content,
                status = memo.status,
                createdAtMs = memo.createdAtMs,
                updatedAtMs = memo.updatedAtMs,
                clearedAtMs = memo.clearedAtMs,
                reminderCount = memo.reminderCount,
                lastReviewedAtMs = memo.lastReviewedAtMs,
                displayOrder = memo.displayOrder,
                pinned = memo.pinned,
                deleted = false,
            )
        }

        fun deletedMemo(memoId: String, deletedAtMs: Long): RemoteMemoDocument {
            return RemoteMemoDocument(
                id = memoId,
                content = "",
                status = MemoStatus.CLEARED,
                createdAtMs = deletedAtMs,
                updatedAtMs = deletedAtMs,
                clearedAtMs = deletedAtMs,
                reminderCount = 0,
                lastReviewedAtMs = deletedAtMs,
                displayOrder = deletedAtMs,
                pinned = false,
                deleted = true,
            )
        }

        fun fromMap(id: String, values: Map<String, Any?>): RemoteMemoDocument {
            return RemoteMemoDocument(
                id = id,
                content = values["content"] as? String ?: "",
                status = MemoStatus.fromWire(values["status"] as? String),
                createdAtMs = values.longValue("created_at_ms"),
                updatedAtMs = values.longValue("updated_at_ms"),
                clearedAtMs = values.nullableLongValue("cleared_at_ms"),
                reminderCount = values.longValue("reminder_count"),
                lastReviewedAtMs = values.nullableLongValue("last_reviewed_at_ms"),
                displayOrder = values.longValue("display_order")
                    .takeIf { it != 0L }
                    ?: values.longValue("updated_at_ms"),
                pinned = values["pinned"] as? Boolean ?: false,
                deleted = values["deleted"] as? Boolean ?: false,
            )
        }
    }
}

private fun MemoStatus.toWire(): String {
    return when (this) {
        MemoStatus.ACTIVE -> "active"
        MemoStatus.CLEARED -> "cleared"
    }
}

private fun Map<String, Any?>.longValue(key: String): Long {
    return nullableLongValue(key) ?: 0L
}

private fun Map<String, Any?>.nullableLongValue(key: String): Long? {
    return when (val value = this[key]) {
        is Long -> value
        is Int -> value.toLong()
        is Double -> value.toLong()
        is Number -> value.toLong()
        else -> null
    }
}
