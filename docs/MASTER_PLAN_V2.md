# Student OS — Master Plan v2
## Product-grade UX/UI + Capability Evolution

Date: 2026-09-30
Base release: v1.3.0
Branch: feat/v2-masterplan
Status: Self-review complete; Phase 0 implementation started

---

# 0. Executive Direction

Student OS v2 is not a collection of more screens.

The target is a **professional Personal Academic Operating System** that feels coherent, fast, trustworthy, calm and purposeful across phone, tablet and desktop-sized Android surfaces.

Two tracks evolve in parallel:

**Track A — Experience**
- UX architecture
- Visual system
- navigation
- information hierarchy
- adaptive layouts
- motion
- accessibility
- onboarding
- empty/error/recovery states
- perceived performance

**Track B — Capability**
- academic command center
- smarter planning
- unified task/exam/course relationships
- academic insights
- trustworthy AI actions
- import/normalization
- history/recovery
- notifications
- offline/sync resilience
- data portability

Core rule:

> Make existing capabilities feel dramatically better before adding large new capabilities.

---

# 1. Self-Grill: Product Questions and Answers

## Q1. What problem is Student OS really solving?
**Answer:** It reduces the student's daily cognitive load by converting scattered academic information into one trustworthy, actionable system.

## Q2. What should the home screen answer in under five seconds?
**Answer:** What is happening now, what is next, and what deserves attention.

## Q3. What makes the app feel professional rather than like a student project?
**Answer:** Consistency, hierarchy, restraint, speed, recoverability, responsive layouts and predictable interactions—not decoration.

## Q4. What visual language should define the brand?
**Answer:** Academic premium: deep petrol/navy foundation, olive accent, quiet surfaces, strong typography, restrained elevation, limited gradients and minimal visual noise.

## Q5. What should be removed from the experience?
**Answer:** Duplicate controls, decorative cards without information value, overly nested dialogs, redundant labels, unnecessary animations, and settings that belong in advanced areas.

## Q6. Should every feature appear in navigation?
**Answer:** No. Navigation should expose the student's primary workflows; secondary capabilities belong in contextual entry points and the module hub.

## Q7. What is the most important UX object in the app?
**Answer:** The academic context object: Course / Task / Exam / Study Session. These should connect naturally instead of behaving like isolated records.

## Q8. What should happen when context changes?
**Answer:** Presentation should adapt. For example, an approaching exam should increase exam/study relevance without changing canonical data.

## Q9. How should the app behave when it has no data?
**Answer:** Explain what is missing, why it matters, and provide one useful next action.

## Q10. How should errors feel?
**Answer:** Recoverable and understandable. Always show what happened, what remains safe, and the next available action.

## Q11. Should AI drive the product?
**Answer:** No. AI is a command and explanation layer over deterministic academic data.

## Q12. What must AI never do silently?
**Answer:** Mutate canonical academic records, invent facts, infer ambiguous data as certain, or overwrite a plan without explicit user intent.

## Q13. What makes AI useful here?
**Answer:** Context. AI should know the current courses, tasks, exams, schedule and relevant academic profile when the user explicitly allows that context.

## Q14. What is the right AI interaction model?
**Answer:** Ask → preview → confirm → execute for consequential actions; answer directly for low-risk informational requests.

## Q15. What should the schedule become?
**Answer:** A time-centric academic timeline on compact screens and a true weekly planner on larger screens, powered by one shared schedule model.

## Q16. What should the task system become?
**Answer:** A unified task layer connected to courses, exams and focus sessions, with clear Today / Priority / Upcoming / Overdue views.

## Q17. What should the course experience become?
**Answer:** A persistent workspace, not just a dialog: overview, schedule, tasks, attendance, grades, exam and focus entry points in one coherent context.

## Q18. What should the dashboard become?
**Answer:** A contextual command center—not a fixed wall of cards.

