package com.december172.daytiles.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface StickerPlacementDao {
    @Query(
        "SELECT * FROM sticker_placements WHERE date BETWEEN :start AND :end " +
            "ORDER BY date, sortOrder, id",
    )
    fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<StickerPlacementEntity>>

    @Query("SELECT * FROM sticker_placements WHERE date = :date ORDER BY sortOrder, id")
    fun observeForDate(date: LocalDate): Flow<List<StickerPlacementEntity>>

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM sticker_placements WHERE date = :date")
    suspend fun nextSortOrder(date: LocalDate): Int

    @Insert
    suspend fun insert(sticker: StickerPlacementEntity): Long

    @Query("DELETE FROM sticker_placements WHERE id = :id")
    suspend fun deleteById(id: Long)
}
