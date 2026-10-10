package com.december172.daytiles.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface StickerPlacementDao {
    @Query("""
        SELECT p.id AS placementId, p.date, p.sortOrder, a.id AS assetId,
            a.kind, a.source, a.displayName
        FROM sticker_placements p
        JOIN sticker_assets a ON a.id = p.assetId
        WHERE p.date BETWEEN :start AND :end
        ORDER BY p.date, p.sortOrder, p.id
    """)
    fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<StickerPlacementWithAsset>>

    @Query("""
        SELECT p.id AS placementId, p.date, p.sortOrder, a.id AS assetId,
            a.kind, a.source, a.displayName
        FROM sticker_placements p
        JOIN sticker_assets a ON a.id = p.assetId
        WHERE p.date = :date
        ORDER BY p.sortOrder, p.id
    """)
    fun observeForDate(date: LocalDate): Flow<List<StickerPlacementWithAsset>>

    @Query("SELECT * FROM sticker_assets ORDER BY createdAt, id")
    fun observeAssets(): Flow<List<StickerAssetEntity>>

    @Query("SELECT id FROM sticker_assets WHERE kind = :kind AND source = :source LIMIT 1")
    suspend fun findAssetId(kind: String, source: String): Long?

    @Insert
    suspend fun insertAsset(asset: StickerAssetEntity): Long

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM sticker_placements WHERE date = :date")
    suspend fun nextSortOrder(date: LocalDate): Int

    @Insert
    suspend fun insert(sticker: StickerPlacementEntity): Long

    @Query("DELETE FROM sticker_placements WHERE id = :id")
    suspend fun deleteById(id: Long)
}
