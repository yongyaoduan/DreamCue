package app.dreamcue.worker

import app.dreamcue.model.Memo

data class ReminderNotificationRequest(
    val memo: Memo?,
    val title: String,
    val text: String,
    val autoCancel: Boolean,
)

data class ReminderNotificationPlan(
    val individualMemos: List<Memo>,
    val summaryCount: Int,
) {
    val summaryText: String
        get() {
            val noun = if (summaryCount == 1) "cue" else "cues"
            return "$summaryCount $noun, please review."
        }

    val requests: List<ReminderNotificationRequest>
        get() {
            val individualRequests = individualMemos.map { memo ->
                ReminderNotificationRequest(
                    memo = memo,
                    title = "Pinned Cue",
                    text = memo.content,
                    autoCancel = false,
                )
            }
            val summaryRequest = if (summaryCount > 0) {
                listOf(
                    ReminderNotificationRequest(
                        memo = null,
                        title = "DreamCue",
                        text = summaryText,
                        autoCancel = true,
                    ),
                )
            } else {
                emptyList()
            }
            return individualRequests + summaryRequest
        }

    companion object {
        fun fromMemos(memos: List<Memo>): ReminderNotificationPlan {
            val activeMemos = memos.filter { it.isActive }
            return ReminderNotificationPlan(
                individualMemos = activeMemos.filter { it.pinned },
                summaryCount = activeMemos.count { !it.pinned },
            )
        }
    }
}
