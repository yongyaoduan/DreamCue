import Foundation

struct RemoteMemoRecord: Equatable {
    let id: String
    let memo: Memo?
    let deleted: Bool
    let updatedAtMs: Int64
}

struct RemoteDeletedMemo: Equatable {
    let id: String
    let deletedAtMs: Int64
}

struct RemoteMergeOutcome: Equatable {
    let memos: [Memo]
    let deletedMemoTombstones: [String: Int64]
    let deletedMemosToUpload: [RemoteDeletedMemo]
}

func mergeRemoteRecords(_ remoteRecords: [RemoteMemoRecord], into localMemos: [Memo]) -> [Memo] {
    mergeRemoteRecordsWithTombstones(
        remoteRecords,
        into: localMemos,
        deletedMemoTombstones: [:]
    ).memos
}

func mergeRemoteRecordsWithTombstones(
    _ remoteRecords: [RemoteMemoRecord],
    into localMemos: [Memo],
    deletedMemoTombstones: [String: Int64]
) -> RemoteMergeOutcome {
    var tombstones = deletedMemoTombstones
    var recordsToMerge: [RemoteMemoRecord] = []
    var deletedMemosToUpload: [RemoteDeletedMemo] = []

    for record in remoteRecords {
        if record.deleted {
            if let localMemo = localMemos.first(where: { $0.id == record.id }) {
                if record.updatedAtMs >= localMemo.updatedAtMs {
                    tombstones[record.id] = max(tombstones[record.id] ?? 0, record.updatedAtMs)
                    recordsToMerge.append(record)
                }
            } else {
                tombstones[record.id] = max(tombstones[record.id] ?? 0, record.updatedAtMs)
                recordsToMerge.append(record)
            }
            continue
        }

        if let deletedAtMs = tombstones[record.id] {
            if deletedAtMs >= record.updatedAtMs {
                deletedMemosToUpload.append(RemoteDeletedMemo(id: record.id, deletedAtMs: deletedAtMs))
                continue
            }
            tombstones.removeValue(forKey: record.id)
        }

        recordsToMerge.append(record)
    }

    return RemoteMergeOutcome(
        memos: mergeRemoteRecordsIgnoringTombstones(recordsToMerge, into: localMemos),
        deletedMemoTombstones: tombstones,
        deletedMemosToUpload: deletedMemosToUpload
    )
}

private func mergeRemoteRecordsIgnoringTombstones(
    _ remoteRecords: [RemoteMemoRecord],
    into localMemos: [Memo]
) -> [Memo] {
    var merged = localMemos

    for record in remoteRecords {
        if record.deleted {
            if let index = merged.firstIndex(where: { $0.id == record.id }),
               record.updatedAtMs >= merged[index].updatedAtMs {
                merged.remove(at: index)
            }
            continue
        }

        guard let remoteMemo = record.memo else { continue }
        if let index = merged.firstIndex(where: { $0.id == remoteMemo.id }) {
            if remoteMemo.displayOrder > merged[index].displayOrder {
                merged[index].displayOrder = remoteMemo.displayOrder
                merged[index].pinned = remoteMemo.pinned
            }
            if remoteMemo.updatedAtMs >= merged[index].updatedAtMs {
                let displayOrder = max(remoteMemo.displayOrder, merged[index].displayOrder)
                merged[index] = remoteMemo
                merged[index].displayOrder = displayOrder
                merged[index].pinned = remoteMemo.pinned
            }
        } else {
            merged.append(remoteMemo)
        }
    }

    return merged
}
