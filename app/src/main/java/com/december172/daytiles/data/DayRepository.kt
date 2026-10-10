package com.december172.daytiles.data

import com.december172.daytiles.data.local.JournalDao
import com.december172.daytiles.data.local.JournalEntryEntity
import com.december172.daytiles.data.local.TaskDao
import com.december172.daytiles.data.local.TaskEntity
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow

class DayRepository(
    private val journalDao: JournalDao,
    private val taskDao: TaskDao,
) {
    fun observeJournals(month: YearMonth): Flow<List<JournalEntryEntity>> =
        journalDao.observeBetween(month.atDay(1), month.atEndOfMonth())

    fun observeTasks(month: YearMonth): Flow<List<TaskEntity>> =
        taskDao.observeBetween(month.atDay(1), month.atEndOfMonth())

    fun observeJournals(date: LocalDate): Flow<List<JournalEntryEntity>> =
        journalDao.observeForDate(date)

    fun observeTasks(date: LocalDate): Flow<List<TaskEntity>> = taskDao.observeForDate(date)

    suspend fun saveJournal(entry: JournalEntryEntity): Long {
        if (entry.content.isBlank()) {
            if (entry.id != 0L) journalDao.deleteById(entry.id)
            return 0
        }
        return if (entry.id == 0L) journalDao.insert(entry) else {
            journalDao.update(entry)
            entry.id
        }
    }

    suspend fun addTask(date: LocalDate, title: String) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        val now = System.currentTimeMillis()
        taskDao.insert(
            TaskEntity(
                date = date,
                title = cleanTitle,
                sortOrder = taskDao.nextSortOrder(date),
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun setTaskCompleted(task: TaskEntity, completed: Boolean) {
        taskDao.update(task.copy(isCompleted = completed, updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateTaskTitle(task: TaskEntity, title: String) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        taskDao.update(task.copy(title = cleanTitle, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteTask(id: Long) = taskDao.deleteById(id)
}
