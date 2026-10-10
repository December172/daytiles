package com.december172.daytiles.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subtasks",
    indices = [Index(value = ["parentTaskId", "sortOrder"])],
)
data class SubtaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parentTaskId: Long,
    val title: String,
    val isCompleted: Boolean = false,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
