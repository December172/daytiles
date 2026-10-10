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
import java.time.LocalDate

private const val CALENDAR_ROUTE = "calendar"
private const val DAY_POPUP_ROUTE = "day/{date}"
private const val JOURNAL_POPUP_ROUTE = "journal/{date}"

@Composable
fun AppNavigation(initialDate: LocalDate? = null, openJournal: Boolean = false) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = CALENDAR_ROUTE) {
        composable(CALENDAR_ROUTE) {
            CalendarRoute(
                onOpenDay = { date -> navController.navigate("day/$date") },
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
