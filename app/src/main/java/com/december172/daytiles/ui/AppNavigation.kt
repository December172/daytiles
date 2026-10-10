package com.december172.daytiles.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
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
import com.december172.daytiles.ui.planner.PlannerArea
import com.december172.daytiles.R
import kotlinx.coroutines.launch
import java.time.LocalDate

private const val CALENDAR_ROUTE = "calendar"
private const val DAY_POPUP_ROUTE = "day/{date}"
private const val JOURNAL_POPUP_ROUTE = "journal/{date}"
private const val TASK_EDITOR_ROUTE = "task/{date}?taskId={taskId}"

@Composable
fun AppNavigation(initialDate: LocalDate? = null, openJournal: Boolean = false) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val destinations = listOf(
        "calendar" to R.string.calendar_tab,
        "tasks" to R.string.tasks_tab,
        "tags" to R.string.tags_title,
        "goals" to R.string.goals_title,
        "quadrants" to R.string.quadrants_title,
        "week" to R.string.week_title,
        "courses" to R.string.courses_title,
        "focus" to R.string.focus_title,
        "settings" to R.string.settings_title,
        "help" to R.string.help_title,
    )
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(stringResource(R.string.app_name), modifier = androidx.compose.ui.Modifier.padding(20.dp), style = MaterialTheme.typography.titleLarge)
                destinations.forEach { (route, label) ->
                    NavigationDrawerItem(
                        label = { Text(stringResource(label)) },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            navController.navigate(route) { launchSingleTop = true }
                        },
                        modifier = androidx.compose.ui.Modifier.padding(horizontal = 12.dp),
                    )
                }
            }
        },
    ) {
    NavHost(navController = navController, startDestination = CALENDAR_ROUTE) {
        composable(CALENDAR_ROUTE) {
            CalendarRoute(
                onOpenDay = { date -> navController.navigate("day/$date") },
                onCreateTask = { date -> navController.navigate("task/$date") },
                onEditTask = { task -> navController.navigate("task/${task.date}?taskId=${task.id}") },
                onOpenJournal = { date -> navController.navigate("journal/$date") },
                initialDate = initialDate,
                openJournalInitially = openJournal,
                onOpenMenu = { scope.launch { drawerState.open() } },
            )
        }
        composable("tasks") {
            CalendarRoute(
                onOpenDay = { date -> navController.navigate("day/$date") },
                onCreateTask = { date -> navController.navigate("task/$date") },
                onEditTask = { task -> navController.navigate("task/${task.date}?taskId=${task.id}") },
                onOpenJournal = { date -> navController.navigate("journal/$date") },
                openTasksInitially = true,
                onOpenMenu = { scope.launch { drawerState.open() } },
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
                // Navigation's LongType cannot represent null; use a sentinel for task creation.
                navArgument("taskId") { type = NavType.LongType; defaultValue = -1L },
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
        listOf("goals", "quadrants", "week", "courses", "focus", "settings", "help", "tags").forEach { route ->
            composable(route) { PlannerArea(route) { navController.navigateUp() } }
        }
    }
    }
}
