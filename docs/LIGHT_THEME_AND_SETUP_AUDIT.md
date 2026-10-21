# Light theme and repeatable setup audit

Date: 2026-10-21. Scope: production navigation and callbacks, setup/skip/revisit,
Room persistence, settings, and light/dark rendering. This complements
`PRODUCT_COMPLETENESS_2_4.md`; it does not certify live external services.

## Fixed in this pass

- Setup completion flags and navigation now follow successful Room writes. Failed
  saves keep the wizard open, report an error, and permit retry. Duplicate submit
  is blocked while saving; cancellation is propagated.
- Production quick setup is additive. Existing course IDs, grades, absences,
  exams, class sessions, notes and academic history survive revisiting setup.
  Previously the repository cleared current academic tables. Summary fields are
  prefilled; revisiting does not automatically select an entire term's courses.
- Text import from the wizard preserves records. Importing the same course in the
  same semester again reuses its identity by course code/name, retaining grades
  and absences while filling provided metadata and missing class sessions. The
  same session is not added twice. Missing fields preserve previous values; the
  course editor supports replacing/removing obsolete details.
- Missing class times remain missing; import no longer fabricates weekdays and
  08:00/10:00 sessions. The schedule lists unscheduled courses with working edit
  actions, so they remain reachable after quick setup or partial import.
- Persian/Arabic digits and the Persian decimal separator are accepted in setup
  summaries. Invalid values disable continuation instead of silently becoming
  zero. An omitted GPA stays null, distinct from a recorded zero.
- The profile editor can update/clear the declared cumulative GPA independently
  of recorded course grades and target GPA. Repository validation rejects
  nonfinite/out-of-range GPA and negative unit counts. Profile editing refreshes
  curriculum reference IDs and closes after successful persistence.
  Setup/skip/import also clear stale reference IDs when academic identity changes;
  a major from another university is never substituted for an unknown university.
- Attendance changes read the latest stored record inside a Room transaction,
  preserve custom limits and avoid lost increments from stale UI state. Each
  course exposes a validated 1–16 session limit; backup already includes this
  entity field. Limits are user supplied, not a university regulation lookup.
- Home retains declared units/GPA when no curriculum match exists and provides a
  direct curriculum completion action without inventing a progress percentage.
- Light surfaces use neutral layered backgrounds and visible boundaries. Settings
  groups have an explicit border; grades, passport and assistant status badges
  use theme-aware foreground colors. Onboarding selections use onPrimary text,
  including in dark mode, and unselected option icons remain visible.

## Capability trace

| Destination / area | Production behavior and storage | Boundary / recovery |
| --- | --- | --- |
| Home | Derived priority, workload, progress and focus statistics | Missing curriculum has a completion action; no assumed degree total |
| Schedule | Course/session CRUD through VM and Room, conflict calculation | Unscheduled courses can be edited directly |
| Tasks | Add/edit/complete/delete, stable IDs and due dates | Empty state opens creation; completion persists |
| Exams | Course-backed exam editing and scheduled/cancelled reminders | Requires course and valid date/time; OS notification permission matters |
| Grades | Recorded scores, weighted GPA, editable grades | Declared cumulative GPA and target GPA are separate settings |
| Attendance | Persisted count and per-course limit, risk alerts | Custom limit is editable and retained on count changes |
| Focus | Durable timer, completion history, daily goals, notes | OS may delay background execution; minutes are credited to completion date |
| Passport | Progress/confidence derived from curriculum and actual history | Partial history is labelled; declared summaries are not fabricated attempts |
| Curriculum | Reference matching and personal curriculum import | Coverage depends on catalog; personal import is the recovery path |
| Semester planner | Prerequisite/workload engine and selected plan persistence | Needs curriculum/history; suggestions are not university enrollment |
| Academic intelligence | Deterministic risk, workload and academic analysis | Reliability depends on entered records |
| Achievements | Derived statistics and milestones | No fictitious completion records added |
| History | Semester/archive and past attempt persistence | User supplies missing historical records |
| Assistant | Local academic answers/actions; online provider integration | Online AI requires Firebase, App Check and service configuration |
| Settings / account | Persistent theme, notifications, focus/GPA goals, profile, file backup/restore | Online login and Bazaar billing remain disabled by existing release policy |

## Verification

Regression cases cover repeated setup/import with existing grades and absences,
missing schedule preservation, invalid GPA, attendance limit edits/increments,
Persian numeric input in the wizard, and recovery of unscheduled courses.
The destination smoke suite is also exercised in light mode, with populated
schedule/task/grade/attendance fixtures, alongside dark mode with large text.
Screenshots are emitted under `app/build/outputs/visual-review/` using
`-Proborazzi.test.record=true`. Exact test/build results belong to the PR checks;
source inspection alone is not a passing test result. Real-device behavior and
live AI/auth/OCR/billing are not certified by these local/emulator tests.
