# Student OS 2.7.0 / 2016

Baseline: main `412b011abc4ae57a2e16290ff2148d4e48ee411d` (2.6.1 / 2015).

## Experience

- Keep the owner-approved paper-cut illustration family. Add six generated companion stickers: settings, profile, help, notes, history and curriculum. Original generated files remain preserved separately; Android selects source rectangles without rewriting image pixels.
- Use the same 24-item family in phone navigation, tablet rail, drawer identity/settings/support, home tools, module hub, introductory tour, notifications and achievements. Keep standard close, search, editing and directional controls recognizable native controls. User-authored text and assistant Markdown parser tokens are not rewritten.
- Bring olive/sage, petrol, terracotta and mustard into paired paper backgrounds and ink colors. Warm the global light canvas and container layers. Dark mode uses separate opaque containers and pale paired inks. Destination labels and native selection semantics remain; status is never conveyed only by color.
- Preserve current data, backend contracts, Firebase project/authentication and production signing identity. Room remains schema 12. No new permissions or network services.

## Functional academic tools

`GradeGoalEngine` computes weighted required scores only across unknown results, leaving entered totals fixed. It reports the maximum attainable GPA and unreachable targets, explicitly recorded zeros, all-recorded and no-data cases. Invalid records are excluded with a visible correction message. UI discloses that an entered midterm-only result must first be completed; stored records do not encode a separate finalization flag. The calculator does not pretend to know instructor weighting. A scenario never writes grades. The student can inspect included courses and copy the scenario summary. Target and expansion state survive recreation.

`ExamScoreEngine` is an additional independent calculator in the grades destination. The student enters already-earned points, the instructor's final-exam maximum and the desired total out of 20. It accepts Persian/Arabic numerals and decimal separators, validates the non-exam allocation and shows impossible targets without clamping to a false success. It does not encode instructor minimum-final rules, and explains that limitation. The empty state requires actual inputs rather than seeded grades. Inputs and expansion state survive recreation.

An empty actual gradebook displays an unknown GPA instead of zero/error styling.

## Verification

Focused coverage: weighted and impossible GPA goals, recorded zero versus unknown, missing/invalid data, exam-weight validation, Persian input, real navigation callbacks, scenario copy/no-write behavior, and WCAG 4.5:1 minimum text contrast for every sticker surface in both themes. Existing native Robolectric screenshots cover light/dark galleries, large text, drawer search/selection and home routes. New screenshots cover colorful home navigation and both academic tools.

Use exact-commit Android CI for native instrumentation, both lints, packaged policy/signature/alignment, backend/Firebase emulator tests and signed offline/install/update smoke. CI results, source revision and generated APK hash belong in the delivery evidence, not as hardcoded passing claims in this design document. Building this version does not activate online AI, live Google login, store publication or physical-device certification.
