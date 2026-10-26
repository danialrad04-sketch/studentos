# Student OS 2.7.0 / 2016 — development candidate

Package and production signing identity are unchanged. The released version reported by the owner is 2.4.2 / 2012; its exact published binary source remains unconfirmed. This candidate has not been submitted or published.

The weekly agenda combines actual tasks and exams with the current semester’s registered class pattern, labels known time conflicts and exports one week as an ICS file. Class occurrences are projections, not a confirmed holiday or term calendar. The course notebook supports search, edits, archive/restore and portable backups. Room 11→12 preserves earlier notes. See STUDENT_WORKSPACE_2_6.md for behavior and limitations.

Account entry and Bazaar billing remain disabled in the packaged public build. Core planning, notes, tasks, timers, local records and backup work without a server. The assistant’s on-device analysis is available offline; online model replies still require the owner’s one-time server setup described in AI_SETUP_FA.md. An APK build does not activate a live provider.

No calendar permission is needed for ICS export; the student saves and imports the file manually. No exact-alarm permission is needed for task reminders. Optional notifications require Android permission and the in-app notification setting; WorkManager delivery may be deferred by Android.

CI verifies unit/UI tests, both lint variants, backend/PostgreSQL tests, emulator instrumentation, the production signature, packaged policy, 16 KB ZIP/ELF alignment, and a signed update from the selected 2012 source baseline. Consult the exact workflow run and release-evidence.json for this candidate’s commit, version and hash. Artifacts alone do not prove all gates passed. Existing privacy, license and data deletion paths are retained.

This candidate extends the original 18-sticker atlas with six companion illustrations and applies their paper palette to navigation and home tools. It adds an instructor-weighted exam score calculator and weighted GPA goal planning with unreachable-target detection. Existing data schema and backend contracts remain unchanged. Owner AI setup is documented in AI_OWNER_QUICKSTART_FA.md.


در ۲.۷.۰ پالت کاغذیِ زیتونی، نفتی، آجری و خردلی وارد خانه و تم برنامه شده است. نوار پایین، نوار تبلت، منوی کناری، راهنمای آغاز و نشان‌های دستاورد از یک خانوادهٔ ۲۴تایی استیکر استفاده می‌کنند. برنامهٔ هدف معدل وزن واحدها و نمره‌های ثبت‌شده را لحاظ می‌کند و ناممکن‌بودن هدف را نشان می‌دهد. محاسبه‌گر نمرهٔ امتحان با بارم دلخواه استاد و ورودی فارسی، بدون تغییر سوابق کار می‌کند. نسخهٔ پایگاه داده همچنان ۱۲ است.
