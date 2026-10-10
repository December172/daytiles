package com.december172.daytiles.ui.day

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.december172.daytiles.R
import com.december172.daytiles.data.local.TaskEntity
import com.december172.daytiles.data.local.StickerPlacementEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun DayEditorRoute(date: LocalDate, onBack: () -> Unit) {
    val viewModel: DayEditorViewModel = viewModel(factory = DayEditorViewModel.factory(date))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DayEditorScreen(
        uiState = uiState,
        onBack = onBack,
        onJournalChanged = viewModel::updateJournal,
        onAddTask = viewModel::addTask,
        onUpdateTaskTitle = viewModel::updateTaskTitle,
        onTaskCompletedChanged = viewModel::setTaskCompleted,
        onDeleteTask = viewModel::deleteTask,
        onAddSticker = viewModel::addSticker,
        onDeleteSticker = viewModel::deleteSticker,
    )
}

@Composable
fun DayEditorScreen(
    uiState: DayEditorUiState,
    onBack: () -> Unit,
    onJournalChanged: (String) -> Unit,
    onAddTask: (String) -> Unit,
    onUpdateTaskTitle: (TaskEntity, String) -> Unit,
    onTaskCompletedChanged: (TaskEntity, Boolean) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onAddSticker: (String) -> Unit,
    onDeleteSticker: (StickerPlacementEntity) -> Unit,
) {
    var newTaskTitle by remember { mutableStateOf("") }
    var taskBeingEdited by remember { mutableStateOf<TaskEntity?>(null) }
    var showStickerPicker by remember { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val dateTitle = remember(uiState.date, locale) {
        uiState.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))
    }
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
                Text(
                    text = dateTitle,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        stringResource(R.string.journal_heading),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = uiState.journalText,
                        onValueChange = onJournalChanged,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        placeholder = { Text(stringResource(R.string.journal_placeholder)) },
                        shape = RoundedCornerShape(14.dp),
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            stringResource(R.string.stickers_heading),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        TextButton(onClick = { showStickerPicker = true }) {
                            Text(stringResource(R.string.add_sticker))
                        }
                    }
                    if (uiState.stickers.isEmpty()) {
                        Text(
                            stringResource(R.string.no_stickers),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            uiState.stickers.forEach { sticker ->
                                TextButton(onClick = { onDeleteSticker(sticker) }) {
                                    Text("${sticker.symbol}  ×")
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(
                        stringResource(R.string.tasks_heading),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = newTaskTitle,
                            onValueChange = { newTaskTitle = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text(stringResource(R.string.add_task_placeholder)) },
                            shape = RoundedCornerShape(14.dp),
                        )
                        Button(
                            onClick = {
                                onAddTask(newTaskTitle)
                                newTaskTitle = ""
                            },
                            enabled = newTaskTitle.isNotBlank(),
                        ) { Text(stringResource(R.string.add)) }
                    }
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(uiState.tasks, key = { it.id }) { task ->
                            TaskRow(
                                task = task,
                                onCompletedChanged = onTaskCompletedChanged,
                                onEdit = { taskBeingEdited = task },
                                onDelete = onDeleteTask,
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
            }
        }
    }
    taskBeingEdited?.let { task ->
        EditTaskDialog(
            task = task,
            onDismiss = { taskBeingEdited = null },
            onSave = { title ->
                onUpdateTaskTitle(task, title)
                taskBeingEdited = null
            },
        )
    }
    if (showStickerPicker) {
        StickerPickerDialog(
            onDismiss = { showStickerPicker = false },
            onStickerSelected = { symbol ->
                onAddSticker(symbol)
                showStickerPicker = false
            },
        )
    }
}

@Composable
private fun TaskRow(
    task: TaskEntity,
    onCompletedChanged: (TaskEntity, Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: (TaskEntity) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { onCompletedChanged(task, it) },
        )
        Text(
            text = task.title,
            modifier = Modifier.weight(1f),
            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
        )
        TextButton(onClick = onEdit) { Text(stringResource(R.string.edit)) }
        TextButton(onClick = { onDelete(task) }) { Text(stringResource(R.string.delete)) }
    }
}

@Composable
private fun EditTaskDialog(
    task: TaskEntity,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var title by remember(task.id, task.title) { mutableStateOf(task.title) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_task)) },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.task_title)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title) },
                enabled = title.isNotBlank(),
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun StickerPickerDialog(
    onDismiss: () -> Unit,
    onStickerSelected: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_sticker)) },
        text = {
            Column {
                builtInStickers.chunked(5).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        row.forEach { symbol ->
                            TextButton(
                                onClick = { onStickerSelected(symbol) },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(symbol, style = MaterialTheme.typography.headlineSmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

private val builtInStickers = listOf(
    "☀️", "🌙", "⭐", "✨", "💛",
    "🌸", "🌿", "🍂", "☕", "🎉",
    "📚", "💡", "✅", "🏃", "😴",
)
