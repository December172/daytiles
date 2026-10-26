package com.december172.daytiles.ui.planner

import android.content.ComponentName
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.december172.daytiles.R
import com.december172.daytiles.ui.components.PlannerBottomBar
import com.december172.daytiles.ui.components.PlannerTopBar
import com.december172.daytiles.widget.*

private data class WidgetOption(val name: Int, val description: Int, val icon: Int, val provider: Class<*>)

@Composable
fun WidgetsScreen(onNavigatePrimary: (String) -> Unit, onBack: () -> Unit, pinViewModel: WidgetPinViewModel = viewModel()) {
    val context = LocalContext.current
    val pin by pinViewModel.state.collectAsStateWithLifecycle()
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val listState = rememberLazyListState()
    LaunchedEffect(pin.status, lifecycle) {
        if (lifecycle == Lifecycle.State.RESUMED) {
            if (pin.status != WidgetPinStatus.IDLE) listState.animateScrollToItem(1)
            pinViewModel.offerUnconfirmedHelp()
        }
    }
    val options = listOf(
        WidgetOption(R.string.todo_widget_name, R.string.todo_widget_description, R.drawable.ic_fluent_clipboard_task_24_regular, TodoWidgetProvider::class.java),
        WidgetOption(R.string.month_widget_name, R.string.month_widget_description, R.drawable.ic_fluent_calendar_month_24_regular, MonthWidgetProvider::class.java),
        WidgetOption(R.string.widget_name, R.string.widget_description, R.drawable.ic_fluent_book_24_regular, DayTilesWidgetProvider::class.java),
        WidgetOption(R.string.week_widget_name, R.string.week_widget_description, R.drawable.ic_fluent_calendar_week_start_24_regular, WeekWidgetProvider::class.java),
        WidgetOption(R.string.focus_widget_name, R.string.focus_widget_description, R.drawable.ic_fluent_timer_24_regular, FocusWidgetProvider::class.java),
    )
    Scaffold(
        topBar = { PlannerTopBar(stringResource(R.string.widgets_tab), onBack) },
        bottomBar = { PlannerBottomBar("widgets", onNavigatePrimary) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), state = listState, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(stringResource(R.string.widgets_hint), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
            }
            if (pin.status != WidgetPinStatus.IDLE) {
                item {
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(pin.name), style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(when (pin.status) {
                                WidgetPinStatus.ADDED -> R.string.widget_pin_added
                                WidgetPinStatus.WAITING -> R.string.widget_pin_waiting
                                WidgetPinStatus.UNCONFIRMED -> R.string.widget_pin_unconfirmed
                                else -> R.string.widget_pin_manual_required
                            }), style = MaterialTheme.typography.bodySmall)
                            if (pin.status != WidgetPinStatus.ADDED) {
                                Row {
                                    TextButton(onClick = pinViewModel::showHelp) { Text(stringResource(R.string.widget_manual_add)) }
                                    if (pin.status != WidgetPinStatus.WAITING) TextButton(onClick = pinViewModel::retry) { Text(stringResource(R.string.retry)) }
                                }
                            }
                        }
                    }
                }
            }
            items(options) { option ->
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface) {
                    Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(painterResource(option.icon), null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.secondary)
                            Text(stringResource(option.name), style = MaterialTheme.typography.titleMedium)
                        }
                        Text(stringResource(option.description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = {
                            pinViewModel.request(ComponentName(context, option.provider), option.name)
                        }, modifier = Modifier.align(Alignment.End)) {
                            Icon(painterResource(R.drawable.ic_planner_add), null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.pin_widget))
                        }
                    }
                }
            }
        }
    }
    if (pin.showHelp) {
        AlertDialog(
            onDismissRequest = pinViewModel::dismissHelp,
            title = { Text(stringResource(R.string.widget_manual_add)) },
            text = { Text(stringResource(R.string.widget_manual_steps, stringResource(R.string.app_name), stringResource(pin.name))) },
            confirmButton = { TextButton(onClick = {
                pinViewModel.dismissHelp()
                context.startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }) { Text(stringResource(R.string.open_home_screen)) } },
            dismissButton = { TextButton(onClick = pinViewModel::dismissHelp) { Text(stringResource(R.string.close)) } },
        )
    }

}

private data class ToolDestination(val route: String, val name: Int, val icon: Int)

@Composable
fun MoreScreen(onNavigatePrimary: (String) -> Unit, onOpen: (String) -> Unit, onBack: () -> Unit) {
    val groups = listOf(
        listOf(
            ToolDestination("full-schedule", R.string.full_schedule_title, R.drawable.ic_fluent_calendar_agenda_24_regular),
            ToolDestination("finance", R.string.finance_title, R.drawable.ic_planner_wallet),
            ToolDestination("recycle-bin", R.string.recycle_bin_title, R.drawable.ic_planner_trash),
        ),
        listOf(
            ToolDestination("focus", R.string.focus_title, R.drawable.ic_fluent_timer_24_regular),
            ToolDestination("tags", R.string.tags_title, R.drawable.ic_fluent_tag_24_regular),
            ToolDestination("quadrants", R.string.quadrants_title, R.drawable.ic_fluent_apps_list_24_regular),
        ),
        listOf(
            ToolDestination("week", R.string.week_title, R.drawable.ic_fluent_calendar_week_start_24_regular),
            ToolDestination("courses", R.string.course_timetable_title, R.drawable.ic_fluent_calendar_agenda_24_regular),
        ),
        listOf(
            ToolDestination("settings", R.string.settings_title, R.drawable.ic_fluent_settings_24_regular),
            ToolDestination("help", R.string.help_title, R.drawable.ic_fluent_question_circle_24_regular),
        ),
    )
    Scaffold(
        topBar = { PlannerTopBar(stringResource(R.string.me_tab), onBack) },
        bottomBar = { PlannerBottomBar("more", onNavigatePrimary) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.size(58.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(painterResource(R.drawable.ic_fluent_calendar_month_24_regular), null, Modifier.size(30.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                        Text(stringResource(R.string.local_notebook), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(groups) { group ->
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface) {
                    Column {
                        group.forEachIndexed { index, tool ->
                            Row(Modifier.fillMaxWidth().clickable { onOpen(tool.route) }.padding(horizontal = 16.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(painterResource(tool.icon), null, Modifier.size(23.dp), tint = MaterialTheme.colorScheme.secondary)
                                Spacer(Modifier.width(16.dp))
                                Text(stringResource(tool.name), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                                Icon(painterResource(R.drawable.ic_planner_chevron_right), null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.outline)
                            }
                            if (index < group.lastIndex) HorizontalDivider(Modifier.padding(start = 55.dp), color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}
