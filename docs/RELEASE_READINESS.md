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
- Android CI runs unit tests, lint, debug build, and emulator instrumentation; the latest final-commit run completed successfully.

## Engineering gates
- [x] Unit-test gate configured
- [x] Lint gate configured
- [x] Debug build gate configured
- [x] Release workflow gate configured
- [x] CI concurrency configured
- [x] Performance budget documented

## Before production release
- [ ] Release workflow is triggered manually with an explicitly verified versionCode/versionName pair; automated push-based release is disabled.
- [x] Server-authoritative subscription/entitlement endpoint defined and consumed; Backend login does not grant Pro implicitly.
- [ ] Live entitlement/paid-tier administration verified on the deployed backend.
- [x] Latest CI run is green on the final commit (`36222824725` on `e7818ecd4a2e1946396e264070d98a1f81d238c1`)
- [ ] Release artifact build/signing succeeds
- [ ] Install/upgrade migration tested
- [ ] Firebase Auth startup/logout/data purge tested
- [x] Backend account deletion endpoint implemented; live deployment/endpoint verification remains
- [ ] Offline -> online sync conflict behavior tested
- [ ] Persian RTL + font scaling reviewed on physical devices
- [ ] Accessibility pass: TalkBack, touch targets, content descriptions
- [ ] Play/Bazaar release metadata and signing configuration verified

- [x] One-step local Undo is implemented for destructive local operations.

No production-release claim is made until the unchecked release gates above are verified. In particular, the Android client must not claim full account deletion while a backend-authenticated account has no verified server-side deletion path.


- Android emulator instrumentation smoke and accessibility smoke gates are configured; latest green CI remains mandatory before release.

- [x] Backend promo redemption is transactional and server-authoritative.

- [ ] Play Billing purchase verification is not enabled until a real Google Play verification service is configured; the server currently fails closed for unverified purchase tokens.


- CI gate policy: Unit tests + Lint + Debug APK + Android emulator smoke + Backend syntax/tests are all mandatory before release.
