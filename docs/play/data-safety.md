# Data Safety Notes

These notes match the current codebase:

- `android/app/src/main/AndroidManifest.xml` declares `android.permission.INTERNET` for Firebase sync.
- Memo data is stored locally in `dreamcue.sqlite3` and can sync to Firestore after sign-in.
- `android:allowBackup` is set to `false` in `AndroidManifest.xml`.
- Firebase Auth email/password login is implemented under `android/app/src/main/java/app/dreamcue/sync`.
- There is no analytics SDK, ads SDK, or crash reporting dependency in `android/app/build.gradle.kts`.

## Suggested Play Console Answer

### Does the App collect or share user data?

Recommended answer for this version: `Yes`.

Reasoning:

- Memo content and timestamps can be uploaded to Firestore after the user signs in.
- Firebase Auth processes the user's email/password credentials for account login.
- Remote memo documents are scoped to `users/{uid}/memos`.
- The App has no ads, analytics SDK, or crash reporting SDK.

## Recheck Triggers

Update this file and the Play Console form before release if the App adds:

- Crash reporting such as `firebase-crashlytics`
- Analytics such as `firebase-analytics`
- Account login through Google or OAuth
- Online AI search using a remote embedding service
- Android auto backup by changing `android:allowBackup` to `true`

## Local Data

The App stores:

- `content`
- `created_at_ms`
- `updated_at_ms`
- `cleared_at_ms`
- `status`
- `reminder_count`
- `memo_events`

The data is used for:

- Local display in `DreamCueApp.kt`
- Local search ranking in `crates/dreamcue-core/src/search.rs`
- Local notification reminders through `NotificationHelper.kt`
- Cross-device sync through Firebase Auth and Firestore

## Remote Data

The App can upload:

- Memo content
- Memo status
- Created, updated, reviewed, and cleared timestamps
- Reminder counters
- Deletion tombstones

Remote memo data is stored under the authenticated user's Firebase UID:

```text
users/{uid}/memos/{memoId}
```

## Deletion

- Users can clear a memo into History through `clear_memo`.
- Users can permanently delete a memo and its related event history through `delete_memo`.
- Remote deletion is propagated through a Firestore tombstone document with `deleted = true`.
