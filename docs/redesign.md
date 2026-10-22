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

## Original v0.2.0 verification

```shell
ANDROID_HOME=/opt/android-sdk ANDROID_SDK_ROOT=/opt/android-sdk \
  ./gradlew --offline assembleDebug testDebugUnitTest lintDebug
```

The v0.2.0 verification run passed all 10 unit tests and Android Lint with zero
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

## Planner follow-up

- Settings now includes a persistent **Show Chinese lunar dates** switch. It defaults to enabled and immediately updates the month calendar without changing journal, task, or sticker data.
- The drawer has one Week view shortcut. Course timetable and Course Scheduler are combined into a single Course timetable destination in both the drawer and Me.
- The course screen uses the installed `com.comingx.zanao` course workflow as a layout reference: a weekly grid, date/week controls, a top add action, clickable class blocks, and a full-page editor. Tap a blank slot to preselect its weekday and time. The toolbar also opens a course list within this screen. Classes retain their existing weekly recurrence, location, and optional reminders; overlapping classes use separate selectable lanes.
- Editing or deleting a course without an existing reminder no longer passes a null PendingIntent to AlarmManager.
- Widget pinning now tracks the launcher's placement callback. An accepted request shows a waiting state; success appears only after the callback identifies a bound widget. Unsupported, failed, or unconfirmed requests provide retry and manual instructions, including an action to open the home screen. The [Android pinning API](https://developer.android.com/reference/android/appwidget/AppWidgetManager#requestPinAppWidget(android.content.ComponentName,%20android.os.Bundle,%20android.app.PendingIntent)) does not report user cancellation as a failure callback, so an unconfirmed request is not treated as successful placement.

The initial follow-up build passed `assembleDebug`, all 13 unit tests, and `lintDebug`
with zero errors, 59 warnings, and two hints. Three new layout tests cover
adjacent classes, overlapping classes, and reuse of lanes across overlapping
groups. No Room schema or entity changes are required.

On the V2425A, lunar labels were toggled off and on in the actual month view,
checked after a process restart, and restored to enabled. The drawer was checked
for duplicate week and course entries. Widget placement testing on vivo was
stopped at the user's request.

On the Android API 37 emulator with the standard launcher, pinning opened the
native confirmation dialog, placed a real task widget, and returned the success
callback. Cancelling placement showed manual instructions without reporting
success; retrying and confirming completed placement. Course checks covered
creation from a blank slot, start/end time selection, editing, deletion without
a reminder, weekly navigation, and the integrated course list.
The setting, timetable, and course editor were also checked in Simplified
Chinese, and the emulator's app language was restored afterward.

A read-only database comparison after installation confirmed that all records
present before these changes were retained. A course added on the device during
the checks was also preserved.

The README screenshots show the current drawer, planning tools, widget feedback,
course timetable, and calendar setting. Course examples use neutral sample data.

## Build warning cleanup

The full rebuild now passes with no Kotlin or Gradle warnings. Android Lint
reports zero errors, warnings, or hints, and all 13 JVM unit tests pass:

```shell
ANDROID_HOME=/opt/android-sdk ANDROID_SDK_ROOT=/opt/android-sdk \
  ./gradlew --offline assembleDebug testDebugUnitTest lintDebug \
  --rerun-tasks --warning-mode all
```

On Android 12 and later, todo widgets use
[RemoteCollectionItems](https://developer.android.com/develop/ui/views/appwidgets/collections#use-remotecollectionitems-to-pass-along-a-collection-directly)
to deliver rows directly. The Android 8–11 branch retains the required service
adapter, with deprecation annotations limited to its two compatibility helpers.
Both adapters share the same task-row rendering. Broadcast updates keep their
pending result until the database read and widget update finish.

Widget preview and target-cell attributes now live in `xml-v31`, alongside the
base descriptors for older Android versions. The cleanup also removes 19 unused
string keys in both languages, uses quantity resources for goal and break labels,
removes checks below the app's API 26 minimum, and updates preference edits and
integer Compose state. Task-widget metadata and expanded month grids use 11sp.
Narrow month widgets retain their compact size to fit all seven columns.

Each launcher-hosted widget layout documents a local exception for Lint's
activity-window background overdraw check. Its opaque widget background is
needed when the launcher draws the RemoteViews. No global Lint baseline or
disabled check is added.

Runtime regression for this cleanup used only the API 37 emulator
(`emulator-5554`). It covered task-widget population and completion, native widget
pin cancellation and retry, the month widget's visible grid, English singular
and plural labels, Chinese quantity labels, focus start/pause/resume/stop with
persisted preferences, lunar display across a process restart, drawer entries,
and course creation/editing/deletion without a reminder. The crash buffer stayed
empty. Temporary database records were removed after the run, and the emulator's
app language and focus preferences were restored. The Android 8–11 compatibility
branch was checked by compilation and Lint; runtime checks used API 37.
