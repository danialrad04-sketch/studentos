# Student planner 2.5.0 / 2013

## Baseline provenance

The owner confirms Cafe Bazaar approved and published 2.4.2 / 2012, but could not identify the uploaded binary ("probably the last PR"). Development therefore starts at PR #21 source `cc05e963fc9ec3b4a99499c20cf3c36a286b79a4`. This is a selected source baseline, not proof of the exact store APK. No 2.4.2 public GitHub tag existed when this work began.

The signed 2012 artifact at run 39550667751 / artifact 12323339013 was inspected locally: package `com.aistudio.studentos.appvzk`, version 2012 / 2.4.2, certificate SHA-256 `a335e71031ec78a7961656b2c36d8c53abcb3bdc28e265998cf6053fb94eb950`, APK SHA-256 `7aa685b93b60d962b8b8b8275d674243db954a7787b3317726c47472b8be08cb`. CI rebuilds the pinned source baseline with the same production identity rather than depending on an expiring artifact. It does not claim byte-for-byte identity with the unidentified store binary.

## Implemented contracts

- Tasks store repeat interval (0/1/7/14/30 days), optional Tehran reminder time, and parent occurrence ID in Room schema 11. Migration 10→11 adds defaults without resetting IDs, completion, courses or semesters.
- Completion reads the current database row and atomically creates at most one child. Overdue repeats skip missed dates while retaining cadence; 30 days means 30 days, not a calendar month. Undo/recomplete retains the existing child. Deleting an occurrence does not delete other occurrences. Archived work cannot spawn a child.
- WorkManager persists reminders across process death/reboot. Unchanged pending work is retained, changed/deleted/completed/inactive reminders are cancelled, and past times are not newly queued. At delivery the worker reads the latest task and verifies its date/time, active semester/course, notification setting and account generation. Clearing academic/account state changes the generation so reused task IDs cannot receive old-account reminders. Notification taps open Tasks through the existing app gate.
- Backup and Node payloads round-trip planning fields; legacy backups default to no repetition/reminder. Invalid planning and duplicate occurrence parents are rejected before task replacement. Firestore writes include stable course/semester relationships and planning metadata; older documents preserve local planning fields when absent.
- Gamification uses actual saved StudyDay minutes and dates. Current streak includes yesterday when today has no completed session; best streak excludes invalid/future rows. Unrecorded grades are excluded from GPA badges. UI explains that history retains up to 90 recorded study days; these metrics are not lifetime/semester totals.

## Verification and limits

Meaningful regressions cover cadence/Noruz, atomic recurrence and undo, legacy backups, rejected restores, schema-10 upgrade through Room validation, reminder invalidation/delivery and pending-work reconciliation, study streaks/date boundaries, and light/dark RTL forms with large text. Consult CI reports for actual results on the exact candidate commit.

Local live AI-provider tests require configured credentials; mock/emulator tests do not prove live-provider availability. OS notification permission and battery scheduling affect delivery. The candidate does not publish a store or GitHub release, change authentication providers, or replace the signing identity.
