package com.december172.daytiles.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        JournalEntryEntity::class,
        TaskEntity::class,
        StickerAssetEntity::class,
        StickerPlacementEntity::class,
    ],
    version = 6,
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
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build().also { instance = it }
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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sticker_assets` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `kind` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `displayName` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_sticker_assets_kind_source` " +
                        "ON `sticker_assets` (`kind`, `source`)",
                )
                db.execSQL(
                    """
                    INSERT INTO `sticker_assets` (`kind`, `source`, `displayName`, `createdAt`)
                    SELECT 'EMOJI', `symbol`, `symbol`, MIN(`createdAt`)
                    FROM `sticker_placements`
                    GROUP BY `symbol`
                    """.trimIndent(),
                )
                db.execSQL("ALTER TABLE `sticker_placements` RENAME TO `sticker_placements_v2`")
                db.execSQL("DROP INDEX IF EXISTS `index_sticker_placements_date`")
                db.execSQL("DROP INDEX IF EXISTS `index_sticker_placements_date_sortOrder`")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sticker_placements` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `date` TEXT NOT NULL,
                        `assetId` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`assetId`) REFERENCES `sticker_assets`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    """
                    INSERT INTO `sticker_placements` (`id`, `date`, `assetId`, `sortOrder`, `createdAt`)
                    SELECT p.`id`, p.`date`, a.`id`, p.`sortOrder`, p.`createdAt`
                    FROM `sticker_placements_v2` p
                    JOIN `sticker_assets` a ON a.`kind` = 'EMOJI' AND a.`source` = p.`symbol`
                    """.trimIndent(),
                )
                db.execSQL("DROP TABLE `sticker_placements_v2`")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_sticker_placements_date` " +
                        "ON `sticker_placements` (`date`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_sticker_placements_date_sortOrder` " +
                        "ON `sticker_placements` (`date`, `sortOrder`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_sticker_placements_assetId` " +
                        "ON `sticker_placements` (`assetId`)",
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `reminderAt` INTEGER")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `tags` TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tasks` ADD COLUMN `details` TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
