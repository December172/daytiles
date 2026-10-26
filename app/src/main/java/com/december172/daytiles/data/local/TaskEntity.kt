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
        Index(value = ["deletedAt"]),
    ],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val title: String,
    val details: String = "",
    val tags: String = "",
    val isCompleted: Boolean = false,
    val reminderAt: Long? = null,
    val importance: Int = 0,
    val urgency: Int = 0,
    val prioritySet: Boolean = false,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val deletedAt: Long? = null,
)
