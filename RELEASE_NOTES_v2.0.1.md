# Student OS v2.0.1

## UI/UX stability release
- Fixed uncontrolled growth of information cards caused by long dynamic text.
- Added bounded summary text with ellipsis.
- Added bounded scrollable viewports for long AI/Markdown content.
- Constrained Course Workspace, Command Center, Notifications, Settings, OCR previews, Onboarding summaries, Gamification and semester surfaces.
- Improved weighted layouts for rows containing long labels.
- Guest mode is now the default entry experience.

## Authentication
- Firebase/Google login remains optional.
- Production signing identity is unchanged.
- SHA-1 remains b25fe31884ec18a96ae09e6fc0070fd20717a766.
- SHA-256 remains a335e71031ec78a7961656b2c36d8c53abcb3bdc28e265998cf6053fb94eb950.

## Release
- versionName: 2.0.1
- versionCode: 6
- targetSdk: 36
- AAB is the primary Play artifact.
- Release CI keeps signing and 16 KB validation gates.

## Product planning
- Expanded v2 roadmap through v2.1 with context graph, command Copilot, planning intelligence, recovery, accessibility, performance and release engineering.