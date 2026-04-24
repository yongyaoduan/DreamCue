# DreamCue

DreamCue is a local memo reminder App. The business layer is written in `Rust`; the Android shell uses `Kotlin` and `Compose`.

## Features

- Save short memos without rewriting the original text.
- Persist `created_at_ms`, `updated_at_ms`, `last_reviewed_at_ms`, and `cleared_at_ms`.
- Remind the user about active memos at a fixed daily time.
- Move cleared memos to History so they no longer enter daily reminders.
- Record create, edit, keep, clear, and reopen events.
- Search current and historical memos.

## Project Layout

- `crates/dreamcue-core`: Rust core for SQLite storage, event logs, daily review queues, and search.
- `crates/dreamcue-android-ffi`: Rust JNI bridge that returns JSON to Android.
- `android/`: Android App with Compose UI, notification permission handling, alarm scheduling, and boot rescheduling.
- `docs/architecture.md`: Architecture notes.
- `scripts/build-rust-android.sh`: Android native library build script.

## Requirements

Install these tools before building:

1. Rust toolchain: `rustup`
2. Android Studio, Android SDK, Android NDK
3. `cargo-ndk`
4. Android Rust targets:

```bash
rustup target add aarch64-linux-android x86_64-linux-android
```

## Build

1. Build the Rust dynamic library:

```bash
./scripts/build-rust-android.sh
```

2. Open [android/settings.gradle.kts](android/settings.gradle.kts) in Android Studio.

3. Sync Gradle and run `app`.

## Current State

- The Rust core covers memo lifecycle, event logs, daily review queues, and search.
- The Android shell covers the base UI, notification permission request, daily reminder scheduling, and boot rescheduling.
- Search is currently a deterministic hybrid search with exact matching, character n-grams, phrase similarity, and synonym hints.
- Semantic search can be added in `crates/dreamcue-core/src/search.rs` with local embedding or server-side embedding.
