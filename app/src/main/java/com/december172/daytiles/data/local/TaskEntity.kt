package com.december172.daytiles.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["date"]),
        Index(value = ["date", "sortOrder"]),
    ],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val title: String,
    val isCompleted: Boolean = false,
    val reminderAt: Long? = null,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
