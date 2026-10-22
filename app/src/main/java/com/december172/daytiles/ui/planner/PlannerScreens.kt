package com.december172.daytiles.ui.planner

import android.content.Context
import android.content.Intent
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.edit
import com.december172.daytiles.ui.components.PlannerBottomBar
import com.december172.daytiles.ui.components.PlannerTopBar
import com.december172.daytiles.DayTilesApplication
import com.december172.daytiles.R
import com.december172.daytiles.data.local.TaskEntity
import com.december172.daytiles.widget.FocusTimerService
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

@Composable
fun PlannerArea(route: String, onNavigatePrimary: (String) -> Unit = {}, onBack: () -> Unit) {
    when (route) {
        "goals" -> GoalsScreen(onBack, onNavigatePrimary)
        "quadrants" -> QuadrantsScreen(onBack)
        "week" -> WeekScreen(onBack)
        "courses", "course_scheduler" -> CoursesScreen(onBack)
        "focus" -> FocusScreen(onBack, onNavigatePrimary)
        "settings" -> SettingsScreen(onBack)
        "help" -> InfoScreen(route, onBack)
        "tags" -> TagsScreen(onBack)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlannerScaffold(
    title: String,
    onBack: () -> Unit,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(topBar = { PlannerTopBar(title, onBack) }, bottomBar = bottomBar) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun GoalsScreen(onBack: () -> Unit, onNavigatePrimary: (String) -> Unit) {
    val repo = (LocalContext.current.applicationContext as DayTilesApplication).repository
    val today = LocalDate.now()
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val locale = LocalConfiguration.current.locales[0]
    var selectedEpoch by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    val selectedDate = LocalDate.ofEpochDay(selectedEpoch)
    var weekly by rememberSaveable { mutableStateOf(false) }
    var target by remember { mutableIntStateOf(3) }
    var savedTarget by remember { mutableStateOf<Int?>(null) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val start = if (weekly) monday else selectedDate
    val end = if (weekly) monday.plusDays(6) else selectedDate
    val tasks by remember(start, end) { repo.observeTasksBetween(start, end) }.collectAsStateWithLifecycle(emptyList())
    val completed = tasks.count { it.isCompleted }
    LaunchedEffect(start, weekly) {
        savedTarget = repo.goal(if (weekly) "WEEK" else "DAY", start)?.targetCount
        target = savedTarget ?: if (weekly) 10 else 3
    }
    Scaffold(
        topBar = { PlannerTopBar(stringResource(R.string.target_tab), onBack) },
        bottomBar = { PlannerBottomBar("goals", onNavigatePrimary) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        floatingActionButton = {
            FloatingActionButton(onClick = { showEditor = true }, shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(60.dp), elevation = FloatingActionButtonDefaults.elevation(0.dp)) {
                Icon(painterResource(R.drawable.ic_planner_add), stringResource(R.string.goal_target), Modifier.size(32.dp))
            }
        },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets)) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp)) {
                (0L..6L).forEach { offset ->
                    val date = monday.plusDays(offset)
                    Column(Modifier.weight(1f).clickable { selectedEpoch = date.toEpochDay(); weekly = false }.padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, locale), fontSize = 12.sp,
                            color = if (offset >= 5) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Box(Modifier.size(30.dp).then(if (date == selectedDate) Modifier.background(MaterialTheme.colorScheme.primary, CircleShape) else Modifier), contentAlignment = Alignment.Center) {
                            Text(date.dayOfMonth.toString(), fontSize = 17.sp,
                                color = if (date == selectedDate) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
            val goal = savedTarget
            if (goal == null) {
                Column(Modifier.weight(1f).fillMaxWidth().padding(bottom = 90.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Icon(painterResource(R.drawable.ic_fluent_target_24_regular), null, Modifier.size(104.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(22.dp))
                    Text(stringResource(R.string.no_goal), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 17.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.add_goal_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            } else {
                Surface(modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface,
                    onClick = { target = goal; showEditor = true }) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(stringResource(if (weekly) R.string.weekly_goal else R.string.daily_goal), style = MaterialTheme.typography.titleMedium)
                        Text(pluralStringResource(R.plurals.goal_progress, goal, completed, goal), style = MaterialTheme.typography.headlineSmall)
                        LinearProgressIndicator(progress = { (completed.toFloat() / goal).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
                        Text(stringResource(R.string.goal_punchcard, completed / goal), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
    if (showEditor) {
        AlertDialog(onDismissRequest = { showEditor = false }, title = { Text(stringResource(R.string.goals_title)) }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(!weekly, { weekly = false }, label = { Text(stringResource(R.string.daily_goal)) })
                    FilterChip(weekly, { weekly = true }, label = { Text(stringResource(R.string.weekly_goal)) })
                }
                Text(stringResource(R.string.goal_target))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { target = (target - 1).coerceAtLeast(1) }) { Text("−") }
                    Text(target.toString(), Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.headlineMedium)
                    IconButton(onClick = { target++ }) { Text("+") }
                }
            }
        }, confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    repo.saveGoal(if (weekly) "WEEK" else "DAY", start, target)
                    savedTarget = target
                    showEditor = false
                }
            }) { Text(stringResource(R.string.save)) }
        }, dismissButton = { TextButton(onClick = { showEditor = false }) { Text(stringResource(R.string.cancel)) } })
    }
}

@Composable
private fun QuadrantsScreen(onBack: () -> Unit) {
    val repo = (LocalContext.current.applicationContext as DayTilesApplication).repository
    val today = LocalDate.now()
    val tasks by repo.observeTasksBetween(today.minusDays(30), today.plusDays(365)).collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    val labels = listOf(
        R.string.quadrant_do, R.string.quadrant_schedule, R.string.quadrant_delegate, R.string.quadrant_eliminate,
    )
    PlannerScaffold(stringResource(R.string.quadrants_title), onBack) {
        Text(stringResource(R.string.quadrants_hint), style = MaterialTheme.typography.bodyMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(labels.indices.toList()) { q ->
                Card {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(stringResource(labels[q]), style = MaterialTheme.typography.titleMedium)
                        val groupImportance = 1 - q / 2
                        val groupUrgency = 1 - q % 2
                        val group = tasks.filter { it.prioritySet && it.importance == groupImportance && it.urgency == groupUrgency }
                        group.forEach { task ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(task.title, Modifier.weight(1f))
                                TextButton(onClick = {
                                    val newQ = (q + 1) % 4
                                    scope.launch {
                                        repo.setTaskPriority(task, 1 - newQ / 2, 1 - newQ % 2)
                                    }
                                }) { Text(stringResource(R.string.move_next_quadrant)) }
                            }
                        }
                        if (group.isEmpty()) Text(stringResource(R.string.no_tasks), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekScreen(onBack: () -> Unit) {
    val repo = (LocalContext.current.applicationContext as DayTilesApplication).repository
    var monday by remember { mutableStateOf(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))) }
    var tasks by remember { mutableStateOf(emptyList<TaskEntity>()) }
    var journals by remember { mutableStateOf(emptyMap<LocalDate, String>()) }
    LaunchedEffect(monday) {
        val end = monday.plusDays(6)
        tasks = repo.tasksBetween(monday, end)
        journals = repo.journalsBetween(monday, end).associate { it.date to it.content }
    }
    PlannerScaffold(stringResource(R.string.week_title), onBack) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { monday = monday.minusWeeks(1) }) { Text("‹") }
            Text("${monday} – ${monday.plusDays(6)}", Modifier.weight(1f))
            TextButton(onClick = { monday = monday.plusWeeks(1) }) { Text("›") }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items((0L..6L).toList()) { offset ->
                val date = monday.plusDays(offset)
                Card {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(date.toString(), style = MaterialTheme.typography.titleMedium)
                        tasks.filter { it.date == date }.forEach { Text("• ${it.title}") }
                        journals[date]?.takeIf(String::isNotBlank)?.let {
                            Text(it.take(140), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FocusScreen(onBack: () -> Unit, onNavigatePrimary: (String) -> Unit) {
    val context = LocalContext.current
    val repository = (context.applicationContext as DayTilesApplication).repository
    var openTasks by remember { mutableStateOf(emptyList<TaskEntity>()) }
    var linkedTask by remember { mutableStateOf<TaskEntity?>(null) }
    var taskMenuExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { openTasks = repository.allOpenTasks() }
    val preferences = remember { context.getSharedPreferences("focus_clock", Context.MODE_PRIVATE) }
    var workMinutes by remember { mutableIntStateOf(preferences.getInt("work", 25)) }
    var breakMinutes by remember { mutableIntStateOf(preferences.getInt("break", 5)) }
    var endAt by remember { mutableLongStateOf(preferences.getLong("end_at", 0L)) }
    var pausedRemaining by remember { mutableLongStateOf(preferences.getLong("remaining", 0L)) }
    var remaining by remember { mutableLongStateOf(0L) }
    var phase by remember { mutableStateOf(preferences.getString(FocusTimerService.KEY_PHASE, FocusTimerService.PHASE_WORK) ?: FocusTimerService.PHASE_WORK) }
    fun startFocus() {
        workMinutes = workMinutes.coerceIn(5, 120)
        preferences.edit {
            putInt("work", workMinutes)
            putInt("break", breakMinutes)
        }
        endAt = System.currentTimeMillis() + workMinutes * 60_000L
        pausedRemaining = 0
        phase = FocusTimerService.PHASE_WORK
        context.startForegroundService(
            Intent(context, FocusTimerService::class.java).setAction(FocusTimerService.ACTION_START)
                .putExtra(FocusTimerService.EXTRA_WORK, workMinutes * 60_000L)
                .putExtra(FocusTimerService.EXTRA_BREAK, breakMinutes * 60_000L)
                .putExtra(FocusTimerService.EXTRA_TASK_TITLE, linkedTask?.title)
                .putExtra(FocusTimerService.EXTRA_TASK_ID, linkedTask?.id ?: 0L),
        )
    }
    val focusNotificationPermission = rememberLauncherForActivityResult(RequestPermission()) { startFocus() }
    LaunchedEffect(endAt, pausedRemaining) {
        while (endAt > 0) {
            remaining = if (endAt > 0) (endAt - System.currentTimeMillis()).coerceAtLeast(0) else pausedRemaining
            if (remaining == 0L) {
                val nextEnd = preferences.getLong(FocusTimerService.KEY_END, 0L)
                if (nextEnd > System.currentTimeMillis()) {
                    phase = preferences.getString(FocusTimerService.KEY_PHASE, FocusTimerService.PHASE_BREAK) ?: FocusTimerService.PHASE_BREAK
                    endAt = nextEnd
                } else {
                    endAt = 0
                    remaining = 0
                }
            } else {
                delay(1000)
            }
        }
        if (pausedRemaining > 0) remaining = pausedRemaining
    }
    PlannerScaffold(
        stringResource(R.string.focus_title),
        onBack,
        bottomBar = {
            PlannerBottomBar("focus", onNavigatePrimary)
        },
    ) {
        Text(stringResource(if (phase == FocusTimerService.PHASE_BREAK) R.string.focus_break_phase else R.string.focus_work_phase), style = MaterialTheme.typography.titleMedium)
        Text("%02d:%02d".format(remaining / 60_000, remaining / 1000 % 60), style = MaterialTheme.typography.displayLarge)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.focus_work))
            IconButton(onClick = { workMinutes = (workMinutes - 5).coerceAtLeast(5) }) { Text("−") }
            Text("$workMinutes min")
            IconButton(onClick = { workMinutes = (workMinutes + 5).coerceAtMost(120) }) { Text("+") }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.focus_break))
            IconButton(onClick = { breakMinutes = (breakMinutes - 1).coerceAtLeast(1) }) { Text("−") }
            Text("$breakMinutes min")
            IconButton(onClick = { breakMinutes = (breakMinutes + 1).coerceAtMost(60) }) { Text("+") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    focusNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else startFocus()
            }, enabled = endAt == 0L && pausedRemaining == 0L) { Text(stringResource(R.string.focus_start)) }
            OutlinedButton(onClick = {
                if (endAt > 0) {
                    pausedRemaining = (endAt - System.currentTimeMillis()).coerceAtLeast(0)
                    endAt = 0
                    context.startService(Intent(context, FocusTimerService::class.java).setAction(FocusTimerService.ACTION_PAUSE))
                } else if (pausedRemaining > 0) {
                    endAt = System.currentTimeMillis() + pausedRemaining
                    pausedRemaining = 0
                    context.startService(Intent(context, FocusTimerService::class.java).setAction(FocusTimerService.ACTION_RESUME))
                }
            }, enabled = endAt > 0 || pausedRemaining > 0) { Text(stringResource(if (endAt > 0) R.string.focus_pause else R.string.focus_resume)) }
            TextButton(onClick = {
                endAt = 0; pausedRemaining = 0; remaining = 0
                context.startService(Intent(context, FocusTimerService::class.java).setAction(FocusTimerService.ACTION_STOP))
            }) { Text(stringResource(R.string.focus_stop)) }
        }
        ExposedDropdownMenuBox(
            expanded = taskMenuExpanded,
            onExpandedChange = { taskMenuExpanded = it },
        ) {
            OutlinedTextField(
                value = linkedTask?.title.orEmpty(),
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(R.string.focus_link_task)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(taskMenuExpanded) },
                modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = taskMenuExpanded, onDismissRequest = { taskMenuExpanded = false }) {
                DropdownMenuItem(text = { Text(stringResource(R.string.none)) }, onClick = { linkedTask = null; taskMenuExpanded = false })
                openTasks.forEach { task ->
                    DropdownMenuItem(text = { Text(task.title) }, onClick = { linkedTask = task; taskMenuExpanded = false })
                }
            }
        }
        Text(pluralStringResource(R.plurals.focus_break_note, breakMinutes, breakMinutes))
    }
}

@Composable
private fun InfoScreen(route: String, onBack: () -> Unit) {
    PlannerScaffold(stringResource(if (route == "help") R.string.help_title else R.string.settings_title), onBack) {
        Text(stringResource(if (route == "help") R.string.help_body else R.string.settings_body))
    }
}

@Composable
private fun TagsScreen(onBack: () -> Unit) {
    val repo = (LocalContext.current.applicationContext as DayTilesApplication).repository
    val today = LocalDate.now()
    val tasks by repo.observeTasksBetween(today.minusDays(3650), today.plusDays(3650)).collectAsStateWithLifecycle(emptyList())
    val tags = tasks.flatMap { it.tags.split(',') }.map(String::trim).filter(String::isNotEmpty).distinct().sorted()
    var selected by remember { mutableStateOf<String?>(null) }
    PlannerScaffold(stringResource(R.string.tags_title), onBack) {
        if (tags.isEmpty()) Text(stringResource(R.string.no_tags))
        tags.forEach { tag ->
            FilterChip(selected == tag, { selected = if (selected == tag) null else tag }, label = { Text(tag) })
        }
        tasks.filter { task -> selected == null || task.tags.split(',').map(String::trim).contains(selected) }
            .forEach { task -> Text(task.title, style = MaterialTheme.typography.bodyLarge) }
    }
}
