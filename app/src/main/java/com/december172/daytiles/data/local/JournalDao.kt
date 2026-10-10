package com.december172.daytiles.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries WHERE date BETWEEN :start AND :end ORDER BY createdAt")
    fun observeBetween(start: LocalDate, end: LocalDate): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE date = :date ORDER BY createdAt")
    fun observeForDate(date: LocalDate): Flow<List<JournalEntryEntity>>

    @Insert
    suspend fun insert(entry: JournalEntryEntity): Long

    @Update
    suspend fun update(entry: JournalEntryEntity)

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun deleteById(id: Long)
}

