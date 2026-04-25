package app.dreamcue.sync

import app.dreamcue.model.MemoStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteMemoConflictPolicyTest {
    @Test
    fun newerLocalDeletionRejectsStaleRemoteMemoAndReuploadsDeleteMarker() {
        val remote = remoteMemo(updatedAtMs = 1_000, deleted = false)

        val decision = decideRemoteMemoApplication(remote, localDeletedAtMs = 2_000)

        assertFalse(decision.applyRemote)
        assertEquals(2_000L, decision.deletedAtMsToUpload)
    }

    @Test
    fun remoteMemoNewerThanLocalDeletionCanBeApplied() {
        val remote = remoteMemo(updatedAtMs = 3_000, deleted = false)

        val decision = decideRemoteMemoApplication(remote, localDeletedAtMs = 2_000)

        assertTrue(decision.applyRemote)
        assertEquals(null, decision.deletedAtMsToUpload)
    }

    @Test
    fun remoteDeleteDocumentsAreApplied() {
        val remote = remoteMemo(updatedAtMs = 3_000, deleted = true)

        val decision = decideRemoteMemoApplication(remote, localDeletedAtMs = 4_000)

        assertTrue(decision.applyRemote)
        assertEquals(null, decision.deletedAtMsToUpload)
    }

    private fun remoteMemo(updatedAtMs: Long, deleted: Boolean): RemoteMemoDocument {
        return RemoteMemoDocument(
            id = "memo-1",
            content = "Remote cue",
            status = MemoStatus.ACTIVE,
            createdAtMs = 900,
            updatedAtMs = updatedAtMs,
            clearedAtMs = null,
            reminderCount = 0,
            lastReviewedAtMs = null,
            displayOrder = updatedAtMs,
            pinned = false,
            deleted = deleted,
        )
    }
}
