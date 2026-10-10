package com.december172.daytiles.ui.day

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import java.time.ZoneId
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
import java.util.Calendar

@Composable
fun DayEditorRoute(date: LocalDate, onBack: () -> Unit) {
    val viewModel: DayEditorViewModel = viewModel(
        key = "day-editor-$date",
        factory = DayEditorViewModel.factory(date),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.importImageSticker(context, it) }
    }
    DayEditorScreen(
        uiState = uiState,
        onBack = onBack,
        onJournalChanged = viewModel::updateJournal,
        onAddSticker = viewModel::addSticker,
        onAddStickerAsset = viewModel::addStickerAsset,
        onImportImage = { imagePicker.launch(arrayOf("image/png", "image/webp")) },
        onImportRemoteImage = { viewModel.importRemoteImageSticker(context, it) },
        onClearRemoteImageError = viewModel::clearRemoteStickerError,
        onDeleteSticker = viewModel::deleteSticker,
    )
}

private data class TaskDraft(
    val title: String,
    val details: String,
    val tags: String,
    val reminderAt: Long?,
)

@Composable
fun TaskEditorRoute(date: LocalDate, taskId: Long?, onBack: () -> Unit) {
    val viewModel: DayEditorViewModel = viewModel(
        key = "task-editor-$date",
        factory = DayEditorViewModel.factory(date),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val task = taskId?.let { id -> uiState.tasks.firstOrNull { it.id == id } }
    val context = LocalContext.current
    var pendingSave by remember { mutableStateOf<TaskDraft?>(null) }

    fun save(draft: TaskDraft, reminderAt: Long? = draft.reminderAt) {
        viewModel.saveTask(task, draft.title, draft.details, draft.tags, reminderAt, onBack)
    }

    val notificationPermission = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        pendingSave?.let { draft -> save(draft, if (granted) draft.reminderAt else task?.reminderAt) }
        pendingSave = null
    }

    if (taskId == null || task != null) {
        TaskEditorScreen(
            date = date,
            task = task,
            onBack = onBack,
            onDelete = task?.let { existingTask ->
                { viewModel.deleteTask(existingTask, onBack) }
            },
            onSave = { draft ->
                if (draft.reminderAt != null && Build.VERSION.SDK_INT >= 33 &&
                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                ) {
                    pendingSave = draft
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    save(draft)
                }
            },
        )
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

@Composable
private fun TaskEditorScreen(
    date: LocalDate,
    task: TaskEntity?,
    onBack: () -> Unit,
    onDelete: (() -> Unit)?,
    onSave: (TaskDraft) -> Unit,
) {
    var title by remember(task?.id) { mutableStateOf(task?.title.orEmpty()) }
    var details by remember(task?.id) { mutableStateOf(task?.details.orEmpty()) }
    var tags by remember(task?.id) { mutableStateOf(task?.tags.orEmpty().split(',').joinToString(", ")) }
    var reminderAt by remember(task?.id) { mutableStateOf(task?.reminderAt) }
    var confirmDelete by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val dateLabel = remember(date, locale) {
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.cancel)) }
                Text(
                    text = stringResource(if (task == null) R.string.new_task else R.string.edit_task),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(
                    onClick = { onSave(TaskDraft(title, details, tags, reminderAt)) },
                    enabled = title.isNotBlank(),
                ) { Text(stringResource(R.string.save)) }
            }
            if (onDelete != null) {
                TextButton(onClick = { confirmDelete = true }) {
                    Text(stringResource(R.string.delete_task))
                }
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            ) {
                Text(
                    text = dateLabel,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.task_title)) },
                singleLine = true,
            )
            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                label = { Text(stringResource(R.string.task_details)) },
                placeholder = { Text(stringResource(R.string.task_details_hint)) },
                minLines = 4,
            )
            OutlinedTextField(
                value = tags,
                onValueChange = { tags = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.task_tags)) },
                supportingText = { Text(stringResource(R.string.task_tags_hint)) },
                singleLine = true,
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(stringResource(R.string.task_reminders), style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = reminderAt?.let {
                                stringResource(
                                    R.string.reminder_time_value,
                                    java.text.SimpleDateFormat("HH:mm", locale).format(java.util.Date(it)),
                                )
                            } ?: stringResource(R.string.no_reminder),
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = {
                            val calendar = Calendar.getInstance().apply {
                                timeInMillis = reminderAt ?: date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                if (reminderAt == null) {
                                    if (date == LocalDate.now()) {
                                        timeInMillis = System.currentTimeMillis()
                                        add(Calendar.HOUR_OF_DAY, 1)
                                    } else {
                                        set(Calendar.HOUR_OF_DAY, 9)
                                        set(Calendar.MINUTE, 0)
                                    }
                                }
                            }
                            TimePickerDialog(
                                context,
                                { _, hour, minute ->
                                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                                    calendar.set(Calendar.MINUTE, minute)
                                    reminderAt = calendar.timeInMillis
                                },
                                calendar.get(Calendar.HOUR_OF_DAY),
                                calendar.get(Calendar.MINUTE),
                                true,
                            ).show()
                        }) { Text(stringResource(R.string.set_reminder)) }
                        if (reminderAt != null) {
                            TextButton(onClick = { reminderAt = null }) {
                                Text(stringResource(R.string.remove_reminder))
                            }
                        }
                    }
                }
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.delete_task)) },
            text = { Text(stringResource(R.string.delete_task_confirmation)) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete?.invoke() }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
}

@Composable
fun DayEditorPopup(date: LocalDate, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .fillMaxHeight(0.9f)
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 8.dp,
    ) {
        DayEditorRoute(date = date, onBack = onDismiss)
    }
}

@Composable
fun JournalQuickEntryPopup(date: LocalDate, onDismiss: () -> Unit) {
    val viewModel: DayEditorViewModel = viewModel(
        key = "journal-quick-entry-$date",
        factory = DayEditorViewModel.factory(date),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(150)
        focusRequester.requestFocus()
        keyboard?.show()
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .fillMaxHeight(0.55f),
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                        .withLocale(LocalConfiguration.current.locales[0])),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = uiState.journalText,
                onValueChange = viewModel::updateJournal,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = { Text(stringResource(R.string.journal_placeholder)) },
                shape = RoundedCornerShape(16.dp),
            )
        }
    }
}

@Composable
fun DayEditorScreen(
    uiState: DayEditorUiState,
    onBack: () -> Unit,
    onJournalChanged: (String) -> Unit,
    onAddSticker: (String) -> Unit,
    onAddStickerAsset: (StickerAssetEntity) -> Unit,
    onImportImage: () -> Unit,
    onImportRemoteImage: (String) -> Unit,
    onClearRemoteImageError: () -> Unit,
    onDeleteSticker: (StickerPlacementWithAsset) -> Unit,
) {
    var showStickerPicker by remember { mutableStateOf(false) }
    val context = LocalContext.current
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
                                val removeDescription = stringResource(
                                    R.string.remove_sticker_accessibility,
                                    sticker.displayName,
                                )
                                TextButton(
                                    onClick = { onDeleteSticker(sticker) },
                                    modifier = Modifier.semantics {
                                        contentDescription = removeDescription
                                    },
                                ) {
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
        }
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
