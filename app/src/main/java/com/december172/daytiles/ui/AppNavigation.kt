package com.december172.daytiles.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.december172.daytiles.ui.calendar.CalendarRoute
import com.december172.daytiles.ui.day.DayEditorPopup
import com.december172.daytiles.ui.day.JournalQuickEntryPopup
import com.december172.daytiles.ui.day.TaskEditorRoute
import java.time.LocalDate

private const val CALENDAR_ROUTE = "calendar"
private const val DAY_POPUP_ROUTE = "day/{date}"
private const val JOURNAL_POPUP_ROUTE = "journal/{date}"
private const val TASK_EDITOR_ROUTE = "task/{date}?taskId={taskId}"

@Composable
fun AppNavigation(initialDate: LocalDate? = null, openJournal: Boolean = false) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = CALENDAR_ROUTE) {
        composable(CALENDAR_ROUTE) {
            CalendarRoute(
                onOpenDay = { date -> navController.navigate("day/$date") },
                onCreateTask = { date -> navController.navigate("task/$date") },
                onEditTask = { task -> navController.navigate("task/${task.date}?taskId=${task.id}") },
                onOpenJournal = { date -> navController.navigate("journal/$date") },
                initialDate = initialDate,
                openJournalInitially = openJournal,
            )
        }
        dialog(
            route = DAY_POPUP_ROUTE,
            arguments = listOf(navArgument("date") { type = NavType.StringType }),
            dialogProperties = DialogProperties(usePlatformDefaultWidth = false),
        ) { entry ->
            val date = LocalDate.parse(requireNotNull(entry.arguments?.getString("date")))
            DayEditorPopup(date = date, onDismiss = navController::navigateUp)
        }
        composable(
            route = TASK_EDITOR_ROUTE,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType },
                navArgument("taskId") { type = NavType.LongType; nullable = true; defaultValue = null },
            ),
        ) { entry ->
            val date = LocalDate.parse(requireNotNull(entry.arguments?.getString("date")))
            val taskId = entry.arguments?.getLong("taskId", -1L)?.takeIf { it > 0L }
            TaskEditorRoute(date = date, taskId = taskId, onBack = navController::navigateUp)
        }
        dialog(
            route = JOURNAL_POPUP_ROUTE,
            arguments = listOf(navArgument("date") { type = NavType.StringType }),
            dialogProperties = DialogProperties(usePlatformDefaultWidth = false),
        ) { entry ->
            val date = LocalDate.parse(requireNotNull(entry.arguments?.getString("date")))
            JournalQuickEntryPopup(date = date, onDismiss = navController::navigateUp)
        }
    }
}
