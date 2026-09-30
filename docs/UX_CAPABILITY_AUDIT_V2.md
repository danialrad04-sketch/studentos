# Student OS v2 — Phase 0 UX & Capability Audit
Date: 2026-09-30
Base: main @ ed34067317e2861bdb6cf0d2b032fc251273c985

## 1. Current UI inventory

### Primary shell / navigation
- MainAppScreen
- MainTabContent
- FloatingIslandNavigationBar
- StudentAdaptiveNavigationRail
- AppBottomNavigation
- StudentModuleHubSheet
- AppDialogManager

### Core academic
- ModernBentoDashboard
- WeeklyScheduleScreen
- ModernScheduleCourseCard
- CourseWorkspaceDialog / CourseWorkspaceDialogV2
- TasksScreen
- ExamsScreen
- GradesScreen
- AttendanceRadarScreen
- CurriculumScreen
- SemesterPlannerScreen
- SemesterHistoryScreen
- AcademicPassportScreen
- AcademicIntelligenceScreen

### Productivity / intelligence
- PomodoroAndNotesScreen
- AcademicCopilotScreen / AcademicCopilotDialog
- CommandCenterDialog
- SpotlightSearchDialog
- StudentGamificationHub
- NotionQuickCommandBar
- Academic risk/workload/study recommendation surfaces

### Identity / data
- LoginRegisterScreen
- LayeredAccountCenter
- AuthAccountDialog
- EditProfileDialog
- BackupRestoreDialog
- PrivacyPolicyDialog
- ConnectionStatusBanner
- PastSemestersHistoryDialog

### Import / export
- OcrScheduleImportDialog
- AcademicExportManager
- PremiumShareExportModal
- CalendarExportManager

### Shared design / motion
- AcademicPremiumPrimitives
- ActionableEmptyState
- StudentGlassModalSheet
- DynamicIslandLiveActivity
- TactileModifier
- AdaptiveLayout
- Motion
- Theme

## 2. UX observations

### Positive foundations
- Strong academic domain coverage already exists.
- Local-first architecture exists.
- Core AI/domain engines exist.
- Guest mode and layered account model exist.
- Adaptive navigation exists.
- Accessibility smoke tests exist.
- Instrumentation and backend integration tests exist.
- v1.3 already introduced Course Workspace and weekly-card improvements.

### Primary UX debt

1. **Multiple visual dialects**
   - Bento dashboard
   - Glass modal patterns
   - Dynamic Island styling
   - Academic Premium primitives
   - legacy card/dialog patterns

   v2 should define one visual grammar and progressively migrate surfaces to it.

2. **Multiple navigation concepts**
   - Floating Island
   - bottom navigation
   - adaptive rail
   - module hub
   - command/search entry

   v2 should separate primary navigation from contextual tools.

3. **Dashboard density**
   ModernBentoDashboard contains many independent sections. The v2 dashboard should prioritize:
   Today → Next → Priority → Academic Pulse → Quick Actions.

4. **Dialog-first workflows**
   Several workflows still treat dialogs as destinations. v2 should reserve dialogs for focused actions and move persistent contexts into screens/workspaces.

5. **Component duplication**
   Multiple card/button/header primitives exist. v2 should create a canonical component hierarchy and migrate by usage rather than deleting old components.

6. **Mixed interaction language**
   Different areas use different patterns for edit, delete, add, focus, reminder and navigation. v2 needs a unified action vocabulary.

## 3. Capability inventory

Already present or substantially present:
- courses
- multi-session schedule
- attendance
- exams
- tasks
- grades / GPA
- curriculum
- semester planning
- academic passport / export
- focus / Pomodoro
- gamification
- academic risk
- workload calculations
- study recommendations
- global search / omnibox
- Copilot
- natural-language action payloads
- OCR timetable import
- backup / restore
- calendar export
- cloud/backend sync
- Firebase/backend authentication
- guest mode
- notifications
- privacy/data controls

## 4. Capability gaps for v2

### High value
- first-class relationships between course/task/exam/focus
- unified academic context
- explainable prioritization
- exam preparation workflow
- deadline collision detection
- study plan preview/accept flow
- sync status visibility
- recovery/change history
- stronger critical UI regression coverage
- richer import preview/validation
- context-aware notification grouping

### Medium value
- richer academic trend analytics
- cross-semester comparisons
- contextual quick actions
- more adaptive large-screen layouts
- improved command suggestions

### Explicitly not a priority
- social feed
- public profiles
- ad-driven features
- opaque AI automation
- decorative 3D-heavy dashboards
- mandatory cloud dependencies

## 5. Architecture risks

- UI components should not become additional sources of canonical state.
- AI payload execution must remain behind deterministic domain logic.
- New relationships should reuse existing entities/repositories unless a schema gap is proven.
- New dependency introduction is discouraged.
- Large UI rewrites increase regression surface; migrate by bounded vertical slices.

## 6. v2 migration strategy

Phase 1:
- canonical design tokens/components
- shell and dashboard
- schedule and course context

Phase 2:
- task/exam interaction unification
- academic context layer

Phase 3:
- Copilot command workflows
- planning intelligence
- import/recovery/notification improvements

Phase 4:
- accessibility/performance/regression/release

## 7. First vertical slice

The first production slice should be:

**Today → Schedule → Course Workspace → Task/Exam action**

This is the highest-leverage path because it connects the core daily student loop without requiring a new backend or schema.

Acceptance:
- one visual language
- one action vocabulary
- no duplicate source of truth
- works offline for core data
- undo/confirmation for destructive actions
- accessibility semantics
- unit + UI + instrumentation coverage
