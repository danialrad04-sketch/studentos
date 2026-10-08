# Student OS — MagicPath design implementation

Design project: `458702222482640896`. Component/current revision: `458702301721423872` / `458821759857205248`.

The interactive design reference covers home, schedule, tasks, grades, account/settings and course workspace. Its data is illustrative, not a live account. The React/CSS source lives in `magicpath/src/components/generated`; the Android UI remains native Compose.

## Visual system

- Light: lavender primary `#6D4DCC`, pale container `#EDE6FF`, warm paper canvas `#F0EEE8`, white surfaces, ink `#252137`.
- Dark: petrol canvas `#0C1B25`, surfaces `#142936`, lavender primary `#CBBEFF`, cyan secondary `#A8D8DF`.
- Preserve green/amber/red for semantic status. Preserve saved course colors and data.
- Use existing Vazirmatn typography, zero Persian tracking, generous line height and accessible controls.
- Keep summaries bounded by line count; provide a full read-only detail sheet rather than clipping glyphs to a fixed height.

## Native implementation

Apply shared theme/shape/token changes across existing Material screens. Put next class first on the dashboard, avoid the duplicated class preview, improve metadata and responsive KPI groups. Keep navigation callbacks and all module entry points, with a transparent navigation container and selection indicators. Let schedule cards grow with supported system text scaling.

Reflow workspace metrics, actions, scores, sessions and attendance using actual content width and font scale. Keep edit/delete in a contextual menu. Preserve access to tasks beyond the first ten and to complete notes, professor names and task titles.

## Validation

Run existing unit/lint/instrumentation/backend gates for this feature branch. Add regressions for task eleven, read-only long notes and large-text control reflow; render light/dark workspace images alongside existing dashboard/tasks/settings screenshots. Review actual rendered output before merge. Preserve database/authentication/backend/signing and the public offline-access policy.

Figma is not connected at the start of this work; no Figma file creation is claimed. The connection was offered separately. This branch does not authorize production release publication; main workflow completion may trigger the existing automatic publisher.

## Study Desk overhaul — 2026-10-08

The second pass responds to flat, indistinguishable cards. The home screen now has saved Today / This semester views: daily next-class/actions/tools, versus recorded academic progress and course workspaces. A petrol timetable folio (`#193E43`) with a lime bookmark (`#DCEAAB`) anchors the daily view. White raised cards sit on a warmer, darker paper canvas; tonal action areas, defined borders and 1.5–4dp elevation establish hierarchy without blur. Tasks, schedule and workspace surfaces use the same edge/depth system. Course colors remain user data, not theme overrides.

Semester course rows show actual units and session counts. The previous hardcoded 75/60/40/80 percent rings are removed; Show all expands the real course list instead of opening the first course. Full-text routes, larger-font reflow, native navigation and all backend/auth/data behavior remain intact.

MagicPath is an interactive design reference; native Compose is the shipped implementation. Their content density differs with real data and accessibility scaling. The prototype is verified by its rendered revision above; native evidence must come from CI for this new commit (older passing runs are not evidence for this overhaul).
