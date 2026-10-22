package com.december172.daytiles.ui.calendar

import android.app.DatePickerDialog
import android.icu.util.ChineseCalendar
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.december172.daytiles.R
import com.december172.daytiles.data.local.StickerAssetKind
import com.december172.daytiles.data.local.TaskEntity
import com.december172.daytiles.domain.model.DayContent
import com.december172.daytiles.domain.model.parseTaskTags
import com.december172.daytiles.ui.components.PlannerBottomBar
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import kotlin.math.abs

@Composable
fun CalendarRoute(
    onOpenDay: (LocalDate) -> Unit,
    onCreateTask: (LocalDate) -> Unit,
    onEditTask: (TaskEntity) -> Unit,
    onOpenJournal: (LocalDate) -> Unit = onOpenDay,
    initialDate: LocalDate? = null,
    openJournalInitially: Boolean = false,
    openTasksInitially: Boolean = false,
    onOpenMenu: () -> Unit = {},
    onNavigatePrimary: (String) -> Unit = {},
    viewModel: CalendarViewModel = viewModel(factory = CalendarViewModel.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(initialDate) {
        initialDate?.let {
            viewModel.selectDateAndShow(it)
            if (openJournalInitially) onOpenJournal(it) else onOpenDay(it)
        }
    }
    var selectedTaskTag by rememberSaveable { mutableStateOf<String?>(null) }
    CalendarScreen(
        uiState = uiState,
        onPreviousMonth = viewModel::showPreviousMonth,
        onNextMonth = viewModel::showNextMonth,
        onToday = viewModel::showToday,
        onDateSelected = viewModel::selectDateAndShow,
        onOpenDay = onOpenDay,
        onCreateTask = onCreateTask,
        onEditTask = onEditTask,
        selectedTab = if (openTasksInitially) CalendarTab.TASKS else CalendarTab.CALENDAR,
        onTaskCompletedChanged = viewModel::setTaskCompleted,
        selectedTaskTag = selectedTaskTag,
        onTaskTagSelected = { selectedTaskTag = it },
        onOpenMenu = onOpenMenu,
        onNavigatePrimary = onNavigatePrimary,
        onMonthSelected = viewModel::showMonth,
    )
}

enum class CalendarTab { CALENDAR, TASKS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    uiState: CalendarUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToday: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onCreateTask: (LocalDate) -> Unit,
    onEditTask: (TaskEntity) -> Unit,
    modifier: Modifier = Modifier,
    selectedTab: CalendarTab = CalendarTab.CALENDAR,
    onTaskCompletedChanged: (TaskEntity, Boolean) -> Unit = { _, _ -> },
    selectedTaskTag: String? = null,
    onTaskTagSelected: (String?) -> Unit = {},
    onOpenMenu: () -> Unit = {},
    onNavigatePrimary: (String) -> Unit = {},
    onMonthSelected: (YearMonth) -> Unit = {},
) {
    val locale = LocalConfiguration.current.locales[0]
    val monthLabel = remember(uiState.visibleMonth, locale) {
        uiState.visibleMonth.format(DateTimeFormatter.ofPattern(if (locale.language == "zh") "yyyy年M月" else "MMM yyyy", locale))
    }
    var showMonthPicker by rememberSaveable { mutableStateOf(false) }
    var dayOnly by rememberSaveable { mutableStateOf(false) }
    var showTaskDates by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val isCalendar = selectedTab == CalendarTab.CALENDAR
    val title = if (!isCalendar && dayOnly) {
        if (uiState.selectedDate == uiState.today) stringResource(R.string.today)
        else uiState.selectedDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
    } else monthLabel
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            CenterAlignedTopAppBar(
                expandedHeight = 56.dp,
                title = {
                    Box {
                        Row(
                            Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                                if (isCalendar) showMonthPicker = true else showTaskDates = true
                            }.padding(horizontal = 6.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                            Icon(painterResource(R.drawable.ic_planner_chevron_down), stringResource(R.string.choose_month), Modifier.size(17.dp))
                        }
                        DropdownMenu(expanded = showTaskDates, onDismissRequest = { showTaskDates = false }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.today)) }, onClick = {
                                onToday(); dayOnly = true; showTaskDates = false
                            })
                            DropdownMenuItem(text = { Text(stringResource(R.string.month_tasks)) }, onClick = {
                                dayOnly = false; showTaskDates = false; showMonthPicker = true
                            })
                            DropdownMenuItem(text = { Text(stringResource(R.string.choose_date)) }, onClick = {
                                showTaskDates = false
                                DatePickerDialog(context, { _, y, m, d ->
                                    onDateSelected(LocalDate.of(y, m + 1, d)); dayOnly = true
                                }, uiState.selectedDate.year, uiState.selectedDate.monthValue - 1, uiState.selectedDate.dayOfMonth).show()
                            })
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Icon(painterResource(R.drawable.ic_planner_menu), stringResource(R.string.open_navigation_menu), Modifier.size(24.dp))
                    }
                },
                actions = {
                    TextButton(onClick = { onToday(); if (!isCalendar) dayOnly = true }) {
                        Text(stringResource(R.string.today), fontSize = 12.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = { PlannerBottomBar(if (isCalendar) "calendar" else "tasks", onNavigatePrimary) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onCreateTask(uiState.selectedDate) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.size(60.dp),
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
            ) {
                Icon(painterResource(R.drawable.ic_planner_add), stringResource(R.string.new_task), Modifier.size(32.dp))
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (isCalendar) {
                WeekdayHeader()
                MonthGrid(uiState, onDateSelected, onOpenDay, onPreviousMonth, onNextMonth, Modifier.weight(1f))
            } else {
                TaskOverview(
                    tasks = if (dayOnly) uiState.tasks.filter { it.date == uiState.selectedDate } else uiState.tasks,
                    selectedTag = selectedTaskTag,
                    onTagSelected = onTaskTagSelected,
                    onTaskCompletedChanged = onTaskCompletedChanged,
                    onEditTask = onEditTask,
                    onOpenDay = onOpenDay,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
    if (showMonthPicker) {
        MonthPicker(uiState.visibleMonth, onDismiss = { showMonthPicker = false }, onSelect = {
            onMonthSelected(it); showMonthPicker = false
        })
    }
}

@Composable
private fun MonthPicker(month: YearMonth, onDismiss: () -> Unit, onSelect: (YearMonth) -> Unit) {
    var year by remember { mutableIntStateOf(month.year) }
    val locale = LocalConfiguration.current.locales[0]
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_month)) },
        text = {
            Column {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { year-- }) {
                        Icon(painterResource(R.drawable.ic_planner_chevron_left), stringResource(R.string.previous_year))
                    }
                    Text(year.toString(), Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = { year++ }) {
                        Icon(painterResource(R.drawable.ic_planner_chevron_right), stringResource(R.string.next_year))
                    }
                }
                (1..12).chunked(3).forEach { months ->
                    Row(Modifier.fillMaxWidth()) {
                        months.forEach { number ->
                            val selected = month == YearMonth.of(year, number)
                            Surface(
                                onClick = { onSelect(YearMonth.of(year, number)) },
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).padding(3.dp),
                            ) {
                                Text(YearMonth.of(year, number).month.getDisplayName(TextStyle.SHORT, locale),
                                    Modifier.padding(vertical = 14.dp), textAlign = TextAlign.Center,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

@Composable
private fun TaskOverview(
    tasks: List<TaskEntity>, selectedTag: String?, onTagSelected: (String?) -> Unit,
    onTaskCompletedChanged: (TaskEntity, Boolean) -> Unit, onEditTask: (TaskEntity) -> Unit,
    onOpenDay: (LocalDate) -> Unit, modifier: Modifier = Modifier,
) {
    val tags = tasks.flatMap { parseTaskTags(it.tags) }.distinct().sorted()
    val filtered = if (selectedTag == null) tasks else tasks.filter { selectedTag in parseTaskTags(it.tags) }
    val locale = LocalConfiguration.current.locales[0]
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).horizontalScroll(rememberScrollState())) {
            (listOf<String?>(null) + tags).forEach { tag ->
                val selected = selectedTag == tag
                Box(
                    Modifier.height(36.dp).widthIn(min = 76.dp).clip(RoundedCornerShape(topStart = 9.dp, topEnd = 9.dp))
                        .background(if (selected) MaterialTheme.colorScheme.secondary else Color.Transparent)
                        .selectable(selected, onClick = { onTagSelected(tag) }, role = Role.Tab)
                        .padding(horizontal = 20.dp), contentAlignment = Alignment.Center,
                ) {
                    Text(tag ?: stringResource(R.string.all_filter), fontSize = 14.sp,
                        color = if (selected) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurface)
                }
            }
        }
        if (filtered.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(bottom = 80.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(painterResource(R.drawable.ic_fluent_clipboard_task_24_regular), null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(18.dp))
                Text(stringResource(if (selectedTag == null) R.string.empty_schedule_title else R.string.no_tasks_for_tag), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.empty_schedule_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(top = 10.dp, bottom = 90.dp)) {
                filtered.groupBy { it.date }.toSortedMap().forEach { (date, dayTasks) ->
                    item(key = "date-$date") {
                        Text(
                            date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)),
                            Modifier.fillMaxWidth().clickable { onOpenDay(date) }.padding(horizontal = 20.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    items(dayTasks, key = { it.id }) { task ->
                        Row(Modifier.fillMaxWidth().clickable { onEditTask(task) }.padding(start = 10.dp, end = 20.dp, top = 2.dp, bottom = 12.dp), verticalAlignment = Alignment.Top) {
                            val completionDescription = stringResource(
                                if (task.isCompleted) R.string.todo_uncheck_accessibility else R.string.todo_checkbox_accessibility, task.title,
                            )
                            IconButton(
                                onClick = { onTaskCompletedChanged(task, !task.isCompleted) },
                                modifier = Modifier.semantics { contentDescription = completionDescription },
                            ) {
                                Surface(
                                    Modifier.size(18.dp), shape = CircleShape,
                                    color = if (task.isCompleted) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    border = if (task.isCompleted) null else BorderStroke(1.3.dp, MaterialTheme.colorScheme.outline),
                                ) {
                                    if (task.isCompleted) Icon(painterResource(R.drawable.ic_planner_check), null, Modifier.padding(2.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                            Column(Modifier.weight(1f).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(task.title, fontSize = 16.sp, lineHeight = 23.sp, textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                                Text(stringResource(if (task.isCompleted) R.string.task_done else if (task.date.isBefore(LocalDate.now())) R.string.task_overdue else R.string.task_todo),
                                    fontSize = 12.sp, color = if (!task.isCompleted && task.date.isBefore(LocalDate.now())) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                                if (task.details.isNotBlank()) Text(task.details, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekdayHeader() {
    val locale = LocalConfiguration.current.locales[0]
    Row(Modifier.fillMaxWidth().height(32.dp)) {
        (0..6).forEach { offset ->
            val day = DayOfWeek.SUNDAY.plus(offset.toLong())
            Text(day.getDisplayName(TextStyle.SHORT_STANDALONE, locale), Modifier.weight(1f).align(Alignment.CenterVertically),
                color = if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun MonthGrid(
    uiState: CalendarUiState, onDateSelected: (LocalDate) -> Unit, onOpenDay: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit, onNextMonth: () -> Unit, modifier: Modifier = Modifier,
) {
    val weeks = remember(uiState.visibleMonth) { buildDisplayMonthWeeks(uiState.visibleMonth) }
    val previous by rememberUpdatedState(onPreviousMonth)
    val next by rememberUpdatedState(onNextMonth)
    Column(modifier.fillMaxWidth().background(MaterialTheme.colorScheme.outlineVariant).pointerInput(Unit) {
        var drag = 0f
        detectHorizontalDragGestures(onDragStart = { drag = 0f }, onHorizontalDrag = { _, amount -> drag += amount }, onDragEnd = {
            if (abs(drag) > 64.dp.toPx()) { if (drag > 0) previous() else next() }
        })
    }, verticalArrangement = Arrangement.spacedBy(0.5.dp)) {
        weeks.forEach { week ->
            Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(0.5.dp)) {
                week.forEach { date ->
                    DayCell(
                        date, uiState.contentByDate[date], date == uiState.today, date == uiState.selectedDate,
                        YearMonth.from(date) == uiState.visibleMonth,
                        onClick = { if (date == uiState.selectedDate) onOpenDay(date) else onDateSelected(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate, content: DayContent?, isToday: Boolean, isSelected: Boolean, isInMonth: Boolean,
    onClick: () -> Unit, modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val description = remember(date, locale) { date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)) }
    val lunar = remember(date) { lunarLabel(date) }
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxHeight().semantics { contentDescription = description },
        shape = RoundedCornerShape(0.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val spacious = maxHeight >= 98.dp
            Column(Modifier.fillMaxSize().padding(top = 6.dp, start = 2.dp, end = 2.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(25.dp).then(if (isToday) Modifier.background(MaterialTheme.colorScheme.primary, CircleShape) else Modifier), contentAlignment = Alignment.Center) {
                        Text(date.dayOfMonth.toString(), fontSize = 15.sp,
                            color = if (isToday) MaterialTheme.colorScheme.onPrimary else if (!isInMonth) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                    }
                    Text(lunar, Modifier.weight(1f), fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Clip, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(3.dp))
                content?.let { DayPreview(it, spacious) }
                Spacer(Modifier.weight(1f))
                content?.stickers?.takeLast(2)?.let { stickers ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        stickers.forEach { sticker ->
                            val label = stringResource(R.string.sticker_accessibility, sticker.displayName)
                            if (sticker.kind == StickerAssetKind.IMAGE) {
                                AsyncImage(File(sticker.source), label, Modifier.size(25.dp), contentScale = ContentScale.Fit)
                            } else {
                                Text(sticker.source, Modifier.semantics { contentDescription = label }, fontSize = 20.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayPreview(content: DayContent, spacious: Boolean) {
    content.tasks.take(if (spacious) 3 else 1).forEach { task ->
        Text(task.title, Modifier.fillMaxWidth().padding(bottom = 2.dp).clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer).padding(horizontal = 2.dp, vertical = 1.dp),
            fontSize = 10.sp, lineHeight = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
            color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null)
    }
    if (spacious || content.tasks.isEmpty()) {
        content.journal?.text?.takeIf { it.isNotBlank() }?.let {
            Text(it, Modifier.fillMaxWidth().clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.secondaryContainer).padding(2.dp),
                fontSize = 9.sp, lineHeight = 12.sp, maxLines = if (spacious) 2 else 1, overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}

/** Android's bundled ICU provides offline lunar dates; no calendar service is used. */
private fun lunarLabel(date: LocalDate): String {
    val calendar = ChineseCalendar().apply { timeInMillis = date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }
    val day = calendar.get(ChineseCalendar.DAY_OF_MONTH)
    if (day == 1) return (if (calendar.get(ChineseCalendar.IS_LEAP_MONTH) == 1) "闰" else "") +
        listOf("正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊")[calendar.get(ChineseCalendar.MONTH)] + "月"
    return when (day) {
        10 -> "初十"
        20 -> "二十"
        30 -> "三十"
        else -> listOf("初", "十", "廿")[day / 10] + listOf("一", "二", "三", "四", "五", "六", "七", "八", "九")[day % 10 - 1]
    }
}
