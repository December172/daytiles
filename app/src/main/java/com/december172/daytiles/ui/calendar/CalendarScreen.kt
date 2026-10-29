package com.december172.daytiles.ui.calendar

import android.app.DatePickerDialog
import android.icu.util.ChineseCalendar
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.december172.daytiles.DayTilesApplication
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
    val settings = (LocalContext.current.applicationContext as DayTilesApplication).settings
    val showLunarDates by settings.showLunarDates.collectAsStateWithLifecycle()
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
        showLunarDates = showLunarDates,
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
    showLunarDates: Boolean = true,
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
                title = {
                    Box {
                        Row(
                            Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                                if (isCalendar) showMonthPicker = true else showTaskDates = true
                            }.padding(horizontal = 6.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                        Text(stringResource(R.string.today))
                    }
                },
            )
        },
        bottomBar = { PlannerBottomBar(if (isCalendar) "calendar" else "tasks", onNavigatePrimary) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onCreateTask(uiState.selectedDate) },
            ) {
                Icon(painterResource(R.drawable.ic_planner_add), stringResource(R.string.new_task))
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (isCalendar) {
                WeekdayHeader()
                MonthGrid(uiState, onDateSelected, onOpenDay, onPreviousMonth, onNextMonth, showLunarDates,
                    Modifier.weight(1f).padding(horizontal = 8.dp).padding(bottom = 8.dp).clip(MaterialTheme.shapes.large))
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
internal fun MonthPicker(month: YearMonth, onDismiss: () -> Unit, onSelect: (YearMonth) -> Unit) {
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
                                shape = MaterialTheme.shapes.medium,
                                modifier = Modifier.weight(1f).padding(3.dp),
                            ) {
                                Text(YearMonth.of(year, number).month.getDisplayName(TextStyle.SHORT, locale),
                                    Modifier.padding(vertical = 14.dp), textAlign = TextAlign.Center,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface)
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
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf<String?>(null) + tags).forEach { tag ->
                val selected = selectedTag == tag
                FilterChip(
                    selected = selected,
                    onClick = { onTagSelected(tag) },
                    label = { Text(tag ?: stringResource(R.string.all_filter)) },
                    leadingIcon = if (selected) { {
                        Icon(painterResource(R.drawable.ic_planner_check), null, Modifier.size(FilterChipDefaults.IconSize))
                    } } else null,
                )
            }
        }
        if (filtered.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(bottom = 80.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(painterResource(R.drawable.ic_fluent_clipboard_task_24_regular), null, Modifier.size(72.dp), tint = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(18.dp))
                Text(stringResource(if (selectedTag == null) R.string.empty_schedule_title else R.string.no_tasks_for_tag), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.empty_schedule_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
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
                            Checkbox(
                                checked = task.isCompleted,
                                onCheckedChange = { onTaskCompletedChanged(task, it) },
                                modifier = Modifier.semantics { contentDescription = completionDescription },
                            )
                            Column(Modifier.weight(1f).padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(task.title, style = MaterialTheme.typography.bodyLarge, textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                                    color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                                Text(stringResource(if (task.isCompleted) R.string.task_done else if (task.date.isBefore(LocalDate.now())) R.string.task_overdue else R.string.task_todo),
                                    style = MaterialTheme.typography.labelMedium, color = if (!task.isCompleted && task.date.isBefore(LocalDate.now())) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                                if (task.details.isNotBlank()) Text(task.details, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp)) {
        (0..6).forEach { offset ->
            val day = DayOfWeek.SUNDAY.plus(offset.toLong())
            Text(day.getDisplayName(TextStyle.SHORT_STANDALONE, locale), Modifier.weight(1f).align(Alignment.CenterVertically),
                color = if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun MonthGrid(
    uiState: CalendarUiState, onDateSelected: (LocalDate) -> Unit, onOpenDay: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit, onNextMonth: () -> Unit, showLunarDates: Boolean, modifier: Modifier = Modifier,
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
                        YearMonth.from(date) == uiState.visibleMonth, showLunarDates,
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
    date: LocalDate, content: DayContent?, isToday: Boolean, isSelected: Boolean, isInMonth: Boolean, showLunarDates: Boolean,
    onClick: () -> Unit, modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    val description = remember(date, locale) { date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)) }
    val lunarMonths = stringArrayResource(R.array.lunar_month_names)
    val lunarLeapPrefix = stringResource(R.string.lunar_leap_prefix)
    val lunar = remember(date, showLunarDates, locale) {
        if (showLunarDates) lunarLabel(date, lunarMonths, lunarLeapPrefix) else null
    }
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxHeight().semantics { contentDescription = description },
        shape = RoundedCornerShape(0.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val spacious = maxHeight >= 128.dp * LocalDensity.current.fontScale
            Column(Modifier.fillMaxSize().padding(top = 4.dp, start = 2.dp, end = 2.dp)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.defaultMinSize(minWidth = 28.dp, minHeight = 28.dp)
                        .then(if (isToday) Modifier.background(MaterialTheme.colorScheme.primary, CircleShape) else Modifier), contentAlignment = Alignment.Center) {
                        Text(date.dayOfMonth.toString(), Modifier.padding(horizontal = 4.dp, vertical = 2.dp), style = MaterialTheme.typography.bodyMedium,
                            color = if (isToday) MaterialTheme.colorScheme.onPrimary else if (!isInMonth) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface)
                    }
                    if (lunar != null) Text(lunar, style = MaterialTheme.typography.labelSmall, maxLines = 1,
                        overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(3.dp))
                Column(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
                    content?.let { DayPreview(it, spacious) }
                }
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
            color = MaterialTheme.colorScheme.onTertiaryContainer,
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
private fun lunarLabel(date: LocalDate, months: Array<String>, leapPrefix: String): String {
    val calendar = ChineseCalendar().apply { timeInMillis = date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() }
    val day = calendar.get(ChineseCalendar.DAY_OF_MONTH)
    if (day == 1) return (if (calendar.get(ChineseCalendar.IS_LEAP_MONTH) == 1) leapPrefix else "") +
        months[calendar.get(ChineseCalendar.MONTH)] + "月"
    return when (day) {
        10 -> "初十"
        20 -> "二十"
        30 -> "三十"
        else -> listOf("初", "十", "廿")[day / 10] + listOf("一", "二", "三", "四", "五", "六", "七", "八", "九")[day % 10 - 1]
    }
}
