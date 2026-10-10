package com.december172.daytiles.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "journal_entries",
    indices = [Index(value = ["date"])],
)
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
)

