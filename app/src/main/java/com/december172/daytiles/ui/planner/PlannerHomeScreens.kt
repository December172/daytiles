package com.december172.daytiles.ui.planner

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
fun WidgetsScreen(onNavigatePrimary: (String) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val manager = AppWidgetManager.getInstance(context)
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
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text(stringResource(R.string.widgets_hint), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))
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
                            if (manager.isRequestPinAppWidgetSupported) {
                                val result = runCatching { manager.requestPinAppWidget(ComponentName(context, option.provider), null, null) }.getOrDefault(false)
                                if (!result) Toast.makeText(context, R.string.widget_pin_failed, Toast.LENGTH_LONG).show()
                            } else Toast.makeText(context, R.string.widget_pin_unavailable, Toast.LENGTH_LONG).show()
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
}

private data class ToolDestination(val route: String, val name: Int, val icon: Int)

@Composable
fun MoreScreen(onNavigatePrimary: (String) -> Unit, onOpen: (String) -> Unit, onBack: () -> Unit) {
    val groups = listOf(
        listOf(
            ToolDestination("focus", R.string.focus_title, R.drawable.ic_fluent_timer_24_regular),
            ToolDestination("tags", R.string.tags_title, R.drawable.ic_fluent_tag_24_regular),
            ToolDestination("quadrants", R.string.quadrants_title, R.drawable.ic_fluent_apps_list_24_regular),
        ),
        listOf(
            ToolDestination("week", R.string.week_title, R.drawable.ic_fluent_calendar_week_start_24_regular),
            ToolDestination("courses", R.string.course_timetable_title, R.drawable.ic_fluent_calendar_agenda_24_regular),
            ToolDestination("course_scheduler", R.string.course_scheduler_title, R.drawable.ic_fluent_book_24_regular),
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
