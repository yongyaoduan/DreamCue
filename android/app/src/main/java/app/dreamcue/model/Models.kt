package app.dreamcue.model

import java.util.Locale
import org.json.JSONArray
import org.json.JSONObject

enum class MemoStatus {
    ACTIVE,
    CLEARED;

    companion object {
        fun fromWire(value: String?): MemoStatus {
            return if (value == "cleared") CLEARED else ACTIVE
        }
    }
}

enum class MemoEventType {
    CREATED,
    EDITED,
    KEPT,
    CLEARED;

    companion object {
        fun fromWire(value: String?): MemoEventType {
            return when (value) {
                "edited" -> EDITED
                "kept" -> KEPT
                "cleared" -> CLEARED
                else -> CREATED
            }
        }
    }
}

data class Memo(
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
) {
    val isActive: Boolean
        get() = status == MemoStatus.ACTIVE

    companion object {
        fun fromJson(json: JSONObject): Memo {
            return Memo(
                id = json.getString("id"),
                content = json.getString("content"),
                status = MemoStatus.fromWire(json.optString("status")),
                createdAtMs = json.getLong("created_at_ms"),
                updatedAtMs = json.getLong("updated_at_ms"),
                clearedAtMs = json.optLongOrNull("cleared_at_ms"),
                reminderCount = json.optLong("reminder_count"),
                lastReviewedAtMs = json.optLongOrNull("last_reviewed_at_ms"),
                displayOrder = json.optLong("display_order", json.optLong("updated_at_ms")),
                pinned = json.optBoolean("pinned", false),
            )
        }
    }
}

data class SearchResult(
    val memo: Memo,
    val score: Double,
    val matchedBy: List<String>,
) {
    companion object {
        fun fromJson(json: JSONObject): SearchResult {
            return SearchResult(
                memo = Memo.fromJson(json.getJSONObject("memo")),
                score = json.optDouble("score"),
                matchedBy = json.optJSONArray("matched_by").toStringList(),
            )
        }
    }
}

data class MemoEvent(
    val id: Long,
    val memoId: String,
    val eventType: MemoEventType,
    val contentSnapshot: String,
    val note: String?,
    val createdAtMs: Long,
) {
    companion object {
        fun fromJson(json: JSONObject): MemoEvent {
            return MemoEvent(
                id = json.getLong("id"),
                memoId = json.getString("memo_id"),
                eventType = MemoEventType.fromWire(json.optString("event_type")),
                contentSnapshot = json.getString("content_snapshot"),
                note = json.optStringOrNull("note"),
                createdAtMs = json.getLong("created_at_ms"),
            )
        }
    }
}

data class ReviewSnapshot(
    val due: List<Memo>,
    val generatedAtMs: Long,
    val activeCount: Int,
    val dueCount: Int,
) {
    companion object {
        fun fromJson(json: JSONObject): ReviewSnapshot {
            return ReviewSnapshot(
                due = json.getJSONArray("due").toMemoList(),
                generatedAtMs = json.getLong("generated_at_ms"),
                activeCount = json.optInt("active_count"),
                dueCount = json.optInt("due_count"),
            )
        }
    }
}

data class ReminderTime(
    val hour: Int,
    val minute: Int,
) {
    fun asText(): String = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)

    companion object {
        val Default = ReminderTime(hour = 21, minute = 0)

        fun fromText(input: String): ReminderTime? {
            val parts = input.trim().split(":")
            if (parts.size != 2) {
                return null
            }
            val hour = parts[0].toIntOrNull() ?: return null
            val minute = parts[1].toIntOrNull() ?: return null
            if (hour !in 0..23 || minute !in 0..59) {
                return null
            }
            return ReminderTime(hour, minute)
        }
    }
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) {
        return emptyList()
    }

    return buildList(length()) {
        for (index in 0 until length()) {
            add(optString(index))
        }
    }
}

private fun JSONArray.toMemoList(): List<Memo> {
    return buildList(length()) {
        for (index in 0 until length()) {
            add(Memo.fromJson(getJSONObject(index)))
        }
    }
}

private fun JSONObject.optLongOrNull(key: String): Long? {
    return if (isNull(key)) null else getLong(key)
}

private fun JSONObject.optStringOrNull(key: String): String? {
    return if (isNull(key)) null else getString(key)
}
