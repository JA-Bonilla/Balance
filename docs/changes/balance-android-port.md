---
slug: balance-android-port
status: applied
version_bump: minor
---
## Planned
- Change type: feature
- Summary: Balance comes to Android. A Kotlin and Jetpack Compose app under `android/` matches the iOS app (onboarding, home, logging, timers with notifications and sounds, goals, custom activities, the Balance score, Bonsi, the living sky, and settings), reads exercise and mindfulness sessions from Health Connect, and stores data in a balance.json file that uses the same format as iOS.
- Breaking: no. The iOS app and its data file do not change.
- Requirements affected: FR-1, FR-2, FR-3, US-1, US-2, AC-1 to AC-24
- Migration notes: None for users. Developers need JDK 17, the Android SDK (platform 36), and Gradle. Build with `cd android && ./gradlew assembleDebug`, test with `cd android && ./gradlew testDebugUnitTest`. The shared file format is in `docs/contracts/balance-json.md`.
