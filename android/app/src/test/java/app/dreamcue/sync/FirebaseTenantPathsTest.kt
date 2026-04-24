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
}
