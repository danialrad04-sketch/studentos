# Student OS — Master Product, UX, Architecture & Release Plan

> **Status:** Planning / Awaiting implementation approval  
> **Code changes:** None as part of this plan  
> **Rule:** No implementation work begins until the owner explicitly approves this plan.  
> **Scope:** Product vision, UX/UI, architecture direction, AI behavior, data, offline/sync, accessibility, performance, testing and release engineering.

---

## 0. Non-Negotiable Implementation Rule

This document is the source of truth for the next Student OS evolution.

Until explicit approval is given:

- Do not modify production/source code.
- Do not delete or rename existing files.
- Do not change database schemas or migrations.
- Do not rewrite existing engines.
- Do not change authentication behavior.
- Do not replace existing navigation or UI wholesale.
- Do not introduce dependencies merely because they appear convenient.
- Do not remove existing features without a documented migration/replacement decision.
- Audit first; implement second.

The implementation philosophy is **Extend > Replace**.

Existing working systems should be preserved, audited, and extended unless a concrete technical reason requires replacement.

---

# 1. Product Vision

Student OS is intended to evolve into a **Personal Academic Operating System** rather than a conventional study-planning application.

Core model:

    Student Data
          ↓
    Academic / Domain Engines
          ↓
    Shared Intelligence Layer
          ↓
    ┌───────────────────────┬──────────────────────┐
    │ Student Experience    │ Copilot              │
    │ Dashboard             │ Natural Language     │
    │ Schedule              │ Commands             │
    │ Courses               │ Assistance           │
    │ Exams                 │ Context              │
    │ Tasks                 │                      │
    │ Grades                │                      │
    │ Focus                 │                      │
    └───────────────────────┴──────────────────────┘

Principles:

1. Student data is the source of truth.
2. Deterministic academic logic remains deterministic.
3. AI assists; it does not silently invent facts or take uncontrolled actions.
4. Context determines presentation.
5. User control is preserved for important actions.
6. Offline academic functionality remains usable.
7. Accessibility is part of the design system.
8. Performance is measurable.
9. Release quality is enforced through gates.

---

# 2. Visual Identity

## 2.1 Brand Direction

Selected direction: **Academic Premium**.

Desired characteristics:

- Professional
- Academic
- Calm
- Organized
- Modern without being flashy
- Premium without excessive decoration

Avoid:

- Generic AI SaaS appearance
- Excessive gradients
- Neon aesthetics
- Unnecessary glassmorphism
- Excessive rounded cards
- Decorative UI that competes with information
- Excessive visual noise

## 2.2 Brand Palette

Primary visual identity:

- **Navy / Deep Petrol Blue (آبی نفتی)** — primary brand foundation
- **Olive Green (سبز زیتونی)** — accent / emphasis

Brand colors must remain separate from semantic status colors.

Semantic statuses use their own accessible tokens:

- Success
- Warning
- Error
- Information

Status meaning must never depend on color alone.

---

# 3. Design System

The design system should be token-driven.

Core token groups:

- Color
- Typography
- Spacing
- Shapes
- Borders
- Elevation
- Motion
- Component states
- Semantic status
- Accessibility

Light and dark themes should have independently defined tokens rather than simply inverting colors.

Every new component must define:

- Normal state
- Pressed state
- Focus state
- Disabled state
- Loading state where relevant
- Error state where relevant
- Accessibility semantics
- RTL behavior
- Dark/light behavior

---

# 4. Adaptive UI

The product should support:

- Phone
- Foldable
- Tablet
- Large screens

The underlying feature/data logic should remain shared.

Examples:

- Phone → compact navigation / agenda
- Tablet → expanded layouts / weekly grid
- Large screen → multi-pane workspace

Adaptive behavior must not create duplicated business logic.

---

# 5. Adaptive Density

Selected direction: **Adaptive Density**.

Different contexts use different information density:

