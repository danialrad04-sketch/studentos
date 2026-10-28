# Student OS 2.8.0 / 2017 — adaptive learning workspace

Baseline: main a10cd2b39f3b613dcb36a666d2c8187dc59c913f (2.7.0 / 2016).

## Information architecture

Keep all existing AppTab identifiers and bottom-navigation destinations. Home replaces the older generic recommendation panel with a compact next-study action and an optional due-review route. It does not repeat the planner or weekly report. The existing focus destination gains two clearly labelled panels: Focus and Study Plan. Today's sessions appear before any expanded settings; goals and completed-session history are collapsed until requested. Other days are selected explicitly instead of dumping seven days of cards into one page. The intelligence destination is labelled Progress and Reports and owns the weekly report. Course recall cards stay inside the corresponding course workspace, not in a new top-level feature menu.

## Persisted behavior

Room 12→13 adds four tables and leaves previous tables/data untouched: per-course goals, weekday availability, confirmed study completions and recall cards. There are deliberately no destructive foreign-key REPLACE cascades on frequently updated course rows; existing orphan cleanup handles actual deletion. Reset/account clearing includes all four tables. Archive/semester changes retain stable course identity. Commands check the current workspace generation inside the Room transaction. Stale card edits/reviews and duplicate completion submissions are rejected. Undo uses tombstones and newer backup revisions win, so an older file cannot resurrect an undone session or overwrite an archived card.

All new records are included in existing automatic local snapshots and portable JSON backup/restore. Old backups default these lists to empty and preserve existing new records. Validation precedes writes; repository restore uses the existing transaction. The cloud/server contracts are intentionally preserved: this version does not claim cross-device live synchronization of new learning records. The planner tells students to use file backup for transfer. No API or subscription is required.

## Planning rules

The student explicitly supplies total study minutes, difficulty and a target date for each course, plus a free-time interval and daily minute cap for each weekday. The seven-day projection subtracts confirmed work, elapsed time, current semester class intervals, occupied completed slots and per-day budgets. It leaves five-minute breaks. Missed work is never counted as done; the next projection redistributes the remainder before the actual deadline. Expired targets and insufficient capacity are visible, rather than silently extending an exam deadline or overbooking time.

Class occurrences are a weekly-pattern projection, not a confirmed holiday/term calendar. Missing or invalid class times produce an explicit warning. Exam durations are not stored, so the whole registered exam day is left unscheduled instead of inventing an end time. Planner dates and times use Tehran. Pressing Start Focus only starts the existing timer and cannot silently complete a study block. The selected block is durably attached to the existing focus-session preference, reserved from new projections, and retained after timer completion until explicitly recorded or released. It survives recreation and clears with existing account planning choices; active timers are still not restored from portable backup. Recording a completion separately asks for the student's actual minutes; future completions are rejected.

## Recall and reporting

Students create/edit/archive/restore course-bound questions and answers. Answers stay hidden until revealed. Again keeps a card due today; Hard/Good/Easy increase its next interval with a 365-day cap. A card is rated once per round; repeating a forgotten card requires a new round. Content changes reset its recall schedule with a visible explanation. No percentage is called objective mastery. Editor drafts survive activity recreation; failed writes retain the text. Management supports search and incremental pages.

The weekly report compares only recorded timer history across two seven-day windows. Confirmed planner minutes are shown separately because a timer and a completion can describe the same study. Recall counts unique cards reviewed in the window. Overdue tasks use real due dates; weekly task completions are not fabricated from a boolean without a completion timestamp. Empty, loading, retry and partial-data states avoid invented progress.

## Verification scope

Meaningful coverage includes schedule/class/exam/budget boundaries, rescheduling and capacity deficits, spacing/recall transitions, non-duplicated report totals, migration from published schema 12, backup round trips and legacy imports, tombstones/newer revisions, stale actions/account generation, deletion/reset, Persian input, draft recreation, failed saves and large-text light/dark layouts. A production-navigation Android device test saves a Persian study goal and rates a real Room-backed recall card. Exact-commit CI and artifact evidence accompany delivery; emulator checks do not imply physical-device or live provider certification.
