package com.december172.daytiles

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.december172.daytiles.data.DayRepository
import com.december172.daytiles.data.local.DayTilesDatabase
import com.december172.daytiles.data.local.JournalEntryEntity
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlannerRepositoryTest {
    private lateinit var db: DayTilesDatabase
    private lateinit var repo: DayRepository
    private val date = LocalDate.of(2026, 10, 26)
    private val context: Context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(context, DayTilesDatabase::class.java).build()
        repo = DayRepository(db)
    }
    @After fun close() { db.close() }

    private suspend fun task(day: LocalDate = date, title: String = "Task") =
        repo.addTask(day, title, "Keep details", "work, study", System.currentTimeMillis() + 86_400_000, 1, 1, true)

    @Test fun timelineIncludesPastAndFutureAcrossAllMonths() = runBlocking {
        task(date.plusYears(10), "Future")
        task(date.minusYears(10), "Past")
        val now = task()
        repo.setTaskCompletedById(now, true)
        val all = repo.observeAllTasks().first()
        assertEquals(listOf("Past", "Task", "Future"), all.map { it.title })
        assertTrue(all[1].isCompleted)
    }

    @Test fun recycleBinHidesTasksFromEveryActiveQueryAndRestoresAllFields() = runBlocking {
        val id = task()
        repo.saveSubtasks(id, listOf("First", "Second"))
        val sub = repo.subtasksForTask(id).first()
        repo.setSubtaskCompleted(sub, true)
        val original = db.taskDao().findById(id)!!
        val children = repo.subtasksForTask(id)
        repo.saveJournal(JournalEntryEntity(date = date, content = "Separate journal", createdAt = 1, updatedAt = 1))
        repo.deleteTask(id)
        assertNull(db.taskDao().findById(id))
        assertTrue(repo.observeAllTasks().first().isEmpty())
        assertTrue(repo.observeTasks(date).first().isEmpty())
        assertTrue(repo.tasksBetween(date.minusYears(20), date.plusYears(20)).isEmpty())
        assertTrue(repo.allOpenTasks().isEmpty())
        assertTrue(repo.recentOpenTasks().isEmpty())
        assertEquals(id, repo.observeDeletedTasks().first().single().id)
        assertEquals(children, repo.subtasksForTask(id))
        assertEquals("Separate journal", repo.journalFor(date))
        repo.restoreTask(id)
        val restored = db.taskDao().findById(id)!!
        assertEquals(original, restored.copy(updatedAt = original.updatedAt))
        assertEquals(children, repo.subtasksForTask(id))
        assertTrue(repo.observeDeletedTasks().first().isEmpty())
    }

    @Test fun permanentDeletionOnlyRemovesBinnedTasksAndTheirSubtasks() = runBlocking {
        val active = task(title = "Active")
        val first = task(title = "First")
        val second = task(title = "Second")
        listOf(active, first, second).forEach { repo.saveSubtasks(it, listOf("Child")) }
        repo.deleteTask(first); repo.deleteTask(second)
        repo.permanentlyDeleteTask(active)
        assertNotNull(db.taskDao().findById(active))
        repo.permanentlyDeleteTask(first)
        assertNull(db.taskDao().findDeletedById(first))
        assertTrue(repo.subtasksForTask(first).isEmpty())
        repo.emptyRecycleBin()
        assertTrue(repo.observeDeletedTasks().first().isEmpty())
        assertTrue(repo.subtasksForTask(second).isEmpty())
        assertEquals(listOf(active), repo.observeAllTasks().first().map { it.id })
        assertEquals(1, repo.subtasksForTask(active).size)
    }

    @Test fun staleEditorsCannotResurrectOrRewriteDeletedTasks() = runBlocking {
        val id = task()
        repo.saveSubtasks(id, listOf("Original child"))
        val original = db.taskDao().findById(id)!!
        repo.deleteTask(id)
        repo.setTaskCompleted(original, true)
        repo.setTaskReminder(original, null)
        repo.setTaskPriority(original, 0, 0)
        repo.updateTask(original, "Changed", "Changed", "Changed", null)
        repo.saveSubtasks(id, listOf("Changed child"))
        repo.setSubtaskCompleted(repo.subtasksForTask(id).single(), true)
        assertNull(db.taskDao().findById(id))
        val trashed = db.taskDao().findDeletedById(id)!!
        assertEquals(original, trashed.copy(deletedAt = null, updatedAt = original.updatedAt))
        assertEquals("Original child", repo.subtasksForTask(id).single().title)
        assertFalse(repo.subtasksForTask(id).single().isCompleted)
    }

    @Test fun expensesUseExactTotalsAndSeparateDatesMonthsAndCurrencies() = runBlocking {
        val january = LocalDate.of(2028, 1, 31)
        val february = january.plusDays(1)
        val first = repo.saveExpense(null, january, "A", 10, "CNY", "food", "note")
        repo.saveExpense(null, january, "B", 20, "CNY", "food", "")
        repo.saveExpense(null, january, "HK", 999, "HKD", "other", "")
        repo.saveExpense(null, february, "Next", 40, "CNY", "other", "")
        assertEquals(30L, repo.observeExpenses(YearMonth.from(january), "CNY").first().sumOf { it.amountMinor })
        assertEquals(999L, repo.observeExpenses(YearMonth.from(january), "HKD").first().sumOf { it.amountMinor })
        repo.saveExpense(first, february, "Edited", 15, "CNY", "learning", "edited note")
        assertEquals(20L, repo.observeExpenses(YearMonth.from(january), "CNY").first().sumOf { it.amountMinor })
        assertEquals(55L, repo.observeExpenses(YearMonth.from(february), "CNY").first().sumOf { it.amountMinor })
        assertEquals("edited note", db.financeDao().findExpense(first)!!.note)
        repo.deleteExpense(first)
        assertEquals(40L, repo.observeExpenses(YearMonth.from(february), "CNY").first().sumOf { it.amountMinor })
        assertNull(db.financeDao().findExpense(first))
    }

    @Test fun dailyAndMonthlyBudgetsAreIndependentAndSupportZeroAndRemoval() = runBlocking {
        repo.saveBudget(date, false, "CNY", 0)
        repo.saveBudget(date, true, "CNY", 10000)
        repo.saveBudget(date, true, "HKD", 5000)
        var budgets = repo.observeBudgets(YearMonth.from(date), "CNY").first()
        assertEquals(2, budgets.size)
        assertEquals(0L, budgets.single { it.periodType == "DAY" }.amountMinor)
        assertEquals(date.withDayOfMonth(1), budgets.single { it.periodType == "MONTH" }.periodStart)
        repo.saveBudget(date, false, "CNY", 1200)
        repo.saveBudget(date, true, "CNY", null)
        budgets = repo.observeBudgets(YearMonth.from(date), "CNY").first()
        assertEquals(1200L, budgets.single().amountMinor)
        assertEquals(5000L, repo.observeBudgets(YearMonth.from(date), "HKD").first().single().amountMinor)
    }

    @Test fun expenseValidationRejectsInvalidAmountsWithoutWriting() = runBlocking {
        for (amount in listOf(0L, -1L, Long.MAX_VALUE)) {
            try {
                repo.saveExpense(null, date, "Invalid", amount, "CNY", "other", "")
                fail("Invalid amount was accepted")
            } catch (_: IllegalArgumentException) { }
        }
        assertTrue(repo.observeExpenses(YearMonth.from(date), "CNY").first().isEmpty())
    }

    @Test fun financeAndRecycleBinPersistAfterDatabaseReopen() = runBlocking {
        val name = "planner-persistence-test.db"
        context.deleteDatabase(name)
        try {
            var disk = Room.databaseBuilder(context, DayTilesDatabase::class.java, name).build()
            var repository = DayRepository(disk)
            val id = repository.addTask(date, "Persist", "", "", null)
            repository.saveSubtasks(id, listOf("Persist child"))
            repository.deleteTask(id)
            repository.saveExpense(null, date, "Lunch", 1250, "CNY", "food", "Keep note")
            repository.saveBudget(date, true, "CNY", 30000)
            disk.close()
            disk = Room.databaseBuilder(context, DayTilesDatabase::class.java, name).build()
            repository = DayRepository(disk)
            assertEquals(id, repository.observeDeletedTasks().first().single().id)
            assertEquals("Persist child", repository.subtasksForTask(id).single().title)
            assertEquals(1250L, repository.observeExpenses(YearMonth.from(date), "CNY").first().single().amountMinor)
            assertEquals(30000L, repository.observeBudgets(YearMonth.from(date), "CNY").first().single().amountMinor)
            disk.close()
        } finally { context.deleteDatabase(name) }
    }
}