- Dashboard → Balanced
- Grades / analytics → Dense
- Onboarding → Spacious
- Focus → Minimal
- Large screens → Can use higher density where appropriate

Density changes presentation, not the underlying data model.

---

# 6. Navigation

Navigation should expose the major academic modules without becoming a cluttered menu.

Core areas:

- Dashboard
- Schedule
- Courses
- Exams
- Tasks
- Grades
- Attendance
- Focus
- Academic Intelligence
- Copilot
- Additional modules

Existing navigation should be audited before restructuring.

Navigation changes must preserve deep links and existing feature access where applicable.

---

# 7. Dashboard

The Dashboard answers:

> **What is happening in my academic life right now?**

Potential information:

- Today
- Academic overview
- Tasks
- Exams
- Courses
- Focus
- Progress

The Dashboard is contextual rather than a permanently fixed collection of cards.

It should prioritize what is relevant without becoming an alert wall.

---

# 8. Academic Intelligence

Selected entry experience: **Overall Academic Status**.

Initial view should provide an understandable summary such as:

- Academic status
- GPA
- Overall progress
- Active courses
- Relevant high-level indicators

Users can then drill into:

- Risk
- Progress
- Attendance
- Grades
- Workload
- Opportunities

## 8.1 Status Calculation

Selected architecture:

**Rule-based / deterministic.**

Academic status must be computed by explicit, testable rules.

AI must not independently decide whether a student's academic condition is "good" or "bad".

## 8.2 Visualization

Selected approach:

**Hybrid Intelligence Visualization**

Use:

- Number
- Visualization
- Context

Only when visualization communicates information better than plain text.

Example:

    GPA 17.42
    ████████░░
    +0.8 compared with previous term
    Status: Good

Do not add charts merely for decoration.

---

# 9. Course Workspace

Each course becomes an adaptive workspace.

Possible contextual priorities:

### Normal course
Overview

### Exam approaching
Exam / Study Priority

### Overdue task
Tasks

### New grade
Grades / Progress

### Completed semester
Academic Summary

All views should be driven from a shared Course model.

Do not create duplicated course state for individual screens.

---

# 10. Adaptive Schedule

Schedule should adapt to context and screen size.

Examples:

### Phone
Agenda / Timeline

### Tablet
Weekly Grid

### Busy day
Today-first presentation

### Exam period
Exam prominence

### Focus mode
Only relevant schedule information

The same schedule data should power every presentation.

---

# 11. Adaptive Task System

Task views can include:

- Today
- Priority
- Deadline
- Course
- Overdue
- Exam-related
- All Tasks

Context examples:

- Dashboard → Today
- Course → Course tasks
- Exam period → Exam-related tasks
- Busy day → Priority / Deadline
- Global task screen → All Tasks

User must always be able to reach the complete task list.

---

# 12. Study Plan

Selected direction: **Manual Planner**.

Study Plan remains student-controlled.

The student controls:

- Study sessions
- Topics
- Times
- Ordering
- Schedule structure

AI or background engines must not silently rewrite the plan.

AI assistance can be offered only through an explicit user action.

---

# 13. Focus Engine

Selected direction: **Adaptive Focus Engine**.

Focus sessions can be linked to:

- Course
- Task
- Duration

Track where appropriate:

- Actual focus duration
- Course
- Task
- Progress
- Streak

The user retains control over:

- Focus duration
- Break duration
- Session start/stop

Focus is an academic activity record, not merely a timer.

---

# 14. Copilot

Copilot is a **Command Layer for Student OS**.

It is not merely a generic chatbot.

Responsibilities may include:

- Answering academic-context questions
- Understanding available Student OS context
- Executing supported commands
- Helping with planning when explicitly requested
- Explaining information
- Guiding users through tasks

## 14.1 Response Style

Selected direction: **Text-first**.

Text is the default response representation.

Structured UI should not be generated merely for visual effect.

## 14.2 Important AI Boundary

