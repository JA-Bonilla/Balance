# Changelog

All notable changes to Balance are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## Unreleased

### Added

- Android app, built with Kotlin and Jetpack Compose, that matches the iOS app: onboarding, Home with the living sky and Bonsi, logging, timers with notifications and alarm sounds, goals, custom activities, the Balance score, and Settings. ([#2](https://github.com/JA-Bonilla/Balance/pull/2))
- Android timers log in the background. When a timer ends while the app is closed, the time is logged and a "Timer done" notification appears with the sound you chose. Stopping a timer early cancels its alarm. ([#2](https://github.com/JA-Bonilla/Balance/pull/2))
- Android Health Connect import, off by default. When turned on, exercise sessions count as Exercise and mindfulness sessions as Meditation, as growth minutes from the last 28 days, marked "From Health". Turning it off removes those entries. Entries you delete stay deleted. ([#2](https://github.com/JA-Bonilla/Balance/pull/2))
- Android reads and writes `balance.json` in the same format as iOS, so you can copy the file between an iPhone and an Android phone without losing goals, entries, custom activities, or settings. ([#2](https://github.com/JA-Bonilla/Balance/pull/2))
- Shared data format documented in `docs/contracts/balance-json.md`, which both apps follow. ([#1](https://github.com/JA-Bonilla/Balance/pull/1))
- Developers: the Android project lives in `android/`. Build with `cd android && ./gradlew assembleDebug` and test with `cd android && ./gradlew testDebugUnitTest`. Requires JDK 17 and Android SDK platform 36. See `README.md`. ([#2](https://github.com/JA-Bonilla/Balance/pull/2))

### Known limits (Android)

- A running timer's alarm does not survive a phone reboot. The timer is still logged the next time you open the app, but its notification is lost.
- Timer notification sounds are chosen per notification channel. Android shows one channel per sound in system settings.
- On Android 8 to 10, other apps with storage access may be able to read `balance.json`.

### Changed

- None for iOS. The iOS app and its `balance.json` format did not change.
