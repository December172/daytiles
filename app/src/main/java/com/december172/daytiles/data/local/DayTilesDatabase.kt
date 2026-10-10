package com.december172.daytiles.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [JournalEntryEntity::class, TaskEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(DateConverters::class)
abstract class DayTilesDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var instance: DayTilesDatabase? = null

        fun getInstance(context: Context): DayTilesDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DayTilesDatabase::class.java,
                    "daytiles.db",
                ).build().also { instance = it }
            }
    }
}

