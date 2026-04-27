# Release Engineering

DreamCue releases are built from GitHub Actions so the Android and macOS artifacts come from the same tagged commit.

## Release Assets

GitHub Releases publish two user-installable files:

- `DreamCue-android-<version>.apk`
- `DreamCue-macos-<version>.zip`

The Android App Bundle is intentionally not attached to GitHub Releases. Play Store delivery is handled separately from the public GitHub release channel.

## Workflow

The release workflow lives at [.github/workflows/release.yml](../.github/workflows/release.yml).

It runs only when a `v*` tag is pushed. Manual workflow dispatch is intentionally disabled so every release is tied to an immutable Git tag.

The workflow has three jobs:

1. `android-release`
   - Installs Java, Gradle, Rust, the Android NDK, and `cargo-ndk`.
   - Writes Firebase and Android signing configuration from GitHub Secrets.
   - Runs Rust tests.
   - Builds the Android native libraries.
   - Runs Android unit tests and Android UI test compilation.
   - Builds a signed release APK.
2. `macos-release`
   - Selects the latest stable Xcode.
   - Runs macOS XCUITest.
   - Imports a Developer ID certificate when signing secrets are configured.
   - Builds a release macOS app.
   - Notarizes and staples the app when Apple notarization secrets are configured.
   - Uploads a zip package.
3. `publish-release`
   - Downloads the Android and macOS artifacts.
   - Publishes the GitHub Release with the APK and macOS zip only.

## Signing Status

The local macOS package is built with Xcode's `Sign to Run Locally` identity. That is suitable for local validation, but it is not a notarized Developer ID distribution.

The GitHub Actions workflow is designed for production signing:

- If `MACOS_CERTIFICATE_P12_BASE64`, `MACOS_CERTIFICATE_PASSWORD`, and `APPLE_TEAM_ID` are configured, the macOS build uses Developer ID signing.
- If `APPLE_ID`, `APPLE_APP_SPECIFIC_PASSWORD`, and `APPLE_TEAM_ID` are also configured, the workflow submits the app for notarization, staples the ticket, validates it, and then creates the final zip.
- If macOS signing secrets are absent, the workflow still builds a local-signing zip for internal validation, but that package should not be treated as a polished public macOS distribution.

## Required GitHub Secrets

Android release signing:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Android Firebase configuration:

- `FIREBASE_PROJECT_ID`
- `FIREBASE_APPLICATION_ID`
- `FIREBASE_API_KEY`
- `FIREBASE_DATABASE_URL`

macOS Developer ID signing:

- `MACOS_CERTIFICATE_P12_BASE64`
- `MACOS_CERTIFICATE_PASSWORD`
- `MACOS_CODE_SIGN_IDENTITY`
- `APPLE_TEAM_ID`

macOS notarization:

- `APPLE_ID`
- `APPLE_APP_SPECIFIC_PASSWORD`
- `APPLE_TEAM_ID`

## Secret Preparation

Encode the Android keystore:

```bash
base64 -i android/keystore/release.jks | pbcopy
```

Encode the macOS Developer ID certificate:

```bash
base64 -i DeveloperIDApplication.p12 | pbcopy
```

Keep keystores, `.p12` files, Firebase local properties, and admin SDK keys out of Git. The repository already ignores `android/keystore/`, `android/firebase.local.properties`, `secrets/`, and local macOS Firebase override files.

## Local Release Build

Build the macOS release zip locally:

```bash
DREAMCUE_RELEASE_VERSION=v1.2.8 ./scripts/build-macos-release.sh
```

Build the Android release APK locally:

```bash
./scripts/build-rust-android.sh
gradle -p android :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:assembleRelease
```

## Release Checklist

Before pushing a release tag:

- Confirm `android/app/build.gradle.kts` has the intended `versionName` and `versionCode`.
- Confirm Firebase Auth email/password sign-in is enabled.
- Confirm Realtime Database rules scope data to `users/{uid}`.
- Run Android unit tests and Android UI test compilation.
- Run Android connected tests on an emulator before shipping behavioral changes.
- Run macOS XCUITest.
- Run the explicit Firebase sync XCUITest when sync code changes.
- Confirm the GitHub Release contains the APK and macOS zip, not the AAB.
