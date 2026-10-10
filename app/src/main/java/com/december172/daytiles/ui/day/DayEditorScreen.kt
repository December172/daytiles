package com.december172.daytiles.ui.day

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.app.TimePickerDialog
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import java.time.ZoneId
import java.util.Calendar
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.december172.daytiles.R
import com.december172.daytiles.data.local.TaskEntity
import com.december172.daytiles.data.local.StickerPlacementWithAsset
import com.december172.daytiles.data.local.StickerAssetEntity
import com.december172.daytiles.data.local.StickerAssetKind
import coil3.compose.AsyncImage
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun DayEditorRoute(date: LocalDate, onBack: () -> Unit) {
    val viewModel: DayEditorViewModel = viewModel(
        key = "day-editor-$date",
        factory = DayEditorViewModel.factory(date),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingReminder by remember { mutableStateOf<Pair<TaskEntity, Long?>?>(null) }
    val notificationPermission = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        val pending = pendingReminder
        pendingReminder = null
        if (granted && pending != null) viewModel.setTaskReminder(pending.first, pending.second)
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importImageSticker(context, it) }
    }
    DayEditorScreen(
        uiState = uiState,
        onBack = onBack,
        onJournalChanged = viewModel::updateJournal,
        onAddTask = viewModel::addTask,
        onUpdateTaskTitle = viewModel::updateTaskTitle,
        onTaskCompletedChanged = viewModel::setTaskCompleted,
        onSetTaskReminder = { task, reminderAt ->
            if (reminderAt != null && Build.VERSION.SDK_INT >= 33 &&
                context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                pendingReminder = task to reminderAt
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                viewModel.setTaskReminder(task, reminderAt)
            }
        },
        onDeleteTask = viewModel::deleteTask,
        onAddSticker = viewModel::addSticker,
        onAddStickerAsset = viewModel::addStickerAsset,
        onImportImage = { imagePicker.launch(arrayOf("image/png", "image/webp")) },
        onImportRemoteImage = { viewModel.importRemoteImageSticker(context, it) },
        onClearRemoteImageError = viewModel::clearRemoteStickerError,
        onDeleteSticker = viewModel::deleteSticker,
    )
}

@Composable
fun DayEditorPopup(date: LocalDate, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.9f)
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 8.dp,
    ) {
        DayEditorRoute(date = date, onBack = onDismiss)
    }
}

@Composable
fun DayEditorScreen(
    uiState: DayEditorUiState,
    onBack: () -> Unit,
    onJournalChanged: (String) -> Unit,
    onAddTask: (String) -> Unit,
    onUpdateTaskTitle: (TaskEntity, String) -> Unit,
    onTaskCompletedChanged: (TaskEntity, Boolean) -> Unit,
    onSetTaskReminder: (TaskEntity, Long?) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    onAddSticker: (String) -> Unit,
    onAddStickerAsset: (StickerAssetEntity) -> Unit,
    onImportImage: () -> Unit,
    onImportRemoteImage: (String) -> Unit,
    onClearRemoteImageError: () -> Unit,
    onDeleteSticker: (StickerPlacementWithAsset) -> Unit,
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
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.close)) }
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
                            .heightIn(min = 96.dp, max = 160.dp),
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
                                    if (sticker.kind == StickerAssetKind.IMAGE) {
                                        AsyncImage(
                                            model = File(sticker.source),
                                            contentDescription = sticker.displayName,
                                            modifier = Modifier.height(32.dp),
                                            contentScale = ContentScale.Fit,
                                        )
                                        Text("  ×")
                                    } else {
                                        Text("${sticker.source}  ×")
                                    }
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
                Column(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .heightIn(max = 420.dp),
                ) {
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
                    LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                        items(uiState.tasks, key = { it.id }) { task ->
                            TaskRow(
                                task = task,
                                onCompletedChanged = onTaskCompletedChanged,
                                onSetReminder = { onSetTaskReminder(task, it) },
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
            assets = uiState.stickerAssets,
            onDismiss = { showStickerPicker = false },
            onStickerSelected = { symbol ->
                onAddSticker(symbol)
                showStickerPicker = false
            },
            onAssetSelected = { asset ->
                onAddStickerAsset(asset)
                showStickerPicker = false
            },
            onImportImage = {
                showStickerPicker = false
                onImportImage()
            },
            onImportRemoteImage = onImportRemoteImage,
            onClearRemoteImageError = onClearRemoteImageError,
            remoteStickerState = uiState.remoteStickerState,
        )
    }
}

