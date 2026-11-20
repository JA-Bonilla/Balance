# Android port of Balance (Kotlin, Jetpack Compose, Health Connect)

## Mode
Plan

## Summary
Build a native Android app under `android/` with full feature parity with the iOS app in `Balance/`: onboarding, home with living sky and Bonsi, logging, per-activity timers with background completion, notifications and sounds, goals, custom activities, settings, Health Connect import, and a `balance.json` file that follows the iOS schema exactly (`docs/contracts/balance-json.md`). iOS code does not change.

## Requirement IDs
`PRODUCT.md` does not exist yet, so no IDs are taken. This plan starts at FR-1, US-1, AC-1. The librarian must keep these IDs when it creates `PRODUCT.md`.

- FR-1: The Android app offers every user-facing feature of the iOS app, adapted to Android conventions (Material 3, Health Connect, Android notifications).
- FR-2: Android reads and writes `balance.json` in the format defined in `docs/contracts/balance-json.md`, so a file can move between iOS and Android without loss.
- FR-3: Android imports Health Connect exercise sessions (as Exercise) and mindfulness sessions (as Meditation) from the last 28 days as growth minutes, when the user turns it on.
- US-1: As an Android user, I can log growth and fun time, run timers, set goals, and see my weekly balance as a sky and a Bonsi, just like on iPhone.
- US-2: As a user switching phones, I can copy my balance.json from iPhone to Android (or back) and keep all my goals, entries, custom activities, and settings.

## Acceptance criteria
1. AC-1: `cd android && ./gradlew assembleDebug` and `cd android && ./gradlew testDebugUnitTest` both succeed. The app id is `com.jabonilla.balance`, minSdk 26, targetSdk 36, compileSdk 36, portrait only, dark theme only.
2. AC-2: The merged manifest has no `android.permission.INTERNET`, sets `android:allowBackup="false"`, and exports only the launcher activity and the two Health Connect privacy entry points. The timer receiver is not exported. Every PendingIntent is `FLAG_IMMUTABLE`.
3. AC-3: `balance(growth, fun)` returns 0 for (0,0), 0 for (60,300), 1 for (90,0), -1 for (0,45), and within 0.001 of -0.0909 for (210,1260), -0.6667 for (60,1500), 0.5385 for (400,600).
4. AC-4: Weekly totals count only entries inside the current week (first day of week from the device locale, in the device time zone), per side and per category. An entry one hour before the week starts is excluded.
5. AC-5: The iOS example file in `docs/contracts/balance-json.md` loads with every field intact: one goal, one entry, one custom category, the active timer, `healthEnabled` true, and one hidden health id. A file with only `{}` loads as empty defaults.
6. AC-6: The v0.1 legacy file in the contract loads as one goal with `minutesPerDay` 12 and `daysPerWeek` 5, and empty `hiddenHealthIDs`.
7. AC-7: A file written by Android has: dates like `2026-11-16T18:30:00Z` (UTC, whole seconds, no fraction), uppercase UUIDs, `blurb` and `archived` on every category, `source` on every entry, no `weeklyMinutes` key, and no `activeTimer` key when no timer runs.
8. AC-8: Saving a goal (15 min x 4 days) and an entry, then loading a new Store from the same file, gives a goal with weekly target 60 and the same entry.
9. AC-9: When `balance.json` holds invalid JSON, loading shows `Couldn't read balance.json. It was saved as balance.unreadable-<unix seconds>.json.`, starts with empty data, and a file whose name contains `unreadable` exists. If the rename fails, the Store does not write `balance.json` for the rest of the session.
10. AC-10: Timer logic matches iOS: a timer due while the app was closed logs its full planned minutes dated at the timer start; a timer not yet due keeps running; stopping early logs the elapsed minutes (round half up), capped at the planned length; stopping under 30 seconds in logs nothing.
11. AC-11: When a running timer's end time passes while the app is in the background or closed, the entry is logged without opening the app and a "Timer done" notification with body `<Activity>: <N min> logged.` appears with the chosen sound. Tapping it opens the app. Stopping a timer early cancels the pending alarm and notification. If notification permission is denied, the timer still logs.
12. AC-12: Settings offers alarm sounds System Default, Chime, Bell, Pulse, Gentle (default Chime). Picking one plays a preview, the choice persists across launches, and the next timer notification uses it.
13. AC-13: Custom start and end time rounds to the nearest 5 minutes, half up: 92 min gives 90, 93 min gives 95, 92.5 min gives 95, 2 min gives 0. Save is disabled when the result is 0 or less.
14. AC-14: Every time picker in the app offers only minutes at 5-minute steps (00, 05, ... 55). No picker allows a date or time after now. End cannot be set before Start.
15. AC-15: The log sheet opens on the last used side and activity. Switching side picks the first activity of the new side if the current one belongs to the other side. Presets 15m, 30m, 1h, 1h 30m, 2h, 3h, 4h plus Custom are offered, and a stepper moves by 5 minutes within 1 to 1440. Only manual entries open for editing. Any entry can be deleted by swiping.
16. AC-16: A custom activity added in Settings appears in the lists for its side only. Removing it hides it from lists, while `category(id)` still returns its name for old entries. Names are trimmed and limited to 30 characters, and Add is disabled when the name is empty.
17. AC-17: Every SF Symbol name in the built-in and custom icon lists maps to a Material Symbol drawable, an unknown name maps to the circle icon, and `chime.wav`, `bell.wav`, `pulse.wav`, `gentle.wav` exist in `res/raw`.
18. AC-18: Bonsi stage is 0 with no entries, 0 at 59 growth minutes, 1 at 60, 2 at 600, 3 at 1800, and 0 at 1800 fun minutes.
19. AC-19: Sky parameters match iOS: `at(-1).rim == 1`, `at(0).stars == 3`, `at(1).desert == 1`, `at(5).desert == 1`, and at 0.25 clouds is between 0 and 1 and sunR is between golden and day. The caption under the sky uses the same 8 texts and thresholds as iOS `BalanceView`.
20. AC-20: With the system setting "Remove animations" on (animator duration scale 0), the sky and Bonsi draw a still frame (t = 0) and balance changes jump without animating. With it off, the sky animates and balance changes ease over 1.4 s.
21. AC-21: In Settings, the Health Connect toggle is disabled when Health Connect is not available. Turning it on asks for read access to exercise (and mindfulness when the device supports it). If access is granted, Home imports the last 28 days on open and on pull to refresh: exercise sessions as Exercise and mindfulness sessions as Meditation, as growth minutes marked "From Health". If access is denied, the toggle stays off and an error is shown. Turning the toggle off removes all health entries.
22. AC-22: A health entry the user deleted stays deleted after the next import, and the same Health Connect record always maps to the same uppercase UUID.
23. AC-23: With no goals, the app shows onboarding. Picking a growth activity and a target (5 to 240 min/day in 5-min steps, 1 to 7 days) and tapping Start creates the goal and shows Home. Home shows the sky with Bonsi, the caption with weekly Growth and Fun totals, the Fun and Growth timer buttons (or the running timer with a live countdown and Stop), goal progress bars (`done / weekly`), and the 20 most recent entries or the empty text.
24. AC-24: In Settings, goals can be edited and added (new goal defaults to the first unused growth activity, 15 min x 5 days). Deleting the last remaining goal is not possible.

