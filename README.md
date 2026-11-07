# Alarm Set

An Android app for the OnePlus 13 (Android 12 or newer) that sets a random spread of alarms between two times. You pick a start time, an end time and optionally how many alarms go in between. The app always sets the start and end, then scatters the rest across the range so they cover it unevenly, for example 6:00, 6:06, 6:10, 6:13, 6:19 ... 7:00.

The alarms are created in the phone's own Clock app, so they ring like normal alarms.

## Install

Copy `AlarmSet.apk` to the phone (USB, Drive, Telegram, anything works), open it and allow "install unknown apps" for whatever app you opened it from. Updating later needs the same `release.keystore`, so keep it.

## Using it

- Tap From or To to pick times. A range that crosses midnight (23:30 to 00:30) works.
- Alarms in between: leave it empty for about one alarm every 5 minutes. A count of 10 gives 12 alarms in total, since start and end are always added. The limit is 48 in between, and the count also can't exceed the free minutes in the range.
- The preview shows exactly what will be set. Shuffle rerolls it.
- Set N alarms sends them to Clock without opening it. Each one is labeled `Spread`.
- Turn off alarms opens the Clock app's alarm page, where you turn them off or delete them.

### Limits

Android lets apps add alarms to the Clock app. It doesn't let them read or delete alarms there. Android does have a "dismiss alarm" request, but OnePlus Clock answers it by opening its own screen without turning anything off. That's why Turn off alarms just opens Clock.

## Build

Needs the JDK at `~/Android/jdk` and the Android SDK at `~/Android/Sdk` (platform 35, build-tools 35.0.0). There's no Gradle.

```sh
./build.sh
```

The script compiles resources with aapt2, compiles Java, runs the `Check` test, dexes, aligns and signs. It produces `AlarmSet.apk` and creates `release.keystore` on first run.

## Project structure

```
AndroidManifest.xml
build.sh                         build + test, no Gradle
src/com/pranav/spread/
  Spread.java                    alarm math, plain Java
  MainActivity.java              the one screen and the Clock intents
res/                             layouts, Material You colors (light/dark), icon
test/Check.java                  runs on the desktop JVM during the build
docs/superpowers/plans/          implementation plan
```

## Design decisions

- Clock app instead of its own alarm engine. Setting alarms matters most and works reliably through `AlarmClock.ACTION_SET_ALARM`. Ringing its own alarms would make delete and off guaranteed, at the cost of a lot more code (ring screen, sound, reboot handling).
- How the spread works. The free minutes between start and end are cut into N equal slices, and each alarm lands on a random minute in its slice. The result is uneven but covers the whole range, and no two alarms share a minute.
- Framework widgets only. No Gradle, no AndroidX. The colors use `@android:color/system_*`, so the app follows the wallpaper theme and dark mode like OnePlus's own apps.
- What you see is what gets set. The Set button sends the previewed times and doesn't reroll.

## Testing

`build.sh` runs `test/Check.java` and fails the build if the math breaks. It covers ordering, start/end placement, midnight wrap, count limits, an equal start and end, the auto count and slice coverage.

The UI hasn't been run on a device or emulator yet.

## Future ideas

- Its own ringing engine, so turning alarms off can happen inside the app (OnePlus Clock ignores dismiss requests).
- Repeat days.
