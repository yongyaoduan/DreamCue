# DreamCue Store Listing

## App Name

`DreamCue`

## Short Description

Write a short memo, review it daily, clear it when done, or keep it active.

## Full Description

DreamCue is a local memo reminder App for short notes that should not be forgotten.

It is designed for one sentence at a time. Add a memo, keep the original text, and review it later. Each memo records `created_at_ms`, `updated_at_ms`, and `cleared_at_ms`.

At the reminder time, active memos appear again. If a memo is done, clear it. If it still matters, keep it active for the next review.

Main features:

- Save short memos locally in `memos`
- Review active memos every day with `last_reviewed_at_ms`
- Move cleared memos to History with `cleared_at_ms`
- Reopen a cleared memo by setting status to `active`
- Search current and historical memos through `dreamcue-core`
- Delete a memo and its event history from `memo_events`
- Keep memo data on the device in `dreamcue.sqlite3`

Good use cases:

- A sentence you want to remember tomorrow
- A small task that needs daily review
- A thought that should stay visible until it is handled

DreamCue uses `POST_NOTIFICATIONS` for reminders and `SCHEDULE_EXACT_ALARM` for fixed-time scheduling. Privacy policy text is maintained in `docs/play/privacy-policy.md`.

## Keywords

- memo
- reminder
- daily reminder
- note
- local notes
- history
- short memo
