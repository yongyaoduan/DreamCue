package app.dreamcue.sync

object FirebaseTenantPaths {
    fun memoCollectionPath(userId: String): String {
        return "users/$userId/memos"
    }

    fun memoDocumentPath(userId: String, memoId: String): String {
        return "${memoCollectionPath(userId)}/$memoId"
    }
}
