package com.example.memolog

import android.content.Context
import androidx.core.content.edit
import com.example.memolog.model.Memo
import com.example.memolog.model.MemoEvent
import com.example.memolog.model.ReminderTime
import com.example.memolog.model.ReviewSnapshot
import com.example.memolog.model.SearchResult
import java.io.File
import org.json.JSONObject

class MemoRepository(context: Context) {
    val appContext: Context = context.applicationContext

    private val prefs = appContext.getSharedPreferences("memo_prefs", Context.MODE_PRIVATE)
    private val databasePath = File(appContext.filesDir, "memolog.sqlite3").absolutePath

    @Volatile
    private var handle: Long? = null

    fun initialize(): Result<Unit> = synchronized(this) {
        val existingHandle = handle
        if (existingHandle != null && existingHandle != 0L) {
            return@synchronized Result.success(Unit)
        }
        if (!NativeBridge.isLoaded()) {
            return@synchronized Result.failure(
                IllegalStateException(
                    NativeBridge.loadError() ?: "Rust native library is missing",
                ),
            )
        }

        return@synchronized runCatching {
            val envelope = JSONObject(NativeBridge.nativeInit(databasePath)).requireOk()
            handle = envelope.getLong("data")
        }
    }

    fun isReady(): Boolean = handle != null && handle != 0L

    fun nativeLoadError(): String? = NativeBridge.loadError()

    fun addMemo(content: String): Memo = parseObject(
        NativeBridge.nativeAddMemo(requireHandle(), content),
        Memo::fromJson,
    )

    fun updateMemo(memoId: String, content: String): Memo = parseObject(
        NativeBridge.nativeUpdateMemo(requireHandle(), memoId, content),
        Memo::fromJson,
    )

    fun keepMemo(memoId: String): Memo = parseObject(
        NativeBridge.nativeKeepMemo(requireHandle(), memoId),
        Memo::fromJson,
    )

    fun clearMemo(memoId: String): Memo = parseObject(
        NativeBridge.nativeClearMemo(requireHandle(), memoId),
        Memo::fromJson,
    )

    fun reopenMemo(memoId: String): Memo = parseObject(
        NativeBridge.nativeReopenMemo(requireHandle(), memoId),
        Memo::fromJson,
    )

    fun deleteMemo(memoId: String) {
        parseUnit(NativeBridge.nativeDeleteMemo(requireHandle(), memoId))
    }

    fun listActiveMemos(): List<Memo> = parseArray(
        NativeBridge.nativeListActiveMemos(requireHandle()),
        Memo::fromJson,
    )

    fun listAllMemos(): List<Memo> = parseArray(
        NativeBridge.nativeListAllMemos(requireHandle()),
        Memo::fromJson,
    )

    fun reviewSnapshot(): ReviewSnapshot = parseObject(
        NativeBridge.nativeReviewSnapshot(requireHandle()),
        ReviewSnapshot::fromJson,
    )

    fun searchMemos(query: String, limit: Int = 20): List<SearchResult> = parseArray(
        NativeBridge.nativeSearchMemos(requireHandle(), query, limit),
        SearchResult::fromJson,
    )

    fun listEvents(limit: Int = 50): List<MemoEvent> = parseArray(
        NativeBridge.nativeListEvents(requireHandle(), limit),
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
        if (NativeBridge.isLoaded()) {
            NativeBridge.nativeDispose(currentHandle)
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
