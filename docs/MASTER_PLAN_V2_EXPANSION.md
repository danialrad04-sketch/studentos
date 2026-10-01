# Student OS v2 — Master Plan Expansion

## هدف این expansion

نسخه 2.0.0 پایهٔ UX و context-first را معرفی کرد. این expansion مسیر 2.0.x تا 2.1 را برای تبدیل Student OS به یک Personal Academic Operating System حرفه‌ای تعریف می‌کند.

## Phase 10 — Information Architecture 2.1
- یک زبان واحد برای Card / Surface / Section / Action / Status.
- اطلاعات خلاصه همیشه bounded است.
- محتوای طولانی فقط در viewport قابل اسکرول نمایش داده می‌شود.
- عنوان‌ها یک‌خطی با Ellipsis؛ توضیح‌ها محدود و actionable.
- هیچ متن کاربری یا AI نباید عرض یا ارتفاع layout را به‌تنهایی کنترل کند.

## Phase 11 — Academic Context Graph
ارتباط canonical بین Course, Session, Task, Exam, Attendance, Grade و Focus.

قواعد:
- یک source of truth برای هر entity.
- relationshipها derived یا deterministic باشند.
- UI فقط snapshot/context مصرف کند.
- حذف یا تغییر داده destructive باید recovery داشته باشد.

## Phase 12 — Today Command Center
- وضعیت امروز
- next class
- overdue work
- exam readiness
- attendance risk
- quick focus
- quick add
- توضیح دلیل priority

## Phase 13 — Planning Intelligence
- workload balancing
- deadline collision detection
- exam countdown planning
- suggested study blocks
- manual override
- preview → accept → execute

هیچ plan تولیدشده‌ای بدون تأیید نباید دادهٔ canonical را overwrite کند.

## Phase 14 — Copilot Command Layer
Copilot از chat صرف به command interface تکامل پیدا می‌کند.

نمونه:
- «برای فردا چه دارم؟»
- «کارهای عقب‌افتاده را نشان بده.»
- «برای فیزیک تا پنج‌شنبه دو جلسه مطالعه بچین.»
- «تکلیف ریاضی را انجام‌شده کن.»

Flow:
Intent → Deterministic Parse → Preview → Confirmation when required → Domain Action → Result

## Phase 15 — Import Center
تمام importها از یک pipeline استفاده کنند:

Extract → Normalize → Validate → Preview → Commit

ورودی‌ها:
- OCR timetable
- pasted text
- structured import
- backup restore

## Phase 16 — Recovery & Trust
- Undo برای تغییرات مهم
- recent changes
- restore point
- sync status
- conflict explanation
- local-first fallback
- export bundle

## Phase 17 — Notification Intelligence
- اولویت
- grouping
- quiet periods
- focus-aware suppression
- exam/task digests
- duplicate suppression

## Phase 18 — Academic Analytics 2.1
تمرکز روی insight قابل اقدام، نه صرفاً نمودار.

شاخص‌ها:
- workload
- task completion
- exam readiness
- attendance trajectory
- grade trajectory
- focus consistency

هر insight باید یک next action قابل اجرا داشته باشد.

## Phase 19 — Adaptive UI
Phone:
- day agenda
- compact cards
- single-column focus

Tablet:
- weekly grid
- split details

Large screen:
- rail navigation
- multi-pane workspace
- persistent context panel

## Phase 20 — Accessibility
- TalkBack
- dynamic font
- keyboard navigation
- RTL correctness
- semantic labels
- contrast
- reduced motion
- touch target compliance

Overflow policy:
- no clipped primary action
- no accidental horizontal overflow
- long text is readable via scroll or expansion

## Phase 21 — Performance
Budgets:
- fast first useful frame
- bounded recomposition
- lazy lists for long collections
- no blocking network call in primary startup
- no live AI call in deterministic unit-test gate
- memory-safe image/import paths

## Phase 22 — Release Engineering
- immutable version tags
- monotonic versionCode
- reproducible signing
- certificate pin verification
- AAB primary artifact
- APK test artifact
- 16 KB page-size validation
- release notes
- Play Console metadata checklist
- staged rollout readiness

## Phase 23 — Authentication & Account
- Guest-first entry
- optional Firebase/Google account
- optional dedicated backend account
- one account center
- clear sync state
- account deletion
- privacy/data controls

Google authentication invariant:
- do not rotate production signing key unless explicitly planned.
- preserve production SHA-1/SHA-256.
- preserve package id and Web Client ID.

## Phase 24 — Product Quality Gates
برای هر feature:

Design → States → Persistence → Offline → Error → Accessibility → Unit → UI → Instrumentation → Performance → Release

Definition of done:
- no known critical UX overflow
- no known blocking auth flow
- deterministic domain tests green
- critical UI flow tested
- release artifact signed and verified

## Phase 25 — v2.1 Roadmap
1. finish overflow migration across every remaining information surface
2. migrate legacy cards to canonical Academic primitives
3. unify Task/Exam actions
4. expand Context Engine coverage
5. upgrade Copilot commands
6. add planning preview/accept
7. add recovery/change history
8. finish adaptive tablet/large-screen layouts
9. complete accessibility/performance audit
10. publish v2.1 release candidate

## Anti-scope
- social feed
- public profiles
- ad-driven features
- opaque autonomous AI actions
- decoration-first 3D dashboard
- mandatory cloud connectivity
- duplicated data sources

## Product standard

> Student OS should reduce academic cognitive load, not increase it.

> Every screen should answer what is happening, what matters, and what the student can do next.

> Visual polish is successful only when it improves comprehension, speed and trust.

## Phase 24 — Product Quality Gates
For every feature:

Design → States → Persistence → Offline → Error → Accessibility → Unit → UI → Instrumentation → Performance → Release

Definition of done:
- no known critical UX overflow
- no known blocking auth flow
- deterministic domain tests green
- critical UI flow tested
- release artifact signed and verified

## Phase 25 — v2.1 execution order
1. finish overflow migration across every remaining information surface
2. migrate legacy cards to canonical Academic primitives
3. unify Task/Exam actions
4. expand Context Engine coverage
5. upgrade Copilot commands
6. add planning preview/accept
7. add recovery/change history
8. finish adaptive tablet/large-screen layouts
9. complete accessibility/performance audit
10. publish v2.1 release candidate
