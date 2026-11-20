---
slug: balance-android-port
platform: android
result: FAIL
---
## Verified
- AC-1: PASS. assembleDebug and testDebugUnitTest succeed (19 tests, 0 failures, clean rerun in round 2). Dark theme text is now readable on the emulator (Surface fix in a1501c4).
- AC-2: PASS with accepted deviation. No INTERNET, allowBackup false, TimerReceiver not exported (a shell broadcast to it was refused), every PendingIntent FLAG_IMMUTABLE. Library components exported by Health Connect and ProfileInstall are not in the plan's list.
- AC-3 to AC-10, AC-13 (rounding), AC-16 (store behavior), AC-17, AC-18, AC-19, AC-22: PASS by unit test.
- Verified on the emulator (Android 16 AVD, round 2): onboarding to home; log an entry (Growth, 1 hr); Bonsi and sky change after logging (Heat wave scene, Growth 1h, Fun 30m); Save disabled when Custom start equals end (AC-13); Add disabled for an empty name; 30-character name limit; name trimmed on save; add goal; add custom activity (saved as star.fill, balance.json matches the iOS schema); Settings screen; timer start and stop (alarm scheduled, then cancelled on Stop); notification permission prompt at timer start; five timer notification channels created; Health Connect toggle (deny leaves toggle off with an error dialog, allow turns it on); an expired timer in balance.json is logged on app open.
- AC-11: PARTLY VERIFIED. Permission prompt, channels, alarm schedule and cancel on Stop work. The alarm is inexact (see Bugs). The notification text and sound at timer end was NOT VERIFIED (15 minute minimum timer, receiver is not exported so it cannot be fired by hand).
- AC-12 (sound preview and next-notification sound), AC-14, AC-15, AC-20, AC-21, AC-23, AC-24: NOT VERIFIED beyond what is listed above. Not exercised on the emulator in this round (no Health Connect data on the device, no preview audio check possible).
- Lint: not run (not defined).

## Behavior that differs from the plan
- Compose BOM 2026.06.01 instead of 2026.11.01. Accepted.
- Built-in fun list is named Category.fun_. Accepted.
- On a fresh install SCHEDULE_EXACT_ALARM is not granted, so the plan's fallback (inexact alarm) is what every user gets.

## Bugs found, with status
- Near-black text on dark background: fixed in round 2 (a1501c4), confirmed on the emulator.
- Health re-import duplicate and future hours in time dialog: fixed in round 1.
- Open, should fix: timer-done notification can arrive up to about 22 minutes late (inexact alarm, exact-alarm permission never granted and no way to grant it).
- Open, minor: red swipe-delete background shows at the rounded corners of the Recent rows on Home even when not swiping.
- Open, minor: Goals and Custom activities cards on Settings have square top corners (the first row's swipe background is not clipped).
- Open, minor: unselected Growth/Fun segment is dark on a dark sheet; text is readable but the border is faint.
- Open, minor: minute list not limited in the current hour; Health session before the window start deleted in Health is not removed.
- Open, left for John by decision: findings 2, 3 and 4 from round 0.
