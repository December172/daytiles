package com.december172.daytiles.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [JournalEntryEntity::class, TaskEntity::class, StickerPlacementEntity::class],
    version = 2,
    exportSchema = true,
)
@TypeConverters(DateConverters::class)
abstract class DayTilesDatabase : RoomDatabase() {
    abstract fun journalDao(): JournalDao
    abstract fun taskDao(): TaskDao
    abstract fun stickerPlacementDao(): StickerPlacementDao

    companion object {
        @Volatile
        private var instance: DayTilesDatabase? = null

        fun getInstance(context: Context): DayTilesDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DayTilesDatabase::class.java,
                    "daytiles.db",
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sticker_placements` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` TEXT NOT NULL,
                        `symbol` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_sticker_placements_date` " +
                        "ON `sticker_placements` (`date`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_sticker_placements_date_sortOrder` " +
                        "ON `sticker_placements` (`date`, `sortOrder`)",
                )
            }
        }
    }
}
