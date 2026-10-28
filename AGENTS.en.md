# Project: Daytiles

[简体中文（默认）](AGENTS.md) | [English](AGENTS.en.md)

## Purpose
Offline-first Android journal and visual planner.

## Stack
- Kotlin
- Jetpack Compose with Material 3
- Room / SQLite
- Coroutines and StateFlow
- Gradle Kotlin DSL

## Requirements
- Full-height monthly calendar
- Native sticker objects attached to dates
- Dedicated daily journal entries
- Checkable daily tasks
- Notes, stickers, and tasks visible in month cells
- Fully offline operation
- No ads, subscriptions, or backend

## Development rules
- Prefer simple, maintainable architecture.
- Use one app module initially.
- Do not replace journal entries with calendar events.
- Keep notes, tasks, and stickers as separate data models.
- Preserve existing user data when changing schemas.
- Never introduce unnecessary external services.
- Do not modify unrelated files.

## Documentation language

- Default documentation, including the root README and release notes, uses Simplified Chinese.
- English documents use `.en.md`; Hong Kong and Taiwan Traditional Chinese use `.zh_HK.md`, `.zh_TW.md`, or their regional directories.
- Maintain all four languages for every published version. GitHub Releases show Simplified Chinese first, with the other languages in expandable sections.
- Preserve the original version's feature scope, validation results, downloads, and limitations when translating. Keep original license text intact.

## Verification
After changing Kotlin or Gradle code:
- Run ./gradlew assembleDebug
- Run relevant unit tests.
- Report build failures and unresolved issues.
- Do not claim features work without testing them.
