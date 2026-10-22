package com.december172.daytiles.ui.planner

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.december172.daytiles.DayTilesApplication
import com.december172.daytiles.R
import com.december172.daytiles.data.local.CourseEntity
import com.december172.daytiles.ui.components.PlannerTopBar
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = (context.applicationContext as DayTilesApplication).repository
    val courses by repo.observeCourses().collectAsStateWithLifecycle(emptyList())
    val locale = LocalConfiguration.current.locales[0]
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()
    var weekEpoch by rememberSaveable { mutableLongStateOf(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay()) }
    val monday = LocalDate.ofEpochDay(weekEpoch)
    var editingCourse by remember { mutableStateOf<CourseEntity?>(null) }
    var showList by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var pendingCourse by remember { mutableStateOf<CourseEntity?>(null) }
    fun persist(course: CourseEntity) {
        saving = true
        scope.launch {
            try {
                if (course.id == 0L) repo.addCourse(course) else repo.updateCourse(course)
                editingCourse = null
            } finally { saving = false }
        }
    }
    val permission = rememberLauncherForActivityResult(RequestPermission()) {
        pendingCourse?.let { persist(it) }
        pendingCourse = null
    }
    fun newCourse(weekday: Int = today.dayOfWeek.value, minute: Int = 9 * 60) {
        showList = false
        editingCourse = CourseEntity(name = "", weekday = weekday, startMinute = minute,
            endMinute = (minute + 60).coerceAtMost(24 * 60 - 1), location = "")
    }
    Scaffold(topBar = {
        PlannerTopBar(stringResource(R.string.course_timetable_title), onBack) {
            IconButton(onClick = { showList = true }) {
                Icon(painterResource(R.drawable.ic_fluent_apps_list_24_regular), stringResource(R.string.manage_courses))
            }
            IconButton(onClick = { newCourse() }) {
                Icon(painterResource(R.drawable.ic_planner_add), stringResource(R.string.add_course), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { weekEpoch -= 7 }) {
                    Icon(painterResource(R.drawable.ic_planner_chevron_left), stringResource(R.string.previous_week))
                }
                TextButton(onClick = {
                    DatePickerDialog(context, { _, year, month, day ->
                        weekEpoch = LocalDate.of(year, month + 1, day).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay()
                    }, monday.year, monday.monthValue - 1, monday.dayOfMonth).show()
                }, modifier = Modifier.weight(1f)) {
                    val format = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT).withLocale(locale)
                    Text(stringResource(R.string.course_week_range, monday.format(format), monday.plusDays(6).format(format)), maxLines = 1, fontSize = 12.sp)
                }
                TextButton(onClick = { weekEpoch = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).toEpochDay() }) {
                    Text(stringResource(R.string.today), fontSize = 12.sp)
                }
                IconButton(onClick = { weekEpoch += 7 }) {
                    Icon(painterResource(R.drawable.ic_planner_chevron_right), stringResource(R.string.next_week))
                }
            }
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(vertical = 8.dp)) {
                Box(Modifier.width(42.dp).align(Alignment.CenterVertically), contentAlignment = Alignment.Center) {
                    Text(monday.monthValue.toString(), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                (0L..6L).forEach { offset ->
                    val date = monday.plusDays(offset)
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale), fontSize = 10.sp, lineHeight = 14.sp, maxLines = 1)
                        Box(Modifier.size(24.dp).then(if (date == today) Modifier.background(MaterialTheme.colorScheme.primary, CircleShape) else Modifier), contentAlignment = Alignment.Center) {
                            Text(date.dayOfMonth.toString(), fontSize = 12.sp, lineHeight = 16.sp,
                                color = if (date == today) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
            if (courses.isEmpty()) {
                Text(stringResource(R.string.course_grid_hint), Modifier.fillMaxWidth().padding(12.dp),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            }
            val startHour = minOf(7, courses.minOfOrNull { it.startMinute / 60 } ?: 7).coerceAtLeast(0)
            val endHour = maxOf(22, courses.maxOfOrNull { (it.endMinute + 59) / 60 } ?: 22).coerceAtMost(24)
            val hourHeight = 64.dp
            Row(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                Column(Modifier.width(42.dp)) {
                    (startHour until endHour).forEach { hour ->
                        Text(courseTime(hour * 60), Modifier.height(hourHeight).fillMaxWidth().padding(top = 6.dp), fontSize = 9.sp, lineHeight = 12.sp,
                            textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(Modifier.weight(1f).height(hourHeight * (endHour - startHour))) {
                    (1..7).forEach { weekday ->
                        val positioned = remember(courses, weekday) { layoutCoursesForDay(courses.filter { it.weekday == weekday }) }
                        BoxWithConstraints(Modifier.weight(1f).fillMaxHeight()) {
                            Column(Modifier.fillMaxSize()) {
                                (startHour until endHour).forEach { hour ->
                                    val label = stringResource(R.string.add_course_at, DayOfWeek.of(weekday).getDisplayName(TextStyle.FULL, locale), courseTime(hour * 60))
                                    Box(Modifier.fillMaxWidth().height(hourHeight).clickable { newCourse(weekday, hour * 60) }
                                        .semantics { contentDescription = label }) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                                    }
                                }
                            }
                            VerticalDivider(Modifier.align(Alignment.CenterStart), color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                            positioned.forEach { (course, lane, count) ->
                                val y = hourHeight * ((course.startMinute - startHour * 60) / 60f)
                                val height = hourHeight * ((course.endMinute - course.startMinute) / 60f)
                                val width = maxWidth / count
                                val palette = listOf(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.primaryContainer)
                                Surface(
                                    onClick = { editingCourse = course },
                                    modifier = Modifier.offset(x = width * lane, y = y).width(width).height(height.coerceAtLeast(24.dp)).padding(1.dp),
                                    shape = RoundedCornerShape(6.dp), color = palette[(course.id % palette.size).toInt()],
                                ) {
                                    Column(Modifier.padding(3.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(course.name, fontSize = 10.sp, lineHeight = 13.sp, maxLines = 3)
                                        if (height >= 52.dp) Text(courseTime(course.startMinute), fontSize = 8.sp, lineHeight = 11.sp, maxLines = 1)
                                        if (height >= 70.dp && course.location.isNotBlank()) Text(course.location, fontSize = 9.sp, lineHeight = 12.sp, maxLines = 2)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showList) {
        ModalBottomSheet(onDismissRequest = { showList = false }) {
            Text(stringResource(R.string.manage_courses), Modifier.padding(horizontal = 20.dp, vertical = 8.dp), style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = { newCourse() }, modifier = Modifier.padding(horizontal = 12.dp)) { Text(stringResource(R.string.add_course)) }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp).navigationBarsPadding()) {
                if (courses.isEmpty()) item { Text(stringResource(R.string.no_courses), Modifier.padding(20.dp)) }
                items(courses, key = CourseEntity::id) { course ->
                    ListItem(headlineContent = { Text(course.name) }, supportingContent = {
                        Text("${DayOfWeek.of(course.weekday).getDisplayName(TextStyle.FULL, locale)} · ${courseTime(course.startMinute)}–${courseTime(course.endMinute)}")
                    }, modifier = Modifier.clickable { showList = false; editingCourse = course })
                }
            }
        }
    }
    editingCourse?.let { course ->
        CourseEditor(course, saving, onDismiss = { if (!saving) editingCourse = null }, onSave = { edited ->
            if (edited.reminderMinutes > 0 && Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                pendingCourse = edited; permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else persist(edited)
        }, onDelete = {
            scope.launch { repo.deleteCourse(course.id); editingCourse = null }
        })
    }
}

@Composable
private fun CourseEditor(course: CourseEntity, saving: Boolean, onDismiss: () -> Unit, onSave: (CourseEntity) -> Unit, onDelete: () -> Unit) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    var name by rememberSaveable(course.id) { mutableStateOf(course.name) }
    var weekday by rememberSaveable(course.id) { mutableIntStateOf(course.weekday) }
    var start by rememberSaveable(course.id) { mutableIntStateOf(course.startMinute) }
    var end by rememberSaveable(course.id) { mutableIntStateOf(course.endMinute) }
    var location by rememberSaveable(course.id) { mutableStateOf(course.location) }
    var reminder by rememberSaveable(course.id) { mutableStateOf(course.reminderMinutes.toString()) }
    var deleting by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = { if (!saving) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Surface(Modifier.fillMaxSize()) {
            Scaffold(modifier = Modifier.imePadding(), topBar = {
                PlannerTopBar(stringResource(if (course.id == 0L) R.string.add_course else R.string.edit_course), onDismiss) {
                    TextButton(enabled = !saving && name.isNotBlank() && end > start, onClick = {
                        onSave(course.copy(name = name.trim(), weekday = weekday, startMinute = start, endMinute = end,
                            location = location.trim(), reminderMinutes = reminder.toIntOrNull() ?: 0))
                    }) { Text(stringResource(R.string.save)) }
                }
            }, containerColor = MaterialTheme.colorScheme.surfaceVariant) { padding ->
                Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Surface(shape = RoundedCornerShape(12.dp)) {
                        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth().padding(16.dp), label = { Text(stringResource(R.string.course_name)) }, singleLine = true, enabled = !saving)
                    }
                    Text(stringResource(R.string.course_repeats_weekly), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            (1..7).toList().chunked(4).forEach { days ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    days.forEach { day ->
                                        FilterChip(weekday == day, { weekday = day }, enabled = !saving,
                                            label = { Text(DayOfWeek.of(day).getDisplayName(TextStyle.SHORT, locale), fontSize = 11.sp, maxLines = 1) }, modifier = Modifier.weight(1f))
                                    }
                                    if (days.size < 4) Spacer(Modifier.weight((4 - days.size).toFloat()))
                                }
                            }
                            ListItem(headlineContent = { Text(stringResource(R.string.course_start)) }, trailingContent = { Text(courseTime(start)) },
                                modifier = Modifier.clickable(enabled = !saving) { TimePickerDialog(context, { _, h, m -> start = h * 60 + m }, start / 60, start % 60, true).show() })
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            ListItem(headlineContent = { Text(stringResource(R.string.course_end)) }, trailingContent = { Text(courseTime(end)) },
                                modifier = Modifier.clickable(enabled = !saving) { TimePickerDialog(context, { _, h, m -> end = h * 60 + m }, end / 60, end % 60, true).show() })
                            if (end <= start) Text(stringResource(R.string.course_invalid_time), color = MaterialTheme.colorScheme.error)
                            OutlinedTextField(location, { location = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.course_location)) }, singleLine = true, enabled = !saving)
                            OutlinedTextField(reminder, { reminder = it.filter(Char::isDigit).take(3) }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.course_reminder_minutes)) }, singleLine = true, enabled = !saving)
                        }
                    }
                    if (course.id != 0L) TextButton(onClick = { deleting = true }, enabled = !saving) { Text(stringResource(R.string.delete_course), color = MaterialTheme.colorScheme.error) }
                }
            }
            if (deleting) AlertDialog(onDismissRequest = { deleting = false }, title = { Text(stringResource(R.string.delete_course)) },
                text = { Text(stringResource(R.string.delete_course_confirmation)) },
                confirmButton = { TextButton(onClick = { deleting = false; onDelete() }) { Text(stringResource(R.string.delete)) } },
                dismissButton = { TextButton(onClick = { deleting = false }) { Text(stringResource(R.string.cancel)) } })
        }
    }
}

private fun courseTime(minute: Int): String = "%02d:%02d".format(minute / 60, minute % 60)
