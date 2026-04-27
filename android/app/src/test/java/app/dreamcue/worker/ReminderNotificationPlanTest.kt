package app.dreamcue.worker

import app.dreamcue.model.Memo
import app.dreamcue.model.MemoStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderNotificationPlanTest {
    @Test
    fun pinnedMemosReceiveIndividualNotificationsAndSummaryCountsRegularMemos() {
        val pinned = memo(id = "pinned", content = "Pinned cue", pinned = true)
        val regular = memo(id = "regular", content = "Regular cue", pinned = false)
        val clearedPinned = memo(
            id = "cleared-pinned",
            content = "Cleared pinned cue",
            pinned = true,
            status = MemoStatus.CLEARED,
        )

        val plan = ReminderNotificationPlan.fromMemos(listOf(pinned, regular, clearedPinned))

        assertEquals(listOf(pinned), plan.individualMemos)
        assertEquals(1, plan.summaryCount)
        assertEquals("1 cue, please review.", plan.summaryText)
        assertEquals(2, plan.requests.size)
        assertEquals(pinned.id, plan.requests[0].memo?.id)
        assertEquals("Pinned Cue", plan.requests[0].title)
        assertEquals(pinned.content, plan.requests[0].text)
        assertEquals(false, plan.requests[0].autoCancel)
        assertEquals(null, plan.requests[1].memo)
        assertEquals("DreamCue", plan.requests[1].title)
        assertEquals("1 cue, please review.", plan.requests[1].text)
        assertEquals(true, plan.requests[1].autoCancel)
    }

    @Test
    fun pinnedOnlyRemindersDoNotCreateSummaryNotification() {
        val pinned = memo(id = "pinned", content = "Pinned cue", pinned = true)

        val plan = ReminderNotificationPlan.fromMemos(listOf(pinned))

        assertEquals(listOf(pinned), plan.individualMemos)
        assertEquals(0, plan.summaryCount)
        assertEquals(1, plan.requests.size)
        assertEquals(pinned.id, plan.requests[0].memo?.id)
        assertEquals("Pinned Cue", plan.requests[0].title)
        assertEquals(pinned.content, plan.requests[0].text)
        assertEquals(false, plan.requests[0].autoCancel)
    }

    @Test
    fun regularOnlyRemindersUseOneSummaryNotification() {
        val first = memo(id = "first", content = "First cue", pinned = false)
        val second = memo(id = "second", content = "Second cue", pinned = false)

        val plan = ReminderNotificationPlan.fromMemos(listOf(first, second))

        assertEquals(emptyList<Memo>(), plan.individualMemos)
        assertEquals(2, plan.summaryCount)
        assertEquals("2 cues, please review.", plan.summaryText)
        assertEquals(1, plan.requests.size)
        assertEquals(null, plan.requests[0].memo)
        assertEquals("DreamCue", plan.requests[0].title)
        assertEquals("2 cues, please review.", plan.requests[0].text)
        assertEquals(true, plan.requests[0].autoCancel)
    }

    private fun memo(
        id: String,
        content: String,
        pinned: Boolean,
        status: MemoStatus = MemoStatus.ACTIVE,
    ): Memo {
        return Memo(
            id = id,
            content = content,
            status = status,
            createdAtMs = 1_000,
            updatedAtMs = 1_000,
            clearedAtMs = if (status == MemoStatus.CLEARED) 1_500 else null,
            reminderCount = 0,
            lastReviewedAtMs = null,
            displayOrder = 1_000,
            pinned = pinned,
        )
    }
}
