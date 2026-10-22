package com.december172.daytiles.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.NavigationDrawerItemDefaults
import com.december172.daytiles.ui.planner.MoreScreen
import com.december172.daytiles.ui.planner.WidgetsScreen
import androidx.compose.foundation.layout.width
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import com.december172.daytiles.ui.calendar.CalendarRoute
import com.december172.daytiles.ui.day.DayEditorPopup
import com.december172.daytiles.ui.day.JournalQuickEntryPopup
import com.december172.daytiles.ui.day.TaskEditorRoute
import com.december172.daytiles.ui.day.TaskQuickEntryPopup
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
        DrawerDestination("calendar", R.string.calendar_tab, R.drawable.ic_fluent_calendar_month_24_regular),
        DrawerDestination("tasks", R.string.tasks_tab, R.drawable.ic_fluent_clipboard_task_24_regular),
        DrawerDestination("tags", R.string.tags_title, R.drawable.ic_fluent_tag_24_regular),
        DrawerDestination("goals", R.string.goals_title, R.drawable.ic_fluent_target_24_regular),
        DrawerDestination("quadrants", R.string.quadrants_title, R.drawable.ic_fluent_apps_list_24_regular),
        DrawerDestination("week", R.string.week_title, R.drawable.ic_fluent_calendar_week_start_24_regular),
        DrawerDestination("courses", R.string.course_timetable_title, R.drawable.ic_fluent_calendar_agenda_24_regular),
        DrawerDestination("focus", R.string.focus_title, R.drawable.ic_fluent_timer_24_regular),
        DrawerDestination("settings", R.string.settings_title, R.drawable.ic_fluent_settings_24_regular),
        DrawerDestination("help", R.string.help_title, R.drawable.ic_fluent_question_circle_24_regular),
    )
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val navigatePrimary: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    val openDestination: (String) -> Unit = { route ->
        scope.launch { drawerState.close() }
        if (route in listOf("calendar", "tasks", "goals", "widgets", "more")) navigatePrimary(route)
        else navController.navigate(route) { launchSingleTop = true }
    }
    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(304.dp), drawerContainerColor = MaterialTheme.colorScheme.surface) {
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Text(stringResource(R.string.app_name), Modifier.padding(start = 24.dp, top = 24.dp), style = MaterialTheme.typography.headlineSmall)
                    Text(stringResource(R.string.local_notebook), Modifier.padding(start = 24.dp, top = 4.dp, bottom = 24.dp),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                        listOf("calendar", "week", "tasks").map { route -> destinations.first { it.route == route } }.forEach { destination ->
                            Column(Modifier.weight(1f).clickable { openDestination(destination.route) }.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(38.dp).background(MaterialTheme.colorScheme.secondaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(painterResource(destination.icon), null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.secondary)
                                }
                                Text(stringResource(destination.label), fontSize = 11.sp, maxLines = 1)
                            }
                        }
                    }
                    HorizontalDivider(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    Text(stringResource(R.string.planner_tools), Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    destinations.filter { it.route !in listOf("calendar", "week", "tasks", "settings", "help") }.forEach { destination ->
                        NavigationDrawerItem(
                            label = { Text(stringResource(destination.label), fontSize = 15.sp) },
                            icon = { Icon(painterResource(destination.icon), null, Modifier.size(22.dp)) },
                            selected = currentRoute == destination.route,
                            onClick = { openDestination(destination.route) },
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.secondary,
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp).height(48.dp),
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.fillMaxWidth().padding(12.dp)) {
                    destinations.takeLast(2).forEach { destination ->
                        androidx.compose.material3.TextButton(onClick = { openDestination(destination.route) }, modifier = Modifier.weight(1f)) {
                            Icon(painterResource(destination.icon), null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(destination.label))
                        }
                    }
                }
            }
        },
    ) {
    NavHost(navController = navController, startDestination = CALENDAR_ROUTE) {
        composable(CALENDAR_ROUTE) {
            CalendarRoute(
                onOpenDay = { date -> navController.navigate("day/$date") },
                onCreateTask = { date -> navController.navigate("new-task/$date") },
                onEditTask = { task -> navController.navigate("task/${task.date}?taskId=${task.id}") },
                onOpenJournal = { date -> navController.navigate("journal/$date") },
                initialDate = initialDate,
                openJournalInitially = openJournal,
                onOpenMenu = { scope.launch { drawerState.open() } },
                onNavigatePrimary = navigatePrimary,
            )
        }
        composable("tasks") {
            CalendarRoute(
                onOpenDay = { date -> navController.navigate("day/$date") },
                onCreateTask = { date -> navController.navigate("new-task/$date") },
                onEditTask = { task -> navController.navigate("task/${task.date}?taskId=${task.id}") },
                onOpenJournal = { date -> navController.navigate("journal/$date") },
                openTasksInitially = true,
                onOpenMenu = { scope.launch { drawerState.open() } },
                onNavigatePrimary = navigatePrimary,
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
        dialog(
            route = "new-task/{date}",
            arguments = listOf(navArgument("date") { type = NavType.StringType }),
            dialogProperties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) { entry ->
            val date = LocalDate.parse(requireNotNull(entry.arguments?.getString("date")))
            TaskQuickEntryPopup(date = date, onDismiss = { navController.navigateUp() })
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
        listOf("goals", "quadrants", "week", "courses", "course_scheduler", "settings", "help", "tags").forEach { route ->
            composable(route) { PlannerArea(route, onNavigatePrimary = navigatePrimary) { navController.navigateUp() } }
        }
        composable("widgets") {
            WidgetsScreen(onNavigatePrimary = navigatePrimary, onBack = navController::navigateUp)
        }
        composable("more") {
            MoreScreen(onNavigatePrimary = navigatePrimary, onOpen = { route ->
                navController.navigate(route) { launchSingleTop = true }
            }, onBack = navController::navigateUp)
        }
        composable("focus") {
            PlannerArea("focus", onBack = navController::navigateUp, onNavigatePrimary = navigatePrimary)
        }
    }
    }
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }
}

private data class DrawerDestination(val route: String, val label: Int, val icon: Int)
