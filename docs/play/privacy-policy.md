# DreamCue Privacy Policy

Effective date: `2026-04-24`

DreamCue is a memo reminder App with local storage and optional Firebase sync.

## 1. Data Processed by the App

DreamCue stores the following data on the user's device:

- Memo text entered by the user
- Created, updated, reviewed, and cleared timestamps
- Reminder status
- Local event history

When sync is enabled, DreamCue also processes:

- Firebase Auth account email
- Synced memo content and timestamps
- Deletion markers used to remove memos from other signed-in devices

## 2. Where Data Is Stored

The Android version stores memo data locally on the device in `dreamcue.sqlite3`.

The macOS version stores memo data locally in the user's Application Support directory.

If the user signs in, memo data is synced to Firebase Firestore under the authenticated Firebase user ID:

```text
users/{uid}/memos/{memoId}
```

## 3. Permissions

DreamCue may request these Android permissions:

- `POST_NOTIFICATIONS`: sends daily reminder notifications.
- `SCHEDULE_EXACT_ALARM`: schedules daily reminders at a fixed time.
- `RECEIVE_BOOT_COMPLETED`: restores reminders after device restart.
- `INTERNET`: signs in to Firebase and syncs memo data.

## 4. User Controls

Users can:

- Edit any memo
- Clear a memo into History
- Reopen a historical memo
- Permanently delete a memo and its related event history
- Sign out of the sync account

## 5. Data Sharing

DreamCue does not sell or rent memo data. When sync is enabled, memo data is sent to Firebase services for authentication and cross-device sync.

## 6. Children's Privacy

DreamCue is not designed specifically for children.

## 7. Policy Updates

This policy must be updated before release if DreamCue adds analytics, crash reporting, online AI search, new account providers, or any other remote service.

## 8. Contact

Replace this section with the developer support email or public website before publishing.