## Contracts
- `docs/contracts/balance-json.md` (new). Documents the existing iOS `balance.json` schema. No field changes for iOS. Android must follow it exactly: field names `goals`, `entries`, `healthEnabled`, `hiddenHealthIDs`, `activeTimer`, `customCategories`; `Goal` {`id`, `categoryID`, `minutesPerDay`, `daysPerWeek`} with legacy `weeklyMinutes` read rule; `Entry` {`id`, `side`, `categoryID`, `minutes`, `date`, `source`}; `ActiveTimer` {`side`, `categoryID`, `start`, `minutes`}; `Category` {`id`, `name`, `symbol`, `side`, `blurb`, `archived`}. Date, UUID, null, and health id rules are in the contract.
- Android preferences (SharedPreferences, not shared with iOS), same key names as iOS `UserDefaults`: `lastSide` (String, `growth` or `fun`, default `growth`), `lastCategory` (String, default `reading`), `timerSound` (String, one of `default`, `chime`, `bell`, `pulse`, `gentle`, default `chime`).

## Asset tasks
None. Icons are existing Material Symbols copied as-is, and the sounds are the existing `Balance/Sounds/*.wav`. The sky and Bonsi are drawn in code, as on iOS. The launcher icon reuses the `eco` symbol (see Android tasks).

## Core tasks
None.

## Android tasks

All paths are under `android/`. Package `com.jabonilla.balance`. Mirror the iOS file split; keep each file to the job its iOS twin does. Strings stay hardcoded English in Kotlin, as on iOS.

