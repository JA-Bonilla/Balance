# Balance: product requirements

Balance is an iOS app (SwiftUI, iPhone only) and an Android app (Kotlin, Jetpack Compose). Both help you balance growth time against fun time, using one local `balance.json` file.

Status values:
- **Done**: implemented and verified.
- **Partly verified**: implemented; one or more parts are not verified on a device (listed).
- **Unit tested**: verified by JVM unit tests only.

Sources: plan `docs/plans/balance-android-port.md`, Android notes `docs/plans/balance-android-port-android.md`, change record `docs/changes/balance-android-port.md`, QA record `docs/changes/balance-android-port.android.md`, data format `docs/contracts/balance-json.md`.

## Requirements

| ID | Requirement | Status | Notes |
|---|---|---|---|
| FR-1 | The Android app offers every user-facing feature of the iOS app, adapted to Android conventions (Material 3, Health Connect, Android notifications). | Partly verified | Device checks pass for onboarding, logging, timers, goals, and Settings, and the sky animation (AC-20). Not verified: real Health Connect import (AC-21), notification audio (AC-11), delete-last-goal rule in the UI (AC-24). |
| FR-2 | Android reads and writes `balance.json` in the format defined in `docs/contracts/balance-json.md`, so a file moves between iOS and Android without loss. | Unit tested | AC-5 to AC-9 pass by unit test. The QA record does not list a device test with a file written by iOS. |
| FR-3 | Android imports Health Connect exercise sessions (as Exercise) and mindfulness sessions (as Meditation) from the last 28 days as growth minutes, when the user turns it on. | Partly verified | Toggle and permission flow verified. Real import NOT VERIFIED (no Health Connect data on the test device). Mapping and stable ids unit tested (AC-22). |

## User stories

| ID | Story | Status | Notes |
|---|---|---|---|
| US-1 | As an Android user, I can log growth and fun time, run timers, set goals, and see my weekly balance as a sky and a Bonsi, just like on iPhone. | Partly verified | Same caveats as FR-1. |
| US-2 | As a user switching phones, I can copy my `balance.json` from iPhone to Android (or back) and keep all my goals, entries, custom activities, and settings. | Unit tested | Format is covered by unit tests. A copy between a real iPhone and Android phone is not recorded in the QA record. |

## Acceptance criteria

