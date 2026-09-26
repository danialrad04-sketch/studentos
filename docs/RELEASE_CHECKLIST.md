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
- [ ] Debug build succeeds
- [ ] Release APK succeeds
- [ ] Release AAB succeeds
- [ ] Signing verifies successfully
- [ ] AAB package/applicationId is correct

## 3. Tests
- [ ] Unit tests pass
- [ ] Integration/data tests pass
- [ ] Authentication flows pass
- [ ] Critical Compose/UI flows pass
- [ ] Room migrations pass
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
- [ ] Reduced motion checked

## 5. Performance
- [ ] Startup benchmark checked
- [ ] Critical screens free of obvious jank
- [ ] Memory checked on target device profile
- [ ] Unnecessary network calls removed
- [ ] APK/AAB size reviewed

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