## Q19. Should we add social/community features?
**Answer:** No in v2. They create moderation, privacy and product-scope complexity without being central to the personal academic OS promise.

## Q20. Should we add a huge AI study chatbot?
**Answer:** No. Improve the existing Academic Copilot into a reliable command layer instead of creating a disconnected chatbot.

## Q21. What capability would provide the most leverage?
**Answer:** Cross-object academic context: course ↔ task ↔ exam ↔ schedule ↔ focus ↔ grade.

## Q22. What is the most dangerous type of feature?
**Answer:** Features that silently change data, introduce hidden cloud dependency, or create duplicated state.

## Q23. How should sync evolve?
**Answer:** Keep local-first behavior, make sync state visible, preserve deterministic conflict rules, and make recovery explicit.

## Q24. What should onboarding optimize for?
**Answer:** Time-to-value. The user should reach a useful dashboard quickly, with the minimum number of questions.

## Q25. What should performance optimize for?
**Answer:** Perceived responsiveness first, then measurable startup, scrolling, memory and background-work budgets.

## Q26. What is the rule for adding a new screen?
**Answer:** A new screen needs a distinct user job. If an existing context can solve the job, extend the existing context.

## Q27. What is the rule for adding a dependency?
**Answer:** No dependency unless it removes meaningful complexity or provides essential capability that cannot be safely maintained in-house.

## Q28. What does “done” mean in v2?
**Answer:** Feature + state coverage + persistence safety + offline behavior + accessibility + tests + performance + analytics/diagnostics where appropriate.

## Q29. What should be measured?
**Answer:** Crash-free sessions, startup, navigation latency, major workflow completion, auth success/failure, sync success, and user-facing error recovery.

## Q30. What is the final product standard?
**Answer:** The app should feel boringly reliable: users should understand what is happening, trust the data, and recover from mistakes without fear.

---

# 2. v2 Product Architecture

## 2.1 Experience Model

Every important academic object follows the same conceptual structure:

**Context → Status → Next action → Details → History**

Examples:

Course:
- current status
- next class / exam
- next task
- attendance risk
- recent grade
- history

Task:
- state
- due date
- course
- exam relationship
- focus action
- history

Exam:
- date/time
- course
- preparation state
- related tasks
- focus action

This creates one product language across modules.

## 2.2 UX Layering

**Global shell**
- App identity
- global search/command
- primary navigation
- notifications
- account

**Context surfaces**
- Dashboard
- Schedule
- Course Workspace
- Task Center
- Exam Center
- Grade / Academic Intelligence

**Deep tools**
- Import
- Curriculum
- Focus configuration
- Data management
- Advanced settings

---

# 3. Track A — UX/UI v2

## Phase A0 — UX Audit
Deliver:
- screen inventory
- duplicate interaction inventory
- information hierarchy map
- navigation map
- component inventory
- responsive behavior matrix
- accessibility gaps
- visual inconsistency map

## Phase A1 — Design System 2.0
Build:
- semantic token architecture
- typography hierarchy
- compact / comfortable / spacious density modes
- component states
- consistent surfaces
- button hierarchy
- input hierarchy
- feedback patterns
- motion primitives
- focus indicators
- dark/light parity

Visual principles:
- deep petrol/navy + olive brand
- neutral surfaces
- limited color accents
- no generic neon/glass SaaS treatment
- information first


### Card Sizing Standard (mandatory)
- Compact information cards must have bounded dynamic text.
- Titles: max 1 line + ellipsis.
- Descriptions: max 2–3 lines + ellipsis.
- Metadata/chips: max 1 line + ellipsis.
- Compact cards must use an explicit height band when their content is structurally fixed.
- Detail/workspace surfaces may show full text only inside a bounded scrolling region.
- No user-provided string may be allowed to determine the unbounded height of a reusable card.
- Every new card component must include a long-content regression case before merge.

