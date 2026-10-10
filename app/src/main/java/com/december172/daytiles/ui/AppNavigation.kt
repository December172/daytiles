package com.december172.daytiles.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.december172.daytiles.ui.calendar.CalendarRoute
import com.december172.daytiles.ui.day.DayEditorRoute
import java.time.LocalDate

private const val CALENDAR_ROUTE = "calendar"
private const val DAY_ROUTE = "day/{date}"

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = CALENDAR_ROUTE) {
        composable(CALENDAR_ROUTE) {
            CalendarRoute(onOpenDay = { date -> navController.navigate("day/$date") })
        }
        composable(
            route = DAY_ROUTE,
            arguments = listOf(navArgument("date") { type = NavType.StringType }),
        ) { entry ->
            val date = LocalDate.parse(requireNotNull(entry.arguments?.getString("date")))
            DayEditorRoute(date = date, onBack = navController::navigateUp)
        }
    }
}