AI does not become the source of truth for academic records.

Canonical data remains in the application's data/domain layer.

---

# 15. Confidence-Aware AI

Copilot and natural-language operations should account for confidence.

### High confidence
Low-risk operation may proceed according to defined rules.

### Medium confidence
Show a preview before committing.

### Low confidence
Ask a clarifying question.

Example:

> "Which Thursday do you mean: date A or date B?"

Important actions should still require confirmation even when confidence is high.

AI must never present an uncertain inference as an established fact.

---

# 16. Search / Omnibox

Selected direction: **Search + Commands**.

Two primary roles:

### Search
Find:

- Courses
- Tasks
- Exams
- Notes
- Other indexed academic data

### Commands
Examples:

- "Exams this week"
- "Tasks due today"
- "Schedule tomorrow"

Do not turn the Omnibox into an unrestricted AI interface.

Existing Spotlight/Omnibox capabilities should be preserved and extended carefully.

---

# 17. Data Entry

Selected direction: **Hybrid Manual + Natural Language**.

### Manual
Traditional forms remain fully supported.

### Natural Language
Example:

> "Math Monday and Wednesday 10 to 12, exam January 10."

Pipeline:

    Natural Language
          ↓
    Parser
          ↓
    Normalized Data
          ↓
    Preview
          ↓
    User Confirmation
          ↓
    Save

Ambiguous information follows the confidence rules.

---

# 18. Import

Selected direction: **Manual Import** for the primary workflow.

The product should not depend on AI extraction for basic data entry.

Existing import capabilities may remain available where already implemented, but the new master UX should not make AI import a mandatory path.

---

# 19. Recovery / History

Selected direction:

**Undo + Edit + History**

Users should be able to:

- Undo important recent operations
- Edit results
- Inspect recent changes where supported

For future multi-step actions, changes should be designed so recovery is safe and understandable.

---

# 20. Settings

Selected direction: **Layered Settings**

Structure:

    Quick Settings
          ↓
       Settings
          ↓
       Advanced

### Quick Settings
Frequent daily options.

### Settings
Normal configuration.

### Advanced
Technical/specialized options such as:

- Data management
- AI configuration
- Privacy
- Debugging
- Advanced synchronization

Advanced controls should not clutter the primary experience.

---

# 21. Account / Identity

Selected direction: **Layered Identity**

Separate:

### Identity
Personal identity information.

### Security
Authentication, sessions, password/login methods.

### Data
Sync, export, deletion.

### Academic Profile
Academic identity such as:

- Major
- Semester
- Curriculum
- Academic information

Account screens should not become a single overloaded page.

---

# 22. Privacy & Data Transparency

Selected direction: **Full Data Transparency**.

The product should make clear:

- What data exists
- Where it is stored
- What is synchronized
- What AI context uses
- What can be exported
- What can be deleted

Example:

    Course Data
    Used for:
    Schedule / Copilot / Analytics

    AI Context
    Used for:
    Copilot responses

    Cloud Data
    Used for:
    Cross-device synchronization

Sensitive deletion operations use:

    Preview → Confirmation → Execute

---

# 23. Notifications

Selected direction: **Adaptive Notification System**.

Notification categories may include:

- Classes
- Tasks
- Exams
- Reminders
- Focus
- Academic insights

Behavior:

- User-controlled
- Priority-aware
- Grouped
- Context-aware
- Low interruption during Focus

Examples:

- Focus active → minimize interruption
- Exam approaching → relevant reminders
- Repeated similar notifications → group them
- Non-critical information → Notification Center

No hidden notification behavior.

---

# 24. Offline Architecture

Selected direction: **Hybrid Intelligent Offline**.

Core academic features should remain usable without internet:

- Schedule
- Tasks
- Exams
- Courses
- Focus
- Academic calculations

Online-only or primarily online capabilities:

- AI Copilot
- Cloud synchronization
- Account operations
- Cross-device sync