## Phase A2 — App Shell 2.0
Improve:
- responsive shell
- compact navigation
- tablet/desktop rail
- module hub
- command/search entry
- notification center
- contextual title/action bars

## Phase A3 — Dashboard 2.0
Replace fixed card wall with:
1. Today timeline
2. Priority actions
3. Academic pulse
4. Upcoming exam/task intelligence
5. Quick actions

Sections should collapse or reorder by context.

## Phase A4 — Schedule 2.0
Phone:
- day agenda
- time rail
- current-time indicator
- conflict indicator

Tablet:
- weekly grid

Large screen:
- weekly grid + selected-day detail pane

## Phase A5 — Course Workspace 3.0
Replace dialog-first mental model with a coherent workspace.

Sections:
- Overview
- Schedule
- Tasks
- Exam
- Attendance
- Grades
- Focus
- Notes / metadata

Contextual top area:
- current status
- next action
- high-signal metrics

## Phase A6 — Task / Exam UX
Unified action vocabulary:
- Start
- Complete
- Snooze
- Reschedule
- Link
- Undo

Priority must be explainable, not mysterious.

## Phase A7 — Onboarding / Account
First-start flow:
- Continue as guest
- Sign in
- Create account

After entry:
- minimal academic profile setup
- optional import
- immediate useful dashboard

## Phase A8 — Accessibility
Verify:
- TalkBack
- dynamic font
- keyboard / hardware navigation
- contrast
- semantics
- reduced motion
- RTL
- touch targets

## Phase A9 — Motion / Polish
Use motion only for:
- navigation continuity
- state change
- hierarchy
- confirmation
- focus

Avoid animation for decoration.

---

# 4. Track B — Capability v2

## Phase B0 — Capability Audit
Inventory current:
- course
- schedule
- attendance
- grades
- exam
- task
- focus
- search
- copilot
- curriculum
- import
- account
- sync
- notifications
- export

For each capability record:
- current state
- dependencies
- tests
- offline behavior
- failure modes
- missing UX

## Phase B1 — Academic Context Graph
Create explicit relationships:

Course ↔ Sessions
Course ↔ Tasks
Course ↔ Exams
Course ↔ Attendance
Course ↔ Grades
Task ↔ Exam
Task ↔ Focus
Exam ↔ Study Plan

Do not duplicate source-of-truth data.

## Phase B2 — Academic Command Center
A single place to answer:
- What should I do now?
- What is overdue?
- What is at risk?
- What is next?

Use deterministic rules and explainable reasons.

## Phase B3 — Planning Intelligence
Capabilities:
- workload-aware suggestions
- exam countdown planning
- deadline collision detection
- study-session suggestions
- manual override

All generated plans are previews until accepted.

## Phase B4 — Copilot 2.0
Upgrade from chat toward commands:

Examples:
- "What do I have tomorrow?"
- "Show overdue tasks."
- "Plan two study sessions for physics before Thursday."
- "Mark my math assignment as complete."

Flow:
Command → parsed intent → preview → confirmation when needed → deterministic action.

## Phase B5 — Natural Language Data Entry
Add structured parser support for:
- course
- session
- task
- exam
- reminder

Ambiguous input must produce a preview rather than a silent mutation.

## Phase B6 — Import Center
One consistent import surface:
- photo timetable
- structured text
- pasted schedule
- backup restore

Every import becomes:
Extract → Normalize → Validate → Preview → Commit

## Phase B7 — Notification Intelligence
Support:
- priority
- grouping
- quiet periods
- focus-aware suppression
- digest behavior

## Phase B8 — Data & Recovery
Expand:
- recent changes
- undo
- restore point
- export bundle
- clear local data
- account unlink
- sync status

## Phase B9 — Offline / Sync
Make status visible:
- Local
- Syncing
- Synced
- Needs attention

Preserve local-first operation.

## Phase B10 — Academic Analytics
Add:
- workload trend
- completion trend
- attendance trend
- grade trend
- focus trend

