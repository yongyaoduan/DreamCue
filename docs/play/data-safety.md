# Data Safety Notes

These notes match the current codebase:

- `android/app/src/main/AndroidManifest.xml` does not declare `android.permission.INTERNET`.
- Memo data is stored in the local SQLite file `dreamcue.sqlite3`.
- `android:allowBackup` is set to `false` in `AndroidManifest.xml`.
- There is no account login code under `android/app/src/main/java/app/dreamcue`.
- There is no analytics SDK, ads SDK, cloud sync, or remote upload dependency in `android/app/build.gradle.kts`.

## Suggested Play Console Answer

### Does the App collect or share user data?

Recommended answer for this version: `No`.

Reasoning:

- Memo content, timestamps, and event logs stay in `dreamcue.sqlite3` on the user's device.
- The code does not call any HTTP client, REST API, or upload endpoint.
- The App has no ads, analytics, cloud sync, or account login flow in `android/app/build.gradle.kts`.

## Recheck Triggers

Update this file and the Play Console form before release if the App adds:

- Cloud sync through a remote API
- Crash reporting such as `firebase-crashlytics`
- Analytics such as `firebase-analytics`
- Account login through Google, email, or OAuth
- Remote backup to a server or cloud drive
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

## Deletion

- Users can clear a memo into History through `clear_memo`.
- Users can permanently delete a memo and its related event history through `delete_memo`.
