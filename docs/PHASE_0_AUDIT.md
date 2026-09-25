# Student OS — Phase 0 Repository Audit

Branch audited: `feat/academic-premium-design-system`

## Current-state map

### Architecture
- Kotlin Android application using Jetpack Compose + Material 3.
- MVVM-style UI/ViewModel layer with repository/domain engines.
- Room remains the local source of truth for academic data.
- Firebase Authentication/Firestore and a self-hosted Node.js/PostgreSQL backend both exist.
- Backend synchronization uses per-data-type timestamps and Last Write Wins.

### Core academic systems
Verified in the repository:
- Dashboard
- Weekly schedule
- Courses / Course Workspace
- Exams
- Tasks
- Grades
- Attendance
- Focus / Pomodoro
- Curriculum
- Academic Passport
- Academic Intelligence
- Copilot
- Search / Command Center
- Semester planner/history
- Backup/restore

### Domain engines
Existing engines include:
- AcademicCopilotEngine
- AcademicGamificationEngine
- ConflictDetectionEngine
- CourseIdentityNormalizer
- CourseStateResolver
- CurriculumMatcher
- CurriculumResolver
- PrerequisiteEngine
- AcademicRiskEngine
- WorkloadEngine
- SemesterPlannerEngine
- StudyPlannerEngine

Existing parsing/date systems include RegistrationTextParser and DateTimeNormalizer.

## What already satisfies the Master Plan

### Design system foundation
- Academic Premium brand tokens exist.
- Petrol/Navy + Olive brand direction is now represented by the primary theme.
- Shared spacing/shape tokens exist.
- Shared academic UI primitives exist.
- RTL is provided centrally by the theme.

### App shell
- Compact phone navigation exists.
- Adaptive navigation rail exists for non-compact widths.
- Shared module hub now exposes the same secondary destinations from both shells.
- Main content uses adaptive width/padding metrics.

### Intelligence
- Academic Intelligence screen exists.
- Deterministic academic risk/workload/planning engines exist.
- Copilot action confirmation is represented in the existing command/action layer.
- Omnibox has deterministic search/command routing.

### Account / privacy
- Layered Account Center separates Identity, Security and Data surfaces.
- Local logout/data purge no longer injects demo records.
- Firestore account-data deletion exists for Firebase-backed users.
- Backend-authenticated account deletion endpoint and Android client contract are now implemented.

### Quality infrastructure
- Unit-test suite is present.
- Compose/UI-related test infrastructure is present.
- Room persistence/data-integrity tests are present.
- Android CI now executes unit tests, lint, and debug build.
- Backend CI performs JavaScript syntax verification.
- Release workflow supports signed APK/AAB publication.

## Data-integrity hardening completed
- Quick Setup no longer synthesizes historical passed-course attempts, class schedules, professor names, exam dates or grades.
- Saving a new course no longer creates a zero-valued grade that could be mistaken for a real failing grade.
- Saving a course without provided sessions no longer creates a default 08:00–10:00 session.
- Backend sync course pulls no longer clear unrelated attendance/grade/exam/task data.

## Additional correctness hardening
- `clearToFreshSlate()` no longer has fabricated student identity defaults, and Copilot course enrollment no longer invents a class location.
- Grade calculations now use the repository's 20-point contract: 6-point midterm + 14-point final; Copilot, Gamification and the grade simulator are aligned.
- Backend-authenticated users no longer receive an implicit Pro entitlement; paid capabilities require a server-authoritative entitlement source.

## Remaining gaps

### Phase 1 — Design system adoption
- Some legacy compatibility tokens still exist for source compatibility.
- Full screen-by-screen migration to shared primitives is not yet complete.
- Accessibility verification on physical devices is not yet completed.

### Phase 2 — App shell
- Adaptive navigation foundation is implemented.
- Physical-device review of compact/medium/expanded behavior remains.
- Deep-link and all-destination navigation regression still needs execution.

### Phase 3/4 — Core and productivity
- Existing features are largely present, but release-grade regression across dashboard, schedule, courses, exams, tasks, grades, attendance, study plan and focus still needs execution.

### Phase 5 — Intelligence
- Core deterministic boundaries exist.
- Confidence-aware behavior and action policies need end-to-end UI regression verification.

### Phase 6 — Account / privacy
- Backend delete endpoint is implemented in the repository.
- Production deployment and live endpoint verification remain required.
- Subscription/entitlement behavior for backend-authenticated accounts needs a server-authoritative policy before production monetization.

### Phase 7 — Offline / sync
- Local-first data model and LWW infrastructure exist.
- Conflict/recovery tests must still cover real offline -> online transitions.
- Pulling a single data type was hardened so a course pull no longer wipes unrelated attendance/grade/exam/task data.

### Phase 8 — Quality
Still required:
- Final CI run on the release commit
- Device/emulator UX regression
- TalkBack
- Dynamic font scaling
- Reduced motion
- Performance baselines
- Memory/jank inspection
- Auth/security integration verification
- Offline/sync integration verification

### Phase 9 — Release
Still required:
- Release signing with production keystore
- Signed AAB verification
- Versioning/tag traceability
- Install/upgrade migration verification
- Cafe Bazaar metadata/signing validation
- Monitoring and rollback verification
- Controlled rollout

## Migration risks

### High
- Account deletion semantics differ between Firebase-backed and backend-authenticated identities.
- Room schema is currently version 9; every future schema change needs explicit migration coverage.
- The feature branch is substantially ahead of main and must not be blindly merged.

### Medium
- Legacy theme/token aliases can create inconsistent visual output if new screens bypass the canonical theme.
- Dual cloud architectures increase authentication/sync test surface.

### Low
- Navigation hub extraction is isolated and does not change the domain model.

## Implementation order

1. Complete design-system adoption and remove/limit legacy visual paths.
2. Finish app-shell/device regression.
3. Run core academic/productivity regression.
4. Complete confidence-aware Copilot verification.
5. Verify backend account deletion deployment.
6. Execute offline/sync conflict and recovery tests.
7. Establish device performance/accessibility baselines.
8. Prepare signed AAB and complete store/release validation.

## Release rule

The project must not claim production readiness while a Critical Gate is unchecked.
