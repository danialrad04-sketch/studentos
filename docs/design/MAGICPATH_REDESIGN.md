# Student OS — MagicPath design implementation

Design project: `458702222482640896`. Initial component/revision: `458702301721423872` / `458702301721423873`.

The interactive design reference covers home, schedule, tasks, grades, account/settings and course workspace. Its data is illustrative, not a live account. The React/CSS source lives in `magicpath/src/components/generated`; the Android UI remains native Compose.

## Visual system

- Light: lavender primary `#6D4DCC`, pale container `#EDE6FF`, cool canvas `#F7F5FC`, white surfaces, ink `#252137`.
- Dark: petrol canvas `#0C1B25`, surfaces `#142936`, lavender primary `#CBBEFF`, cyan secondary `#A8D8DF`.
- Preserve green/amber/red for semantic status. Preserve saved course colors and data.
- Use existing Vazirmatn typography, zero Persian tracking, generous line height and accessible controls.
- Keep summaries bounded by line count; provide a full read-only detail sheet rather than clipping glyphs to a fixed height.

## Native implementation

Apply shared theme/shape/token changes across existing Material screens. Put next class first on the dashboard, avoid the duplicated class preview, improve metadata and responsive KPI groups. Keep navigation callbacks and all module entry points. Let schedule cards grow with supported system text scaling.

Reflow workspace metrics, actions, scores, sessions and attendance using actual content width and font scale. Keep edit/delete in a contextual menu. Preserve access to tasks beyond the first ten and to complete notes, professor names and task titles.

## Validation

Run existing unit/lint/instrumentation/backend gates for this feature branch. Add regressions for task eleven, read-only long notes and large-text control reflow; render light/dark workspace images alongside existing dashboard/tasks/settings screenshots. Review actual rendered output before merge. Preserve database/authentication/backend/signing and the public offline-access policy.

Figma is not connected at the start of this work; no Figma file creation is claimed. The connection was offered separately. This branch does not authorize production release publication; main workflow completion may trigger the existing automatic publisher.