Offline must not make the core academic experience unusable.

---

# 25. Sync

Selected conflict strategy:

**Last Write Wins**

Keep the model intentionally simple.

Requirements:

- Reliable local persistence
- Clear sync boundaries
- Migration-safe data
- No silent corruption
- Recovery from failed sync
- Deterministic conflict behavior

---

# 26. Onboarding

Selected direction: **Guided Onboarding**.

After authentication, ask only a small number of useful questions, such as:

- Major
- Semester
- Goal
- Intended usage

Do not create a long setup form.

Onboarding should quickly reach a useful Dashboard.

---

# 27. Accessibility

Selected direction: **Accessibility by Design**.

Requirements:

- Dynamic font sizing
- TalkBack support
- Proper semantics
- Contrast
- Touch target sizing
- RTL correctness
- Reduced motion
- Keyboard/hardware navigation where relevant
- Color-independent status communication
- Adaptive layouts

Every new component must have accessibility requirements before implementation.

---

# 28. Performance

Selected direction: **Performance Budget**.

Performance must be measurable.

Track budgets for:

- Startup
- Frame rendering / jank
- Memory
- Battery
- App size
- Network activity

Exact numeric budgets should be established during the audit/benchmark phase using the actual target device profile rather than guessed numbers.

A feature that materially violates an agreed budget must not silently ship.

---

# 29. Error & Empty States

Selected direction: **Context-aware Recovery**.

Examples:

### Offline

Explain that local data remains available.

### Sync error

Offer retry and last-sync information.

### AI unavailable

Keep offline/core features available.

### Missing data

Offer the appropriate Add/Import action.

### Empty state

Explain what is missing and provide the most useful first action.

Do not expose technical stack traces to normal users.

---

# 30. Architecture Direction

Target architectural direction:

**Kotlin + Jetpack Compose + Material 3 + MVVM/Clean Architecture**, preserving the project's existing architecture where practical.

Conceptual flow:

    UI
      ↓
    ViewModel
      ↓
    Use Cases
      ↓
    Domain / Academic Engines
      ↓
    Repositories
      ↓
    Local / Remote Data

Existing engines should be audited before modification.

Known Student OS systems to preserve/audit include:

- GlobalSearchEngine
- AcademicGamificationEngine
- AcademicCopilotEngine
- CurriculumEngine
- AcademicRiskEngine
- RegistrationTextParser
- DateTimeNormalizer
- Existing scheduling/task/grade/focus systems

These names are architectural references from the planning phase; the actual repository structure must be verified during Phase 0.

---

# 31. Implementation Principle: Extend > Replace

Before changing an existing system:

1. Locate it.
2. Understand its current responsibility.
3. Identify consumers.
4. Check tests.
5. Check persistence implications.
6. Check navigation/UI dependencies.
7. Identify migration risks.
8. Propose the smallest safe change.
9. Implement only after approval.

No "clean rewrite" simply because a module could theoretically be cleaner.

---

# 32. Implementation Phases

## Phase 0 — Repository Audit

No functional redesign yet.

Audit:

- Project structure
- Gradle configuration
- Modules
- Architecture
- Models
- Room entities/DAOs
- Repositories
- ViewModels
- Navigation
- Existing engines
- Theme
- Components
- Auth
- Sync
- Tests
- Build configuration
- Release/signing
- Current Cafe Bazaar requirements

Output:

- Current-state map
- Dependency map
- Risk list
- Migration list
- Exact implementation plan

**No code changes.**

---

## Phase 1 — Design System

Implement after approval:

- Navy / petrol blue
- Olive green
- Semantic colors
- Typography
- Spacing
- Shapes
- Elevation
- Motion
- Light/Dark tokens
- Accessibility foundations
- Core reusable components

---

## Phase 2 — App Shell

- Navigation
- Responsive/adaptive layout
- Dashboard shell
- Window-size behavior
- Shared scaffolding

