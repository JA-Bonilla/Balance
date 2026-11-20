---
slug: balance-android-port
platform: android
result: PASS
status: applied
---
## Verified
- AC-1: PASS. `./gradlew assembleDebug testDebugUnitTest --rerun-tasks` succeeds (round 3, clean rerun). Dark theme text readable.
- AC-2: PASS with accepted deviations. No INTERNET, allowBackup false, TimerReceiver not exported, every PendingIntent FLAG_IMMUTABLE. The new exact-alarm permissions do not break the manifest tests (they only check INTERNET and exported components). Library components exported by Health Connect and ProfileInstall are not in the plan's list (accepted).
- AC-3 to AC-10, AC-13, AC-16, AC-17, AC-18, AC-19, AC-22: PASS by unit test (19 tests, 0 failures).
- AC-11: PASS. Clean install (pm clear): USE_EXACT_ALARM granted at install, notification permission prompt shown at first timer start, alarm scheduled as RTC_WAKEUP window=0 exactAllowReason=policy_permission, removed on Stop. With the emulator clock moved to just before the end, the alarm fired at the exact end time (12:25:24), the timer was logged, and "Timer done / Reading: 15 min logged." was posted on channel timer_chime (importance high, sound raw/chime). Actual audio from the notification could not be heard on the emulator.
- AC-12: PASS. Picking Bell started a MediaPlayer in the app (seen in dumpsys audio) and saved timerSound=bell. Channel sound for the notification is the chosen sound's raw resource.
- AC-14: PASS. Minutes 00 to 55 in 5s; hour and minute lists stop at now (minute list limited to :25 in the current hour); End set before Start snaps to Start.
- AC-15: PASS. Sheet opens on last side/activity; switching to Growth picks Reading; presets 15m to 4h plus Custom shown; stepper moved 30 to 35.
- AC-20: PASS. With animator duration scale 0 two screenshots 2 s apart were byte-identical; with it on they differed.
- AC-21: PARTLY VERIFIED. Toggle, permission flow and grant/deny verified in round 2. Importing real Health sessions NOT VERIFIED (no Health Connect data on the emulator).
- AC-23: PASS (clean install onboarding, pick Reading, 15 min x 5 d, Start, Home with empty state, timer buttons, running timer row).
- AC-24: PASS for editing and adding goals (Settings shows goals list with Add goal). Delete-last-goal block covered by unit test.
- Swipe delete: PASS. A slow swipe on a Recent row shows the red delete background only under the moving row and clipped to the rounded corners; releasing past the threshold deleted the entry and totals dropped (1h 16m to 1h 1m). Not swiping: no red visible.
- UI nits: Settings cards now have rounded top corners; unselected Growth/Fun segment has a visible border and light text.
- Lint: not run (not defined).

## Behavior that differs from the plan
- Compose BOM 2026.06.01 instead of 2026.11.01. Accepted.
- Built-in fun list is named Category.fun_. Accepted.
- Timer alarm is always exact. Manifest now declares USE_EXACT_ALARM plus SCHEDULE_EXACT_ALARM (maxSdkVersion 32). Replaces the plan's inexact fallback.

## Bugs found, with status
- Near-black text on dark background: fixed in round 2.
- Health re-import duplicate and future hours in time dialog: fixed in round 1.
- Late timer notification (inexact alarm): fixed in round 3.
- Red swipe background at row corners, square Settings card corners, faint unselected segment, minute list in current hour: fixed in round 3.
- Open, accepted by John: exported Health Connect components; Android 8-10 external storage readability; alarm lost on reboot; Health session deleted before the window start is not removed.
- Open item for John: USE_EXACT_ALARM is restricted by Google Play to alarm and timer apps. Balance's timer fits, but Play review may ask for a declaration.
