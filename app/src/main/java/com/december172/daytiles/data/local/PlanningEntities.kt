package com.december172.daytiles.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "task_goals", indices = [Index(value = ["periodStart", "periodType"], unique = true)])
data class TaskGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val periodStart: String,
    val periodType: String,
    val targetCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(tableName = "courses", indices = [Index(value = ["weekday", "startMinute"])])
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val weekday: Int,
    val startMinute: Int,
    val endMinute: Int,
    val location: String,
    val reminderMinutes: Int = 0,
    val isEnabled: Boolean = true,
)
