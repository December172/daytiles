package com.december172.daytiles.data

import android.content.Context
import android.net.Uri
import com.december172.daytiles.data.local.JournalDao
import com.december172.daytiles.data.local.JournalEntryEntity
import com.december172.daytiles.data.local.TaskDao
import com.december172.daytiles.data.local.TaskEntity
import com.december172.daytiles.data.local.SubtaskDao
import com.december172.daytiles.data.local.SubtaskEntity
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

class DayRepository(
    private val journalDao: JournalDao,
    private val taskDao: TaskDao,
    private val subtaskDao: SubtaskDao,
    private val stickerPlacementDao: StickerPlacementDao,
    private val appContext: Context? = null,
) {
    fun observeJournals(month: YearMonth): Flow<List<JournalEntryEntity>> =
        journalDao.observeBetween(month.atDay(1), month.atEndOfMonth())

    fun observeTasks(month: YearMonth): Flow<List<TaskEntity>> =
        taskDao.observeBetween(month.atDay(1), month.atEndOfMonth())

    suspend fun recentOpenTasks(limit: Int = 3): List<TaskEntity> = taskDao.recentOpen(limit)

    suspend fun allOpenTasks(): List<TaskEntity> = taskDao.allOpen()

    suspend fun setTaskCompletedById(id: Long, completed: Boolean) {
        val task = taskDao.findById(id) ?: return
        setTaskCompleted(task, completed)
    }

    suspend fun setTaskReminder(task: TaskEntity, reminderAt: Long?) {
        val updated = task.copy(reminderAt = reminderAt, updatedAt = System.currentTimeMillis())
        taskDao.update(updated)
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

    suspend fun setSubtaskCompleted(subtask: SubtaskEntity, completed: Boolean) {
        subtaskDao.update(subtask.copy(isCompleted = completed, updatedAt = System.currentTimeMillis()))
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
            return 0
        }
        val savedId = if (entry.id == 0L) journalDao.insert(entry) else {
            journalDao.update(entry)
            entry.id
        }
        appContext?.let(DayTilesWidgetProvider::refreshJournal)
        return savedId
    }

    suspend fun addTask(
        date: LocalDate,
        title: String,
        details: String,
        tags: String,
        reminderAt: Long?,
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
        val updated = task.copy(isCompleted = completed, updatedAt = System.currentTimeMillis())
        taskDao.update(updated)
        appContext?.let {
            if (completed) TaskReminderScheduler.cancel(it, task.id)
            else if (task.reminderAt != null) TaskReminderScheduler.schedule(it, task)
        }
        refreshWidgets()
    }

    suspend fun updateTask(task: TaskEntity, title: String, details: String, tags: String, reminderAt: Long?) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        val cleanTags = normalizeTaskTags(tags)
        val updated = task.copy(
            title = cleanTitle,
            details = details.trim(),
            tags = cleanTags,
            reminderAt = reminderAt,
            updatedAt = System.currentTimeMillis(),
        )
        taskDao.update(updated)
        appContext?.let {
            if (reminderAt == null) TaskReminderScheduler.cancel(it, task.id)
            else TaskReminderScheduler.schedule(it, updated)
        }
        refreshWidgets()
    }

    suspend fun deleteTask(id: Long) {
        taskDao.deleteById(id)
        subtaskDao.deleteForTask(id)
        appContext?.let { TaskReminderScheduler.cancel(it, id) }
        refreshWidgets()
    }

    private fun refreshWidgets() {
        appContext?.let(DayTilesWidgetProvider::refresh)
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
