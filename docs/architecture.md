# Architecture

## Goal

DreamCue handles one main flow: write a short memo, review it every day, then decide whether it should stay active or move to history.

The Android project is split into four layers:

1. `Rust core`
2. `JNI bridge`
3. `Android shell`
4. `Firebase sync`

The macOS project is a native `SwiftUI` shell with local JSON storage and Firebase REST sync.

## Rust Core

Path: [crates/dreamcue-core/src/lib.rs](../crates/dreamcue-core/src/lib.rs)

Responsibilities:

- Create memos
- Edit memos
- Clear memos
- Reopen cleared memos
- Mark active memos as kept
- Generate the daily review queue
- Write memo events
- Search memos
- Apply remote memo changes
- Delete remote memo tombstones

### Tables

`memos`

- `id`
- `content`
- `status`
- `created_at_ms`
- `updated_at_ms`
- `cleared_at_ms`
- `reminder_count`
- `last_reviewed_at_ms`

`memo_events`

- `id`
- `memo_id`
- `event_type`
- `content_snapshot`
- `note`
- `created_at_ms`

### Daily Review Rules

- Only `active` memos can enter the daily queue.
- A memo enters the queue when `last_reviewed_at_ms` is empty or belongs to an earlier day.
- Choosing keep writes a `kept` event and updates `last_reviewed_at_ms`.
- Choosing clear writes a `cleared` event and sets `cleared_at_ms`.

## Search

Path: [crates/dreamcue-core/src/search.rs](../crates/dreamcue-core/src/search.rs)

The current search engine is local and deterministic. It combines:

- Exact substring match
- Token overlap
- Character n-gram match
- Synonym hints
- Subsequence match

This is enough for short memo text. Semantic search can replace this file later with local embedding or server-side embedding.

## JNI Bridge

Path: [crates/dreamcue-android-ffi/src/lib.rs](../crates/dreamcue-android-ffi/src/lib.rs)

The Android layer calls Rust through JNI. Rust returns a JSON envelope, and Kotlin parses that response without duplicating business rules.

## Android Shell

Key files:

- [android/app/src/main/java/app/dreamcue/MainActivity.kt](../android/app/src/main/java/app/dreamcue/MainActivity.kt)
- [android/app/src/main/java/app/dreamcue/ui/DreamCueApp.kt](../android/app/src/main/java/app/dreamcue/ui/DreamCueApp.kt)
- [android/app/src/main/java/app/dreamcue/worker/ReminderScheduler.kt](../android/app/src/main/java/app/dreamcue/worker/ReminderScheduler.kt)
- [android/app/src/main/java/app/dreamcue/worker/DailyReviewReceiver.kt](../android/app/src/main/java/app/dreamcue/worker/DailyReviewReceiver.kt)

Responsibilities:

- Compose UI
- Notification permission request
- Daily reminder scheduling
- Reminder rebuild after device boot
- Notification tap handling
- Firebase Auth email/password sign-in
- Realtime Database listener
- Realtime Database upload after local mutations

## Firebase Sync

Key files:

- [android/app/src/main/java/app/dreamcue/sync/FirebaseSyncCoordinator.kt](../android/app/src/main/java/app/dreamcue/sync/FirebaseSyncCoordinator.kt)
- [android/app/src/main/java/app/dreamcue/sync/FirebaseTenantPaths.kt](../android/app/src/main/java/app/dreamcue/sync/FirebaseTenantPaths.kt)
- [android/app/src/main/java/app/dreamcue/sync/RemoteMemoDocument.kt](../android/app/src/main/java/app/dreamcue/sync/RemoteMemoDocument.kt)
- [macos/DreamCueMac/FirebaseRestSyncService.swift](../macos/DreamCueMac/FirebaseRestSyncService.swift)

Tenant isolation is based on Firebase Auth user IDs. Remote memo documents live under:

```text
users/{uid}/memos/{memoId}
```

The Android client uses a Realtime Database listener for realtime updates. The macOS client uses Firebase Auth and Realtime Database REST endpoints with periodic polling.

The client-side conflict rule is last-write-wins on `updated_at_ms`. Deleted memos are sent as Realtime Database tombstones with `deleted = true` so other devices can remove the local copy.

## macOS Shell

Key files:

- [macos/DreamCueMac/ContentView.swift](../macos/DreamCueMac/ContentView.swift)
- [macos/DreamCueMac/MemoStore.swift](../macos/DreamCueMac/MemoStore.swift)
- [macos/DreamCueMac/FirebaseRestSyncService.swift](../macos/DreamCueMac/FirebaseRestSyncService.swift)

Responsibilities:

- SwiftUI navigation
- Local memo CRUD
- Local search
- Firebase Auth REST sign-in and account creation
- Realtime Database REST upload, deletion tombstones, and polling

## Limits

1. Search is not embedding-based semantic search yet.
2. Notifications open the Android app for review. Pinned due cues receive individual notifications, while regular due cues are represented by one summary notification.
3. macOS sync uses polling instead of a Realtime Database stream because the macOS client avoids bundling the Firebase Apple SDK in this repository version.
