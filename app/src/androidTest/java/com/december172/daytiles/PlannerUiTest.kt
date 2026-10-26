package com.december172.daytiles

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.SystemClock
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.december172.daytiles.data.local.DayTilesDatabase
import java.io.File
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
class PlannerUiTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context get() = instrumentation.targetContext
    private val app get() = context.applicationContext as DayTilesApplication
    private val repo get() = app.repository
    private val db get() = DayTilesDatabase.getInstance(context)
    private lateinit var device: UiDevice
    private val tasks = mutableListOf<Long>()
    private val expenses = mutableListOf<Long>()
    private val today = LocalDate.now()
    private lateinit var originalCurrency: String
    private var scenario: ActivityScenario<MainActivity>? = null
    private fun text(id: Int) = context.getString(id)
    private fun find(selector: BySelector): UiObject2 =
        requireNotNull(device.wait(Until.findObject(selector), 10_000)) { "Missing UI element: $selector" }
    private fun click(id: Int) = find(By.text(text(id))).click()
    private fun await(condition: () -> Boolean) {
        val deadline = SystemClock.uptimeMillis() + 10_000
        while (!condition()) {
            if (SystemClock.uptimeMillis() > deadline) fail("Timed out waiting for persisted state")
            SystemClock.sleep(30)
        }
    }
    private fun scrollTo(selector: BySelector, listTag: String? = null): UiObject2 {
        repeat(12) {
            device.findObject(selector)?.let { found -> if (found.visibleBounds.height() > 0) return found }
            val scroll = device.wait(Until.findObject(if (listTag == null) By.scrollable(true) else By.res(listTag)), 2_000)
            if (scroll == null) return find(selector)
            scroll.scroll(Direction.DOWN, 0.75f)
        }
        return find(selector)
    }
    private fun openFeature(id: Int, rootTag: String? = null) {
        scenario?.close()
        scenario = ActivityScenario.launch(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        click(R.string.me_tab)
        scrollTo(By.text(text(id))).click()
        if (rootTag != null) find(By.res(rootTag))
    }

    @Before fun prepare() {
        device = UiDevice.getInstance(instrumentation)
        originalCurrency = app.settings.ledgerCurrency
    }

    @After fun clean() = runBlocking<Unit> {
        scenario?.close()
        tasks.forEach { repo.deleteTask(it); repo.permanentlyDeleteTask(it) }
        expenses.forEach { repo.deleteExpense(it) }
        for (code in listOf("CNY", "HKD")) {
            repo.saveBudget(today, false, code, null)
            repo.saveBudget(today, true, code, null)
        }
        app.settings.ledgerCurrency = originalCurrency
    }

    @Test fun timelineCompletionDeleteRecoverAndPermanentRemoval() = runBlocking<Unit> {
        val id = repo.addTask(today, "UI recover task", "Keep task details", "sample", null)
        tasks += id
        tasks += repo.addTask(today.minusMonths(1), "UI earlier month", "", "", null)
        tasks += repo.addTask(today.plusMonths(2), "UI later month", "", "", null)
        repo.saveSubtasks(id, listOf("UI preserved child"))
        val child = repo.subtasksForTask(id).single()
        repo.setSubtaskCompleted(child, true)
        openFeature(R.string.full_schedule_title, "full-schedule-list")
        scrollTo(By.text("UI earlier month"), "full-schedule-list")
        scrollTo(By.text("UI later month"), "full-schedule-list")
        click(R.string.today)
        scrollTo(By.res("timeline-check-$id"), "full-schedule-list").click()
        await { runBlocking { db.taskDao().findById(id)?.isCompleted == true } }
        assertTrue(device.wait(Until.hasObject(By.res("timeline-check-$id").checked(true)), 5_000))
        capture("full-schedule")
        scrollTo(By.res("timeline-task-$id"), "full-schedule-list").click()
        find(By.text(text(R.string.edit_task)))
        scrollTo(By.text(text(R.string.delete_task))).click()
        find(By.text(text(R.string.delete_task_confirmation)))
        click(R.string.delete)
        await { runBlocking { db.taskDao().findDeletedById(id) != null } }
        assertTrue(repo.subtasksForTask(id).single().isCompleted)
        openFeature(R.string.recycle_bin_title)
        find(By.res("restore-task-$id"))
        capture("recycle-bin")
        find(By.res("restore-task-$id")).click()
        await { runBlocking { db.taskDao().findById(id) != null } }
        assertEquals("Keep task details", db.taskDao().findById(id)!!.details)
        assertTrue(db.taskDao().findById(id)!!.isCompleted)
        assertTrue(repo.subtasksForTask(id).single().isCompleted)
        openFeature(R.string.full_schedule_title, "full-schedule-list")
        scrollTo(By.res("timeline-check-$id"), "full-schedule-list")
        assertTrue(device.wait(Until.hasObject(By.res("timeline-check-$id").checked(true)), 5_000))
        repo.deleteTask(id)
        openFeature(R.string.recycle_bin_title)
        find(By.res("purge-task-$id")).click()
        find(By.text(text(R.string.permanent_delete_confirmation)))
        click(R.string.cancel)
        assertNotNull(db.taskDao().findDeletedById(id))
        find(By.res("purge-task-$id")).click()
        click(R.string.delete)
        await { runBlocking { db.taskDao().findDeletedById(id) == null } }
        assertTrue(repo.subtasksForTask(id).isEmpty())
        find(By.text(text(R.string.empty_bin_hint)))
    }

    @Test fun emptyBinRequiresConfirmationAndKeepsActiveTasks() = runBlocking<Unit> {
        val active = repo.addTask(today, "UI active stays", "", "", null)
        val discarded = repo.addTask(today, "UI empty bin", "", "", null)
        tasks += listOf(active, discarded)
        repo.deleteTask(discarded)
        openFeature(R.string.recycle_bin_title)
        click(R.string.empty_bin)
        click(R.string.cancel)
        assertNotNull(db.taskDao().findDeletedById(discarded))
        click(R.string.empty_bin)
        click(R.string.delete)
        await { runBlocking { repo.observeDeletedTasks().first().isEmpty() } }
        assertNotNull(db.taskDao().findById(active))
        find(By.text(text(R.string.empty_bin_hint)))
    }

    @Test fun expensesBudgetsOverrunsEditingAndCurrencySeparation() = runBlocking<Unit> {
        val anotherDay = today.withDayOfMonth(if (today.dayOfMonth > 1) today.dayOfMonth - 1 else 2)
        expenses += repo.saveExpense(null, anotherDay, "UI another date", 200, "CNY", "transport", "")
        openFeature(R.string.finance_title, "expense-list")
        currency("CNY")
        budget("edit-day-budget", "10.00")
        budget("edit-month-budget", "100.00")
        expense("UI lunch", "12.50", "CNY")
        val id = repo.observeExpenses(YearMonth.from(today), "CNY").first().single { it.title == "UI lunch" }.id
        expenses += id
        val overPrefix = context.getString(R.string.budget_over, "")
        assertTrue(find(By.textStartsWith(overPrefix)).text.contains("2.50"))
        assertNull(device.findObject(By.text("UI another date")))
        capture("daily-budget")
        click(R.string.finance_monthly)
        assertTrue(device.wait(Until.gone(By.res("daily-summary")), 5_000))
        scrollTo(By.text("UI another date"), "expense-list")
        capture("monthly-budget")
        scrollTo(By.text("UI lunch"), "expense-list").click()
        find(By.res("expense-amount")).text = "13.25"
        find(By.res("expense-note")).text = "Updated note"
        find(By.res("save-expense").enabled(true)).click()
        await { runBlocking { db.financeDao().findExpense(id)?.amountMinor == 1325L } }
        device.wait(Until.gone(By.res("save-expense")), 10_000)
        assertEquals("Updated note", db.financeDao().findExpense(id)!!.note)
        currency("HKD")
        find(By.text(text(R.string.no_expenses)))
        expense("UI HKD", "25.00", "HKD")
        expenses += repo.observeExpenses(YearMonth.from(today), "HKD").first().single { it.title == "UI HKD" }.id
        assertEquals(2500L, repo.observeExpenses(YearMonth.from(today), "HKD").first().sumOf { it.amountMinor })
        currency("CNY")
        assertEquals(1325L, repo.observeExpenses(YearMonth.from(today), "CNY").first().single { it.id == id }.amountMinor)
        scrollTo(By.text("UI lunch"), "expense-list").click()
        find(By.res("expense-title"))
        scrollTo(By.res("delete-expense")).click()
        click(R.string.cancel)
        assertNotNull(db.financeDao().findExpense(id))
        scrollTo(By.res("delete-expense")).click()
        click(R.string.delete)
        await { runBlocking { db.financeDao().findExpense(id) == null } }
    }

    @Test fun zeroBudgetsAndInvalidExpenseAmountsAreHandled() = runBlocking<Unit> {
        openFeature(R.string.finance_title, "expense-list")
        currency("CNY")
        budget("edit-day-budget", "0")
        find(By.res("add-expense")).click()
        find(By.res("expense-title")).text = "UI invalid expense"
        val field = find(By.res("expense-amount"))
        for (invalid in listOf("0", "-1", "12.345")) {
            field.text = invalid
            assertTrue(device.wait(Until.hasObject(By.res("save-expense").enabled(false)), 5_000))
        }
        field.text = "0.01"
        find(By.res("save-expense").enabled(true)).click()
        await { runBlocking { repo.observeExpenses(YearMonth.from(today), "CNY").first().any { it.title == "UI invalid expense" } } }
        val id = repo.observeExpenses(YearMonth.from(today), "CNY").first().single { it.title == "UI invalid expense" }.id
        expenses += id
        val overPrefix = context.getString(R.string.budget_over, "")
        assertTrue(find(By.textStartsWith(overPrefix)).text.contains("0.01"))
        scrollTo(By.res("edit-day-budget"), "expense-list").click()
        click(R.string.remove_budget)
        await { runBlocking { repo.observeBudgets(YearMonth.from(today), "CNY").first().none { it.periodType == "DAY" } } }
    }

    private fun currency(code: String) {
        find(By.res("ledger-currency")).click()
        // The menu item and the anchor can share text; prefer the last visible match.
        device.wait(Until.hasObject(By.text(code)), 5_000)
        device.findObjects(By.text(code)).last().click()
        await { app.settings.ledgerCurrency == code }
    }

    private fun budget(tag: String, value: String) {
        scrollTo(By.res(tag), "expense-list").click()
        find(By.res("budget-amount")).text = value
        find(By.res("save-budget").enabled(true)).click()
        assertTrue(device.wait(Until.gone(By.res("save-budget")), 10_000))
    }

    private fun expense(title: String, amount: String, currency: String) {
        find(By.res("add-expense")).click()
        find(By.res("expense-title")).text = title
        find(By.res("expense-amount")).text = amount
        find(By.res("save-expense").enabled(true)).click()
        await { runBlocking { repo.observeExpenses(YearMonth.from(today), currency).first().any { it.title == title } } }
        expenses += runBlocking { repo.observeExpenses(YearMonth.from(today), currency).first().first { it.title == title }.id }
        assertTrue(device.wait(Until.gone(By.res("save-expense")), 10_000))
    }

    /** Crop on Android before exporting, so no personal/system chrome is included. */
    private fun capture(name: String) {
        instrumentation.waitForIdleSync()
        val folder = requireNotNull(context.getExternalFilesDir("feature-screenshots"))
        folder.mkdirs()
        val original = File(folder, "$name-raw.png")
        assertTrue(device.takeScreenshot(original))
        val bitmap = requireNotNull(BitmapFactory.decodeFile(original.path))
        val windows = device.executeShellCommand("dumpsys window displays")
        val top = Regex("type=statusBars frame=\\[0,0]\\[\\d+,(\\d+)]").find(windows)!!.groupValues[1].toInt()
        val bottom = Regex("type=navigationBars frame=\\[0,(\\d+)]").find(windows)!!.groupValues[1].toInt()
        val cropped = Bitmap.createBitmap(bitmap, 0, top, bitmap.width, bottom - top)
        File(folder, "$name.png").outputStream().use { assertTrue(cropped.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        cropped.recycle(); bitmap.recycle()
    }
}
