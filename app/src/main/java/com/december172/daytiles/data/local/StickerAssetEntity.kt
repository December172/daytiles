package com.december172.daytiles.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sticker_assets",
    indices = [Index(value = ["kind", "source"], unique = true)],
)
data class StickerAssetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val source: String,
    val displayName: String,
    val createdAt: Long,
)

object StickerAssetKind {
    const val EMOJI = "EMOJI"
    const val IMAGE = "IMAGE"
}