### Project setup
- `android/settings.gradle.kts`, `android/build.gradle.kts`, `android/gradle.properties`, `android/gradle/wrapper/*`, `android/gradlew`, `android/gradlew.bat`: single module `:app`. Generate the wrapper with the installed Gradle (`gradle wrapper`) so `./gradlew` exists for the commands in CLAUDE.md. `gradle.properties`: `android.useAndroidX=true`, `org.gradle.jvmargs=-Xmx2g`. Set `JAVA_HOME=/opt/homebrew/opt/openjdk@17` and `ANDROID_HOME=/opt/homebrew/share/android-commandlinetools` in the shell, never in committed files.
- `android/.gitignore`: `.gradle/`, `build/`, `.kotlin/`, `local.properties`, `*.iml`, `.idea/`.
- `android/app/build.gradle.kts`: `namespace` and `applicationId` `com.jabonilla.balance`; minSdk 26; targetSdk 36; compileSdk 36; versionName and versionCode copied from `project.yml` (`MARKETING_VERSION` 0.1, `CURRENT_PROJECT_VERSION` 1); Java and Kotlin target 17; `buildFeatures { compose = true }`; `testOptions.unitTests.isReturnDefaultValues = true`. No R8 or release signing config (debug builds only for now).
- Dependencies (all new, flagged in Risks). Use these versions, the latest stable on Google Maven and Maven Central at planning time. If one does not resolve or does not work with Gradle 9.9.2, take the nearest stable version that does and record it in `docs/plans/balance-android-port-android.md`. Add nothing else.
  - Plugins: Android Gradle Plugin `com.android.application` 9.5.2; Kotlin `org.jetbrains.kotlin.plugin.compose` and `org.jetbrains.kotlin.plugin.serialization` 2.4.21 (add `org.jetbrains.kotlin.android` 2.4.21 only if AGP's built-in Kotlin support needs it).
  - `androidx.compose:compose-bom:2026.11.01` with `androidx.compose.ui:ui`, `androidx.compose.foundation:foundation`, `androidx.compose.material3:material3`. Do not add `material-icons-extended` or `ui-tooling`.
  - `androidx.activity:activity-compose:1.13.0` (ComponentActivity, setContent, BackHandler, permission launchers, edge-to-edge).
  - `androidx.health.connect:connect-client:1.1.0` (contains `MindfulnessSessionRecord`, behind the `@ExperimentalMindfulnessSessionApi` opt-in, and `HealthConnectFeatures`).
  - `org.jetbrains.kotlinx:kotlinx-serialization-json:1.13.0`.
  - Test: `junit:junit:4.13.2` only.

### Manifest and resources
- `app/src/main/AndroidManifest.xml`:
  - `<application android:name=".BalanceApplication" android:allowBackup="false" android:label="Balance" android:icon="@mipmap/ic_launcher" android:theme="@style/Theme.Balance">`.
  - `MainActivity`: exported, launcher, `android:screenOrientation="portrait"`, `windowSoftInputMode="adjustResize"`.
  - `PrivacyActivity`: shows the Health Connect privacy text. Exported, with intent filter `androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE` (Android 13 and lower), plus an `activity-alias` `ViewPermissionUsageActivity` targeting it, exported, `android:permission="android.permission.START_VIEW_PERMISSION_USAGE"`, intent filter `android.intent.action.VIEW_PERMISSION_USAGE` with category `android.intent.category.HEALTH_PERMISSIONS` (Android 14 and up). Text: Balance reads exercise sessions and mindfulness sessions from the last 28 days, counts them as growth minutes, stores them only in balance.json on this phone, and sends nothing anywhere.
  - `TimerReceiver`: `android:exported="false"`.
  - Permissions: `android.permission.POST_NOTIFICATIONS`, `android.permission.SCHEDULE_EXACT_ALARM`, `android.permission.health.READ_EXERCISE`, `android.permission.health.READ_MINDFULNESS`. No INTERNET.
  - `<queries><package android:name="com.google.android.apps.healthdata" /></queries>` so Health Connect is found on Android 13 and lower.
- `app/src/main/res/raw/`: copy `chime.wav`, `bell.wav`, `pulse.wav`, `gentle.wav` from `Balance/Sounds/` unchanged.
- `app/src/main/res/drawable/ic_<name>.xml`: Material Symbols Rounded, filled, 24 px Android vector drawables (Apache 2.0), fetched from `https://raw.githubusercontent.com/google/material-design-icons/master/symbols/android/<name>/materialsymbolsrounded/<name>_fill1_24px.xml`. Set the fill color to white so `Icon(tint=...)` colors them. Name the source and license in `android/THIRD_PARTY_NOTICES.md` (not inside `res/`, where non-resource files break the build). Every name below was checked and exists.
- `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` (and `ic_launcher_round.xml`): adaptive icon, background color `#1D1C22`, foreground `ic_eco` tinted growth amber `#F2B84B`, inset to the safe zone. iOS has no app icon yet, so this is a placeholder, not an asset task.
- `app/src/main/res/values/themes.xml`: `Theme.Balance` parent `android:Theme.Material.NoActionBar`, window background `#1D1C22`, light (white) status bar and navigation bar icons for the dark background.

### SF Symbol to Material Symbol map (`Icons.kt`)
- `app/src/main/java/com/jabonilla/balance/Icons.kt`: `fun symbolIcon(sf: String): Int` returns the drawable id from one `Map<String, Int>`; unknown names return `R.drawable.ic_circle`. `fun symbolLabel(sf: String)` returns the iOS accessibility label rule (drop `.fill`, dots to spaces). Map:

| SF Symbol | Material Symbol |
|---|---|
| book.fill | menu_book |
| figure.run | directions_run |
| character.bubble.fill | translate |
| wrench.and.screwdriver.fill | handyman |
| brain.head.profile | psychology |
| square.and.pencil | edit_square |
| paintbrush.pointed.fill | brush |
| graduationcap.fill | school |
| music.note | music_note |
| frying.pan.fill | skillet |
| chevron.left.forwardslash.chevron.right | code |
| chart.line.uptrend.xyaxis | trending_up |
| bubble.left.and.bubble.right.fill | forum |
| play.tv.fill | smart_display |
| gamecontroller.fill | sports_esports |
| party.popper.fill | celebration |
| tv.fill | tv |
| safari.fill | explore |
| headphones | headphones |
| star.fill | star |
| heart.fill | favorite |
| leaf.fill | eco |
| flame.fill | local_fire_department |
| bolt.fill | bolt |
| sun.max.fill | sunny |
| moon.fill | dark_mode |
| figure.walk | directions_walk |
| figure.yoga | self_improvement |
| dumbbell.fill | fitness_center |
| bicycle | directions_bike |
| tennisball.fill | sports_tennis |
| soccerball | sports_soccer |
| basketball.fill | sports_basketball |
| book.closed.fill | book |
| pencil | edit |
| paintpalette.fill | palette |
| camera.fill | photo_camera |
| guitars.fill | library_music |
| pianokeys | piano |
| globe | public |
| laptopcomputer | laptop_mac |
| film.fill | movie |
| popcorn.fill | theaters |
| cup.and.saucer.fill | local_cafe |
| wineglass.fill | wine_bar |
| fork.knife | restaurant |
| cart.fill | shopping_cart |
| car.fill | directions_car |
| airplane | flight |
| pawprint.fill | pets |
| person.2.fill | group |
| bubble.left.fill | chat_bubble |
| music.mic | mic |
| circle (fallback) | circle |

  UI-only icons (direct drawable use, not in the map): `settings` (gearshape), `add` (plus), `remove` (stepper minus), `timer`, `delete` (trash), `arrow_back`.

### Model (`Models.kt`), port of `Models.swift` and `bonsiStage`
- `Side` (`growth`, `fun`, label "Growth"/"Fun"), `Source` (`manual`, `health`), `Category`, `Goal`, `Entry`, `ActiveTimer`, `AppData` as `@Serializable` data classes with the contract's field names. Dates are `java.time.Instant` with a custom serializer: write `instant.truncatedTo(SECONDS)` with `DateTimeFormatter.ISO_INSTANT`; read with `OffsetDateTime.parse(...)` so offsets and fractions are accepted. UUIDs are `String`, generated with `UUID.randomUUID().toString().uppercase()`, normalized to uppercase on read.
- `Goal` decodes the legacy `weeklyMinutes` rule (nullable `minutesPerDay`, `daysPerWeek`, `weeklyMinutes` in a surrogate, normalized after decode). `Goal` never encodes `weeklyMinutes`. `weeklyMinutes` is a computed property.
- One `Json` instance: `ignoreUnknownKeys = true`, `encodeDefaults = true`, `explicitNulls = false`, `prettyPrint = true`.
- Built-in `Category.growth` and `Category.fun` lists, `customSymbols` list, all copied verbatim from `Models.swift` (ids, names, SF symbol names, blurbs, order).
- `AppData.categories(side)`, `category(id)`, `minutes(side, start, end)`, `minutes(category, start, end)` (inclusive start and end, like `DateInterval.contains`), `GROWTH_PER_FUN = 0.2`, `balance(growth, fun)`, `roundedMinutes(start, end)`, `bonsiStage`, `minutesText`, `minutesShort`, `thisWeek(now, zone, locale)` (week start from `WeekFields.of(locale).firstDayOfWeek`, start of that day in `zone`, plus 7 days).
- Rounding: use `Double.roundToInt()` (half up). Never `kotlin.math.round`, which rounds half to even and breaks the 92.5 min case in AC-13.

### Storage (`Store.kt`), port of `Store.swift`
- `class Store(val file: File)`, plain JVM code (no Android imports) so unit tests run without an emulator. Holds `data` and `error` in Compose `mutableStateOf`. Methods: `load`, `save`, `upsert(entry)`, `delete(entry)` (health entries go into `hiddenHealthIDs`), `upsert(goal)`, `delete(goal)`, `add(category)`, `archive(category)`, `startTimer`, `stopTimer(now)`, `finishTimerIfDue(now)`, `replaceHealthEntries(entries, start, end)`, `setHealthEnabled(on)` (off removes all health entries). Same behavior and same user-facing error texts as iOS.
- `save` writes `balance.json.tmp` then moves it over `balance.json` with `Files.move(ATOMIC_MOVE, REPLACE_EXISTING)`.
- `load` on unreadable file: rename to `balance.unreadable-<epochSeconds>.json`; if the rename fails, set a `readOnly` flag so `save` does nothing (iOS ignores this case and would overwrite the file; Android must not).
- `BalanceApplication.kt`: `class BalanceApplication : Application` owns the one `Store`, created lazily at `File(getExternalFilesDir(null) ?: filesDir, "balance.json")`. The activity, the timer receiver, and Health sync all use this instance, so a timer cannot be logged twice. All Store calls run on the main thread.

### Theme (`Theme.kt`), port of `Theme.swift`
- Colors (dark variants only, the app is always dark): growth `#F2B84B`, fun `#B79CFF`, life `#8FD694`, accentInk `#A7A8FF`, appBackground `#1D1C22`, surface `#27262E`. `sideColor(side)`. A `darkColorScheme` with primary growth, background appBackground, surface surface. `BalanceTheme { }` wraps every screen.

### App shell (`MainActivity.kt`), port of `BalanceApp.swift`
- `enableEdgeToEdge()`, `BalanceTheme`. Shows `OnboardingScreen` when `goals` is empty, else Home. Navigation is a single `var screen` state (Home or Settings) with `BackHandler`; no navigation library. One `AlertDialog` titled "Balance" with OK shows `store.error` and clears it.
- On every `ON_RESUME` call `store.finishTimerIfDue()` (iOS `scenePhase == .active`).
- Read reduce motion once per resume: `Settings.Global.getFloat(contentResolver, ANIMATOR_DURATION_SCALE, 1f) == 0f`, passed down via a `CompositionLocal`.

### Home (`HomeScreen.kt`), port of `HomeView.swift`
- `PullToRefreshBox` (Material 3) around a `LazyColumn` on appBackground. Item 1: `SkyBackdrop` full width, aspect 342:380, drawn under the status bar, so it scrolls away with the list. Item 2: `BalanceCaption` with 20 dp side padding, 12 dp bottom.
- Settings and plus buttons float over the sky at the top (status bar inset): white 40 dp glyphs on a black 22% circle with a white 22% 1 dp ring, 48 dp touch targets, labels "Settings" and "Log minutes". They stay fixed while the list scrolls, like the iOS toolbar.
- Timer card on surface: running timer row (side-colored timer icon, activity name, "<N min> timer", countdown `m:ss` or `h:mm:ss` updated every second, Stop button labeled "Stop timer and log time so far") or the two pill buttons "Fun Timer" (left) and "Growth Timer" (right).
- "Goals" section: icon + name, `done / weekly` with `minutesShort`, a growth-colored `LinearProgressIndicator` capped at 1.
- "Recent" section: 20 newest entries, or "Nothing logged yet. Tap + to log minutes." Row: side-colored icon, name, date as short weekday plus localized short time, a heart icon labeled "From Health" for health entries, `minutesText`. Tap opens the editor for manual entries only. `SwipeToDismissBox` end-to-start deletes (red background, trash icon).
- Effects: sync Health on first composition and on pull to refresh (`healthEnabled` and Health Connect available only; on failure set `Couldn't read Health: <message>`). While a timer runs, a `LaunchedEffect(activeTimer)` waits until its end and calls `finishTimerIfDue`.

### Balance caption and sky (`BalanceView.kt`), port of `BalanceView.swift`
- `BalanceCaption(growth, fun)`: sun icon + "Growth <short>" left, moon icon + "Fun <short>" right (icons in side colors, text white), caption text centered, same 8 texts and same thresholds as iOS. A soft text shadow. Semantics merged into one node: content description = caption, state description = "<g> growth minutes, <f> fun minutes this week".
- `SkyBackdrop(growth, fun, stage)`: balance is 0 when both are 0. Animates balance with `animateFloatAsState(tween(1400, FastOutSlowInEasing))`, or snaps when reduce motion is on. Draws `LivingSky` and a top scrim: vertical gradient from appBackground at alpha `0.1 + 0.27 * clamp(balance, 0, 1)` to clear, 140 dp tall.

### Living sky (`LivingSky.kt`), port of `LivingSky.swift`
- `SkyParams` with the same fields, the same 5 stop values (eclipse, dusk, golden, day, desertSky) and the same `at(balance)` interpolation. Pure Kotlin so AC-19 runs as a unit test.
- `LivingSky(balance, stage)`: a `Canvas` scaled to the 342 x 380 design box that draws, in order, the same layers with the same coordinates: gradient sky, 14 twinkling stars, horizon glow, drifting clouds, sun glow and pulse, desert rings, sun disc, moon glow, disc, rim and craters, ground, grass tufts, dunes, cracks, cactus, then Bonsi at translate (61, 117) scale 1.1 with mood sleepy below -0.3, worried when desert > 0.5, else happy. Bottom 18% fades to transparent (offscreen layer plus a `DstIn` vertical gradient from 0.82 to 1.0). Decorative: no semantics.
- Blur: Compose `Canvas` has no per-shape blur, so draw the glows, horizon, and clouds with `Brush.radialGradient` from the color to transparent at the blurred radius. This keeps the look and works on API 26 without `RenderEffect`.
- Time: one frame clock (`withFrameNanos` loop in a `LaunchedEffect`) writes seconds into a float state that is read only inside the draw lambda, so frames redraw without recomposing. With reduce motion on, the loop does not run and t stays 0.

### Bonsi (`Bonsi.kt`), port of `BonsiRenderer` in `BonsiView.swift`
- `drawBonsi(scope, stage, mood, wilt, t)` in the 200 x 200 design box: pot, sway and breath transform, seed (stage 0), trunk and branches, foliage puffs fading to dry colors with wilt, flowers at stage 3, face (eyes, blink, sleepy eyes, brows, mouth by mood, cheeks), sweat drop when wilt > 0.5, drifting "z" text when sleepy (`drawText` with a `TextMeasurer`). Same numbers as Swift. Skip the standalone `BonsiView` (only used in iOS previews).

### Log sheet (`LogSheet.kt`), port of `LogSheet.swift`
- `ModalBottomSheet` (fully expanded) with a header row Cancel / title ("Log Time" or "Edit Entry") / Save, tinted by side.
- Side segmented buttons (`SingleChoiceSegmentedButtonRow`); on side change apply the iOS category reset rule.
- `ActivityPicker(side, selection)`: an `ExposedDropdownMenuBox` of icon + name from `categories(side)`, plus the current selection if it is archived.
- `MinutesPicker(minutes, color, custom?)`: 4-column grid of preset chips (7 presets plus optional "Custom"), selected chip in side color, accessibility labels as on iOS ("1 hr 30 min", "Custom start and end time"); when not custom, a stepper row (minus and plus icon buttons, label `minutesText`, 1 to 1440, step 5).
- Custom mode: "Start" and "End" `DateTimeField`s, "Time spent" value (`minutesText` or an em dash), footer "Rounded to the nearest 5 minutes.". Not custom: one "When" field.
- `DateTimeField(label, value, min, max)`: a row showing the date and time; the date opens Material 3 `DatePickerDialog` with `selectableDates` limited to dates up to today; the time opens a dialog with an hour dropdown (0 to 23, localized 12 or 24 hour display) and a minute dropdown (00 to 55 in steps of 5). The result is clamped into `[min, max]`. Defaults: End is now rounded down to 5 min, Start is End minus 60 min, When is now rounded down to 5 min for new entries.
- Save: custom mode sets minutes from `roundedMinutes` and date from Start; upserts; stores `lastSide` and `lastCategory`; disabled when custom minutes are 0 or less.

### Timer (`TimerSheet.kt`), port of `TimerSheet.swift`
- `TimerSheet(side)`: `ActivityPicker` (default `lastCategory` if it is on this side, else the first), `MinutesPicker` without Custom, header "Timer length", footer copied from iOS, title "<Side> Timer", Cancel and Start. Start: on Android 13+, request `POST_NOTIFICATIONS` if not yet granted (continue either way), `store.startTimer`, `TimerAlarm.schedule`.
- `TimerSound` enum: `default` ("System Default"), `chime`, `bell`, `pulse`, `gentle`; `current` from prefs key `timerSound`, default `chime`; `preview()` plays the raw file with `MediaPlayer` (release on completion) or the default notification sound via `RingtoneManager`.
- Notification channels: one per sound, ids `timer_default`, `timer_chime`, `timer_bell`, `timer_pulse`, `timer_gentle`, names "Timer (<label>)", importance high, sound set to `android.resource://com.jabonilla.balance/raw/<name>` (or the default notification URI) with notification audio attributes. Android fixes a channel's sound when it is created, so one channel per sound is the only way to honor the setting.
- `TimerAlarm.schedule(context, timer)`: explicit intent to `TimerReceiver`, request code 1, `FLAG_IMMUTABLE`; `setExactAndAllowWhileIdle(RTC_WAKEUP, end)` when `canScheduleExactAlarms()` (always true below API 31), else `setAndAllowWhileIdle`. `TimerAlarm.cancel(context)` cancels the alarm and the notification (id 1). The Stop button calls `store.stopTimer()` then `TimerAlarm.cancel`.
- `TimerReceiver.onReceive`: read the timer's activity name and minutes from the Store before finishing, call `store.finishTimerIfDue()`, and if that logged the timer, post "Timer done" / "<Activity>: <N min> logged." on the current sound's channel with a content intent to `MainActivity` (`FLAG_IMMUTABLE`), auto-cancel. If notifications are not allowed, skip posting.
- shortcut: the alarm does not survive a reboot. The timer is still logged on next app open by `finishTimerIfDue`; only the notification is lost. Upgrade with a boot receiver if that matters.

### Onboarding (`OnboardingScreen.kt`), port of `OnboardingView.swift`
- Step 1: "What do you want to grow?" (large bold), "Pick just one to start. You can add more later.", adaptive grid (min 150 dp) of growth category cards on surface with 14 dp corners: side-colored icon, name, blurb.
- Step 2 (back returns to step 1): title "Weekly target", header icon + name, `TargetFields` (stepper "<n> min a day" 5 to 240 step 5, stepper "<d> day(s) a week" 1 to 7, "Weekly target" value), footer "Start with something doable. You can change it anytime.", the fun explanation text, bold Start button that upserts the goal.

### Settings (`SettingsScreen.kt`), port of `SettingsView.swift`
- Top bar with back arrow, title "Settings".
- Goals: rows "<name>" with "<m> min x <d>d" (use the multiplication sign), tap opens `GoalSheet`; swipe to delete unless it is the last goal; "Add goal" with the iOS default rule.
- Custom activities: active ones with icon and side label, swipe to remove (archive), "Add activity" opens `ActivitySheet`; footer copied from iOS.
- Timer: "Alarm sound" dropdown of `TimerSound`, preview on change, saved to prefs.
- Data sources: switch "Count Health Connect exercise & mindfulness". Disabled unless `HealthConnectClient.getSdkStatus(context) == SDK_AVAILABLE`. Footer: "Exercise sessions count as Exercise and mindfulness sessions as Meditation." plus, when disabled, "Install or update Health Connect to use this.".
- Footer text: "Your data is a single file, balance.json, in Android/data/com.jabonilla.balance/files on this phone. Nothing leaves your phone."
- `GoalSheet`: `ActivityPicker(.growth)`, `TargetFields`, Cancel / Save.
- `ActivitySheet`: side segmented buttons, name field (trimmed, max 30), 6-column grid of the 36 custom icons (48 dp targets, selected = white icon on a side-colored circle, label from `symbolLabel`, selected state in semantics), Cancel / Add (disabled when name is empty), id `custom-<UPPERCASE UUID>`.

### Health Connect (`HealthSync.kt`), port of `HealthSync.swift`
- `isAvailable(context)`: SDK status is `SDK_AVAILABLE`.
- `permissions(client)`: `HealthPermission.getReadPermission(ExerciseSessionRecord::class)`, plus the mindfulness read permission when `client.features.getFeatureStatus(HealthConnectFeatures.FEATURE_MINDFULNESS_SESSION) == FEATURE_STATUS_AVAILABLE`. Opt in to `ExperimentalMindfulnessSessionApi` in this file only.
- Request access from Settings with `PermissionController.createRequestPermissionResultContract()`. Turn `healthEnabled` on only if the exercise permission is granted; otherwise set `Couldn't get Health Connect access.` and leave it off.
- `fetch(client, days = 28)`: start = start of today minus 28 days in the device zone, end = now. Read `ExerciseSessionRecord` and (if permitted and available) `MindfulnessSessionRecord` with `TimeRangeFilter.between`. Map each record with `healthEntry(recordId, start, end, categoryId)`: id per the contract, `growth`, minutes round half up, `source = health`; drop 0-minute entries. Return entries plus the window for `replaceHealthEntries`. Comment: `shortcut: imports the last 28 days, one page of up to 1000 records per type; widen if users want older history.`
- `healthEntry` is a pure function so AC-22 runs as a unit test.

### Tests (`app/src/test/java/com/jabonilla/balance/`)
- One JUnit 4 test per AC that can run on the JVM, named after the AC (for example `ac3_balanceScore`): AC-3, AC-4, AC-5 (paste the contract example as a fixture), AC-6, AC-7, AC-8, AC-9, AC-10, AC-13, AC-16, AC-17 (map covers every symbol, and `src/main/res/raw/<name>.wav` files exist), AC-18, AC-19, AC-22. Use temp folders for Store files.
- AC-1, AC-2, AC-11, AC-12, AC-14, AC-15, AC-20, AC-21, AC-23, AC-24 need a device or the emulator. The developer checks them on the API 36 emulator if disk allows (see Risks) and lists results in `docs/plans/balance-android-port-android.md`. AC-2 is checked from the merged manifest in `app/build/intermediates`.

## iOS tasks
None.

## Security considerations
- No network: the app declares no INTERNET permission, has no backend, and sends no analytics. AC-2 checks this.
- Health data (MASVS-PRIVACY): read-only access to exercise and mindfulness, requested only when the user turns the toggle on, with a privacy rationale screen as Health Connect requires. Only minutes, start date, and a derived id are stored. Raw Health Connect records, titles, and notes are never stored or logged.
- Storage (MASVS-STORAGE): `balance.json` lives in the app-specific external folder (`Android/data/com.jabonilla.balance/files`). Other apps cannot read it on Android 11 and up; a person with the unlocked phone on USB can, which matches the iOS Files sharing the user already has. It is not encrypted, same as iOS. `allowBackup="false"` keeps health-derived data out of cloud backups so "Nothing leaves your phone" stays true. See open question 2.
- Writes are atomic (temp file then move), and an unreadable file is never overwritten (AC-9), so a crash or bad file cannot destroy data.
- Exported components: only the launcher activity and the Health Connect privacy entry points (the Android 14+ alias is guarded by `START_VIEW_PERMISSION_USAGE`). The timer receiver is not exported and is reached by an explicit, immutable PendingIntent.
- No secrets, keys, or tokens exist in this app. No personal data in logs: do not log entries, activity names, or health records.
- Imported JSON is untrusted input: decoding failures go to the backup path, never crash. Enum values outside the contract make the file unreadable (same as iOS).

## Risks and rejected alternatives
Stop flags for the manager:
- New dependencies (all first-time, because `android/` is new): Android Gradle Plugin, Kotlin Compose and Serialization compiler plugins, Compose BOM (ui, foundation, material3), activity-compose, Health Connect client, kotlinx-serialization-json, and JUnit 4 for tests. Reasons: Compose and activity-compose are the requested UI stack; Health Connect client is the only API to read Health Connect; kotlinx-serialization gives JVM-testable JSON with defaults, legacy decoding, and custom date handling in far less code than hand-written `org.json` parsing (which is also stubbed out in JVM unit tests).
- Schema migration: none. The Android app adopts the existing iOS schema unchanged.
- No changes to authentication, payments, or encryption. No breaking change. `version_bump: minor` in the change record (new platform); the actual version number is John's call.

Other risks:
- Disk: about 5.6 GB free. Expect roughly 0.2 GB for the Gradle wrapper distribution, 1 to 1.5 GB for the Gradle dependency cache, and 0.2 to 0.4 GB of build output. An emulator AVD can grow by several GB. Create the AVD with a small data partition (`-p` / `disk.dataPartition.size=2G`) only if room remains, and delete `android/app/build` and the AVD when done if space runs low. If the emulator does not fit, device-only ACs are listed as Not verified.
- Exact alarms: on Android 14 and up, `SCHEDULE_EXACT_ALARM` is off by default, so the "Timer done" notification can arrive a few minutes late unless the user allows "Alarms & reminders". Logged minutes are not affected because they come from the timer's start and planned length. See open question 1.
- Mindfulness in Health Connect depends on the device's Health Connect version. When the feature is not available, only exercise is imported. The API is marked experimental in connect-client 1.1.0, so its opt-in stays in `HealthSync.kt`.
- Health Connect on Android 8 to 13 needs the Health Connect app installed from Play. The toggle stays disabled with a hint when it is missing.
- Material Symbols have no guitar or popcorn icon. `guitars.fill` maps to `library_music` and `popcorn.fill` to `theaters`. Visual parity with SF Symbols is close, not exact.
- Sky glows use radial gradients instead of Gaussian blur, so edges may look slightly different from iOS. Accepted for API 26 support and frame cost.
- The "z" text in Bonsi and the caption use the default Android font, not SF.
- Five notification channels appear in system settings (one per sound). This is the standard way to change a notification sound on Android 8 and up.
- Gradle 9.9.2 with AGP 9.5.2 is not yet proven on this machine. The developer may need the nearest compatible AGP; any change is recorded, not silently swapped.

Rejected alternatives:
- minSdk 34 (Health Connect built in): would exclude most Android phones for a small saving. minSdk 26 is chosen because it is the Health Connect client's own minimum, has `java.time` without core library desugaring (no extra dependency), and has notification channels and adaptive icons, so no pre-26 code paths exist. The large majority of active Android devices run 26 or higher.
- `material-icons-extended`: a large, unmaintained artifact (tens of MB, slow debug dexing) for about 60 icons. Vector drawables copied from Material Symbols are smaller and current.
- Navigation Compose: two screens plus sheets do not need a navigation library; a screen state and `BackHandler` cover it.
- Foreground service for timers: needs a foreground service type and a persistent notification for the whole countdown. An alarm plus a receiver matches the iOS behavior (scheduled notification, logging on completion) with less code.
- WorkManager for timer completion: no exact timing guarantee and an extra dependency.
- Room or DataStore: the contract requires one JSON file; a database would break cross-platform compatibility.
- Shared Kotlin Multiplatform core with iOS: out of scope; the iOS app stays Swift.
- Internal `filesDir` storage: more private but leaves the user no way to copy the file between phones, which FR-2 and US-2 need (see open question 2).

## Open questions
1. Timer alarm on Android 14 and up: is it OK that the "Timer done" notification may be a few minutes late unless the user allows "Alarms & reminders" in system settings? Default if John agrees: yes, no extra prompt. Alternative: add a one-line hint with a button to that system setting in the Timer section of Settings.
2. Where `balance.json` lives: the plan uses `Android/data/com.jabonilla.balance/files/` (copyable over USB, closest to iOS Files sharing) with cloud backup off. Alternative: private internal storage (not copyable) or an explicit Export/Import button in Settings using the system file picker (more code, but works without a computer). Default if John agrees: the plan as written.
