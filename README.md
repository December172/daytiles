# DayTiles

DayTiles is a free, open-source, offline-first Android visual calendar, journal, and daily planner. Its primary interface is an expanded month view where stickers, journal previews, and checkable tasks appear directly inside each day tile.

## Current status

Milestone 1 established the single-module Jetpack Compose application with:

- An adaptive four-to-six-week month layout
- Previous month, next month, today, and date selection controls
- Large day cells that adjust preview density to available height
- Separate journal, task, and sticker preview models
- Light and dark Material 3 themes
- English as the default language and Simplified Chinese (`zh-CN`) resources

Milestone 2 provides:

- Room-backed plain-text daily journals with automatic saving
- Persistent daily tasks with add, edit, delete, and completion controls
- Stable task ordering and completed-task strikethrough
- A dedicated day editor reached from the selected calendar date
- Batched month queries so the calendar does not query each cell separately
- Versioned Room schema export for future non-destructive migrations

Milestone 3 begins with:

- A built-in offline sticker palette
- Custom emoji stickers entered or pasted by the user
- Reusable sticker assets separated from date-specific placements
- Local PNG/WebP importing into private offline app storage
- User-provided HTTPS PNG/WebP URLs downloaded into the same private offline cache
- Room-backed sticker placements attached to individual dates
- Adding and removing stickers from the daily editor
- Persistent sticker previews in month cells
- A select-then-tap-again shortcut that opens the daily editor as a popup over the month
- Non-destructive Room migrations through v3 that preserve existing content

## Roadmap

### Milestone 4 — Daily workflow and navigation (in progress)

- Shape the month screen, daily task list, and quick actions around the on-device reference app
- Add a compact bottom navigation bar for Calendar and Tasks
- Create a dedicated task overview with date grouping, completion toggles, and shortcuts into the day popup
- Keep the month calendar as the primary screen and open day details in a dismissible popup

Implemented in this milestone:

- Calendar and Tasks bottom navigation
- Month-scoped task overview grouped by date, with completion toggles and day-popup shortcuts
- Compact, scrollable day editor popup

### Milestone 5 — Home screen widgets (complete)

- Provide three independent widgets: scrollable todo list, compact task agenda, and small month view
- Let users check off todo items directly from the list widget
- Support optional local reminder alarms for tasks and show reminder times in the list widget
- Refresh all widgets after task edits and preserve reminders across device restarts

Implemented in this milestone:

- Scrollable todo widget containing every open task, checkbox actions, due dates, and optional reminder times
- Resizable task agenda widget with a compact date header and the next open tasks
- Small current-month widget with localized weekday headings and today's date highlighted
- Optional task reminders with Android notification permission handling and reboot rescheduling
- Shared widget refresh after task creation, completion changes, reminder changes, title edits, and deletion

### Milestone 6 — Interaction and visual refinement (next)

- Improve task ordering and overdue/today affordances
- Add accessibility labels and compact-screen layout checks
- Refine the month, todo, bottom bar, and widget visual system using the on-device reference
- Review import failure states and local sticker library management

## Requirements

- JDK 17 or newer (Java 17 source target)
- Android SDK 37
- Android SDK Build Tools 36.0.0
- The checked-in Gradle Wrapper; no system Gradle installation is required

Dependency versions are centralized in `gradle/libs.versions.toml`.

## Build and test

```shell
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Project structure

```text
app/src/main/java/com/december172/daytiles/
├── data/              # Room persistence and repositories
├── domain/model/      # Independent journal, task, and sticker models
└── ui/
    ├── calendar/      # Month state, date algorithms, and calendar UI
    ├── day/           # Daily journal and task editor
    └── theme/         # Material 3 theme
```

## Privacy and connectivity

Core functionality is fully offline. Optional user-requested sticker downloads require internet access, then remain available from private local storage. DayTiles contains no accounts, advertisements, analytics, tracking, cloud services, or backend dependency.

## Credits

- December172 — project creator and maintainer
- OpenAI Codex / ChatGPT — development collaboration, implementation, testing, and documentation

See [CONTRIBUTORS.md](CONTRIBUTORS.md) for contribution details.
