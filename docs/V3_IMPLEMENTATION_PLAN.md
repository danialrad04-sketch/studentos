# Student OS V3 — personal, offline-first student ecosystem

Approved scope: existing five phone destinations and original sticker assets remain. APK is capped at 100,000,000 bytes; user-imported documents are separate app data. No new paid API, online AI, classroom groups or instructor/server role is part of 3.0. Version name 3.0.0, proposed code2020 (verify distributed versions before cutting the final package). Baseline main4a5a72f8a794eaf8a9e2dfdad369954bf5587f6a (2.9.1/code2019/schema13).

## Delivery slices

1. One native visual language, useful states, performance baselines and bounded content.
2. Per-course PDF/image library, page progress, bookmarks and notes; content-addressed private files; full checked ZIP backup, legacy JSON support; projects, milestones/subtasks and existing-task links.
3. Manual question bank (single-correct choice and self-rated short answer), immutable timed practice attempts, resume, expiry and explicit mistakes-to-review; unified effort-aware scheduling for real task/project estimates without double counting; integrated search; three native widgets.
4. Light/dark/RTL/200% text/device and upgrade verification, comparable release performance evidence, <=100MB production-signed APK/AAB/BIN delivery. Store publication is separate.

## Durable contracts

Room13->14 is additive. Existing table IDs and authentication/backend identities remain. New attachment/bookmark/project/step/question/attempt tables share Room and account ownership. Task project links, actual completion timestamps and effort estimates are additive; legacy completions retain unknown timestamps. Practice question snapshots outlive question edits; list projections exclude question/answer JSON. New learning completions distinguish coursework from ordinary course goals.

Library uses Android PdfRenderer and image decoding, not a bundled renderer/LLM. Import streams off the UI thread; visible page bitmaps are bounded. File blobs are immutable SHA-256 addressed. Soft delete is recoverable; permanent purge retains a newer metadata tombstone and only removes unreferenced bytes. Full ZIP includes manifest+hashes+metadata+all non-purged blobs and validates everything before metadata commit. Newer local learning/V3 records win on merge. Legacy JSON remains metadata-only. Local recovery snapshots are atomic, owner-bound and coalesced in background work.

The new local fields are not a promise of cloud synchronization. Existing remote contracts are preserved; old cloud task restores retain local planning extensions. New data/file transfer is through full backup. All data-changing operations check workspace ownership. Bulk restore/reset/sign-out share a barrier so old forms and pending pickers cannot write into a new workspace.

## Experience

Library lives in the hamburger menu and course workspace. Projects live beside tasks, practice beside exams. Home prioritizes today, next action, priority and progress; three compact shortcuts and an expandable tools section keep navigation calm. Preserve approved stickers, current olive/petrol colors and Persian typography. New forms have fixed save actions, readable feedback and retained drafts. Scrolling controls clear the floating navigation. Widgets use the real Room data and existing focus deadline; no independent timer and no minute polling when idle.

## Gates

Retain the existing suite and add migration13->14, real ZIP/file round trips and corruption rejection, owner/stale edits, immutable/expired attempts, work budget and duplicate-work boundaries, full production UI paths and native PDF/widgets. Inspect actual new and existing screenshots, not just passing test counts. Signed/R8 smoke must exercise the new portable format and practice engine. Compare startup, frame p95 and memory against the same-emulator 2.9.1 source baseline; investigate >10% verified regressions before delivery. Validate identity, certificate, permissions, hashes, all ABIs and ZIP/ELF16KB alignment. Report emulator/physical/live-provider limits accurately.
