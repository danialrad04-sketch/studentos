# Student OS v2.0.0 — Release Notes

## Product
Student OS 2.0 is the first product-grade UX and capability evolution release built on the stable 1.3 architecture.

## Experience
- Unified account entry with Guest, Email/Firebase and Google paths.
- Context-first Dashboard with a Today command strip.
- Context-first Schedule with day summary and clearer course presentation.
- Course Workspace 2.0 carried forward and integrated into the new interaction language.
- Task Center gains a contextual summary for open, overdue and exam-linked work.
- Exam Center shows preparation progress from related course tasks.
- Refined navigation and information hierarchy.

## Intelligence
- Added a shared deterministic Academic Context Engine.
- Dashboard and task/exam experiences can consume one consistent academic snapshot.
- Kept AI as an explanation/command layer rather than a source of truth.

## Authentication
- Google authentication continues to use Android Credential Manager + Firebase Authentication.
- Google Web Client ID remains the existing production client.
- Production signing certificate is intentionally unchanged:
  - SHA-1: b25fe31884ec18a96ae09e6fc0070fd20717a766
  - SHA-256: a335e71031ec78a7961656b2c36d8c53abcb3bdc28e265998cf6053fb94eb950

## Release
- versionName: 2.0.0
- versionCode: 5
- targetSdk: 36
- Android App Bundle (AAB) is the primary Play distribution artifact.
- Release CI verifies APK signing, package version and 16 KB native alignment.

## Safety
- No intentional Room schema migration.
- No intentional backend API contract break.
- No change of production signing identity.
- Core offline-first data model remains the source of truth.
