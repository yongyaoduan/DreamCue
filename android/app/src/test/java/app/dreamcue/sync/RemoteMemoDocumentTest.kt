package app.dreamcue.sync

import app.dreamcue.model.Memo
import app.dreamcue.model.MemoStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteMemoDocumentTest {
    @Test
    fun memoDocumentRoundTripsStableFields() {
        val memo = Memo(
            id = "memo-1",
            content = "Renew passport",
            status = MemoStatus.ACTIVE,
            createdAtMs = 1_000,
            updatedAtMs = 2_000,
            clearedAtMs = null,
            reminderCount = 3,
            lastReviewedAtMs = 1_500,
            displayOrder = 2_000,
            pinned = true,
        )

        val document = RemoteMemoDocument.fromMemo(memo).toMap()
        val decoded = RemoteMemoDocument.fromMap("memo-1", document).toMemo()

        assertEquals(memo, decoded)
        assertFalse(document.getValue("deleted") as Boolean)
        assertEquals(2_000L, document.getValue("display_order"))
        assertEquals(true, document.getValue("pinned"))
    }

    @Test
    fun deleteDocumentMarksMemoAsDeleted() {
        val document = RemoteMemoDocument.deletedMemo(
            memoId = "memo-2",
            deletedAtMs = 3_000,
        )

        assertTrue(document.deleted)
        assertEquals(3_000, document.updatedAtMs)
        assertEquals(true, document.toMap()["deleted"])
    }
}
