package app.dreamcue

import android.content.Context
import androidx.core.content.edit
import app.dreamcue.model.Memo
import app.dreamcue.model.MemoEvent
import app.dreamcue.model.ReminderTime
import app.dreamcue.model.ReviewSnapshot
import app.dreamcue.model.SearchResult
import java.io.File
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

    fun saveReminderTime(reminderTime: ReminderTime) {
        prefs.edit {
            putInt(KEY_HOUR, reminderTime.hour)
            putInt(KEY_MINUTE, reminderTime.minute)
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
    }
}
