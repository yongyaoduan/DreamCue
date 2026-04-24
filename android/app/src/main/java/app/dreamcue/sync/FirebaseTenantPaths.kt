package app.dreamcue.sync

object FirebaseTenantPaths {
    fun memoCollectionPath(userId: String): String {
        return "users/$userId/memos"
    }
}