---

## Phase 3 — Core Academic UX

- Dashboard
- Schedule
- Courses
- Exams
- Tasks
- Grades
- Attendance

Preserve existing business logic where possible.

---

## Phase 4 — Productivity

- Study Plan
- Focus
- Task context
- Academic progress

---

## Phase 5 — Intelligence

- Academic Intelligence
- Rule-based status
- Copilot
- Confidence-aware operations
- Search + Commands

---

## Phase 6 — Account & Privacy

- Layered Account
- Security
- Data controls
- Academic Profile
- Privacy transparency

---

## Phase 7 — Offline / Sync

- Local-first core flows
- Sync
- Last Write Wins
- Recovery
- Migration verification

---

## Phase 8 — Quality

- Unit tests
- Integration tests
- UI tests
- Regression tests
- Auth/security tests
- Offline/sync tests
- Accessibility checks
- Performance benchmarks
- Error-state coverage

---

## Phase 9 — Release

- Production gates
- Signing
- AAB validation
- Cafe Bazaar validation
- Release notes
- Git tagging
- Monitoring
- Rollback preparation
- Controlled rollout

---

# 33. Production Quality Gate

A release is not considered ready merely because the project builds.

Required pipeline:

    Code Quality
          ↓
    Unit Tests
          ↓
    Integration Tests
          ↓
    Critical UI Flows
          ↓
    Auth & Security
          ↓
    Offline / Sync
          ↓
    Accessibility
          ↓
    Performance Budget
          ↓
    Release Signing
          ↓
    AAB Validation
          ↓
    Cafe Bazaar Validation
          ↓
    Release

Any **Critical Gate** failure blocks release.

---

# 34. Controlled Production Pipeline

Target rollout:

    Development
          ↓
    Internal Testing
          ↓
    Closed Testing
          ↓
    Open Testing
          ↓
    Production

Each stage should provide a chance to detect:

- Crashes
- Regression
- Performance issues
- Authentication problems
- Sync issues
- User-facing UX problems

Release artifacts should be traceable to Git commits/tags.

---

# 35. Release Engineering

Required principles:

- Reproducible builds where practical
- Secure signing
- Correct versioning
- Database migration versioning
- AAB validation
- Cafe Bazaar validation
- Release notes
- Git tag per release
- Crash monitoring
- Performance monitoring
- Rollback strategy

Signing secrets must never be committed to the repository.

---

# 36. Definition of Done

A feature is not done when the screen exists.

A feature is done when:

- UI is implemented
- Domain behavior is correct
- Existing behavior has not regressed
- Persistence is safe
- Offline behavior is defined
- Error states exist
- Empty states exist
- Accessibility is verified
- Performance is acceptable
- Tests cover critical behavior
- Navigation works
- RTL works
- Light/Dark behavior works
- Release impact is understood

---

# 37. Final Product Principles

1. **Student control first**
2. **Deterministic academic logic**
3. **AI as assistant, not source of truth**
4. **Context-aware presentation**
5. **Minimal personalization**
6. **Academic Premium visual identity**
7. **Navy + Olive brand system**
8. **Accessibility by Design**
9. **Offline-capable academic core**
10. **Last Write Wins synchronization**
11. **Manual Study Planning**
12. **Text-first Copilot**
13. **Search + Commands Omnibox**
14. **Performance Budget**
15. **Production Quality Gates**
16. **Controlled Release Pipeline**
17. **Extend > Replace**
18. **Audit before implementation**
19. **No silent destructive changes**
20. **No implementation before explicit approval**

---

# 38. Approval Gate

The next permitted step after owner approval is:

**Phase 0 — Repository Audit**

The audit must first report:

- What already exists
- What already satisfies this plan
- What needs modification
- What needs extension
- What is missing
- What is risky
- What can be implemented without migration
- What requires migration
- Recommended implementation order

Only after that audit is reviewed should implementation begin.
