# Android implementation notes: balance-android-port

Plan: `docs/plans/balance-android-port.md`. Contract: `docs/contracts/balance-json.md`. Branch `android/balance-android-port`, base `plan/balance-android-port`.

## What was built
All files under `android/` (package `com.jabonilla.balance`), one file per iOS twin: `Models.kt`, `Store.kt`, `BalanceApplication.kt`, `Theme.kt`, `Icons.kt`, `MainActivity.kt`, `HomeScreen.kt`, `BalanceView.kt`, `LivingSky.kt`, `Bonsi.kt`, `LogSheet.kt`, `TimerSheet.kt` (also `TimerSound`, `TimerAlarm`, `TimerReceiver`), `OnboardingScreen.kt`, `SettingsScreen.kt`, `HealthSync.kt`, `PrivacyActivity.kt`. 60 Material Symbol drawables (white fill, tint attribute removed), 4 wav files copied from `Balance/Sounds/`, adaptive launcher icon, `THIRD_PARTY_NOTICES.md`, Gradle wrapper (9.9.2).

## Deviations from the plan
- Compose BOM is `2026.06.01` (Compose 1.11.4, material3 1.4.0), not `2026.11.01`. BOMs 2026.09.00 and newer pull in libraries that need compileSdk 37 (AAR metadata check fails), and the plan fixes compileSdk 36. All other versions are as planned (AGP 9.5.2, Kotlin 2.4.21, activity-compose 1.13.0, connect-client 1.1.0, serialization 1.13.0, JUnit 4.13.2). No `org.jetbrains.kotlin.android` plugin was needed.
- Built-in fun list is `Category.fun_` (`fun` is a Kotlin keyword). `Side` constants are `GROWTH` and `FUN`; JSON values stay `growth` / `fun`.
- Clouds are plain translucent ovals (iOS blurs them by 3 px; not visible at that size). Other glows use radial gradients as planned.
- Rename-failure message (not in the plan): `Couldn't read balance.json and couldn't set it aside. Changes won't be saved this session.`
- `TimerAlarm.finishIfDue` (alarm receiver and the open app's end-of-timer effect) logs the due timer and posts "Timer done", so the notification still appears when the app is open. App resume only calls `store.finishTimerIfDue()`, as planned.
- Merged manifest also contains library entries the plan did not list: the Health Connect client's exported `HealthDataSdkService` and the profileinstaller `ProfileInstallReceiver` (exported, guarded by `android.permission.DUMP`). Not removed because the Health service may be needed for Health Connect to work; confirm on a device before stripping it with `tools:node="remove"`.

## Unit tests (`app/src/test/java/com/jabonilla/balance/BalanceTest.kt`)
`ac3_balanceScore`, `ac4_weeklyTotalsUseCurrentWeekOnly`, `ac5_iosFileLoadsIntact`, `ac6_legacyFileLoads`, `ac7_writtenFileFormat`, `ac8_saveAndReload`, `ac9_unreadableFileIsSetAside`, `ac9_failedRenameBlocksSaving` (makes the folder read-only; skipped if the runner is root), `ac10_timerLogic`, `ac13_customRounding`, `ac16_customActivities` (store behavior; the 30-character limit and trim are in `ActivitySheet`), `ac17_iconsAndSounds`, `ac18_bonsiStage`, `ac19_skyParamsAndCaption`, `ac22_healthIdsAreStableAndStayDeleted`.

## Commands (run from `android/` with JAVA_HOME=/opt/homebrew/opt/openjdk@17, ANDROID_HOME=/opt/homebrew/share/android-commandlinetools)
- `./gradlew assembleDebug`: success
- `./gradlew testDebugUnitTest`: 15 tests, 0 failures
- Lint: not defined for Android in `CLAUDE.md`, not run.

## Device checks: NOT run (no emulator or device; disk is tight)
AC-11, AC-12, AC-14, AC-15, AC-20, AC-21, AC-23, AC-24 and the visual check of the sky and Bonsi need a device. AC-1 is covered by the two commands above. AC-2 was checked on the merged manifest (`app/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml`): no INTERNET, `allowBackup="false"`, `TimerReceiver` not exported, only `MainActivity`, `PrivacyActivity` and its alias exported from the app's own manifest (plus the two library entries above). PendingIntents all use `FLAG_IMMUTABLE`.
