package app.dreamcue

import android.content.Context
import androidx.core.content.edit
import app.dreamcue.model.Memo
import app.dreamcue.model.MemoEvent
import app.dreamcue.model.ReminderTime
import app.dreamcue.model.ReviewSnapshot
import app.dreamcue.model.SearchResult
import java.io.File
import org.json.JSONArray
import org.json.JSONObject.NULL
import org.json.JSONObject

class DreamCueRepository(context: Context) {
    val appContext: Context = context.applicationContext

    private val prefs = appContext.getSharedPreferences("dreamcue_prefs", Context.MODE_PRIVATE)
    private val databasePath = File(appContext.filesDir, "dreamcue.sqlite3").absolutePath

    @Volatile
    private var handle: Long? = null

    fun initialize(): Result<Unit> = synchronized(this) {
        val existingHandle = handle
        if (existingHandle != null && existingHandle != 0L) {
            return@synchronized Result.success(Unit)
        }
        if (!DreamCueBridge.isLoaded()) {
            return@synchronized Result.failure(
                IllegalStateException(
                    DreamCueBridge.loadError() ?: "Rust native library is missing",
                ),
            )
        }

        return@synchronized runCatching {
            val envelope = JSONObject(DreamCueBridge.nativeInit(databasePath)).requireOk()
            handle = envelope.getLong("data")
        }
    }

    fun isReady(): Boolean = handle != null && handle != 0L

    fun nativeLoadError(): String? = DreamCueBridge.loadError()

    fun addMemo(content: String): Memo = parseObject(
        DreamCueBridge.nativeAddMemo(requireHandle(), content),
        Memo::fromJson,
    )

    fun updateMemo(memoId: String, content: String): Memo = parseObject(
        DreamCueBridge.nativeUpdateMemo(requireHandle(), memoId, content),
        Memo::fromJson,
    )

    fun keepMemo(memoId: String): Memo = parseObject(
        DreamCueBridge.nativeKeepMemo(requireHandle(), memoId),
        Memo::fromJson,
    )

    fun clearMemo(memoId: String): Memo = parseObject(
        DreamCueBridge.nativeClearMemo(requireHandle(), memoId),
        Memo::fromJson,
    )

    fun reopenMemo(memoId: String): Memo = parseObject(
        DreamCueBridge.nativeReopenMemo(requireHandle(), memoId),
        Memo::fromJson,
    )

    fun deleteMemo(memoId: String) {
        parseUnit(DreamCueBridge.nativeDeleteMemo(requireHandle(), memoId))
    }

    fun deleteMemoWithTombstone(memoId: String, deletedAtMs: Long = System.currentTimeMillis()): Long {
        deleteMemo(memoId)
        markMemoDeleted(memoId, deletedAtMs)
        return deletedAtMs
    }

    fun applyRemoteDeletedMemo(memoId: String, deletedAtMs: Long) {
        val localMemo = listAllMemos().firstOrNull { it.id == memoId }
        if (localMemo == null || localMemo.updatedAtMs <= deletedAtMs) {
            markMemoDeleted(memoId, deletedAtMs)
            deleteRemoteMemo(memoId, deletedAtMs)
        }
    }

    fun deletedMemoAt(memoId: String): Long? {
        val key = deletedMemoKey(memoId)
        return if (prefs.contains(key)) prefs.getLong(key, 0L) else null
    }

    fun markMemoDeleted(memoId: String, deletedAtMs: Long) {
        prefs.edit {
            putLong(deletedMemoKey(memoId), deletedAtMs)
        }
    }

    fun clearDeletedMemo(memoId: String) {
        prefs.edit {
            remove(deletedMemoKey(memoId))
        }
    }

    fun setMemoPinned(memoId: String, pinned: Boolean): Memo = parseObject(
        DreamCueBridge.nativeSetMemoPinned(requireHandle(), memoId, if (pinned) 1 else 0),
        Memo::fromJson,
    )

    fun applyRemoteMemo(memo: Memo): Memo = parseObject(
        DreamCueBridge.nativeApplyRemoteMemo(requireHandle(), memo.toJson().toString()),
        Memo::fromJson,
    )

