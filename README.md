# DreamCue

DreamCue is a memo reminder App with local-first storage and optional Firebase sync. The shared Android business layer is written in `Rust`; the Android shell uses `Kotlin` and `Compose`. The macOS app is a native `SwiftUI` client.

## Features

- Save short memos without rewriting the original text.
- Persist `created_at_ms`, `updated_at_ms`, `last_reviewed_at_ms`, and `cleared_at_ms`.
- Remind the user about active memos at a fixed daily time.
- Move cleared memos to History so they no longer enter daily reminders.
- Record create, edit, keep, clear, and reopen events.
- Search current and historical memos.
- Sync memos across Android and macOS through Firebase Auth and Realtime Database.
- Keep remote data tenant-scoped under `users/{uid}/memos`.

## Project Layout

- `crates/dreamcue-core`: Rust core for SQLite storage, event logs, daily review queues, and search.
- `crates/dreamcue-android-ffi`: Rust JNI bridge that returns JSON to Android.
- `android/`: Android App with Compose UI, notification permission handling, alarm scheduling, and boot rescheduling.
- `macos/`: macOS SwiftUI App with local JSON storage, Firebase Auth REST login, and Realtime Database REST sync polling.
- `docs/architecture.md`: Architecture notes.
- `scripts/build-rust-android.sh`: Android native library build script.

## Requirements

Install these tools before building:

1. Rust toolchain: `rustup`
2. Android Studio, Android SDK, Android NDK
3. `cargo-ndk`
4. Xcode for the macOS App
5. Android Rust targets:

```bash
rustup target add aarch64-linux-android x86_64-linux-android
```

## Android Build

1. Build the Rust dynamic library:

```bash
./scripts/build-rust-android.sh
```

2. Open [android/settings.gradle.kts](android/settings.gradle.kts) in Android Studio.

3. Sync Gradle and run `app`.

## macOS Build

Build the macOS App from the repository root:

```bash
xcodebuild -project macos/DreamCueMac.xcodeproj -scheme DreamCueMac -configuration Debug -destination 'platform=macOS' build
```

## Firebase Setup

Android reads Firebase configuration from these string resources:

- `firebase_project_id`
- `firebase_application_id`
- `firebase_api_key`
- `firebase_database_url`

macOS reads Firebase configuration from `macos/DreamCueMac/FirebaseConfig.plist`:

- `ProjectID`
- `APIKey`
- `DatabaseURL`

Both clients require Firebase Auth email/password sign-in. All synced memos are written under `users/{uid}/memos`, where `uid` is the authenticated Firebase user ID.

Use Realtime Database rules equivalent to [database.rules.json](database.rules.json).

```text
users/$uid: auth != null && auth.uid === $uid
```

## Test

Run the Rust and Android checks:

```bash
cargo test --workspace
gradle -p android :app:testDebugUnitTest :app:assembleDebug :app:assembleDebugAndroidTest
```

Build the macOS App and XCUITest bundle:

```bash
xcodebuild -project macos/DreamCueMac.xcodeproj -scheme DreamCueMac -configuration Debug -destination 'platform=macOS' build-for-testing
```

## Current State

- The Rust core covers memo lifecycle, event logs, daily review queues, and search.
- The Android shell covers the base UI, notification permission request, daily reminder scheduling, boot rescheduling, Firebase Auth, and Realtime Database sync.
- The macOS shell covers local memo CRUD, search, Firebase Auth REST login, and Realtime Database REST sync polling.
- Search is currently a deterministic hybrid search with exact matching, character n-grams, phrase similarity, and synonym hints.
- Semantic search can be added in `crates/dreamcue-core/src/search.rs` with local embedding or server-side embedding.
