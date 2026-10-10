package com.december172.daytiles.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE date BETWEEN :start AND :end ORDER BY date, sortOrder, id")
    fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY sortOrder, id")
    fun observeForDate(date: LocalDate): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY date, sortOrder, id LIMIT :limit")
    suspend fun recentOpen(limit: Int): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY date, sortOrder, id")
    suspend fun allOpen(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): TaskEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM tasks WHERE date = :date")
    suspend fun nextSortOrder(date: LocalDate): Int

    @Insert
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteById(id: Long)
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
}
