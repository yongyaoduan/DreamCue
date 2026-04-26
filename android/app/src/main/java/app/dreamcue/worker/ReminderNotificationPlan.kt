package app.dreamcue.worker

import app.dreamcue.model.Memo

data class ReminderNotificationPlan(
    val individualMemos: List<Memo>,
    val summaryCount: Int,
) {
    val summaryText: String
        get() {
            val noun = if (summaryCount == 1) "cue" else "cues"
            return "$summaryCount $noun, please review."
        }

    companion object {
        fun fromMemos(memos: List<Memo>): ReminderNotificationPlan {
            val activeMemos = memos.filter { it.isActive }
            return ReminderNotificationPlan(
                individualMemos = activeMemos.filter { it.pinned },
                summaryCount = activeMemos.size,
            )
        }
    }
}
