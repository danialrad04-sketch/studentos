# Release Readiness

## Current branch
`feat/academic-premium-design-system`

## Product layers
- [x] Academic design tokens
- [x] Persian typography system
- [x] Adaptive layout metrics
- [x] Dashboard intelligence surfaces
- [x] Deterministic study recommendations
- [x] Focus/Pomodoro UX
- [x] Course workspace
- [x] Grades
- [x] Attendance
- [x] Account identity/security/data layers
- [x] Offline state UX
- [x] Copilot deterministic boundary
- [x] Search / command-center surfaces

## Engineering gates
- [x] Unit-test gate configured
- [x] Lint gate configured
- [x] Debug build gate configured
- [x] Release workflow gate configured
- [x] CI concurrency configured
- [x] Performance budget documented

## Before production release
- [ ] Latest CI run is green on the final commit
- [ ] Release artifact build/signing succeeds
- [ ] Install/upgrade migration tested
- [ ] Firebase Auth startup/logout/data purge tested
- [ ] Offline -> online sync conflict behavior tested
- [ ] Persian RTL + font scaling reviewed on physical devices
- [ ] Accessibility pass: TalkBack, touch targets, content descriptions
- [ ] Play/Bazaar release metadata and signing configuration verified

No production-release claim is made until the unchecked release gates above are verified.
