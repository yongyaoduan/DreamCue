package app.dreamcue.worker

import app.dreamcue.model.Memo
import app.dreamcue.model.MemoStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderNotificationPlanTest {
    @Test
    fun pinnedMemosReceiveIndividualNotificationsAndSummaryCountsAllDueMemos() {
        val pinned = memo(id = "pinned", content = "Pinned cue", pinned = true)
        val regular = memo(id = "regular", content = "Regular cue", pinned = false)

        val plan = ReminderNotificationPlan.fromMemos(listOf(pinned, regular))

        assertEquals(listOf(pinned), plan.individualMemos)
        assertEquals(2, plan.summaryCount)
        assertEquals("2 cues, please review.", plan.summaryText)
    }

    @Test
    fun regularOnlyRemindersUseOneSummaryNotification() {
        val first = memo(id = "first", content = "First cue", pinned = false)
        val second = memo(id = "second", content = "Second cue", pinned = false)

        val plan = ReminderNotificationPlan.fromMemos(listOf(first, second))

        assertEquals(emptyList<Memo>(), plan.individualMemos)
        assertEquals(2, plan.summaryCount)
        assertEquals("2 cues, please review.", plan.summaryText)
    }

    private fun memo(id: String, content: String, pinned: Boolean): Memo {
        return Memo(
            id = id,
            content = content,
            status = MemoStatus.ACTIVE,
            createdAtMs = 1_000,
            updatedAtMs = 1_000,
            clearedAtMs = null,
            reminderCount = 0,
            lastReviewedAtMs = null,
            displayOrder = 1_000,
            pinned = pinned,
        )
    }
}
