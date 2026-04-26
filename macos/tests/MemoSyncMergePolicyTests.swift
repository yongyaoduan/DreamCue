import Foundation

@main
enum MemoSyncMergePolicyTests {
    static func main() {
        let localMemo = Memo(
            id: "memo-a",
            content: "Local stale cue",
            status: .active,
            createdAtMs: 1_700_000_000_000,
            updatedAtMs: 1_700_000_010_000,
            clearedAtMs: nil,
            reminderCount: 0,
            lastReviewedAtMs: nil,
            displayOrder: 1_700_000_000_000,
            pinned: false
        )

        let deletedRecord = RemoteMemoRecord(
            id: "memo-a",
            memo: nil,
            deleted: true,
            updatedAtMs: 1_700_000_020_000
        )

        let mergedAfterDelete = mergeRemoteRecords([deletedRecord], into: [localMemo])
        check(mergedAfterDelete.isEmpty, "Newer remote deletion must remove the local memo.")

        let staleDeletedRecord = RemoteMemoRecord(
            id: "memo-a",
            memo: nil,
            deleted: true,
            updatedAtMs: 1_700_000_005_000
        )

        let mergedAfterStaleDelete = mergeRemoteRecords([staleDeletedRecord], into: [localMemo])
        check(mergedAfterStaleDelete == [localMemo], "Older remote deletion must not remove a newer local memo.")

        let remoteMemo = Memo(
            id: "memo-b",
            content: "Remote active cue",
            status: .active,
            createdAtMs: 1_700_000_000_000,
            updatedAtMs: 1_700_000_030_000,
            clearedAtMs: nil,
            reminderCount: 0,
            lastReviewedAtMs: nil,
            displayOrder: 1_700_000_030_000,
            pinned: false
        )

        let remoteRecord = RemoteMemoRecord(
            id: remoteMemo.id,
            memo: remoteMemo,
            deleted: false,
            updatedAtMs: remoteMemo.updatedAtMs
        )

        let mergedAfterInsert = mergeRemoteRecords([remoteRecord], into: [])
        check(mergedAfterInsert == [remoteMemo], "Missing remote memo must be inserted locally.")

        var localOrderedMemo = localMemo
        localOrderedMemo.displayOrder = 1_700_000_010_000
        let remoteOrderedMemo = Memo(
            id: localOrderedMemo.id,
            content: "Local stale cue",
            status: .active,
            createdAtMs: localOrderedMemo.createdAtMs,
            updatedAtMs: 1_700_000_005_000,
            clearedAtMs: nil,
            reminderCount: 0,
            lastReviewedAtMs: nil,
            displayOrder: 1_700_000_040_000,
            pinned: true
        )
        let remoteOrderRecord = RemoteMemoRecord(
            id: remoteOrderedMemo.id,
            memo: remoteOrderedMemo,
            deleted: false,
            updatedAtMs: remoteOrderedMemo.updatedAtMs
        )
        let mergedAfterOrderChange = mergeRemoteRecords([remoteOrderRecord], into: [localOrderedMemo])
        check(mergedAfterOrderChange[0].displayOrder == remoteOrderedMemo.displayOrder, "Newer remote order must be merged even when content is older.")
        check(mergedAfterOrderChange[0].pinned, "Remote pin state must follow the newer remote order.")

        var localPinnedMemo = localMemo
        localPinnedMemo.displayOrder = 1_700_000_050_000
        localPinnedMemo.updatedAtMs = 1_700_000_040_000
        localPinnedMemo.pinned = true
        let remoteUnpinnedMemo = Memo(
            id: localPinnedMemo.id,
            content: localPinnedMemo.content,
            status: .active,
            createdAtMs: localPinnedMemo.createdAtMs,
            updatedAtMs: 1_700_000_060_000,
            clearedAtMs: nil,
            reminderCount: 0,
            lastReviewedAtMs: nil,
            displayOrder: 1_700_000_030_000,
            pinned: false
        )
        let remoteUnpinRecord = RemoteMemoRecord(
            id: remoteUnpinnedMemo.id,
            memo: remoteUnpinnedMemo,
            deleted: false,
            updatedAtMs: remoteUnpinnedMemo.updatedAtMs
        )
        let mergedAfterRemoteUnpin = mergeRemoteRecords([remoteUnpinRecord], into: [localPinnedMemo])
        check(!mergedAfterRemoteUnpin[0].pinned, "Newer remote unpin must clear the local pin state.")
        check(
            mergedAfterRemoteUnpin[0].displayOrder == localPinnedMemo.displayOrder,
            "Remote unpin must preserve the higher local display order."
        )

        let staleRemoteMemo = Memo(
            id: "memo-c",
            content: "Stale remote cue",
            status: .active,
            createdAtMs: 1_700_000_000_000,
            updatedAtMs: 1_700_000_020_000,
            clearedAtMs: nil,
            reminderCount: 0,
            lastReviewedAtMs: nil,
            displayOrder: 1_700_000_020_000,
            pinned: false
        )
        let staleRemoteRecord = RemoteMemoRecord(
            id: staleRemoteMemo.id,
            memo: staleRemoteMemo,
            deleted: false,
            updatedAtMs: staleRemoteMemo.updatedAtMs
        )
        let tombstoneOutcome = mergeRemoteRecordsWithTombstones(
            [staleRemoteRecord],
            into: [],
            deletedMemoTombstones: ["memo-c": 1_700_000_040_000]
        )
        check(tombstoneOutcome.memos.isEmpty, "Newer local deletion must not restore a stale remote memo.")
        check(
            tombstoneOutcome.deletedMemosToUpload == [
                RemoteDeletedMemo(id: "memo-c", deletedAtMs: 1_700_000_040_000)
            ],
            "Stale remote memo must trigger a delete-marker reupload."
        )
    }

    private static func check(_ condition: @autoclosure () -> Bool, _ message: String) {
        if !condition() {
            FileHandle.standardError.write((message + "\n").data(using: .utf8)!)
            exit(1)
        }
    }
}
