# Quickstart — 001 Yokai Tennis MVP

## Prereqs (this machine, verified 2026-07-07)

- JDK: `C:/Users/e48994/.jdks/corretto-25.0.3` (java is NOT on PATH)
- Android SDK: `%LOCALAPPDATA%/Android/Sdk` (platform android-36.1)

## Commands (Git Bash)

```bash
export JAVA_HOME="C:/Users/e48994/.jdks/corretto-25.0.3"
cd /c/Users/e48994/IdeaProjects/com/cafeyokai/tennis

./gradlew test                    # engine unit tests (:core)
./gradlew desktop:run             # play on desktop, 1280x720
./gradlew android:assembleDebug   # APK at android/build/outputs/apk/debug/
```

## Desktop controls

- WASD / arrows: move character
- Mouse drag/flick on right stick area: aim + shot (slow=lob, fast=smash, lateral jerk at apex=slice/topspin)
- Mouse click: serve meter taps (aim → start power → lock power → accuracy)

## Fast verification loop

Main Menu → Settings → Match length: **Single Set** → Play. Full DoD run uses default Best of 3.
