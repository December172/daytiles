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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.december172.daytiles.DayTilesApplication
import com.december172.daytiles.R
import com.december172.daytiles.data.local.CourseEntity
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
        "goals" -> GoalsScreen(onBack)
        "quadrants" -> QuadrantsScreen(onBack)
        "week" -> WeekScreen(onBack)
        "courses" -> CoursesScreen(onBack)
        "course_scheduler" -> CourseSchedulerScreen(onBack)
        "focus" -> FocusScreen(onBack, onNavigatePrimary)
        "settings", "help" -> InfoScreen(route, onBack)
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
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.back)) } },
        )
    }, bottomBar = bottomBar) { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun GoalsScreen(onBack: () -> Unit) {
    val repo = (LocalContext.current.applicationContext as DayTilesApplication).repository
    val today = LocalDate.now()
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    var weekly by remember { mutableStateOf(false) }
    var target by remember { mutableIntStateOf(5) }
    var completed by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val start = if (weekly) monday else today
    LaunchedEffect(weekly) {
        target = repo.goal(if (weekly) "WEEK" else "DAY", start)?.targetCount ?: if (weekly) 10 else 3
        val tasks = repo.tasksBetween(start, if (weekly) monday.plusDays(6) else today)
        completed = tasks.count { it.isCompleted }
    }
    PlannerScaffold(stringResource(R.string.goals_title), onBack) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FilterChip(selected = !weekly, onClick = { weekly = false }, label = { Text(stringResource(R.string.daily_goal)) })
            Spacer(Modifier.width(8.dp))
            FilterChip(selected = weekly, onClick = { weekly = true }, label = { Text(stringResource(R.string.weekly_goal)) })
        }
        Text(stringResource(R.string.goal_progress, completed, target), style = MaterialTheme.typography.headlineSmall)
        LinearProgressIndicator(progress = { (completed.toFloat() / target).coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth())
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.goal_target))
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { target = (target - 1).coerceAtLeast(1) }) { Text("−") }
            Text(target.toString())
            IconButton(onClick = { target += 1 }) { Text("+") }
        }
        Button(onClick = { scope.launch { repo.saveGoal(if (weekly) "WEEK" else "DAY", start, target) } }) {
            Text(stringResource(R.string.save))
        }
        Text(stringResource(R.string.goal_punchcard, (completed / target).coerceAtLeast(0)), style = MaterialTheme.typography.titleMedium)
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
private fun CoursesScreen(onBack: () -> Unit) {
    val repo = (LocalContext.current.applicationContext as DayTilesApplication).repository
    val courses by repo.observeCourses().collectAsStateWithLifecycle(emptyList())
    var monday by remember { mutableStateOf(LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))) }
    val locale = LocalConfiguration.current.locales[0]
    val dayLabels = (0L..6L).map { monday.plusDays(it).dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, locale) }
    val startMinute = 7 * 60
    val endMinute = 22 * 60
    val hourHeight = 64.dp
    val labelWidth = 48.dp
    PlannerScaffold(stringResource(R.string.course_timetable_title), onBack) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { monday = monday.minusWeeks(1) }) { Text("‹") }
            Text(stringResource(R.string.course_week_range, monday.toString(), monday.plusDays(6).toString()), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            TextButton(onClick = { monday = monday.plusWeeks(1) }) { Text("›") }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(labelWidth))
            (0L..6L).forEach { offset ->
                val day = monday.plusDays(offset)
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(dayLabels[offset.toInt()], style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        Row(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
            Column(Modifier.width(labelWidth)) {
                (startMinute / 60 until endMinute / 60).forEach { hour ->
                    Text("%02d:00".format(hour), style = MaterialTheme.typography.labelSmall, modifier = Modifier.height(hourHeight).padding(top = 2.dp))
                }
            }
            Row(Modifier.weight(1f).height(hourHeight * ((endMinute - startMinute) / 60))) {
                (1..7).forEach { weekday ->
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        Column(Modifier.fillMaxSize()) {
                            repeat((endMinute - startMinute) / 60) {
                                Box(Modifier.fillMaxWidth().weight(1f)) {
                                    HorizontalDivider(Modifier.align(Alignment.TopStart), color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                        VerticalDivider(Modifier.align(Alignment.CenterStart), color = MaterialTheme.colorScheme.outlineVariant)
                        courses.filter { it.weekday == weekday && it.endMinute > startMinute && it.startMinute < endMinute }.forEachIndexed { index, course ->
                            val visibleStart = course.startMinute.coerceAtLeast(startMinute)
                            val visibleEnd = course.endMinute.coerceAtMost(endMinute)
                            val y = ((visibleStart - startMinute) / 60f * 64f).dp
                            val height = (((visibleEnd - visibleStart) / 60f) * 64f).dp.coerceAtLeast(34.dp)
                            val palette = listOf(Color(0xFFFFE9C7), Color(0xFFDDF1FF), Color(0xFFF6DDF1), Color(0xFFE0F4E8), Color(0xFFEAE5FF))
                            Column(
                                Modifier.fillMaxWidth().padding(horizontal = 1.dp).offset(y = y).height(height)
                                    .background(palette[index % palette.size], RoundedCornerShape(7.dp)).padding(3.dp),
                                verticalArrangement = Arrangement.spacedBy(1.dp),
                            ) {
                                Text(course.name, style = MaterialTheme.typography.labelSmall, maxLines = 3, lineHeight = MaterialTheme.typography.labelSmall.lineHeight)
                                if (height >= 52.dp) Text(formatMinute(course.startMinute), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                if (course.location.isNotBlank() && height >= 64.dp) Text(course.location, style = MaterialTheme.typography.labelSmall, maxLines = 2)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CourseSchedulerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = (context.applicationContext as DayTilesApplication).repository
    val courses by repo.observeCourses().collectAsStateWithLifecycle(emptyList())
    val locale = LocalConfiguration.current.locales[0]
    val scope = rememberCoroutineScope()
    var editingCourse by remember { mutableStateOf<CourseEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingCourse by remember { mutableStateOf<CourseEntity?>(null) }
    val reminderPermission = rememberLauncherForActivityResult(RequestPermission()) {
        pendingCourse?.let { course -> scope.launch { if (course.id == 0L) repo.addCourse(course) else repo.updateCourse(course) } }
        pendingCourse = null
    }
    PlannerScaffold(stringResource(R.string.course_scheduler_title), onBack) {
        Text(stringResource(R.string.course_scheduler_hint), style = MaterialTheme.typography.bodyMedium)
        Button(onClick = { editingCourse = null; showEditor = true }) { Text(stringResource(R.string.add_course)) }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(courses, key = CourseEntity::id) { course ->
                Card {
                    ListItem(
                        headlineContent = { Text(course.name) },
                        supportingContent = { Text("${DayOfWeek.of(course.weekday).getDisplayName(java.time.format.TextStyle.FULL, locale)} · ${formatMinute(course.startMinute)}–${formatMinute(course.endMinute)}${course.location.takeIf(String::isNotBlank)?.let { " · $it" } ?: ""}") },
                        trailingContent = {
                            Row {
                                TextButton(onClick = { editingCourse = course; showEditor = true }) { Text(stringResource(R.string.edit)) }
                                TextButton(onClick = { scope.launch { repo.deleteCourse(course.id) } }) { Text(stringResource(R.string.delete)) }
                            }
                        },
                    )
            }
        }
        }
    }
    if (showEditor) {
        CourseEditorDialog(
            initial = editingCourse,
            onDismiss = { showEditor = false },
            onSave = { course ->
                showEditor = false
                if (course.reminderMinutes > 0 && Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    pendingCourse = course
                    reminderPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else scope.launch {
                    if (course.id == 0L) repo.addCourse(course) else repo.updateCourse(course)
                }
            },
        )
    }
}

@Composable
private fun CourseEditorDialog(initial: CourseEntity?, onDismiss: () -> Unit, onSave: (CourseEntity) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var weekday by remember(initial) { mutableIntStateOf(initial?.weekday ?: 1) }
    var start by remember(initial) { mutableStateOf(initial?.let { formatMinute(it.startMinute) } ?: "09:00") }
    var end by remember(initial) { mutableStateOf(initial?.let { formatMinute(it.endMinute) } ?: "10:00") }
    var location by remember(initial) { mutableStateOf(initial?.location.orEmpty()) }
    var reminder by remember(initial) { mutableStateOf((initial?.reminderMinutes ?: 15).toString()) }
    val startMinute = parseMinute(start)
    val endMinute = parseMinute(end)
    val valid = name.isNotBlank() && startMinute != null && endMinute != null && endMinute > startMinute
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.add_course else R.string.edit_course)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.course_name)) }, singleLine = true)
                Text(stringResource(R.string.weekday), style = MaterialTheme.typography.labelLarge)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    (1..7).toList().chunked(4).forEach { days ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            days.forEach { day ->
                                FilterChip(
                                    selected = weekday == day,
                                    onClick = { weekday = day },
                                    label = { Text(DayOfWeek.of(day).getDisplayName(java.time.format.TextStyle.SHORT, locale), maxLines = 1) },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(start, { start = it }, label = { Text(stringResource(R.string.course_start)) }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(end, { end = it }, label = { Text(stringResource(R.string.course_end)) }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(location, { location = it }, label = { Text(stringResource(R.string.course_location)) }, singleLine = true)
                OutlinedTextField(reminder, { reminder = it.filter(Char::isDigit).take(3) }, label = { Text(stringResource(R.string.course_reminder_minutes)) }, singleLine = true)
                if (startMinute != null && endMinute != null && endMinute <= startMinute) Text(stringResource(R.string.course_invalid_time), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { TextButton(enabled = valid, onClick = { onSave(CourseEntity(id = initial?.id ?: 0, name = name.trim(), weekday = weekday, startMinute = startMinute!!, endMinute = endMinute!!, location = location.trim(), reminderMinutes = reminder.toIntOrNull() ?: 0, isEnabled = initial?.isEnabled ?: true)) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
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
        preferences.edit().putInt("work", workMinutes).putInt("break", breakMinutes).apply()
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
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = { onNavigatePrimary("calendar") },
                    icon = { Icon(painterResource(R.drawable.ic_fluent_calendar_month_24_regular), contentDescription = null) },
                    label = { Text(stringResource(R.string.calendar_tab)) },
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onNavigatePrimary("tasks") },
                    icon = { Icon(painterResource(R.drawable.ic_fluent_clipboard_task_24_regular), contentDescription = null) },
                    label = { Text(stringResource(R.string.tasks_tab)) },
                )
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(painterResource(R.drawable.ic_fluent_timer_24_regular), contentDescription = null) },
                    label = { Text(stringResource(R.string.focus_title)) },
                )
            }
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
        Text(stringResource(R.string.focus_break_note, breakMinutes))
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

private fun parseMinute(value: String): Int? = runCatching {
    val parts = value.split(":")
    require(parts.size == 2)
    parts[0].toInt().also { require(it in 0..23) } * 60 + parts[1].toInt().also { require(it in 0..59) }
}.getOrNull()

private fun formatMinute(value: Int): String = "%02d:%02d".format(value / 60, value % 60)
