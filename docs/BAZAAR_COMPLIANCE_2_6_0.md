# Student OS 2.6.0 / 2014 — development candidate

Package and production signing identity are unchanged. The released version reported by the owner is 2.4.2 / 2012; its exact published binary source remains unconfirmed. This candidate has not been submitted or published.

The weekly agenda combines actual tasks and exams with the current semester’s registered class pattern, labels known time conflicts and exports one week as an ICS file. Class occurrences are projections, not a confirmed holiday or term calendar. The course notebook supports search, edits, archive/restore and portable backups. Room 11→12 preserves earlier notes. See STUDENT_WORKSPACE_2_6.md for behavior and limitations.

Account entry and Bazaar billing remain disabled in the packaged public build. Core planning, notes, tasks, timers, local records and backup work without a server. The assistant’s on-device analysis is available offline; online model replies still require the owner’s one-time server setup described in AI_SETUP_FA.md. An APK build does not activate a live provider.

No calendar permission is needed for ICS export; the student saves and imports the file manually. No exact-alarm permission is needed for task reminders. Optional notifications require Android permission and the in-app notification setting; WorkManager delivery may be deferred by Android.

CI verifies unit/UI tests, both lint variants, backend/PostgreSQL tests, emulator instrumentation, the production signature, packaged policy, 16 KB ZIP/ELF alignment, and a signed update from the selected 2012 source baseline. Consult the exact workflow run and release-evidence.json for this candidate’s commit, version and hash. Artifacts alone do not prove all gates passed. Existing privacy, license and data deletion paths are retained.
