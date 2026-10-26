# Student OS 2.5.0 / 2013 — development candidate

Package and production signing identity are unchanged. The released version reported by the owner is 2.4.2 / 2012. This candidate has not been submitted or published.

The existing offline access policy remains: account entry and Bazaar billing are disabled in the packaged public build. Core planning, tasks, timers, local records and backup do not require a server. No exact-alarm permission is needed for task reminders. Optional notifications require Android permission and the in-app notification setting; WorkManager delivery may be deferred by Android.

CI must verify unit/UI tests, both lint variants, backend/PostgreSQL tests, emulator instrumentation, production signature, packaged policy, 16 KB ZIP/ELF alignment, and a real signed update from the selected 2012 source baseline. Artifacts alone are not evidence that these checks passed; consult the exact workflow run for the candidate commit.

See `STUDENT_PLANNER_2_5.md` for baseline provenance and behavior. Existing privacy, license and data deletion paths are retained.
