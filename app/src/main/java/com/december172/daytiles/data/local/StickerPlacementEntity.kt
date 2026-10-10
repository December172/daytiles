package com.december172.daytiles.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(
    tableName = "sticker_placements",
    indices = [
        Index(value = ["date"]),
        Index(value = ["date", "sortOrder"]),
    ],
)
data class StickerPlacementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val symbol: String,
    val sortOrder: Int,
    val createdAt: Long,
)
