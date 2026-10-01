# Student OS v2.0.1

## UI Stability
- Fixed uncontrolled growth of information cards caused by long dynamic text.
- Summary titles and descriptions now use bounded rendering.
- Long Markdown and AI responses use bounded scrollable viewports.
- Course Workspace, Command Center, Notifications, Settings, OCR, Onboarding, Gamification and semester information surfaces were hardened.
- Long-row actions retain their space through weighted layouts.
- Added shared AcademicInfoText and AcademicBoundedCard primitives.

## Entry Experience
- Local Guest mode is now the default entry path.
- Firebase/Google account and cloud sync remain optional.

## Authentication
- Production signing identity is unchanged.
- SHA-1: b25fe31884ec18a96ae09e6fc0070fd20717a766
- SHA-256: a335e71031ec78a7961656b2c36d8c53abcb3bdc28e265998cf6053fb94eb950

## Release
- versionName: 2.0.1
- versionCode: 6
- targetSdk: 36
- AAB is the primary Play artifact.
- Release CI verifies certificate and 16 KB compatibility.

## Product
- Expanded v2 roadmap covers context graph, command Copilot, planning intelligence, recovery, accessibility, performance and release engineering.