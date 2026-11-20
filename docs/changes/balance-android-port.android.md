---
slug: balance-android-port
platform: android
result: PASS
---
## Verified
- AC-1: PASS for build and unit tests (assembleDebug and testDebugUnitTest succeed; 19 tests, 0 failures, rerun in round 1). Manifest sdk values and app id checked in the merged manifest. Portrait and dark theme set by code review only.
- AC-2: PASS with accepted deviation. No INTERNET, allowBackup false, TimerReceiver not exported, every PendingIntent FLAG_IMMUTABLE (unit tests ac2_*). The merged manifest also exports library components: Health Connect HealthDataSdkService, and ProfileInstallReceiver (guarded by the DUMP permission). Not in the plan's list.
- AC-3, AC-4, AC-5, AC-6, AC-7, AC-8, AC-9, AC-10, AC-13 (rounding), AC-16 (store behavior), AC-17, AC-18, AC-19 (params and caption text), AC-22: PASS by unit test, and the Kotlin logic matches the iOS Swift source (balance score, rounding, week interval, legacy goal rule, timer cap, health replace).
- AC-11, AC-12, AC-14, AC-15, AC-16 (30-char limit and trim), AC-20, AC-21, AC-23, AC-24: NOT VERIFIED. They need a device or emulator (none available). Code reviewed only.
- Lint: not run (not defined).

## Behavior that differs from the plan
- Compose BOM 2026.06.01 instead of 2026.11.01 (compileSdk 36 limit). Accepted.
- Built-in fun list is named Category.fun_ (Kotlin keyword). No effect on the file format. Accepted.
- Library exported components listed under AC-2.
- Time dialog hour list is now limited to the current hour when the date is today (fixed in round 1). The minute list still shows all 5-minute marks; a later minute in the current hour is clamped to now.

## Bugs found, with status
- Health re-import duplicated an entry that straddles the 28-day window start (duplicate id, double count, possible Home crash): fixed in round 1. Store.replaceHealthEntries now also removes existing Health entries whose id is in the incoming list. New test ac22_reimportOfSessionBeforeWindowDoesNotDuplicate. Hidden IDs and non-Health entries are unchanged.
- Hour list offered future hours when date is today: fixed in round 1 (hours limited to now; result was already clamped).
- Open, left for John by decision: findings 2, 3 and 4 from round 0 (see the PR comment on round 0).
- Open, minor: minute list is not limited in the current hour; picking a later minute silently snaps back to now.
- Open, minor: a Health session before the window start that is deleted in Health is not removed (not in the incoming list, date outside window).
