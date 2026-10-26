# Full schedule, Recycle Bin, and expense budgets

The drawer and Me page now provide three additional planning destinations.

- **Full schedule** shows all active tasks in chronological date groups across past and future months. It includes completion filters, editing, quick task creation, completion checkboxes, and a Today shortcut.
- **Recycle Bin** retains deleted tasks, including their original dates, details, tags, completion state, reminder metadata, and subtasks. Restore returns a task to its original date. Permanent deletion and Empty bin require confirmation; task and subtask removal runs in one transaction.
- **Expenses & budgets** records dated expenses with amounts, categories, and optional notes. Daily and monthly views show the applicable records, totals, budgets, and remaining or over-budget amounts. Expenses can be edited or deleted; budgets can be edited or removed independently.

Money uses integer minor units with exact decimal parsing. Inputs with excess decimal places, nonpositive expense amounts, and out-of-range amounts are rejected. Zero budgets are supported. CNY, HKD, TWD, and USD are separate ledgers; changing the selected currency does not convert existing amounts.

## Persistence

Room schema 9 adds a nullable deletion timestamp to tasks and introduces separate expense and budget tables. Existing journal, task, sticker, subtask, target, and course records are retained. Active task queries exclude recycled tasks, including calendar views, targets, tags, focus choices, and widgets.

Moving a task to the bin cancels its pending reminder. Restore schedules a reminder only for an unfinished task whose reminder remains in the future. Stale task or subtask editors cannot rewrite a task while it is in the bin.

## Emulator verification — 2026-10-26

All runtime tests ran on the Android API 37 emulator, using a disposable, read-only AVD session. No physical device was used.

| Test group | Cases |
| --- | ---: |
| Existing calendar, course layout, tag, and date-converter unit tests | 13 |
| Exact decimal amounts, validation, boundaries, and currency formatting | 5 |
| Timeline, recovery, permanent removal, expense/budget isolation, and persistence | 8 |
| Room 8→9 migration with all seven existing entity tables | 1 |
| Supported Chinese locale resources | 1 |
| End-to-end timeline, recycle-bin, expense, and budget UI flows | 4 |
| **Total on emulator** | **32** |

The pure unit-test source directory is also included in the Android test source set, so the same assertions execute inside Android. Test method names use DEX-compatible identifiers.

The UI checks cover completion and recovery state, cancellation and confirmation of permanent removal, daily versus monthly records, over-budget and zero-budget display, currency isolation, editing, and deletion. Application restart/database reopen persistence is checked separately.

An additional real APK upgrade from version 8 to version 9 preserved every original field and row in all seven existing entity tables. Android's SQLite integrity check passed. The same comparison passed after the UI tests cleaned up their temporary records. English, Simplified Chinese, Hong Kong Traditional Chinese, and Taiwan Traditional Chinese navigation and all three new screens were also checked on the emulator.

Build and Lint completed successfully. Kotlin compilation emitted no warnings. Lint has zero errors and three tool-version update advisories for the existing Gradle, Android Gradle Plugin, and Kotlin versions.

Start a disposable API 37 emulator before running:

```shell
ANDROID_HOME=/opt/android-sdk ANDROID_SDK_ROOT=/opt/android-sdk \
  ANDROID_SERIAL=emulator-5554 ./gradlew --offline \
  assembleDebug connectedDebugAndroidTest lintDebug --no-configuration-cache
```

The first connected run needs network access to resolve Android's test-platform components. Connected test tasks install and remove debug applications; the UI tests also export screenshots cropped inside Android before copying them out of the emulator.
