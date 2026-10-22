# Reference appearance milestone

DayTiles now follows the main layout of the installed reference app,
`com.zjzy.calendartime`. The implementation uses native Compose components,
DayTiles icons, and the existing local repository.

## Included

- White surfaces, orange actions, light separators, blue task filters, and compact centered toolbars; a corresponding dark palette.
- Five primary destinations: Schedule, View, Target, Widget, and Me. The drawer and Me keep the focus clock, tags, priority matrix, week view, and course tools accessible.
- A full-height, Sunday-first month grid with adjacent-month dates, an orange today marker, pale task/journal previews, and date-attached stickers.
- Horizontal swipes and a year/month picker for calendar navigation; Today returns to the current date.
- Offline lunar date labels from Android's bundled ICU calendar.
- A flat task list with circular completion controls and existing tag/date filters. Tap a task to open its full editor; tap a date heading to open its journal and stickers.
- A bottom quick-entry panel with title, details, tags, priority, and optional reminders. Existing tasks retain full-screen editing and subtasks.
- A target page with a week strip, a real empty state, and a goal editor for the existing daily/weekly completion targets.
- A widget catalogue that requests the launcher's add-widget flow, with instructions when the launcher does not support pin requests.
- A grouped Me page for local planning tools.

The Room schema and journal/task/sticker entities are unchanged. Journals remain
independent of tasks, and no account or external service is required. Reference
screenshots, branding, subscriptions, and cloud features are not bundled.

## Verification

```shell
ANDROID_HOME=/opt/android-sdk ANDROID_SDK_ROOT=/opt/android-sdk \
  ./gradlew --offline assembleDebug testDebugUnitTest lintDebug
```

The final verification run passed all 10 unit tests and Android Lint with zero
errors. Lint still reports 50 warnings. The month display tests include adjacent
months across a year boundary, leap days, every possible week start, and a
four-week February.

Device checks on the connected V2425A covered quick task creation, task
completion, full editing with the keyboard visible, saving/reopening a subtask,
and deleting the temporary task. Navigation checks covered the five main tabs,
the goal editor, focus clock, drawer scrolling and back handling, month swipes,
and the month picker.
Light/dark appearance and Simplified Chinese were checked; the device's original
light mode and app locale settings were restored afterward.

A database comparison after the checks confirmed that every pre-existing table
contained exactly its original rows. No temporary test task, subtask, or goal was
left behind. Screenshots use an empty month or neutral tool pages and exclude
system bars and personal entries.

The widget catalogue button was exercised on this device, but successful desktop
pinning was not confirmed. Widget placement remains dependent on the launcher.

Current screenshots are in the README. The APK is
`app/build/outputs/apk/debug/app-debug.apk`. Visual equivalence is based on the
reference installed on the connected device; this is not a pixel-by-pixel match
for every reference screen or theme. Lunar labels do not include solar terms or
public-holiday annotations.
