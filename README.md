# Balance

Balance helps you balance time spent on growth (reading, exercise, language, skill practice, meditation, and so on) against time spent on fun (social media, video, gaming, TV, and so on).

You log time by hand with start and end pickers, or with a timer for each activity. Workouts and mindful minutes can come in from Apple Health (iOS) or Health Connect (Android). A target ratio of growth to fun and per-day goals drive the Balance view, a Bonsi character, and a living sky.

Your data is one file, `balance.json`. There is no backend, no account, and no analytics.

## Apps

| App | Stack | Platform | Data file |
|---|---|---|---|
| iOS | SwiftUI, HealthKit | iOS 26+, iPhone only | `Documents/balance.json` in the app container |
| Android | Kotlin, Jetpack Compose, Health Connect | Android 8.0 (API 26) and up, portrait only | `Android/data/com.jabonilla.balance/files/balance.json` on the phone |

Both apps read and write the same `balance.json` format, so you can copy the file between an iPhone and an Android phone. The format is documented in `docs/contracts/balance-json.md`.

The Android app declares no INTERNET permission. Health data stays in `balance.json` on the phone.

## Repo layout

| Path | What it is |
|---|---|
| `Balance/` | iOS app source (SwiftUI views, `Models.swift`, `Store.swift`, `HealthSync.swift`, `Theme.swift`, `Sounds/`, `Info.plist`, entitlements) |
| `BalanceTests/` | iOS unit tests |
| `project.yml` | XcodeGen spec for the iOS project. `Balance.xcodeproj` is generated from it and is gitignored |
| `android/` | Android app (Gradle single module `:app`, package `com.jabonilla.balance`) |
| `docs/plans/` | Implementation plans and per-platform notes |
| `docs/changes/` | Change records and QA verification records |
| `docs/contracts/` | Shared formats, starting with `balance-json.md` |
| `docs/audits/` | Repo audit reports |
| `design/` | Style guide, sprite manifest, and design assets |
| `tools/` | Helper scripts (`make_sounds.py`) |
| `projects/balance/` | Planning and seed notes |
| `PRODUCT.md` | Requirements, user stories, and acceptance criteria with status |
| `CHANGELOG.md` | Release notes in Keep a Changelog format |

## Build and test

### iOS

```sh
xcodegen generate   # after changing project.yml or adding or removing source files
xcodebuild -scheme Balance -destination 'platform=iOS Simulator,name=iPhone 17e' build
xcodebuild -scheme Balance -destination 'platform=iOS Simulator,name=iPhone 17e' test
```

Lint: TODO: confirm

### Android

```sh
cd android && ./gradlew assembleDebug
cd android && ./gradlew testDebugUnitTest
```

Lint: TODO: confirm

## Android toolchain notes

- JDK 17 is required. Point `JAVA_HOME` at your JDK 17 install in your shell.
- Android SDK platform 36 is required (compileSdk 36, targetSdk 36). Point `ANDROID_HOME` at your SDK in your shell.
- The Gradle wrapper (`android/gradlew`) pins Gradle 9.9.2. Use the wrapper, not a system Gradle.
- Debug builds only. There is no release signing or R8 configuration yet.
- Local paths and SDK locations belong in your shell profile or `android/local.properties`, which is gitignored. Do not commit them.
- Health Connect on Android 8 to 13 needs the Health Connect app installed from Google Play. The Health Connect toggle in Settings stays disabled with a hint when it is missing.
- Compose BOM is 2026.06.01. Newer BOMs need compileSdk 37, which the project does not use yet.

## Data and privacy

- iOS stores `balance.json` in the app's Documents folder.
- Android stores `balance.json` in `Android/data/com.jabonilla.balance/files`. You can copy it to a computer over USB. Android 11 and up keep other apps from reading it. The file is not encrypted, the same as on iOS.
- Android backups are off, so health-derived entries are not sent to cloud backup.
- If `balance.json` cannot be read, the app renames it to `balance.unreadable-<unix seconds>.json` in the same folder and starts with empty data. It never overwrites a file it could not read.

## Documentation

- `PRODUCT.md`: what the product must do and the verification status of each requirement.
- `CHANGELOG.md`: what changed in each release.
- `docs/contracts/balance-json.md`: the shared data file format.
- `docs/plans/`: implementation plans and platform notes.
- `docs/changes/`: change records and QA results.
