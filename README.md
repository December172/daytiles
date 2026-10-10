# DayTiles

DayTiles is a free, open-source, offline-first Android calendar, journal, and visual planner. The month calendar is the home for dated notes, stickers, and checkable tasks. The app is built with Kotlin, Jetpack Compose, Room, and Material 3.

## Screenshots

### Month calendar

The calendar provides a full-height month grid, date selection, and quick access to the selected day.

![DayTiles month calendar](docs/screenshots/month-view.png)

## Features

- Full-height month calendar with month navigation, a Today shortcut, and selectable dates
- Daily journal entries stored separately from tasks and stickers, with automatic saving
- Dated tasks with a dedicated full-screen create/edit form for title, details, tags, and optional reminder alarms
- Ordered, checkable subtasks on tasks, with completion progress and data-preserving Room migration
- Daily and weekly completed-task targets with progress bars and goal punch-card counts
- Optional importance/urgency classification with a separate four-quadrant priority matrix
- Task completion, deletion, due-date grouping, tag filtering, and overdue indicators in the task overview
- Day editor popup focused on journal text and sticker management; add/remove stickers independently of task workflows
- Built-in emoji stickers, custom emoji input, local PNG/WebP imports, and optional HTTPS PNG/WebP downloads
- Reusable sticker library with date-specific sticker placements and calendar-cell previews
- Calendar and Tasks bottom navigation, including a date-grouped task list
- Side navigation for calendar, tasks, tags, goals, priority matrix, week view, course planner, focus clock, settings, and help
- Week-at-a-glance screen showing tasks and journal snippets
- Recurring weekly course planner with weekday, time, location, and optional local reminders
- Configurable focus clock with pause/resume, automatic break phase, optional task association, persistent notification, and widget start
- Five resizable Android home-screen widgets with compact and expanded layouts:
  - Todo list with open tasks, due dates, reminder times, and completion actions
  - Daily journal preview with a one-tap editor for today's entry
  - Compact localized month calendar with today's date highlighted
  - Week overview with dated tasks and journal previews
  - Focus clock for starting/checking a focus session
- English as the default language and Simplified Chinese (`zh-CN`) localization
- Offline-first storage, with no account, ads, analytics, tracking, cloud sync, or backend

## Feature completion checklist

The following work is intentionally split into small steps. Implement and verify one item at a time; do not bundle the whole list into one release.

### Task organization

- [x] **Subtasks** — support a parent task with an ordered checklist of child tasks, including completion state and a clear parent progress summary. Preserve existing tasks during the Room schema migration.
- [x] **Task targets and aims** — define daily and weekly goals, show completion progress, and provide a compact punch-card count. Goal records are distinct from journal entries and tasks.
- [x] **Four-quadrant prioritization** — optionally classify tasks by importance and urgency, then view them in a separate four-quadrant matrix. Ordinary task entry remains unclassified by default.

### Navigation and calendar views

- [x] **Side menu** — provide a navigation drawer for settings, tags, goals, help, and planner views, with calendar and tasks directly accessible.
- [x] **Week view** — provide a week-at-a-glance screen with dated tasks and journal snippets, plus a compact week-view widget.
- [x] **Schedule and recurring items** — provide a weekly course planner for class names, weekdays, start/end times, locations, and local reminders. Course sessions are distinct from journal entries and tasks.

### Focus and home-screen widgets

- [x] **Focus clock** — provide an offline timer with configurable work/break durations, pause/resume, persistent notification, and optional task association.
- [x] **Focus clock widget** — provide a compact widget for starting or checking a focus session.
- [x] **Widget layout options** — make todo, journal, month, week, and focus widgets resizable, with content adapted for compact and expanded sizes.

## Build and test

Requirements:

- JDK 17 or newer
- Android SDK 37
- Android SDK Build Tools 36.0.0
- The checked-in Gradle Wrapper (no system Gradle installation required)

Run the verification tasks from the project root:

```shell
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew lintDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Dependency versions are centralized in `gradle/libs.versions.toml`.

## Project structure

```text
app/src/main/java/com/december172/daytiles/
├── data/              # Room persistence and repositories
├── domain/model/      # Independent journal, task, and sticker models
├── widget/            # Android home-screen widgets
└── ui/
    ├── calendar/      # Month state, date algorithms, and calendar UI
    ├── day/           # Daily journal and task editor
    └── theme/         # Material 3 theme
```

## Privacy and connectivity

DayTiles works offline. Internet access is used only when the user explicitly downloads a sticker from a URL; downloaded stickers are kept in private local storage. There are no accounts, advertisements, analytics, tracking, cloud services, or backend dependency.

## Credits

- December172 — project creator and maintainer
- OpenAI Codex / ChatGPT — development collaboration, implementation, testing, and documentation

See [CONTRIBUTORS.md](CONTRIBUTORS.md) for contribution details.
