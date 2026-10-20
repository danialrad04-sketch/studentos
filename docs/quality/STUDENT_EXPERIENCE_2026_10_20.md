# Student OS: home, navigation and assistant review

Review date: 2026-10-20. Base: `99dacc21b81033450c883e27b76bb397f7af5412`.

## Delivered changes

- Home has a first-step card based on whether courses exist, working schedule/task and assistant actions, and an optional search entry.
- Home no longer sends assistant actions back to the dashboard when optional callbacks are absent. Focus opens its workspace and starts only when appropriate.
- Pending tasks are labelled as pending tasks rather than today's tasks. Unknown curriculum totals do not appear as “0 out of 0” or a fabricated completion percentage.
- KPI cards stack at large text sizes.
- The drawer has a branded header, explicit close action, account card, searchable grouped destinations, selected-page state and distinct icons for all 14 destinations. Account, settings and support remain available.
- All internal pages have a short context description and a directly accessible menu button. Shared empty-state actions use readable typography and at least 48dp touch targets.
- Assistant UI uses theme-aware accents, actionable Persian errors and an explicit offline badge on the exceptional fallback path. Account changes reset the visible conversation and service status. Cancellation propagates instead of producing a fake fallback reply; a changed account cancels the prior request. Suggested prompts use actual study workflows instead of asserting specific university regulations, and Send is disabled during generation. Provider debug details stay out of chat.
- Server AI handling rejects unusable provider bodies and filters non-answer parts. Timetable rows are checked against Android's import requirements before completing the scan, allowing existing transaction logic to refund failed scans.

## Executed locally

| Check | Result |
| --- | --- |
| `node --test functions/test/premium-core.test.js` | 17 passed; includes all four conversation modes, malformed/empty/blocked/network failure responses, quotas and timetable validation. Provider and Firestore doubles are used; this is not a production AI check. |
| `npm test` in `backend` | 3 passed; PostgreSQL integration suite skipped because a disposable database was not configured. |
| `python3 -m unittest discover -s scripts -p 'test_*.py' -v` | 5 passed. |
| JavaScript syntax checks | Passed for the changed server module, functions entry point and premium service. |
| `git diff --check` | Passed. |
| Gradle `help` | Passed after installing Gradle, Android SDK and JDK in the temporary workspace. |
| Android unit/UI tests, debug APK and instrumentation compilation | Blocked before Kotlin compilation: environment proxy began returning HTTP 503 while downloading Maven artifacts. Offline retry confirms required AARs are absent. No local Android pass or visual sign-off is claimed. |

## Automated Android coverage added

`NavigationAndHomeExperienceTest` checks selection/closing for every destination, no-result search and support at large text in dark mode, plus working home schedule and assistant fallback routes and clearing the draft on account change. It captures light/dark RTL drawer and new-student home images.

`DestinationScreensSmokeTest` renders 13 internal destination surfaces at 360dp, 150% text and dark theme, checks their shared menu action and primary navigation, and captures each page. It handles the planner/history/chat's own scroll containers separately. Existing `ReleaseVisualTest` supplies home light/dark/large-text coverage; existing form, persistence and academic engine suites remain in place.

These tests are included in the repository's existing Android CI unit test job. Images and test reports are retained in the `studentos-visual-review` artifact when that job executes. A rendered screenshot still needs human inspection; a render test alone does not certify the whole user experience.

## Remaining external verification

- `ReleaseAccessPolicy.onlineAccountEntryEnabled` is false in the current public release. A newly installed guest cannot sign in to use online AI. Deployment, a configured server provider, Firebase/App Check and a real authenticated account are required before live AI can be certified. This change does not enable login, billing or deploy functions.
- Run and inspect the Android CI results and generated screenshots. No physical device, emulator, TalkBack, screen rotation or tablet execution was available locally.
- Check signed release startup, database upgrade, cloud synchronization and actual notification delivery using the existing release/instrumentation jobs and real-device checks.

This review improves shared navigation, home and assistant behavior. It does not certify every feature complete or every screen visually approved.

## CI observation

The first PR commit (`dcaad97592e8106a34794c0ca0279487d17927a1`) passed Premium Firebase verification, including the Firestore emulator transaction and security-rule step: https://github.com/danialrad04-sketch/studentos/actions/runs/39463750805. Android CI was still queued at the time of this update. Subsequent commits require their own results; this does not certify a live provider connection.
