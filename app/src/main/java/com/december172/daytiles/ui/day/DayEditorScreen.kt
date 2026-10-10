package com.december172.daytiles.ui.day

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.december172.daytiles.R
import com.december172.daytiles.data.local.TaskEntity
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
        onTaskCompletedChanged = viewModel::setTaskCompleted,
        onDeleteTask = viewModel::deleteTask,
    )
}

@Composable
fun DayEditorScreen(
    uiState: DayEditorUiState,
    onBack: () -> Unit,
    onJournalChanged: (String) -> Unit,
    onAddTask: (String) -> Unit,
    onTaskCompletedChanged: (TaskEntity, Boolean) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
) {
    var newTaskTitle by remember { mutableStateOf("") }
    val locale = LocalConfiguration.current.locales[0]
    val dateTitle = remember(uiState.date, locale) {
        uiState.date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))
    }
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
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
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(stringResource(R.string.journal_heading), style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = uiState.journalText,
                onValueChange = onJournalChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                placeholder = { Text(stringResource(R.string.journal_placeholder)) },
            )
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.tasks_heading), style = MaterialTheme.typography.titleMedium)
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
                    TaskRow(task, onTaskCompletedChanged, onDeleteTask)
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun TaskRow(
    task: TaskEntity,
    onCompletedChanged: (TaskEntity, Boolean) -> Unit,
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
        TextButton(onClick = { onDelete(task) }) { Text(stringResource(R.string.delete)) }
    }
}
