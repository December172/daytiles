package com.december172.daytiles.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import java.time.LocalDate

@Entity(
    tableName = "sticker_placements",
    foreignKeys = [
        ForeignKey(
            entity = StickerAssetEntity::class,
            parentColumns = ["id"],
            childColumns = ["assetId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["date"]),
        Index(value = ["date", "sortOrder"]),
        Index(value = ["assetId"]),
    ],
)
data class StickerPlacementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val assetId: Long,
    val sortOrder: Int,
    val createdAt: Long,
)

data class StickerPlacementWithAsset(
    val placementId: Long,
    val date: LocalDate,
    val sortOrder: Int,
    val assetId: Long,
    val kind: String,
    val source: String,
    val displayName: String,
)
