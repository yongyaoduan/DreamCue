# Architecture

## Goal

DreamCue handles one main flow: write a short memo, review it every day, then decide whether it should stay active or move to history.

The project is split into three layers:

1. `Rust core`
2. `JNI bridge`
3. `Android shell`

## Rust Core

Path: [crates/memo-core/src/lib.rs](../crates/memo-core/src/lib.rs)

Responsibilities:

- Create memos
- Edit memos
- Clear memos
- Reopen cleared memos
- Mark active memos as kept
- Generate the daily review queue
- Write memo events
- Search memos

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

Path: [crates/memo-core/src/search.rs](../crates/memo-core/src/search.rs)

The current search engine is local and deterministic. It combines:

- Exact substring match
- Token overlap
- Character n-gram match
- Synonym hints
- Subsequence match

This is enough for short memo text. Semantic search can replace this file later with local embedding or server-side embedding.

## JNI Bridge

Path: [crates/memo-android-ffi/src/lib.rs](../crates/memo-android-ffi/src/lib.rs)

The Android layer calls Rust through JNI. Rust returns a JSON envelope, and Kotlin parses that response without duplicating business rules.

## Android Shell

Key files:

- [android/app/src/main/java/com/example/memolog/MainActivity.kt](../android/app/src/main/java/com/example/memolog/MainActivity.kt)
- [android/app/src/main/java/com/example/memolog/ui/MemoApp.kt](../android/app/src/main/java/com/example/memolog/ui/MemoApp.kt)
- [android/app/src/main/java/com/example/memolog/worker/ReminderScheduler.kt](../android/app/src/main/java/com/example/memolog/worker/ReminderScheduler.kt)
- [android/app/src/main/java/com/example/memolog/worker/DailyReviewReceiver.kt](../android/app/src/main/java/com/example/memolog/worker/DailyReviewReceiver.kt)

Responsibilities:

- Compose UI
- Notification permission request
- Daily reminder scheduling
- Reminder rebuild after device boot
- Notification tap handling

## Limits

1. Search is not embedding-based semantic search yet.
2. Notifications open the App for review; they do not yet provide per-memo actions inside the notification.
