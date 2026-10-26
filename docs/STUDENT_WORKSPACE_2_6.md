# Student workspace 2.6.0 / 2014

This development stage builds on the reviewed assistant source `f2d7e635aa5742b33c3bb23aaf9b79f2a1eae730` (PR #23). It does not publish a store or GitHub release or activate the AI provider. The published 2012 binary's exact source remains unconfirmed; selected-baseline provenance is documented in STUDENT_PLANNER_2_5.md.

## Weekly agenda

- A new destination combines the current semester's weekly class pattern, dated exams and open task deadlines. Saturday is day zero and Friday is supported. Week navigation crosses Jalali month/year boundaries. Today is selected by default; day chips and an all-week view keep the important records in reach.
- Classes are projections of the registered weekly pattern; term end dates and holidays are not supplied and are explicitly not confirmed. Exams and tasks retain their recorded dates. Missing or invalid times, locations and task dates remain unknown; completed work is omitted. Overdue and undated tasks have separate recovery routes.
- Filters and summary counts use the actual snapshot. Known class overlaps and exam start times during a class are labeled with text; missing exam end times are never guessed.
- Calendar export takes an immutable snapshot of the full selected week. ICS uses Tehran-to-UTC timed events, exclusive all-day ends, per-owner/data-generation stable identifiers, escaped text and UTF-8-safe 75-octet folding. Export neither subscribes to a live calendar nor requests calendar permissions. The student chooses the destination using Android's document picker and imports the file into their calendar app.
- Pending export survives activity recreation in the ViewModel, checks account/generation before writing, and reports success after the stream closes. A lost process requires another export; a missing request never produces a fake success message.

## Course notebook

- The previously existing NoteEntity storage now has a full screen for create/read/edit, title/body/course search, course filters, archive and restore. General notes and historical course links are retained. Search normalizes Persian/Arabic letter and digit variants. Lists reveal more results in batches rather than composing an unbounded number of cards.
- Saving validates title/body and course linkage in a Room transaction. A stale timestamp cannot overwrite a newer sync/edit. Archive/restore also checks the displayed version. The UI waits for actual persistence acknowledgement, keeps failed drafts, validates oversized content without silently truncating it, and scopes editor/search restoration to the current owner.
- Archive is stored on the canonical note instead of hard-deleting a record that an older cloud copy could resurrect. Room 11→12 adds isArchived=false without replacing any rows. Schema 10→11→12 and legacy backup paths remain supported. Node snapshots/portable backups include the field; Firestore explicitly writes it and missing legacy fields preserve existing local archive state.
- The focus screen's existing quick notes in profile.notes remain available; they are not silently moved or duplicated into notebook records.

## Shared UI polish

- Both new destinations are reachable from the drawer/module hub and home shortcuts.
- A quiet theme-based canvas, bounded reading width and tonal page headers apply across the configured app. Existing olive/neutral light and navy/petrol dark palettes are preserved. When the drawer button is present, quick search moves into the overflow menu to keep the page title readable on narrow phones.
- Each animated destination owns its scroll layout. Chat, semester planning and semester history receive finite viewports instead of being nested inside an unbounded vertical scroller. Chat context scrolls with the conversation; its composer owns IME padding, hides optional prompt chips while typing and exposes cancellation at the send control.
- Assistant action states use semantic theme colors. Attendance alerts use an error container and the student's configured limit instead of asserting a universal university rule. The idle focus screen no longer runs a decorative perpetual glow.

## Verification

Domain/export, Room migration/backup/concurrency, owner draft boundaries, keyboard forms and rendered light/dark large-text states have dedicated regressions. A production-root test visits every destination and exercises actual note persistence; the device version also checks the real IME. Consult the exact-head CI and build evidence for completed results. No live AI activation, live multi-device cloud sync, physical-device certification or store approval is implied by mock/emulator results.
