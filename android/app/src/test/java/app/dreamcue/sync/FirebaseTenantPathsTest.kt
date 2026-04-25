package app.dreamcue.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class FirebaseTenantPathsTest {
    @Test
    fun memoCollectionIsScopedByAuthenticatedUser() {
        assertEquals(
            "users/user-123/memos",
            FirebaseTenantPaths.memoCollectionPath("user-123"),
        )
    }

    @Test
    fun memoDocumentIsScopedByAuthenticatedUser() {
        assertEquals(
            "users/user-123/memos/memo-456",
            FirebaseTenantPaths.memoDocumentPath("user-123", "memo-456"),
        )
    }
}
