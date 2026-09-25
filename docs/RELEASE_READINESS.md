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

## Current verification notes
- Backend delete route is implemented at `DELETE /api/auth/account` and the Android client calls it for Backend-authenticated accounts.
- The endpoint has not been live-verified from the production VPS in this audit.
- Android CI is configured to run unit tests, lint, and debug build; the current PR head has not yet reported a workflow result through the connected GitHub integration.

## Engineering gates
- [x] Unit-test gate configured
- [x] Lint gate configured
- [x] Debug build gate configured
- [x] Release workflow gate configured
- [x] CI concurrency configured
- [x] Performance budget documented

## Before production release
- [ ] Server-authoritative subscription/entitlement endpoint defined and consumed; Backend login must not grant Pro implicitly.
- [ ] Latest CI run is green on the final commit
- [ ] Release artifact build/signing succeeds
- [ ] Install/upgrade migration tested
- [ ] Firebase Auth startup/logout/data purge tested
- [x] Backend account deletion endpoint implemented; live deployment/endpoint verification remains
- [ ] Offline -> online sync conflict behavior tested
- [ ] Persian RTL + font scaling reviewed on physical devices
- [ ] Accessibility pass: TalkBack, touch targets, content descriptions
- [ ] Play/Bazaar release metadata and signing configuration verified

No production-release claim is made until the unchecked release gates above are verified. In particular, the Android client must not claim full account deletion while a backend-authenticated account has no verified server-side deletion path.
