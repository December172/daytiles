package com.december172.daytiles.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanningDao {
    @Query("SELECT * FROM task_goals WHERE periodType = :periodType AND periodStart = :periodStart LIMIT 1")
    suspend fun findGoal(periodType: String, periodStart: String): TaskGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGoal(goal: TaskGoalEntity)

    @Query("SELECT * FROM courses WHERE isEnabled = 1 ORDER BY weekday, startMinute, id")
    fun observeCourses(): Flow<List<CourseEntity>>

    @Insert
    suspend fun addCourse(course: CourseEntity): Long

    @Update
    suspend fun updateCourse(course: CourseEntity)

    @Query("DELETE FROM courses WHERE id = :id")
    suspend fun deleteCourse(id: Long)
}