@Composable
private fun TaskRow(
    task: TaskEntity,
    onCompletedChanged: (TaskEntity, Boolean) -> Unit,
    onSetReminder: (Long?) -> Unit,
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
        val context = LocalContext.current
        Text(
            text = task.title,
            modifier = Modifier.weight(1f),
            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
        )
        TextButton(onClick = {
            if (task.reminderAt != null) {
                onSetReminder(null)
            } else {
                val calendar = Calendar.getInstance().apply {
                    timeInMillis = task.date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    if (task.date == LocalDate.now()) {
                        timeInMillis = System.currentTimeMillis()
                        add(Calendar.HOUR_OF_DAY, 1)
                    } else {
                        set(Calendar.HOUR_OF_DAY, 9)
                        set(Calendar.MINUTE, 0)
                    }
                }
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hour)
                        calendar.set(Calendar.MINUTE, minute)
                        onSetReminder(calendar.timeInMillis)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true,
                ).show()
            }
        }) {
            Text(if (task.reminderAt == null) stringResource(R.string.set_reminder) else "⏰")
        }
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
    assets: List<StickerAssetEntity>,
    onDismiss: () -> Unit,
    onStickerSelected: (String) -> Unit,
    onAssetSelected: (StickerAssetEntity) -> Unit,
    onImportImage: () -> Unit,
    onImportRemoteImage: (String) -> Unit,
    onClearRemoteImageError: () -> Unit,
    remoteStickerState: RemoteStickerState,
) {
    var customEmoji by remember { mutableStateOf("") }
    var remoteUrl by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.choose_sticker)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
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
                val imageAssets = assets.filter { it.kind == StickerAssetKind.IMAGE }
                if (imageAssets.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.imported_stickers),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        imageAssets.forEach { asset ->
                            TextButton(onClick = { onAssetSelected(asset) }) {
                                AsyncImage(
                                    model = File(asset.source),
                                    contentDescription = asset.displayName,
                                    modifier = Modifier.height(44.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            }
                        }
                    }
                }
                TextButton(
                    onClick = onImportImage,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.import_sticker_image))
                }
                OutlinedTextField(
                    value = remoteUrl,
                    onValueChange = {
                        remoteUrl = it
                        onClearRemoteImageError()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.remote_sticker_url)) },
                    placeholder = { Text(stringResource(R.string.remote_sticker_url_placeholder)) },
                    singleLine = true,
                    isError = remoteStickerState == RemoteStickerState.ERROR,
                    supportingText = if (remoteStickerState == RemoteStickerState.ERROR) {
                        { Text(stringResource(R.string.remote_sticker_error)) }
                    } else {
                        null
                    },
                )
                TextButton(
                    onClick = { onImportRemoteImage(remoteUrl.trim()) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = remoteUrl.isNotBlank() &&
                        remoteStickerState != RemoteStickerState.DOWNLOADING,
                ) {
                    if (remoteStickerState == RemoteStickerState.DOWNLOADING) {
                        CircularProgressIndicator(modifier = Modifier.height(24.dp))
                    } else {
                        Text(stringResource(R.string.download_remote_sticker))
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = customEmoji,
                    onValueChange = { customEmoji = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.custom_emoji)) },
                    placeholder = { Text(stringResource(R.string.custom_emoji_placeholder)) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onStickerSelected(customEmoji.trim()) },
                enabled = customEmoji.isNotBlank(),
            ) { Text(stringResource(R.string.add)) }
        },
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
