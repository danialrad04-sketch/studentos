# Student OS — frosted navigation and personal dashboard

Baseline: main b53f209234e6f84b82827d982681bfa4de3c81e6 (2.8.0 / 2017).

## Design references and decisions

Read nextlevelbuilder/ui-ux-pro-max-skill at the recorded revision below, including SKILL.md and native pro-rules.md. Its generated design-system search returned a marketing pattern that did not fit a native student app; a targeted style search verified Glassmorphism, and the Jetpack Compose search verified UI-owned animation APIs. Apply those principles using this repository's actual Compose BOM 2024.09.00, not the catalog's newer platform version.

Keep the approved olive/cream and navy/petrol palettes and Persian Vazirmatn typography. Use a consistent Rounded vector family for structural navigation; retain the existing custom sticker family in academic content. No external artwork, web CSS, generated palette, font or framework dependency is imported.

The bottom bar is an inset floating capsule with five stable destinations, equal Android touch targets, accessible selected-tab semantics, RTL-aware moving selection and brief interruptible icon/color transitions. System animation preferences are respected. At large font scale the grade label uses the concise «نمره» so a narrow phone never leaves the final Persian letter on a separate line. Content padding includes the full bar and system navigation inset once. Tablet/short landscape continues using a scrollable rail with both account and the tool hub reachable. The legacy alternate bottom-bar function delegates to the same implementation to prevent visual drift.

The shared frosted surface uses high-opacity translucent layers, diffuse static canvas color, a reflective gradient edge and restrained elevation. It is a glass-inspired material, not live bitmap screen capture or a promise of real-time backdrop blur. Text and form contents are never blurred. Transparent scaffolds explicitly provide onBackground so unwrapped text retains the correct foreground in both themes. This works on the existing minSdk 24 without adding a GPU-heavy blur dependency. Reading-heavy academic cards keep semantic surfaces; shared shape tokens, headers, navigation, account and settings use the updated material family.

## Account information architecture

A real ACCOUNT destination opens from the Home avatar, drawer identity and feature hub; the five existing phone destinations remain stable. The top-level page shows persisted profile identity, actual current-term course/open-task counts, and working routes to their source lists. Theme choices update the same saved preference as Settings and reflect changes from either entry. At large text/narrow width, theme options stack instead of shrinking.

Study defaults, notification preference, profile editing, backup, account management, support/privacy and full settings use the existing production commands/dialogs. Section-specific settings entry preserves parent/back behavior. Existing local/online account access and billing policies are unchanged: the page does not promise login, live cloud sync or AI service availability. No new tables, schema changes, permissions, providers or signing identities are needed.

## Verification

Focused UI tests cover real routes, radio selection, notification state, long Persian identity, dark 150% text, phone navigation at 320dp/200%, and account/tool access on a tablet rail. A native production-navigation test edits the Room profile, persists the theme, recreates the Activity and exercises section/back navigation. Existing root-viewport tests automatically cover the new destination. Inspect exact-revision screenshots and signed/R8 install/upgrade evidence before delivery; passing emulator checks do not certify physical devices or live services.

UI UX Pro Max source revision: 28b3b8dee8c976df4fa3e94aed4e092ad43fcb03
