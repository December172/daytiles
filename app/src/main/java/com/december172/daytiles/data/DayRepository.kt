package com.december172.daytiles.data

import androidx.room.withTransaction
import com.december172.daytiles.data.local.DayTilesDatabase
import com.december172.daytiles.data.local.ExpenseEntity
import com.december172.daytiles.data.local.BudgetEntity
import com.december172.daytiles.domain.model.Money
import android.content.Context
import android.net.Uri
import com.december172.daytiles.data.local.JournalDao
import com.december172.daytiles.data.local.JournalEntryEntity
import com.december172.daytiles.data.local.TaskDao
import com.december172.daytiles.data.local.TaskEntity
import com.december172.daytiles.data.local.SubtaskDao
import com.december172.daytiles.data.local.SubtaskEntity
import com.december172.daytiles.data.local.PlanningDao
import com.december172.daytiles.data.local.TaskGoalEntity
import com.december172.daytiles.data.local.CourseEntity
import com.december172.daytiles.data.local.StickerPlacementDao
import com.december172.daytiles.data.local.StickerPlacementEntity
import com.december172.daytiles.data.local.StickerAssetEntity
import com.december172.daytiles.data.local.StickerAssetKind
import com.december172.daytiles.data.local.StickerPlacementWithAsset
import com.december172.daytiles.domain.model.normalizeTaskTags
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import com.december172.daytiles.widget.DayTilesWidgetProvider
import com.december172.daytiles.data.CourseReminderScheduler
import com.december172.daytiles.widget.WeekWidgetProvider

