package com.example.memolog

class NativeBridge private constructor() {
    companion object {
        private var loaded = false
        private var loadFailureMessage: String? = null

        init {
            try {
                System.loadLibrary("memo_android_ffi")
                loaded = true
            } catch (throwable: Throwable) {
                loadFailureMessage = throwable.message ?: throwable.javaClass.simpleName
            }
        }

        @JvmStatic
        external fun nativeInit(dbPath: String): String

        @JvmStatic
        external fun nativeDispose(handle: Long)

        @JvmStatic
        external fun nativeAddMemo(handle: Long, content: String): String

        @JvmStatic
        external fun nativeUpdateMemo(handle: Long, memoId: String, content: String): String

        @JvmStatic
        external fun nativeKeepMemo(handle: Long, memoId: String): String

        @JvmStatic
        external fun nativeClearMemo(handle: Long, memoId: String): String

        @JvmStatic
        external fun nativeReopenMemo(handle: Long, memoId: String): String

        @JvmStatic
        external fun nativeDeleteMemo(handle: Long, memoId: String): String

        @JvmStatic
        external fun nativeListActiveMemos(handle: Long): String

        @JvmStatic
        external fun nativeListAllMemos(handle: Long): String

        @JvmStatic
        external fun nativeReviewSnapshot(handle: Long): String

        @JvmStatic
        external fun nativeSearchMemos(handle: Long, query: String, limit: Int): String

        @JvmStatic
        external fun nativeListEvents(handle: Long, limit: Int): String

        fun isLoaded(): Boolean = loaded

        fun loadError(): String? = loadFailureMessage
    }
}
