package app.dreamcue.sync

data class RemoteMemoDecision(
    val applyRemote: Boolean,
    val deletedAtMsToUpload: Long? = null,
)

fun decideRemoteMemoApplication(
    remote: RemoteMemoDocument,
    localDeletedAtMs: Long?,
): RemoteMemoDecision {
    if (remote.deleted) {
        return RemoteMemoDecision(applyRemote = true)
    }

    if (localDeletedAtMs != null && localDeletedAtMs >= remote.updatedAtMs) {
        return RemoteMemoDecision(
            applyRemote = false,
            deletedAtMsToUpload = localDeletedAtMs,
        )
    }

    return RemoteMemoDecision(applyRemote = true)
}
