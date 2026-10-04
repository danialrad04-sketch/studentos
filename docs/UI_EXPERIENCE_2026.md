# Student OS 2.2.0 — settings, account and modal experience

Implementation review: 2026-10-04. Brand direction is the user's latest olive accent on neutral light / navy dark surfaces.

## Research used

- [Material 3 in Compose](https://developer.android.com/develop/ui/compose/designsystems/material3): hierarchy, shape, color and purposeful motion; Expressive is an expansion of Material 3, not a requirement to add decoration everywhere.
- [Google's May 2026 adaptive guidance](https://android-developers.googleblog.com/2026/05/android-adaptive-development-ecosystem.html): responsive window sizes and input, Compose-first development.
- [Official Android adaptive skill](https://github.com/android/skills/blob/main/jetpack-compose/adaptive/SKILL.md), revision `02f939443d7495aa132c7ba83a0f264f090c4e13`: review different form factors and prefer edge navigation on larger windows. This app currently uses Navigation 2 / explicit state; only compatible screenshot/window-size guidance was applied. Navigation 3 scenes and experimental Compose Grid/FlexBox are not introduced by this UI change.
- [Native bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets) and [dialogs](https://developer.android.com/develop/ui/compose/components/dialog): native dismiss/drag/focus behavior, simple confirmations, dedicated containers for complex forms.
- [Compose accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults): minimum touch sizes, merged semantics and selectable groups.
- [Animation guidance](https://developer.android.com/develop/ui/compose/animation/quick-guide): interruptible spring motion and appropriate transitions; respect the system animation preference.

## Implemented experience

| Area | Result |
| --- | --- |
| Navigation | Persian modal drawer with identity, account, settings, all academic destinations and support. The home menu and bottom/rail More control open the same drawer. Tablets use the existing rail based on available window width. |
| Settings | Dedicated full-page destination with appearance, reminders, data/recovery and about/support categories. Long content scrolls below a fixed back/title bar. Version is read from BuildConfig. No fake roadmap completion or hard-coded version card. About provides copyable build identity for support instead of APK/keystore developer instructions. |
| Account | Dedicated guest/connected account destination; academic profile, subscription status, backup, privacy and support. Guest sign-in reuses AccountGateV2, the production startup form. Sign-in actions use the same Firebase handlers as startup. |
| Sync | One provider-aware ViewModel sync action, an in-progress disabled button, actual success/error feedback and retry. The displayed timestamp updates on successful manual sync. Previously the account dialog issued both cloud and backend requests. |
| Destructive controls | Explicit confirmations describe the actual local-cache purge on sign-out and irreversible delete/reset. Guest accounts do not expose account deletion. Connected accounts expose the existing email password-reset action. |
| Shared modals | Active form/information sheets use one native Material modal shell with consistent title, subtitle, 48dp close control, surface, width and bounded height. Native scrim/back/drag behavior; cancel buttons wait for hide. Ticket detail keeps its own contextual back control. |
| Other windows | Course workspace, command center, tour and Jalali calendar share restrained fade/scale motion and delayed dismissal. Native AlertDialog confirmations remain native. |
| Forms | Shared family captions raised to 14sp, main controls at least 48dp and long modal titles given flexible width. Profile uses a full-page form with separate action row, saved draft state and Persian/Arabic numeric input validation; optional student ID. Task form rewritten with a scrollable field area and separate action row; Jalali due date defaults to today, rather than a hard-coded date. |
| Motion | Settings section transition uses a short fade/settling spring. Custom windows avoid bounce and honor disabled animations. Reduced-motion preference observes runtime system changes. Native sheets use Compose's system duration scale. |
| Return path | Settings/account child destinations keep a parent stack, so closing profile/backup/privacy returns to the entry destination and settings category. Logout/delete clear the stack. |

## Verification

New Robolectric tests cover compact light/dark settings, 150% text, radio semantics, destructive confirmation, actual build identity, guest reuse, sync failure/retry, task validation/current date, a short window, profile/course forms, drawer destinations and tablet rail. Images are written to build output, not to existing golden references. Device instrumentation additionally exercises settings return and task save with an open keyboard. Existing navigation, domain/data, backend, lint and release identity/signing/16KB gates also run.

Local Gradle execution is blocked by this execution environment's network access to the Gradle distribution. GitHub Android CI is the build/test authority. CI results and screenshots must be reviewed before merge.

This is a UI and navigation overhaul. It does not claim completion of deferred master-plan features, production Google account configuration, multi-device synchronization acceptance or Cafe Bazaar approval. No database schema or signing identity changes.
