# Alarm Set Implementation Plan

**Goal:** Standalone Android app (OnePlus 13, Android 15) that sets a random spread of alarms between a start and end time in the system Clock app.

**Architecture:** One activity plus one pure-Java math class. Alarms go to the Clock app through `AlarmClock.ACTION_SET_ALARM` with `EXTRA_SKIP_UI`. Turning alarms off happens in the Clock app, since Android has no API to delete another app's alarms and OnePlus Clock ignores dismiss requests. SharedPreferences keep the last range and count.

**Tech Stack:** Java 11 on the framework SDK (API 35, min 31), no Gradle and no AndroidX. Built with javac, d8, aapt2, zipalign and apksigner. Material You colors come from `@android:color/system_*`.

## Global Constraints
- Package `com.pranav.spread`, app name "Alarm Set" (renamed from Alarm Spread). Shares nothing with `~/Android/random-alarms`.
- Start and end are always set. The count means alarms *between* them (10 gives 12 alarms in total).
- Empty count means auto: about one alarm every 5 minutes.
- No more than 48 alarms in between, so the Clock app isn't flooded.
- Every alarm is labeled `Spread`.

## Review Focus
- Range crossing midnight (23:30 to 00:30): times wrap correctly and stay in order. Covered by `Check`.
- Start equals end: no alarms, clear message. Covered by `Check`.
- Count larger than the minutes available (6:00 to 6:05, count 10): rejected with the maximum shown. Covered by `Check`.
- Count of 0: only start and end. Covered by `Check`.
- Clock app ignores the dismiss request: the UI says this up front and offers "Open Clock".

### Task 1: Alarm math (`src/com/pranav/spread/Spread.java`, `test/Check.java`)
- `static int span(int start, int end)`: minutes from start to end, wrapping midnight. Returns 0 when they are equal.
- `static int maxBetween(int start, int end)`: `min(48, max(0, span-1))`.
- `static int autoCount(int start, int end)`: `round(span/5) - 1`, clamped to `[0, maxBetween]`.
- `static int[] times(int start, int end, int n, Random r)`: `[start, n interior, end]`, or null. Split the `span-1` interior minutes into n equal slices and pick one random minute in each, so times are unique, ordered and cover the whole range.
- Check: lengths, endpoints, strictly increasing offsets, null cases, `autoCount(6:00,7:00) == 11`, varied gaps.

### Task 2: UI and Clock integration (`MainActivity.java`, `res/`, `AndroidManifest.xml`, `build.sh`)
- From/To tiles open a TimePickerDialog. The count field's hint is "Auto". The preview grid rerolls on every change and has a Shuffle button.
- "Set N alarms" sends the previewed times as they are.
- No list of set alarms (removed on request). One "Turn off alarms" button opens Clock (`ACTION_SHOW_ALARMS`), because OnePlus Clock ignores `ACTION_DISMISS_ALARM` and just opens itself (tested on device, 2026-11-07).
- Verify: `./build.sh` runs Check and produces a signed `AlarmSet.apk`.
