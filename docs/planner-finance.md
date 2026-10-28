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

## Physical-device verification — 2026-10-28

The same 32 tests passed on a connected vivo V2425A running Android 16 (API 36), with zero failures, in 84.01 seconds. This includes the four end-to-end UI cases and the existing calendar, course layout, tag, date, money, repository, migration, and localization checks listed above.

The automated suite used the current production sources with a temporary application ID, `com.december172.daytiles.phonetest`, and a separate test application. This kept Empty bin and budget-editing tests separate from the user's existing records. UI test launches now reset the task through the shell; screenshot cropping ignores empty navigation-bar sources reported by the phone. No production feature code changed during this verification.

The original application was backed up and updated in place. Its database migrated from schema 8 to 9. A separate Android-native check compared the original columns, row counts, and row hashes in all seven existing entity tables, checked preferences and imported sticker file hashes, and passed SQLite's integrity check. Android's generated `profileInstalled` marker was excluded from the user-file comparison. The original application's Full schedule, Recycle Bin, and Expenses & budgets pages also passed navigation checks.

Both normal and isolated debug APK/test APK builds completed successfully. All temporary test applications were removed, leaving the updated original application open on its main calendar. Widget pinning was not tested on vivo.

## Version 0.3.0 release verification — 2026-10-28

Version 0.3.0 uses version code 4. After the version update and physical-device test-harness changes, `assembleDebug`, `assembleDebugAndroidTest`, and `lintDebug` passed. All 32 connected tests passed again on the disposable API 37 emulator, with zero failures, errors, or skipped cases. Lint reported zero errors and two existing tool-version update advisories.

The packaged APK's application ID, version name, and version code were checked directly. Its signing certificate matches the published 0.2.0 APK, preserving in-place update compatibility. The release includes the installable debug-signed APK and its SHA256SUMS file.
