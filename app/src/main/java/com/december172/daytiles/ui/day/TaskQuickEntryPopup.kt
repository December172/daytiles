package com.december172.daytiles.ui.day

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.december172.daytiles.R
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Calendar

@Composable
fun TaskQuickEntryPopup(date: LocalDate, onDismiss: () -> Unit) {
    val viewModel: DayEditorViewModel = viewModel(key = "new-task-$date", factory = DayEditorViewModel.factory(date))
    var title by rememberSaveable { mutableStateOf("") }
    var details by rememberSaveable { mutableStateOf("") }
    var tags by rememberSaveable { mutableStateOf("") }
    var reminderAt by rememberSaveable { mutableStateOf<Long?>(null) }
    var showTags by rememberSaveable { mutableStateOf(false) }
    var showPriority by rememberSaveable { mutableStateOf(false) }
    var importance by rememberSaveable { mutableIntStateOf(0) }
    var urgency by rememberSaveable { mutableIntStateOf(0) }
    var prioritySet by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }
    fun save(reminder: Long?) {
        if (saving || title.isBlank()) return
        saving = true
        viewModel.saveTask(null, title, details, tags, reminder, emptyList(), importance, urgency, prioritySet) {
            keyboard?.hide()
            onDismiss()
        }
    }
    val notificationPermission = rememberLauncherForActivityResult(RequestPermission()) { granted -> save(if (granted) reminderAt else null) }
    LaunchedEffect(Unit) { focus.requestFocus(); keyboard?.show() }
    Box(Modifier.fillMaxSize().imePadding().semantics { testTagsAsResourceId = true }) {
        Box(Modifier.matchParentSize().clickable(enabled = !saving, onClick = onDismiss))
        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()) {
            Surface(shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                Column(Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.new_task), style = MaterialTheme.typography.titleLarge)
                    Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)),
                        style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth().focusRequester(focus).testTag("quick-task-title"),
                        label = { Text(stringResource(R.string.add_task_placeholder)) },
                        maxLines = 3, enabled = !saving)
                    OutlinedTextField(details, { details = it }, Modifier.fillMaxWidth().testTag("quick-task-details"),
                        label = { Text(stringResource(R.string.task_details)) }, maxLines = 4, enabled = !saving)
                    if (showTags) {
                        OutlinedTextField(tags, { tags = it }, Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.task_tags)) }, singleLine = true,
                            supportingText = { Text(stringResource(R.string.task_tags_hint)) })
                    }
                    if (showPriority) {
                        Column(Modifier.padding(horizontal = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(prioritySet, { prioritySet = it })
                                Text(stringResource(R.string.enable_priority), style = MaterialTheme.typography.bodySmall)
                            }
                            if (prioritySet) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(importance == 1, { importance = 1 - importance }, label = { Text(stringResource(R.string.important)) })
                                    FilterChip(urgency == 1, { urgency = 1 - urgency }, label = { Text(stringResource(R.string.urgent)) })
                                }
                            }
                        }
                    }
                    reminderAt?.let { reminder ->
                        Row(Modifier.padding(start = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(java.text.SimpleDateFormat("HH:mm", locale).format(java.util.Date(reminder)), color = MaterialTheme.colorScheme.primary)
                            TextButton(onClick = { reminderAt = null }) { Text(stringResource(R.string.remove_reminder)) }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            val calendar = Calendar.getInstance().apply {
                                timeInMillis = reminderAt ?: date.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                                if (reminderAt == null && date == LocalDate.now()) timeInMillis = System.currentTimeMillis() + 3_600_000
                            }
                            TimePickerDialog(context, { _, hour, minute ->
                                calendar.set(Calendar.HOUR_OF_DAY, hour); calendar.set(Calendar.MINUTE, minute)
                                calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
                                reminderAt = calendar.timeInMillis
                            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
                        }) {
                            Icon(painterResource(R.drawable.ic_fluent_timer_24_regular), stringResource(R.string.set_reminder), tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { showPriority = !showPriority }) {
                            Icon(painterResource(R.drawable.ic_fluent_apps_list_24_regular), stringResource(R.string.task_priority), tint = MaterialTheme.colorScheme.secondary)
                        }
                        IconButton(onClick = { showTags = !showTags }) {
                            Icon(painterResource(R.drawable.ic_fluent_tag_24_regular), stringResource(R.string.task_tags), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = {
                                if (reminderAt != null && Build.VERSION.SDK_INT >= 33 &&
                                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else save(reminderAt)
                            }, enabled = title.isNotBlank() && !saving, modifier = Modifier.testTag("quick-task-save"),
                        ) {
                            Icon(painterResource(R.drawable.ic_planner_check), null, Modifier.size(ButtonDefaults.IconSize))
                            Spacer(Modifier.width(ButtonDefaults.IconSpacing))
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            }
        }
    }
}
