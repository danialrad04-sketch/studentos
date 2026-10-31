# V3 interface polish

Baseline: main `de9e1a03e81dead04997bf4f6c93dc6a8f88ba5d` (3.0.0/2020).

## Changes

- The initial tour uses the live theme for its canvas, cards, field surfaces, text and buttons. Removed fixed navy field backgrounds that conflicted with light text tokens. Student number and target GPA stack vertically, the panel is bounded on wide windows, and the keyboard reduces the usable panel area.
- Library, project, question, reader and search-detail dialogs share the existing workspace background, explicit edge-to-edge inset ownership, a maximum content width and flexible titles. Colored sticker accents occupy a small leading tile instead of a second full-width card inside each card.
- Settings headers use the workspace canvas instead of an opaque contrasting strip.
- Project estimates, practice duration, correct-choice index and reader page number request numeric keyboards. Practice duration explains its range inline and prevents starting with an invalid value or while busy; Persian/Arabic digit normalization remains supported.
- Project/question editor save buttons show a spinner and saving label while the existing persistence operation runs. Persistence and error recovery remain owned by the existing ViewModel.
- Study planning and practice introductory guidance can be acknowledged once on this installation. Later visits show a small explicit help action; users can reopen it. This preference contains no academic/account data. Suggestions that represent real scheduled work are not silently discarded or timed out.
- Course-title suggestions keep a flexible label beside their expand button. Weekly course workspace buttons have a minimum 48dp height and contrast-adjusted foreground on the actual course accent.
- Assistant code blocks use theme surfaces, outlines and foreground instead of fixed dark colors.

## Scope and verification

Source inspection covered shared theme/typography/adaptive components, modal/settings/navigation surfaces, V3 workspaces, planning, course editing, weekly schedule, onboarding/tour and assistant rendering. This is targeted refinement of the current visual system, not a claim that every possible screen/state has been physically tested.

Final local Robolectric run: 49 tests, 0 failures/errors/skips. Covers InterfacePolishTest, SettingsExperienceTest, StudentV3ExperienceTest, LearningExperienceTest, NavigationAndHomeExperienceTest and ReleaseVisualTest. The dialog stress test sets the system font scale to 2.0 and asserts the actual density inside the dialog, rather than assuming an outer CompositionLocal propagates to another window.

Inspected screenshots in `app/build/outputs/interface-polish/`: narrow 320dp light/dark dialog at 200% font, light tour form, light settings, profile form and dark project workspace. The first rendered dark dialog exposed a missing foreground token on its transparent Surface; fixed and rerendered successfully. Existing baseline settings/project images were compared before regeneration. Images are fresh captures, not replacements for golden comparisons.

Physical-device keyboard/system-bar behavior has not been exercised in this local run. Native CI remains a separate check. Standalone `:app:lintDebug --no-parallel` passed with all checks enabled. The initial combined test/lint run encountered an internal IntelliJ/Lint service exception; no check or detector was disabled. No database schema, backend/authentication contract, signing identity, version number or sticker assets changed. No store release is published by this change.

## 3.0.1 / 2021 layout follow-up

Course cards now separate the full title, overflow menu, wrapping schedule/status badges, professor/location and workspace action. Task cards give the complete title/course/deadline their own rows and use an explicit completion checkbox. Shared summary titles/meta allow two lines; body height follows actual font metrics rather than a separate height cap. Course-workspace edit/delete actions move below the identity header.

The weekly matrix sizes rows from their content instead of clipping everything into 76dp. Each overlapping course opens its own destination. Time-slot boundaries include real session start/end times, covering midday and early/late classes that the old fixed five slots omitted. Version and candidate packaging are now 3.0.1/2021; no publisher is dispatched. User explicitly authorized merge after checks and delivery of an installable APK.