class DayRepository(
    private val database: DayTilesDatabase,
    private val appContext: Context? = null,
) {
    private val journalDao = database.journalDao()
    private val taskDao = database.taskDao()
    private val subtaskDao = database.subtaskDao()
    private val planningDao = database.planningDao()
    private val stickerPlacementDao = database.stickerPlacementDao()
    private val financeDao = database.financeDao()

    fun observeAllTasks() = taskDao.observeAll()
    fun observeDeletedTasks() = taskDao.observeDeleted()

    suspend fun restoreTask(id: Long) {
        val restored = database.withTransaction {
            val task = taskDao.findDeletedById(id) ?: return@withTransaction null
            taskDao.restore(id, System.currentTimeMillis())
            task.copy(deletedAt = null)
        } ?: return
        if (!restored.isCompleted && (restored.reminderAt ?: 0) > System.currentTimeMillis()) {
            appContext?.let { TaskReminderScheduler.schedule(it, restored) }
        }
        refreshWidgets()
    }

    suspend fun permanentlyDeleteTask(id: Long) {
        database.withTransaction {
            if (taskDao.findDeletedById(id) != null) {
                subtaskDao.deleteForTask(id)
                taskDao.deleteById(id)
            }
        }
        refreshWidgets()
    }

    suspend fun emptyRecycleBin() {
        database.withTransaction {
            subtaskDao.deleteForBinnedTasks()
            taskDao.emptyBin()
        }
        refreshWidgets()
    }

    fun observeExpenses(month: YearMonth, currency: String) =
        financeDao.observeExpenses(month.atDay(1), month.atEndOfMonth(), currency)

    fun observeBudgets(month: YearMonth, currency: String) =
        financeDao.observeBudgets(month.atDay(1), month.atEndOfMonth(), currency)

    suspend fun saveExpense(
        id: Long?, date: LocalDate, title: String, amountMinor: Long,
        currency: String, category: String, note: String,
    ): Long {
        require(title.isNotBlank())
        require(currency in Money.currencies && amountMinor in 1..Money.MAX_AMOUNT_MINOR)
        return database.withTransaction {
            val existing = id?.let { requireNotNull(financeDao.findExpense(it)) }
            val now = System.currentTimeMillis()
            val expense = ExpenseEntity(
                id = existing?.id ?: 0, date = date, title = title.trim(), amountMinor = amountMinor,
                currency = currency, category = category, note = note.trim(),
                createdAt = existing?.createdAt ?: now, updatedAt = now,
            )
            if (existing == null) financeDao.insertExpense(expense)
            else { financeDao.updateExpense(expense); existing.id }
        }
    }

    suspend fun deleteExpense(id: Long) = financeDao.deleteExpense(id)

    suspend fun saveBudget(date: LocalDate, monthly: Boolean, currency: String, amountMinor: Long?) {
        require(currency in Money.currencies)
        val start = if (monthly) date.withDayOfMonth(1) else date
        val type = if (monthly) "MONTH" else "DAY"
        if (amountMinor == null) financeDao.deleteBudget(start, type, currency)
        else {
            require(amountMinor in 0..Money.MAX_AMOUNT_MINOR)
            financeDao.saveBudget(BudgetEntity(start, type, currency, amountMinor, System.currentTimeMillis()))
        }
    }

    fun observeJournals(month: YearMonth): Flow<List<JournalEntryEntity>> =
        journalDao.observeBetween(month.atDay(1), month.atEndOfMonth())

    fun observeTasks(month: YearMonth): Flow<List<TaskEntity>> =
        taskDao.observeBetween(month.atDay(1), month.atEndOfMonth())

    suspend fun tasksBetween(start: LocalDate, end: LocalDate) = taskDao.tasksBetween(start, end)

    suspend fun journalsBetween(start: LocalDate, end: LocalDate) = journalDao.journalsBetween(start, end)

    fun observeTasksBetween(start: LocalDate, end: LocalDate): Flow<List<TaskEntity>> = taskDao.observeBetween(start, end)

    suspend fun recentOpenTasks(limit: Int = 3): List<TaskEntity> = taskDao.recentOpen(limit)

    suspend fun allOpenTasks(): List<TaskEntity> = taskDao.allOpen()

    suspend fun setTaskCompletedById(id: Long, completed: Boolean) {
        val task = taskDao.findById(id) ?: return
        setTaskCompleted(task, completed)
    }

    suspend fun setTaskPriority(task: TaskEntity, importance: Int, urgency: Int) {
        taskDao.updatePriority(task.id, importance.coerceIn(0, 1), urgency.coerceIn(0, 1), System.currentTimeMillis())
        refreshWidgets()
    }

    suspend fun setTaskReminder(task: TaskEntity, reminderAt: Long?) {
        val updated = task.copy(reminderAt = reminderAt, updatedAt = System.currentTimeMillis())
        val saved = database.withTransaction {
            val current = taskDao.findById(task.id) ?: return@withTransaction false
            taskDao.update(updated.copy(deletedAt = current.deletedAt))
            true
        }
        if (!saved) return
        appContext?.let {
            if (reminderAt == null) TaskReminderScheduler.cancel(it, task.id)
            else TaskReminderScheduler.schedule(it, updated)
        }
        refreshWidgets()
    }

    fun observeJournals(date: LocalDate): Flow<List<JournalEntryEntity>> =
        journalDao.observeForDate(date)

    suspend fun journalFor(date: LocalDate): String =
        journalDao.observeForDate(date).first().firstOrNull()?.content.orEmpty()

    fun observeTasks(date: LocalDate): Flow<List<TaskEntity>> = taskDao.observeForDate(date)

    fun observeSubtasks(taskId: Long): Flow<List<SubtaskEntity>> = subtaskDao.observeForTask(taskId)

    suspend fun subtasksForTask(taskId: Long): List<SubtaskEntity> = subtaskDao.forTask(taskId)

    suspend fun saveSubtasks(taskId: Long, titles: List<String>) {
        database.withTransaction {
            if (taskDao.findById(taskId) == null) return@withTransaction
            val now = System.currentTimeMillis()
            val existing = subtaskDao.forTask(taskId).groupBy { it.title }.mapValues { it.value.toMutableList() }
            subtaskDao.deleteForTask(taskId)
            titles.map(String::trim).filter(String::isNotEmpty).forEachIndexed { index, title ->
                val previous = existing[title]?.removeFirstOrNull()
                subtaskDao.insert(
                    SubtaskEntity(
                        parentTaskId = taskId,
                        title = title,
                        isCompleted = previous?.isCompleted ?: false,
                        sortOrder = index,
                        createdAt = previous?.createdAt ?: now,
                        updatedAt = now,
                    ),
                )
            }
        }
    }

    suspend fun setSubtaskCompleted(subtask: SubtaskEntity, completed: Boolean) {
        database.withTransaction {
            if (taskDao.findById(subtask.parentTaskId) != null) {
                subtaskDao.update(subtask.copy(isCompleted = completed, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    suspend fun goal(periodType: String, periodStart: LocalDate): TaskGoalEntity? =
        planningDao.findGoal(periodType, periodStart.toString())

    suspend fun saveGoal(periodType: String, periodStart: LocalDate, targetCount: Int) {
        val old = goal(periodType, periodStart)
        val now = System.currentTimeMillis()
        planningDao.saveGoal(
            TaskGoalEntity(
                id = old?.id ?: 0,
                periodStart = periodStart.toString(),
                periodType = periodType,
                targetCount = targetCount.coerceAtLeast(1),
                createdAt = old?.createdAt ?: now,
                updatedAt = now,
            ),
        )
    }

    fun observeCourses() = planningDao.observeCourses()

    suspend fun addCourse(course: CourseEntity): Long {
        val id = planningDao.addCourse(course)
        appContext?.let { CourseReminderScheduler.schedule(it, course.copy(id = id)) }
        return id
    }

    suspend fun updateCourse(course: CourseEntity) {
        planningDao.updateCourse(course)
        appContext?.let {
            CourseReminderScheduler.cancel(it, course.id)
            CourseReminderScheduler.schedule(it, course)
        }
    }

    suspend fun deleteCourse(id: Long) {
        planningDao.deleteCourse(id)
        appContext?.let { CourseReminderScheduler.cancel(it, id) }
    }

    fun observeStickers(month: YearMonth): Flow<List<StickerPlacementWithAsset>> =
        stickerPlacementDao.observeBetween(month.atDay(1), month.atEndOfMonth())

    fun observeStickers(date: LocalDate): Flow<List<StickerPlacementWithAsset>> =
        stickerPlacementDao.observeForDate(date)

    fun observeStickerAssets(): Flow<List<StickerAssetEntity>> = stickerPlacementDao.observeAssets()

    suspend fun saveJournal(entry: JournalEntryEntity): Long {
        if (entry.content.isBlank()) {
            if (entry.id != 0L) journalDao.deleteById(entry.id)
            appContext?.let(DayTilesWidgetProvider::refreshJournal)
            appContext?.let(WeekWidgetProvider::refresh)
            return 0
        }
        val savedId = if (entry.id == 0L) journalDao.insert(entry) else {
            journalDao.update(entry)
            entry.id
        }
        appContext?.let(DayTilesWidgetProvider::refreshJournal)
        appContext?.let(WeekWidgetProvider::refresh)
        return savedId
    }

    suspend fun addTask(
        date: LocalDate,
        title: String,
        details: String,
        tags: String,
        reminderAt: Long?,
        importance: Int = 0,
        urgency: Int = 0,
        prioritySet: Boolean = false,
    ): Long {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return 0L
        val now = System.currentTimeMillis()
        val task = TaskEntity(
            date = date,
            title = cleanTitle,
            details = details.trim(),
            tags = normalizeTaskTags(tags),
            reminderAt = reminderAt,
            importance = importance,
            urgency = urgency,
            prioritySet = prioritySet,
            sortOrder = taskDao.nextSortOrder(date),
            createdAt = now,
            updatedAt = now,
        )
        val taskId = taskDao.insert(task)
        if (taskId > 0) appContext?.let {
            if (reminderAt == null) TaskReminderScheduler.cancel(it, taskId)
            else TaskReminderScheduler.schedule(it, task.copy(id = taskId))
        }
        refreshWidgets()
        return taskId
    }

    suspend fun setTaskCompleted(task: TaskEntity, completed: Boolean) {
        if (taskDao.setCompleted(task.id, completed, System.currentTimeMillis()) == 0) return
        appContext?.let {
            if (completed) TaskReminderScheduler.cancel(it, task.id)
            else if (task.reminderAt != null) TaskReminderScheduler.schedule(it, task)
        }
        refreshWidgets()
    }

    suspend fun updateTask(
        task: TaskEntity,
        title: String,
        details: String,
        tags: String,
        reminderAt: Long?,
        importance: Int = task.importance,
        urgency: Int = task.urgency,
        prioritySet: Boolean = task.prioritySet,
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        val cleanTags = normalizeTaskTags(tags)
        val updated = task.copy(
            title = cleanTitle,
            details = details.trim(),
            tags = cleanTags,
            reminderAt = reminderAt,
            importance = importance,
            urgency = urgency,
            prioritySet = prioritySet,
            updatedAt = System.currentTimeMillis(),
        )
        val saved = database.withTransaction {
            val current = taskDao.findById(task.id) ?: return@withTransaction false
            taskDao.update(updated.copy(deletedAt = current.deletedAt))
            true
        }
        if (!saved) return
        appContext?.let {
            if (reminderAt == null) TaskReminderScheduler.cancel(it, task.id)
            else TaskReminderScheduler.schedule(it, updated)
        }
        refreshWidgets()
    }

    suspend fun deleteTask(id: Long) {
        if (taskDao.moveToBin(id, System.currentTimeMillis()) == 0) return
        appContext?.let { TaskReminderScheduler.cancel(it, id) }
        refreshWidgets()
    }

    private fun refreshWidgets() {
        appContext?.let {
            DayTilesWidgetProvider.refresh(it)
            WeekWidgetProvider.refresh(it)
        }
    }

    suspend fun addSticker(date: LocalDate, symbol: String) {
        val cleanSymbol = symbol.trim()
        if (cleanSymbol.isEmpty()) return
        val assetId = stickerPlacementDao.findAssetId(StickerAssetKind.EMOJI, cleanSymbol)
            ?: stickerPlacementDao.insertAsset(
                StickerAssetEntity(
                    kind = StickerAssetKind.EMOJI,
                    source = cleanSymbol,
                    displayName = cleanSymbol,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        placeStickerAsset(date, assetId)
    }

    suspend fun placeStickerAsset(date: LocalDate, assetId: Long) {
        stickerPlacementDao.insert(
            StickerPlacementEntity(
                date = date,
                assetId = assetId,
                sortOrder = stickerPlacementDao.nextSortOrder(date),
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun importImageSticker(context: Context, date: LocalDate, uri: Uri) = withContext(Dispatchers.IO) {
        val mimeType = context.contentResolver.getType(uri)
        val extension = when (mimeType) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> return@withContext
        }
        val sourceStream = context.contentResolver.openInputStream(uri) ?: return@withContext
        saveImageSticker(context, date, extension, sourceStream)
    }

    suspend fun importRemoteImageSticker(context: Context, date: LocalDate, url: String) =
        withContext(Dispatchers.IO) {
            val uri = URI(url.trim())
            require(uri.scheme.equals("https", ignoreCase = true))
            val connection = (uri.toURL().openConnection() as HttpURLConnection).apply {
                connectTimeout = NETWORK_TIMEOUT_MS
                readTimeout = NETWORK_TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("Accept", "image/png,image/webp")
            }
            try {
                require(connection.responseCode in 200..299)
                require(connection.url.protocol.equals("https", ignoreCase = true))
                val contentLength = connection.contentLengthLong
                require(contentLength < 0 || contentLength <= MAX_STICKER_BYTES)
                val extension = when (connection.contentType?.substringBefore(';')?.lowercase()) {
                    "image/png" -> "png"
                    "image/webp" -> "webp"
                    else -> error("Unsupported image type")
                }
                saveImageSticker(context, date, extension, connection.inputStream)
            } finally {
                connection.disconnect()
            }
        }

    private suspend fun saveImageSticker(
        context: Context,
        date: LocalDate,
        extension: String,
        sourceStream: InputStream,
    ) {
        val stickerDirectory = File(context.filesDir, "stickers").apply { mkdirs() }
        val destination = File(stickerDirectory, "${UUID.randomUUID()}.$extension")
        try {
            sourceStream.use { input ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var totalBytes = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        totalBytes += count
                        require(totalBytes <= MAX_STICKER_BYTES)
                        output.write(buffer, 0, count)
                    }
                }
            }
            val now = System.currentTimeMillis()
            val assetId = stickerPlacementDao.insertAsset(
                StickerAssetEntity(
                    kind = StickerAssetKind.IMAGE,
                    source = destination.absolutePath,
                    displayName = destination.name,
                    createdAt = now,
                ),
            )
            stickerPlacementDao.insert(
                StickerPlacementEntity(
                    date = date,
                    assetId = assetId,
                    sortOrder = stickerPlacementDao.nextSortOrder(date),
                    createdAt = now,
                ),
            )
        } catch (error: Exception) {
            destination.delete()
            throw error
        }
    }

    suspend fun deleteSticker(id: Long) = stickerPlacementDao.deleteById(id)

    private companion object {
        const val MAX_STICKER_BYTES = 10L * 1024L * 1024L
        const val NETWORK_TIMEOUT_MS = 15_000
    }
}
