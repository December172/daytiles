package com.december172.daytiles.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL AND date BETWEEN :start AND :end ORDER BY date, sortOrder, id")
    fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL AND date BETWEEN :start AND :end ORDER BY date, sortOrder, id")
    suspend fun tasksBetween(start: LocalDate, end: LocalDate): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL AND date = :date ORDER BY sortOrder, id")
    fun observeForDate(date: LocalDate): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL AND isCompleted = 0 ORDER BY date, sortOrder, id LIMIT :limit")
    suspend fun recentOpen(limit: Int): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL AND isCompleted = 0 ORDER BY date, sortOrder, id")
    suspend fun allOpen(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL AND id = :id LIMIT 1")
    suspend fun findById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE deletedAt IS NULL ORDER BY date, sortOrder, id")
    fun observeAll(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC, id DESC")
    fun observeDeleted(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE deletedAt IS NOT NULL AND id = :id")
    suspend fun findDeletedById(id: Long): TaskEntity?

    @Query("UPDATE tasks SET deletedAt = :now, updatedAt = :now WHERE id = :id AND deletedAt IS NULL")
    suspend fun moveToBin(id: Long, now: Long): Int

    @Query("UPDATE tasks SET deletedAt = NULL, updatedAt = :now WHERE id = :id AND deletedAt IS NOT NULL")
    suspend fun restore(id: Long, now: Long)

    @Query("UPDATE tasks SET isCompleted = :completed, updatedAt = :now WHERE id = :id AND deletedAt IS NULL")
    suspend fun setCompleted(id: Long, completed: Boolean, now: Long): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM tasks WHERE date = :date")
    suspend fun nextSortOrder(date: LocalDate): Int

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("UPDATE tasks SET importance = :importance, urgency = :urgency, prioritySet = 1, updatedAt = :updatedAt WHERE id = :taskId AND deletedAt IS NULL")
    suspend fun updatePriority(taskId: Long, importance: Int, urgency: Int, updatedAt: Long)

    @Query("DELETE FROM tasks WHERE id = :id AND deletedAt IS NOT NULL")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM tasks WHERE deletedAt IS NOT NULL")
    suspend fun emptyBin()
}

@Dao
interface SubtaskDao {
    @Query("SELECT * FROM subtasks WHERE parentTaskId = :parentTaskId ORDER BY sortOrder, id")
    fun observeForTask(parentTaskId: Long): Flow<List<SubtaskEntity>>

    @Query("SELECT * FROM subtasks WHERE parentTaskId = :parentTaskId ORDER BY sortOrder, id")
    suspend fun forTask(parentTaskId: Long): List<SubtaskEntity>

    @Insert
    suspend fun insert(subtask: SubtaskEntity): Long

    @Update
    suspend fun update(subtask: SubtaskEntity)

    @Query("DELETE FROM subtasks WHERE parentTaskId = :parentTaskId")
    suspend fun deleteForTask(parentTaskId: Long)

    @Query("DELETE FROM subtasks WHERE parentTaskId IN (SELECT id FROM tasks WHERE deletedAt IS NOT NULL)")
    suspend fun deleteForBinnedTasks()
}
