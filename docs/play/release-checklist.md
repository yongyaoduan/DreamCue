# DreamCue Release Checklist

## Local Files

- Android App Bundle: `dist/DreamCue-release.aab`
- Release APK: `dist/DreamCue-release.apk`
- Store listing copy: `docs/play/store-listing.md`
- Privacy policy: `docs/play/privacy-policy.md`
- Data safety notes: `docs/play/data-safety.md`
- Phone screenshots: `dist/play/`
- 512 px icon: `dist/play/icon-512.png`
- Feature graphic: `dist/play/feature-graphic.png`

## Play Console

1. Create an app with package name `app.dreamcue`.
2. Fill App content:
   - Privacy policy
   - Data safety
   - Content rating
   - Ads declaration: `No`
   - Target audience
3. Fill Store listing:
   - App name: `DreamCue`
   - Short description
   - Full description
   - App icon
   - Feature graphic
   - At least `2` phone screenshots
4. Upload `dist/DreamCue-release.aab` to Production.
5. Set pricing and distribution before the first production release.

## Suggested Store Settings

- Category: `Productivity`
- Ads: `No`
- Target users: general adult users

## Permissions

- `POST_NOTIFICATIONS`: sends the daily reminder notification.
- `SCHEDULE_EXACT_ALARM`: schedules the daily reminder at a fixed time.
- `RECEIVE_BOOT_COMPLETED`: rebuilds reminders after device restart.
- `INTERNET`: signs in to Firebase and syncs memo data.

## Firebase

- Enable Firebase Auth email/password sign-in.
- Create Realtime Database.
- Configure Android `firebase_project_id`, `firebase_application_id`, `firebase_api_key`, and `firebase_database_url`.
- Configure macOS `macos/DreamCueMac/FirebaseConfig.plist`.
- Apply Realtime Database rules that restrict memo access to `users/{uid}/memos`.

## Backup Policy

`android:allowBackup` is set to `false` in `android/app/src/main/AndroidManifest.xml`.

This prevents Android auto backup from copying local memo data outside the App-controlled sync path.

## Pre-Review Checks

- The App launches without crashing.
- First launch requests notification and alarm permissions.
- Today, Archive, Rhythm, and Account tabs open correctly.
- A cleared memo moves to History.
- Archive search updates only after tapping the search action.
- Edited memos move to the top of the current list.
- Sync sign-in controls appear in Settings.
- Two signed-in devices use the same Firebase Auth account before sync testing.
- The privacy policy is hosted at a public URL.

## External Tasks

- Finish Google Play developer identity and merchant setup.
- Host `docs/play/privacy-policy.html` at a public URL.
- Confirm the final price by country or region.
- Replace draft screenshots if final store screenshots are different.
