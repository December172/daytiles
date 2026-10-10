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

Milestone 2 is in progress, adding Room-backed journal entries and tasks. Sticker persistence remains a later milestone.

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
├── data/              # Room, repositories, and temporary sticker samples
├── domain/model/      # Independent journal, task, and sticker models
└── ui/
    ├── calendar/      # Month state, date algorithms, and calendar UI
    ├── day/           # Daily journal and task editor
    └── theme/         # Material 3 theme
```

## Privacy and connectivity

Core functionality is fully offline. DayTiles contains no accounts, advertisements, analytics, tracking, cloud services, or backend dependency.

## Credits

- December172 — project creator and maintainer
- OpenAI Codex / ChatGPT — development collaboration, implementation, testing, and documentation

See [CONTRIBUTORS.md](CONTRIBUTORS.md) for contribution details.
