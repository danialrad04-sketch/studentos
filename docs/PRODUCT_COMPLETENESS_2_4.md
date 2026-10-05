# Student OS 2.4 — product completeness audit

Date: 2026-10-05. This audit follows actual UI → ViewModel → repository paths. Historical master-plan checkmarks are not evidence of live service readiness.

| Area | User-visible implementation / corrections | Remaining external verification |
| --- | --- | --- |
| Home / navigation | Olive shell, five primary destinations, grouped hub, actual daily data and useful actions | Physical-device TalkBack / keyboard review |
| Courses / schedule | Multi-session Room records, CRUD, workspace; workspace edit opens full editor instead of clearing sessions; new courses bind to the actual current semester | Real-device rotation / tablet review |
| Tasks | Add, edit, completion, confirmed deletion; edits retain ID, semester and completion; stable course links | Device upgrade / sync scenarios |
| Exams | First-class add/edit/remove screen, valid Persian dates/times, sorted exam list, selectable 15-minute/hour/day reminders and cancellation | Battery-dependent reminder delivery on actual devices |
| Grades | First-grade insertion and upsert; explicit recorded-zero flag; Persian decimal input; hypothetical GPA preview no longer overwrites actual grades | University-specific grading policies are not asserted |
| Attendance | Existing local controls/risk summaries; only active-course rows shown | Campus attendance policy may differ |
| Curriculum | Actual reference catalog and profile entry year replace a fabricated universal chemical-engineering match; explicit correction route | Reference coverage is limited; an explicit personal curriculum import supports other majors with prerequisite validation and full-backup portability |
| Semester planning | Target plan is persisted and displayed after navigation/process restart, distinct from actual university enrollment | Automatic calendar allocation remains future scope; target choice is actually saved |
| Focus / notes | Persisted deadline, pause/resume/reset, duration selection, real course/study context, durable completion notification; saved notes | Device battery policy may defer the background notification |
| Academic intelligence | Existing explainable priorities, risks/workload and navigation retained, scoped to current academic records | Real archived-term GPA trend and comparison added; no invented GPA for missing terms |
| Passport / history | Academic export, semester history and real GPA trend; archive GPA includes midterm and recorded zero; repeated history summaries replace rather than duplicate. Missing values remain unknown in exports; sample credits/IDs, fake verification QR and university approval seals removed | Personal reports and student-provided charts are clearly labelled and never certified as university references |
| Import | Editable preview (name, credits, day/time, instructor/location/exam), validation, overlap acknowledgement; preserve existing profile and semester during additive import | Live image OCR needs configured server AI |
| Account / privacy | Existing guest / Firebase auth / reset / account deletion / backup/export; academic choices and reminders cleared at account/semester boundaries. GPA goals persist separately from actual GPA; failed profile writes have an honest error state | Production identity, Firestore/App Check and deletion verification |
| Cloud / AI / subscription | Existing server-verified Bazaar billing and server-only AI retained; no client-side entitlement grant | Deployment, secrets, SKUs, App Check and a real purchase/restore/refund test remain required |
| Support | Server-owned atomic/idempotent submission, replies and closure; no memory-only fake success; student/staff access enforced | Deploy the four support callables and Firestore rules; supportStaff claims may only be set by a trusted administrator |

## Persistence / release

- Application ID and original production signing certificate stay unchanged.
- Version 2.4.0 / code 2010 exceeds the distributed 2.3.0 test build.
- Room 9 → 10 adds `grades.isRecorded` without destructive migration. Positive legacy grades are marked recorded; old zero placeholders remain unentered.
- New targeted policy, Room/migration and Compose workflow tests cover the corrected paths. CI results must be linked after execution.
- Local Gradle bootstrap is blocked by network access to services.gradle.org; executable Android validation is performed by GitHub Actions.
- The 2.4 test prerelease publisher only runs for the current immutable `main` SHA after all four Android CI jobs and Firebase verification succeed. It reuses those exact signed candidates, verifies identity/certificate and the Bazaar AAB/BIN pair, and publishes test assets without changing the stable release.
- WorkManager reminders survive process recreation/reboot but do not promise exact delivery. See https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work.

This document does not certify live AI, marketplace sales, unsupported curricula, device accessibility or completion of every future master-plan item.