No chart without actionable interpretation.

---

# 5. Anti-Features

Explicitly out of scope for v2 unless evidence changes:

- social feed
- public student profiles
- gamified leaderboard as a primary experience
- ad-driven UX
- mandatory cloud connectivity
- opaque AI automation
- decorative 3D dashboards
- excessive glassmorphism
- duplicate feature screens
- unnecessary microservices

Gamification may remain as an optional reinforcement layer, never the core academic model.

---

# 6. Architecture Rules

1. Extend > Replace.
2. One canonical source of truth per data object.
3. UI never owns canonical academic state.
4. AI never becomes the source of truth.
5. Domain calculations remain deterministic.
6. Offline core flows remain functional.
7. New state must have persistence implications documented.
8. Every destructive action has recovery/confirmation.
9. Every new asynchronous operation has loading/success/error states.
10. No new dependency without an explicit cost/benefit decision.

---

# 7. Quality Strategy v2

## Unit
- parsers
- domain rules
- planning
- scheduling
- analytics
- confidence logic

## Integration
- Room
- repositories
- sync
- auth
- backend

## UI
Critical flows:
- onboarding
- guest
- sign-in
- schedule
- course workspace
- task completion
- exam creation
- grade entry
- focus
- export

## Accessibility
- semantics
- font scaling
- TalkBack
- keyboard
- reduced motion

## Performance
Measure:
- cold start
- warm start
- first useful frame
- major screen transition
- schedule scroll
- task scroll
- memory
- background work
- network use

## Release
- version
- signing
- artifact verification
- AAB
- APK
- release notes
- staged rollout

---

# 8. v2 Definition of Done

Student OS v2 is complete when:

- the primary workflows share one interaction language
- navigation is coherent across form factors
- Course / Task / Exam / Schedule relationships are first-class
- AI actions are explicit and trustworthy
- import uses preview-before-commit
- offline core functionality is reliable
- sync status is understandable
- destructive actions are recoverable
- accessibility is verified
- performance is measured
- release artifacts are signed and reproducible
- critical flows have regression tests
- no known critical blocker remains

---

# 9. Execution Order

1. Audit current screens and capabilities.
2. Freeze canonical data models.
3. Upgrade design tokens/components.
4. Redesign app shell.
5. Redesign Dashboard.
6. Redesign Schedule.
7. Upgrade Course Workspace.
8. Unify Task / Exam interaction patterns.
9. Build Academic Context Graph.
10. Upgrade Copilot to command layer.
11. Upgrade Planning Intelligence.
12. Build Import Center.
13. Improve notifications/recovery.
14. Complete accessibility/performance.
15. Full regression.
16. v2 release candidate.

The order intentionally front-loads UX consistency and shared context before adding large new capabilities.

---

# 10. Immediate Implementation Sprint

The first implementation sprint after this plan is intentionally small:

### Sprint 1
- audit all main screens and shared components ✅
- create UX inventory ✅
- identify duplicate UI patterns ✅
- define v2 component conventions ✅
- establish Dashboard/Schedule/Course information hierarchy ✅
- add tests around newly shared context relationships ✅
- enforce card sizing/long-text standards across core screens ✅
- no backend/schema migration

### Sprint 2
- implement shell 2.0
- implement Dashboard 2.0 ✅ first contextual layer
- implement Schedule 2.0 ✅ first contextual layer
- unify bounded-card behavior across core academic surfaces ✅

### Sprint 3
- Course Workspace 3.0
- Task / Exam unified UX

### Sprint 4
- Academic Context Graph
- Copilot command layer
- planning intelligence

### Sprint 5
- import/recovery/notifications
- accessibility/performance

### Sprint 6
- regression + release candidate

---

# 11. Success Criteria

The v2 success test is not “more features”.

It is:

> A student can open Student OS, understand their current academic state immediately, decide what to do next, execute it quickly, and trust that their data is safe.

That is the product standard for v2.