| ID | Criterion | Result | Notes |
|---|---|---|---|
| AC-1 | `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` both succeed. App id `com.jabonilla.balance`, minSdk 26, targetSdk 36, compileSdk 36, portrait only, dark theme only. | PASS | Clean rerun in QA round 3. Lint not run (not defined). |
| AC-2 | Merged manifest has no INTERNET permission, `allowBackup="false"`, exports only the launcher activity and the Health Connect privacy entry points, the timer receiver is not exported, and every PendingIntent is immutable. | PASS (accepted deviation) | Accepted: Health Connect and ProfileInstall library components are also exported (see Accepted open items). |
| AC-3 | Balance score: 0 for (0,0), 0 for (60,300), 1 for (90,0), -1 for (0,45), and the listed values for (210,1260), (60,1500), (400,600). | Unit tested | PASS. |
| AC-4 | Weekly totals count only entries in the current week (first day from the device locale, device time zone), per side and per category. | Unit tested | PASS. |
| AC-5 | The iOS example in the data contract loads with every field intact. A file with only `{}` loads as empty defaults. | Unit tested | PASS. |
| AC-6 | The v0.1 legacy file loads as one goal with 12 minutes per day and 5 days per week, and empty `hiddenHealthIDs`. | Unit tested | PASS. |
| AC-7 | A file written by Android uses UTC dates in whole seconds, uppercase UUIDs, `blurb` and `archived` on every category, `source` on every entry, no `weeklyMinutes` key, and no `activeTimer` key when no timer runs. | Unit tested | PASS. |
| AC-8 | Saving a goal and an entry, then loading a new store from the same file, gives the same goal (weekly target 60) and entry. | Unit tested | PASS. |
| AC-9 | Invalid JSON shows the "Couldn't read balance.json" message, starts with empty data, and renames the file to `balance.unreadable-<unix seconds>.json`. If the rename fails, the app does not write `balance.json` for the session. | Unit tested | PASS. The rename-failure message differs from the plan (see Behavior that differs). |
| AC-10 | Timer logic matches iOS: a timer due while closed logs its full planned minutes dated at its start; a timer not yet due keeps running; stopping early logs elapsed minutes (round half up, capped at planned length); stopping under 30 seconds logs nothing. | Unit tested | PASS. |
| AC-11 | When a timer ends while the app is in the background or closed, the entry is logged and a "Timer done" notification (`<Activity>: <N min> logged.`) appears with the chosen sound. Tapping it opens the app. Stopping early cancels the alarm and notification. If notification permission is denied, the timer still logs. | PASS, notification audio NOT VERIFIED | On the emulator the alarm fired at the exact end time, the time was logged, and the notification was posted. The sound could not be heard on the emulator. |
| AC-12 | Settings offers System Default, Chime, Bell, Pulse, and Gentle (default Chime). Picking one plays a preview, the choice persists, and the next timer notification uses it. | PASS | Preview verified (Bell played). Notification sound is set through the channel for the chosen sound; audible result not verified (see AC-11). |
| AC-13 | Custom start and end time rounds to the nearest 5 minutes, half up (92 gives 90, 93 gives 95, 92.5 gives 95, 2 gives 0). Save is disabled at 0 or less. | Unit tested | PASS. |
| AC-14 | Every time picker offers only 5-minute steps. No picker allows a time after now. End cannot be set before Start. | PASS | Device check. |
| AC-15 | The log sheet opens on the last used side and activity. Switching side picks the first activity of the new side when needed. Presets 15m to 4h plus Custom are offered, the stepper moves 5 minutes within 1 to 1440, only manual entries open for editing, and any entry can be swiped to delete. | PASS | Device check, including swipe delete. |
| AC-16 | A custom activity appears only in lists for its side. Removing it hides it, but old entries still show its name. Names are trimmed and limited to 30 characters. Add is disabled when the name is empty. | Unit tested | PASS (store behavior, unit test). The trim, 30-character limit, and disabled Add button are UI rules; the QA record does not show a device check of them. |
| AC-17 | Every SF Symbol name in the icon lists maps to a Material Symbol, an unknown name maps to the circle icon, and the four sound files exist in `res/raw`. | Unit tested | PASS. |
| AC-18 | Bonsi stage: 0 with no entries, 0 at 59 growth minutes, 1 at 60, 2 at 600, 3 at 1800, and 0 at 1800 fun minutes. | Unit tested | PASS. |
| AC-19 | Sky parameters match iOS, and the caption under the sky uses the same 8 texts and thresholds as iOS. | Unit tested | PASS. |
| AC-20 | With "Remove animations" on, the sky and Bonsi draw a still frame and balance changes jump. With it off, the sky animates and balance changes ease over 1.4 s. | PASS | Device check: identical frames with animations off, different frames with them on. |
| AC-21 | In Settings the Health Connect toggle is disabled when Health Connect is unavailable. Turning it on asks for read access. If granted, Home imports the last 28 days on open and on pull to refresh, as "From Health" growth minutes. If denied, the toggle stays off and an error shows. Turning it off removes all health entries. | PARTLY VERIFIED | Toggle, permission flow, and grant or deny verified. Real import NOT VERIFIED (no Health Connect data on the test device). |
| AC-22 | A health entry the user deleted stays deleted after the next import. The same Health Connect record always maps to the same uppercase UUID. | Unit tested | PASS. |
| AC-23 | With no goals the app shows onboarding. Picking a growth activity and a target (5 to 240 min/day in 5-min steps, 1 to 7 days) and tapping Start creates the goal and shows Home with the sky, Bonsi, weekly totals, timer buttons or running timer, goal progress, and recent entries. | PASS | Clean install walkthrough. |
| AC-24 | In Settings, goals can be edited and added (new goal defaults to the first unused growth activity, 15 min x 5 days). Deleting the last remaining goal is not possible. | PARTLY VERIFIED | Editing and adding verified on device. The delete-last-goal rule is NOT VERIFIED in the UI (unit test only). |

## Behavior that differs from the plan

- Compose BOM is 2026.06.01, not the planned 2026.11.01. Newer BOMs need compileSdk 37, and the plan fixes compileSdk 36. Accepted.
- The built-in fun category list is named `Category.fun_` in code (`fun` is a Kotlin keyword). Code-only; no user-visible change.
- The timer alarm is always exact. The manifest declares `USE_EXACT_ALARM` as well as `SCHEDULE_EXACT_ALARM` (the latter limited to Android 12L and below). This replaces the plan's inexact fallback.
- Rename-failure message (not in the plan): "Couldn't read balance.json and couldn't set it aside. Changes won't be saved this session."

## Accepted open items

- Health Connect and ProfileInstall library components are exported in the merged manifest, in addition to the app's own exported components. AC-2 wording in the plan lists only the app's own components. Accepted.
- Android 8 to 10: other apps with storage access may be able to read `balance.json`. Accepted.
- Timer alarm is lost on phone reboot. The timer is still logged on next open; only its notification is lost. Accepted.
- A Health Connect session deleted before the 28-day window moves past it is not removed from the app. Accepted.
- Google Play restricts `USE_EXACT_ALARM` to alarm and timer apps. Balance's timer fits, but Play review may ask for a declaration. Accepted, to be handled at Play submission.
- Compose BOM 2026.06.01 instead of 2026.11.01 (see above). Accepted.

## Not verified

- Real Health Connect import (AC-21, FR-3, FR-1).
- Notification audio when a timer ends (AC-11).
- The delete-last-goal rule in the Settings UI (AC-24).
- Copying a real iPhone-written `balance.json` to an Android phone (FR-2, US-2).
- Lint for iOS and Android: TODO: confirm (not defined in `CLAUDE.md`).
