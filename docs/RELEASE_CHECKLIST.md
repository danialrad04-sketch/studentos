# Student OS — Production Release Checklist

This checklist is part of the controlled production pipeline.

## 1. Source & Version
- [ ] Release branch points to the intended commit
- [ ] Version code is incremented exactly once for the release
- [ ] Version name is intentional
- [ ] Git tag maps to the release artifact
- [ ] Verify `VERSION_CODE` is strictly greater than the latest published release and `VERSION_NAME` matches the release tag
- [ ] No secrets, keystores or real .env files are committed

## 2. Build
- [x] Debug build succeeds
- [x] Release APK succeeds
- [x] Release AAB succeeds
- [x] Signing verifies successfully
- [x] AAB package/applicationId is correct

## 3. Tests
- [x] Unit tests pass
- [x] Integration/data tests pass
- [x] Authentication flows pass (backend lifecycle + Guest UI smoke)
- [x] Critical Compose/UI smoke flows pass
- [x] Room migrations pass
- [ ] Offline behavior passes
- [ ] Sync behavior passes

## 4. UX / Accessibility
- [ ] RTL checked
- [ ] Light theme checked
- [ ] Dark theme checked
- [ ] Dynamic font checked
- [ ] TalkBack semantics checked
- [ ] Touch targets checked
- [ ] Status meaning does not depend on color alone
- [x] Reduced motion checked

## 5. Performance
- [ ] Startup benchmark checked
- [ ] Critical screens free of obvious jank
- [ ] Memory checked on target device profile
- [x] Known manual-sync race removed; network behavior remains subject to production-device review
- [x] APK/AAB size reviewed in release artifact validation

## 6. Store Readiness
- [ ] App name and Persian metadata reviewed
- [ ] Icon 512x512 PNG validated
- [ ] Screenshots reviewed
- [ ] Release notes prepared
- [ ] Cafe Bazaar AAB/signing requirements validated

## 7. Rollout
- [ ] Internal test
- [ ] Closed test
- [ ] Open test
- [ ] Production
- [ ] Crash/performance monitoring active
- [ ] Rollback path documented
