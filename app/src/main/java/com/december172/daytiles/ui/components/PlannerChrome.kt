package com.december172.daytiles.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.december172.daytiles.R

private data class PrimaryDestination(val route: String, val label: Int, val icon: Int)
private val primaryDestinations = listOf(
    PrimaryDestination("tasks", R.string.schedule_tab, R.drawable.ic_fluent_clipboard_task_24_regular),
    PrimaryDestination("calendar", R.string.view_tab, R.drawable.ic_planner_view),
    PrimaryDestination("goals", R.string.target_tab, R.drawable.ic_fluent_target_24_regular),
    PrimaryDestination("widgets", R.string.widgets_tab, R.drawable.ic_planner_widget),
    PrimaryDestination("more", R.string.me_tab, R.drawable.ic_planner_person),
)

@Composable
fun PlannerBottomBar(selectedRoute: String, onNavigate: (String) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        primaryDestinations.forEach { destination ->
            NavigationBarItem(
                selected = selectedRoute == destination.route,
                onClick = { if (selectedRoute != destination.route) onNavigate(destination.route) },
                icon = { Icon(painterResource(destination.icon), null, Modifier.size(24.dp)) },
                label = { Text(stringResource(destination.label), maxLines = 2, overflow = TextOverflow.Ellipsis) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerTopBar(
    title: String,
    onBack: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    CenterAlignedTopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_planner_chevron_left), stringResource(R.string.back))
            }
        },
        actions = actions,
    )
}
