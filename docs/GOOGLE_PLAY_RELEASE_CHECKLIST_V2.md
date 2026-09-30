# Google Play / Production Release Checklist — Student OS v2

## Build & signing
- [x] targetSdk 36 / Android 16 target
- [x] versionCode 5
- [x] versionName 2.0.0
- [x] AAB build configured
- [x] APK build configured for test/debug distribution
- [x] Release keystore remains unchanged
- [x] Release certificate SHA-1 pinned
- [x] Release certificate SHA-256 pinned
- [x] 16 KB APK ZIP alignment gate
- [x] 16 KB ELF PT_LOAD alignment gate

## Authentication
- [x] Firebase Authentication remains enabled
- [x] Credential Manager Google flow
- [x] Existing production Web Client ID retained
- [x] Existing Android package name retained
- [x] Existing production signing certificate retained
- [ ] Physical device Google sign-in smoke test before Play submission

## Account & data
- [x] In-app account deletion exists
- [x] Sign-out exists
- [x] Cloud user-data deletion is attempted before Firebase account deletion
- [x] Guest mode exists
- [ ] Public HTTPS account-deletion URL must be entered in Play Console
- [ ] Public HTTPS privacy-policy URL must be entered in Play Console
- [ ] Play Console Data Safety form must be reviewed against the final build
- [ ] Content rating questionnaire must be completed/revalidated

## Quality
- [x] Unit tests
- [x] Android lint
- [x] Backend unit tests
- [x] Backend integration tests
- [x] Android instrumentation smoke tests
- [x] Signed release artifact validation
- [x] 16 KB compatibility validation
- [ ] Physical device accessibility/performance pass

## Release process
- [ ] Merge v2 PR only after all checks are green
- [ ] Create immutable tag v2.0.0 on the merged main commit
- [ ] Publish GitHub Release
- [ ] Upload AAB to Play Console
- [ ] Review Play Console pre-launch report
- [ ] Submit staged rollout


## CI
- CI canonical feature validation is executed on the pull request; direct feature-branch pushes do not create a duplicate Android CI run.
