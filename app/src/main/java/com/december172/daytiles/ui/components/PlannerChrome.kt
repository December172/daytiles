package com.december172.daytiles.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    Column(Modifier.background(MaterialTheme.colorScheme.surface).navigationBarsPadding()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
        Row(Modifier.fillMaxWidth().height(60.dp)) {
            primaryDestinations.forEach { destination ->
                val selected = selectedRoute == destination.route
                val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                Column(
                    Modifier.weight(1f).fillMaxHeight().selectable(
                        selected = selected, role = Role.Tab,
                        onClick = { if (!selected) onNavigate(destination.route) },
                    ).padding(top = 10.dp, bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(painterResource(destination.icon), null, Modifier.size(25.dp), tint = color)
                    Text(stringResource(destination.label), fontSize = 11.sp, lineHeight = 14.sp, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
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
        expandedHeight = 56.dp,
        title = { Text(title, fontSize = 20.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_planner_chevron_left), stringResource(R.string.back))
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
    )
}
