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

The import-fix commit (`a2cf94dd87e45e83f814036bfc3722a7ebecb815`) compiled and passed Android instrumentation and backend integration in run https://github.com/danialrad04-sketch/studentos/actions/runs/39465023448. Unit/UI execution completed 206 tests: 204 passed, one failed and one skipped. The failing existing drawer test showed Settings was below the first viewport. Settings is restored above the searchable destination list and the account card retains the explicit account label; the navigation regression test now checks Settings without scrolling. All 17 newly added UI tests passed in that run. The corrected drawer still requires a fresh green run. Visual outputs are retained in the run's studentos-visual-review artifact; they have not been visually signed off from this disconnected local environment.

## Academic journey and study activity follow-up

The home journey now shows curriculum-derived passed/current/remaining credits, GPA provenance, and the independently stored GPA target. The previous per-course percentages were fixed display data and have been removed; course rows now show recorded sessions, tasks and absences. The journey is near the top of home and adapts its facts/actions to large text.

Study settings now include a daily study goal, a default focus duration and a GPA target. Completed focus sessions contribute to daily totals and a seven-day journal (up to 90 daily records retained). Completion claims and their history updates use one durable preferences commit and a shared lock, including when a worker and a recreated repository compete. A delayed worker attributes time to the completion date. Today refreshes when the local date changes.

Study settings/history are included in manually exported backups. Invalid study metadata is rejected before database import; legacy backups without that field remain supported. Restoring a backup leaves the timer idle. Clearing academic/account choices clears study history and goals.

Drawer destinations scroll independently in normal portrait windows. Large-text footer actions use full-width rows; short windows scroll the whole drawer. Course schedule labels adjust arbitrary user colors to at least 4.5:1 contrast against the actual badge surface, and schedule cards can grow vertically with text size.

Validation for this follow-up includes navigation/search, light/dark large-text renders, semantic and custom-color contrast, preference recreation, duplicate completion claims, delayed completion, and backup validation. Final results and artifacts are recorded in PR #21 against its latest commit; earlier CI results above describe earlier revisions only. Online AI and device-specific behavior still require configured credentials and real-device testing.
