package com.december172.daytiles

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.december172.daytiles.data.DayRepository
import com.december172.daytiles.data.local.DayTilesDatabase
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlannerMigrationTest {
    @Test fun migrationFromVersion8PreservesEveryExistingEntity() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "planner-v8-migration-test.db"
        context.deleteDatabase(name)
        val old = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(name), null)
        val schema = JSONObject(InstrumentationRegistry.getInstrumentation().context.assets
            .open("com.december172.daytiles.data.local.DayTilesDatabase/8.json").bufferedReader().use { it.readText() }).getJSONObject("database")
        val entities = schema.getJSONArray("entities")
        for (index in 0 until entities.length()) {
            val entity = entities.getJSONObject(index)
            val table = entity.getString("tableName")
            old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
            val indices = entity.getJSONArray("indices")
            for (i in 0 until indices.length()) old.execSQL(indices.getJSONObject(i).getString("createSql").replace("\${TABLE_NAME}", table))
        }
        val setup = schema.getJSONArray("setupQueries")
        for (index in 0 until setup.length()) old.execSQL(setup.getString(index))
        old.execSQL("INSERT INTO journal_entries VALUES (1, '2026-10-26', 'Journal preserved', 101, 102)")
        old.execSQL("INSERT INTO tasks (id,date,title,details,tags,isCompleted,reminderAt,importance,urgency,prioritySet,sortOrder,createdAt,updatedAt) VALUES (1,'2026-10-26','Task preserved','Details preserved','work',1,1900000000000,1,1,1,2,101,102)")
        old.execSQL("INSERT INTO subtasks VALUES (1,1,'Completed child',1,0,101,102)")
        old.execSQL("INSERT INTO sticker_assets VALUES (1,'EMOJI','book','Book',101)")
        old.execSQL("INSERT INTO sticker_placements VALUES (1,'2026-10-26',1,0,101)")
        old.execSQL("INSERT INTO task_goals VALUES (1,'2026-10-26','DAY',3,101,102)")
        old.execSQL("INSERT INTO courses VALUES (1,'Math',1,540,600,'A101',0,1)")
        val tables = listOf("journal_entries", "tasks", "subtasks", "sticker_assets", "sticker_placements", "task_goals", "courses")
        val snapshots = tables.associateWith { table ->
            old.rawQuery("SELECT * FROM $table ORDER BY id", null).use { cursor ->
                buildList { while (cursor.moveToNext()) add((0 until cursor.columnCount).map { if (cursor.isNull(it)) null else cursor.getString(it) }) }
            }
        }
        old.version = 8; old.close()
        val db = Room.databaseBuilder(context, DayTilesDatabase::class.java, name)
            .addMigrations(DayTilesDatabase.MIGRATION_8_9).build()
        try {
            // Opening through Room validates the entire migrated schema, not just selected columns.
            val sqlite = db.openHelper.writableDatabase
            assertEquals(9, sqlite.version)
            for (table in tables) {
                sqlite.query("SELECT * FROM $table ORDER BY id").use { cursor ->
                    val original = snapshots.getValue(table)
                    val actual = buildList { while (cursor.moveToNext()) add((0 until original.first().size).map { if (cursor.isNull(it)) null else cursor.getString(it) }) }
                    assertEquals(table, original, actual)
                }
            }
            val task = db.taskDao().findById(1)!!
            assertNull(task.deletedAt)
            assertEquals(1900000000000L, task.reminderAt)
            assertTrue(task.isCompleted)
            assertTrue(db.subtaskDao().forTask(1).single().isCompleted)
            val repo = DayRepository(db)
            assertTrue(repo.observeDeletedTasks().first().isEmpty())
            assertTrue(repo.observeExpenses(YearMonth.of(2026, 10), "CNY").first().isEmpty())
            assertTrue(repo.observeBudgets(YearMonth.of(2026, 10), "CNY").first().isEmpty())
            repo.saveExpense(null, LocalDate.of(2026, 10, 26), "New expense", 1250, "CNY", "food", "")
            assertEquals(1250L, repo.observeExpenses(YearMonth.of(2026, 10), "CNY").first().single().amountMinor)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