    fun deleteRemoteMemo(memoId: String, deletedAtMs: Long = Long.MAX_VALUE) {
        parseUnit(DreamCueBridge.nativeDeleteRemoteMemo(requireHandle(), memoId, deletedAtMs))
    }

    fun reorderActiveMemos(orderedIds: List<String>): List<Memo> = parseArray(
        DreamCueBridge.nativeReorderActiveMemos(requireHandle(), JSONArray(orderedIds).toString()),
        Memo::fromJson,
    )

    fun listActiveMemos(): List<Memo> = parseArray(
        DreamCueBridge.nativeListActiveMemos(requireHandle()),
        Memo::fromJson,
    )

    fun listAllMemos(): List<Memo> = parseArray(
        DreamCueBridge.nativeListAllMemos(requireHandle()),
        Memo::fromJson,
    )

    fun reviewSnapshot(): ReviewSnapshot = parseObject(
        DreamCueBridge.nativeReviewSnapshot(requireHandle()),
        ReviewSnapshot::fromJson,
    )

    fun searchMemos(query: String, limit: Int = 20): List<SearchResult> = parseArray(
        DreamCueBridge.nativeSearchMemos(requireHandle(), query, limit),
        SearchResult::fromJson,
    )

    fun listEvents(limit: Int = 50): List<MemoEvent> = parseArray(
        DreamCueBridge.nativeListEvents(requireHandle(), limit),
        MemoEvent::fromJson,
    )

    fun reminderTime(): ReminderTime {
        val hour = prefs.getInt(KEY_HOUR, ReminderTime.Default.hour)
        val minute = prefs.getInt(KEY_MINUTE, ReminderTime.Default.minute)
        return ReminderTime(hour, minute)
    }

    fun reminderEnabled(): Boolean {
        return prefs.getBoolean(KEY_REMINDER_ENABLED, true)
    }

    fun saveReminderTime(reminderTime: ReminderTime) {
        prefs.edit {
            putInt(KEY_HOUR, reminderTime.hour)
            putInt(KEY_MINUTE, reminderTime.minute)
        }
    }

    fun saveReminderEnabled(enabled: Boolean) {
        prefs.edit {
            putBoolean(KEY_REMINDER_ENABLED, enabled)
        }
    }

    fun dispose() {
        val currentHandle = handle ?: return
        if (DreamCueBridge.isLoaded()) {
            DreamCueBridge.nativeDispose(currentHandle)
        }
        handle = null
    }

    private fun requireHandle(): Long {
        return handle ?: throw IllegalStateException("Native service has not been initialized")
    }

    private fun <T> parseObject(response: String, mapper: (JSONObject) -> T): T {
        val payload = JSONObject(response).requireOk().getJSONObject("data")
        return mapper(payload)
    }

    private fun <T> parseArray(response: String, mapper: (JSONObject) -> T): List<T> {
        val payload = JSONObject(response).requireOk().getJSONArray("data")
        return buildList(payload.length()) {
            for (index in 0 until payload.length()) {
                add(mapper(payload.getJSONObject(index)))
            }
        }
    }

    private fun parseUnit(response: String) {
        JSONObject(response).requireOk()
    }

    private fun JSONObject.requireOk(): JSONObject {
        if (!optBoolean("ok")) {
            throw IllegalStateException(optString("error", "Unknown native error"))
        }
        return this
    }

    companion object {
        private const val KEY_HOUR = "reminder_hour"
        private const val KEY_MINUTE = "reminder_minute"
        private const val KEY_REMINDER_ENABLED = "reminder_enabled"
        private const val KEY_DELETED_MEMO_PREFIX = "deleted_memo_"

        private fun deletedMemoKey(memoId: String): String {
            return KEY_DELETED_MEMO_PREFIX + memoId
        }
    }
}

private fun Memo.toJson(): JSONObject {
    return JSONObject()
        .put("id", id)
        .put("content", content)
        .put("status", if (isActive) "active" else "cleared")
        .put("created_at_ms", createdAtMs)
        .put("updated_at_ms", updatedAtMs)
        .put("cleared_at_ms", clearedAtMs ?: NULL)
        .put("reminder_count", reminderCount)
        .put("last_reviewed_at_ms", lastReviewedAtMs ?: NULL)
        .put("display_order", displayOrder)
        .put("pinned", pinned)
}
