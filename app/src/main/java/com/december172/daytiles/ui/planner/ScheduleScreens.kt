package com.december172.daytiles.ui.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.december172.daytiles.DayTilesApplication
import com.december172.daytiles.R
import com.december172.daytiles.data.local.TaskEntity
import com.december172.daytiles.ui.components.PlannerTopBar
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun FullScheduleScreen(onBack: () -> Unit, onEdit: (TaskEntity) -> Unit, onAdd: (LocalDate) -> Unit) {
    val repo = (LocalContext.current.applicationContext as DayTilesApplication).repository
    val tasks by remember(repo) { repo.observeAllTasks() }.collectAsStateWithLifecycle(emptyList())
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val filtered = remember(tasks, filter) {
        tasks.filter { filter == 0 || it.isCompleted == (filter == 2) }
    }
    val groups = remember(filtered) { filtered.groupBy { it.date }.entries.toList() }
    val locale = LocalConfiguration.current.locales[0]
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val today = LocalDate.now()
    Scaffold(
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        topBar = { PlannerTopBar(stringResource(R.string.full_schedule_title), onBack) {
            TextButton(enabled = groups.isNotEmpty(), onClick = {
                val index = groups.indexOfFirst { !it.key.isBefore(today) }.let { if (it < 0) groups.lastIndex else it }
                scope.launch { listState.animateScrollToItem(index) }
            }) { Text(stringResource(R.string.today)) }
        } },
        floatingActionButton = { FloatingActionButton(onClick = { onAdd(today) }) {
            Icon(painterResource(R.drawable.ic_planner_add), stringResource(R.string.new_task))
        } },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(stringResource(R.string.full_schedule_hint), Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(R.string.all_filter, R.string.task_todo, R.string.task_done).forEachIndexed { index, label ->
                    FilterChip(filter == index, { filter = index }, label = { Text(stringResource(label)) })
                }
            }
            if (groups.isEmpty()) FeatureEmptyState(stringResource(R.string.no_tasks))
            LazyColumn(Modifier.weight(1f).testTag("full-schedule-list"), state = listState,
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(groups, key = { it.key.toString() }) { (date, dayTasks) ->
                    Row {
                        Column(Modifier.width(20.dp).heightIn(min = 72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.padding(top = 9.dp).size(9.dp).background(MaterialTheme.colorScheme.primary, CircleShape))
                            Box(Modifier.padding(top = 5.dp).width(1.dp).height(38.dp).background(MaterialTheme.colorScheme.outlineVariant))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)),
                                Modifier.padding(vertical = 4.dp), style = MaterialTheme.typography.titleSmall,
                                color = if (date == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                            dayTasks.forEach { task ->
                                ListItem(
                                    headlineContent = { Text(task.title, textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null) },
                                    supportingContent = task.details.takeIf(String::isNotBlank)?.let { details -> { Text(details, maxLines = 2) } },
                                    leadingContent = { Checkbox(task.isCompleted, { completed ->
                                        scope.launch { repo.setTaskCompleted(task, completed) }
                                    }, Modifier.testTag("timeline-check-${task.id}")) },
                                    modifier = Modifier.testTag("timeline-task-${task.id}").clickable { onEdit(task) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecycleBinScreen(onBack: () -> Unit) {
    val repo = (LocalContext.current.applicationContext as DayTilesApplication).repository
    val tasks by remember(repo) { repo.observeDeletedTasks() }.collectAsStateWithLifecycle(emptyList())
    var pending by remember { mutableStateOf<TaskEntity?>(null) }
    var emptyAll by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snack = remember { SnackbarHostState() }
    val errorMessage = stringResource(R.string.save_failed)
    val locale = LocalConfiguration.current.locales[0]
    fun perform(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try { action() }
            catch (error: Exception) {
                if (error is CancellationException) throw error
                snack.showSnackbar(errorMessage)
            } finally { busy = false }
        }
    }
    Scaffold(
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        topBar = { PlannerTopBar(stringResource(R.string.recycle_bin_title), onBack) {
            TextButton(enabled = tasks.isNotEmpty() && !busy, onClick = { emptyAll = true }) {
                Text(stringResource(R.string.empty_bin))
            }
        } },
        snackbarHost = { SnackbarHost(snack) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Text(stringResource(R.string.recycle_bin_hint), Modifier.padding(16.dp), style = MaterialTheme.typography.bodySmall)
            if (tasks.isEmpty()) FeatureEmptyState(stringResource(R.string.empty_bin_hint))
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(tasks, key = TaskEntity::id) { task ->
                    OutlinedCard(Modifier.fillMaxWidth().testTag("binned-task-${task.id}")) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(task.title, style = MaterialTheme.typography.titleMedium)
                            Text(task.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)), style = MaterialTheme.typography.bodySmall)
                            if (task.details.isNotBlank()) Text(task.details, maxLines = 3)
                            task.deletedAt?.let { time ->
                                Text(stringResource(R.string.deleted_on, Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault())
                                    .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(locale))),
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(enabled = !busy, onClick = { pending = task }, modifier = Modifier.testTag("purge-task-${task.id}")) {
                                    Text(stringResource(R.string.delete_forever), color = MaterialTheme.colorScheme.error)
                                }
                                TextButton(enabled = !busy, onClick = { perform { repo.restoreTask(task.id) } }, modifier = Modifier.testTag("restore-task-${task.id}")) {
                                    Text(stringResource(R.string.restore_task))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (pending != null || emptyAll) {
        AlertDialog(onDismissRequest = { if (!busy) { pending = null; emptyAll = false } },
            title = { Text(stringResource(if (emptyAll) R.string.empty_bin else R.string.delete_forever)) },
            text = { Text(stringResource(R.string.permanent_delete_confirmation)) },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                val task = pending
                perform {
                    if (emptyAll) repo.emptyRecycleBin() else task?.let { repo.permanentlyDeleteTask(it.id) }
                    pending = null; emptyAll = false
                }
            }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(enabled = !busy, onClick = { pending = null; emptyAll = false }) { Text(stringResource(R.string.cancel)) } })
    }
}

@Composable
internal fun FeatureEmptyState(message: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
    }
}
