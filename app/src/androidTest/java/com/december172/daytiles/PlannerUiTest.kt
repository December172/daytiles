package com.december172.daytiles

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.december172.daytiles.data.ExchangeRateClient
import com.december172.daytiles.data.ExchangeRateRepository
import com.december172.daytiles.data.ExchangeRateTable
import com.december172.daytiles.data.local.BudgetEntity
import com.december172.daytiles.data.local.DayTilesDatabase
import com.december172.daytiles.domain.model.Money
import java.io.File
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
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
    private fun setText(tag: String, value: String) {
        // Dialog and keyboard transitions can replace an accessibility node after lookup.
        await {
            try {
                find(By.res(tag)).text = value
                true
            } catch (_: StaleObjectException) { false }
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
    private fun openHome() {
        // Launch through the shell so repeated task resets also work on physical devices.
        val launch = device.executeShellCommand("am start -W -f 0x10008000 -n ${context.packageName}/${MainActivity::class.java.name}")
        assertTrue(launch, launch.contains("Status: ok"))
        find(By.desc(text(R.string.open_navigation_menu)))
    }
    private fun openFeature(id: Int, rootTag: String? = null) {
        openHome()
        click(R.string.me_tab)
        scrollTo(By.text(text(id))).click()
        if (rootTag != null) find(By.res(rootTag))
    }

    @Before fun prepare() {
        device = UiDevice.getInstance(instrumentation)
        originalCurrency = app.settings.budgetCurrency
        app.settings.budgetCurrency = "CNY"
    }

    @After fun clean() = runBlocking<Unit> {
        device.pressHome()
        tasks.forEach { repo.deleteTask(it); repo.permanentlyDeleteTask(it) }
        expenses.forEach { repo.deleteExpense(it) }
        repo.saveBudget(today, false, "CNY", null)
        repo.saveBudget(today, true, "CNY", null)
        app.settings.budgetCurrency = originalCurrency
    }

    @Test fun primaryNavigationDrawerAndLunarSettingRemainUsable() {
        openHome()
        capture("material-calendar")
        for ((label, name) in listOf(
            R.string.schedule_tab to "tasks", R.string.target_tab to "goals",
            R.string.widgets_tab to "widgets", R.string.me_tab to "more", R.string.view_tab to "calendar",
        )) {
            click(label)
            find(By.selected(true).hasDescendant(By.text(text(label))))
            capture("material-$name")
        }
        find(By.desc(text(R.string.open_navigation_menu))).click()
        find(By.selected(true).hasDescendant(By.text(text(R.string.calendar_tab))))
        capture("material-drawer")
        click(R.string.week_title)
        find(By.desc(text(R.string.back)))
        device.pressBack()

        val original = app.settings.showLunarDates.value
        try {
            openFeature(R.string.settings_title)
            find(By.res("lunar-date-setting")).click()
            await { app.settings.showLunarDates.value == !original }
            find(By.res("lunar-date-setting").checked(!original))
            capture("material-settings")
            // Re-enter the screen to verify the toggle's persisted state.
            openFeature(R.string.settings_title)
            find(By.res("lunar-date-setting").checked(!original))
        } finally {
            app.settings.setShowLunarDates(original)
        }
    }

    @Test fun quickEntryWithKeyboardAndTaskCheckboxSaveChanges() = runBlocking<Unit> {
        openHome()
        find(By.desc(text(R.string.new_task))).click()
        find(By.res("quick-task-save").enabled(false))
        setText("quick-task-title", "Material quick entry")
        setText("quick-task-details", "Details entered with the keyboard visible")
        scrollTo(By.res("quick-task-save").enabled(true))
        capture("material-quick-entry")
        find(By.res("quick-task-save").enabled(true)).click()
        await { runBlocking { repo.observeAllTasks().first().any { it.title == "Material quick entry" } } }
        val task = repo.observeAllTasks().first().single { it.title == "Material quick entry" }
        tasks += task.id
        assertEquals("Details entered with the keyboard visible", task.details)
        assertTrue(device.wait(Until.gone(By.res("quick-task-save")), 5_000))
        click(R.string.schedule_tab)
        find(By.text(task.title))
        find(By.desc(context.getString(R.string.todo_checkbox_accessibility, task.title))).click()
        await { runBlocking { db.taskDao().findById(task.id)?.isCompleted == true } }
        capture("material-completed-task")
        find(By.desc(context.getString(R.string.todo_uncheck_accessibility, task.title)))
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

    @Test fun expensesBudgetsOverrunsAndEditing() = runBlocking<Unit> {
        val anotherDay = today.withDayOfMonth(if (today.dayOfMonth > 1) today.dayOfMonth - 1 else 2)
        expenses += repo.saveExpense(null, anotherDay, "UI another date", 200, "CNY", "transport", "")
        openFeature(R.string.finance_title, "expense-list")
        budget("edit-day-budget", "10.00")
        budget("edit-month-budget", "100.00")
        expense("UI lunch", "12.50", "CNY")
        val id = repo.observeExpenses(YearMonth.from(today)).first().single { it.title == "UI lunch" }.id
        val overPrefix = context.getString(R.string.budget_over, "")
        assertTrue(find(By.textStartsWith(overPrefix)).text.contains("2.50"))
        assertNull(device.findObject(By.text("UI another date")))
        capture("daily-budget")
        click(R.string.finance_monthly)
        assertTrue(device.wait(Until.gone(By.res("daily-summary")), 5_000))
        scrollTo(By.text("UI another date"), "expense-list")
        capture("monthly-budget")
        scrollTo(By.text("UI lunch"), "expense-list").click()
        setText("expense-amount", "13.25")
        setText("expense-note", "Updated note")
        find(By.res("save-expense").enabled(true)).click()
        await { runBlocking { db.financeDao().findExpense(id)?.amountMinor == 1325L } }
        device.wait(Until.gone(By.res("save-expense")), 10_000)
        assertEquals("Updated note", db.financeDao().findExpense(id)!!.note)
        assertEquals(1325L, repo.observeExpenses(YearMonth.from(today)).first().single { it.id == id }.amountMinor)
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
        budget("edit-day-budget", "0")
        find(By.res("add-expense")).click()
        setText("expense-title", "UI invalid expense")
        for (invalid in listOf("0", "-1", "12.345")) {
            setText("expense-amount", invalid)
            assertTrue(device.wait(Until.hasObject(By.res("save-expense").enabled(false)), 5_000))
        }
        setText("expense-amount", "0.01")
        find(By.res("save-expense").enabled(true)).click()
        await { runBlocking { repo.observeExpenses(YearMonth.from(today)).first().any { it.title == "UI invalid expense" } } }
        val id = repo.observeExpenses(YearMonth.from(today)).first().single { it.title == "UI invalid expense" }.id
        expenses += id
        val overPrefix = context.getString(R.string.budget_over, "")
        assertTrue(find(By.textStartsWith(overPrefix)).text.contains("0.01"))
        scrollTo(By.res("edit-day-budget"), "expense-list").click()
        click(R.string.remove_budget)
        await { runBlocking { repo.observeBudgets(YearMonth.from(today)).first().daily.isEmpty() } }
    }

    @Test fun dailyBudgetAppliesToEveryDateAndEditsAndRemovalAreGlobal() = runBlocking<Unit> {
        val previousDay = today.minusDays(1)
        val previousMonth = today.minusMonths(1)
        expenses += repo.saveExpense(null, today, "UI daily lunch", 1250, "CNY", "food", "")
        expenses += repo.saveExpense(null, previousDay, "UI yesterday bus", 250, "CNY", "transport", "")
        expenses += repo.saveExpense(null, previousMonth, "UI previous month", 500, "CNY", "shopping", "")
        repo.saveBudget(today, false, "CNY", 1000)
        repo.saveBudget(today, true, "CNY", 100000)
        fun money(amount: Long) = Money.format(amount, "CNY", context.resources.configuration.locales[0])
        fun assertDaily(spent: Long, limit: Long) {
            find(By.res("daily-summary").hasDescendant(By.text(money(spent))))
            find(By.res("daily-summary").hasDescendant(By.text(context.getString(R.string.budget_amount_value, money(limit)))))
            find(By.res("daily-summary").hasDescendant(By.text(context.getString(
                if (spent > limit) R.string.budget_over else R.string.budget_left, money(kotlin.math.abs(limit - spent))))))
        }
        openFeature(R.string.finance_title, "expense-list")
        assertDaily(1250, 1000)
        find(By.res("finance-previous")).click()
        assertDaily(250, 1000)
        budget("edit-day-budget", "20.00")
        assertDaily(250, 2000)
        find(By.res("finance-next")).click()
        assertDaily(1250, 2000)
        capture("shared-daily-budget-current")
        click(R.string.finance_monthly)
        find(By.res("finance-previous")).click()
        click(R.string.daily_goal)
        assertDaily(500, 2000)
        capture("shared-daily-budget-previous-month")
        find(By.res("edit-day-budget")).click()
        find(By.text(text(R.string.daily_budget_hint)))
        click(R.string.remove_budget)
        find(By.res("daily-summary").hasDescendant(By.text(text(R.string.no_budget))))
        click(R.string.today)
        find(By.res("daily-summary").hasDescendant(By.text(text(R.string.no_budget))))
        assertEquals(100000L, repo.observeBudgets(YearMonth.from(today)).first().monthly.single().amountMinor)
    }

    @Test fun categoryViewShowsTheWholeMonthAndCanFilterAndEditExpenses() = runBlocking<Unit> {
        val month = YearMonth.from(today)
        val otherDay = month.atDay(if (today.dayOfMonth == 1) 2 else 1)
        val previousMonth = today.minusMonths(1)
        val rent = repo.saveExpense(null, today, "Fixture rent", 126000, "CNY", "housing", "")
        val repair = repo.saveExpense(null, otherDay, "Fixture repair", 28000, "HKD", "housing", "")
        val checkup = repo.saveExpense(null, today, "Fixture checkup", 40000, "CNY", "health", "")
        val oldRent = repo.saveExpense(null, previousMonth, "Fixture previous rent", 124000, "CNY", "housing", "")
        expenses += listOf(rent, repair, checkup, oldRent)

        openFeature(R.string.finance_title, "expense-list")
        assertNull(device.findObject(By.res("expense-$repair")))
        find(By.res("expenses-by-category")).click()
        find(By.res("category-expense-list"))
        find(By.text(text(R.string.category_housing)))
        find(By.res("expense-$rent"))
        val locale = context.resources.configuration.locales[0]
        val expectedDate = otherDay.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale))
        find(By.res("expense-$repair"))
        find(By.text(expectedDate))
        find(By.text(Money.format(28000, "HKD", locale)))
        scrollTo(By.text(text(R.string.category_health)), "category-expense-list")
        scrollTo(By.res("expense-$checkup"), "category-expense-list")
        assertNull(device.findObject(By.res("expense-$oldRent")))
        capture("monthly-expenses-by-category")

        fun filter(label: Int) {
            find(By.res("expense-category-filter")).click()
            // The open menu is a popup. The category heading can also have this label.
            device.wait(Until.hasObject(By.text(text(label))), 5_000)
            device.findObjects(By.text(text(label))).last().click()
        }
        filter(R.string.category_housing)
        find(By.res("expense-$rent"))
        find(By.res("expense-$repair"))
        assertTrue(device.wait(Until.gone(By.res("expense-$checkup")), 5_000))
        find(By.res("expense-$repair")).click()
        find(By.res("expense-amount").text("280.00"))
        setText("expense-title", "Fixture repaired housing")
        find(By.res("save-expense").enabled(true)).click()
        find(By.text("Fixture repaired housing"))
        assertEquals("HKD", db.financeDao().findExpense(repair)?.currency)
        assertEquals(28000L, db.financeDao().findExpense(repair)?.amountMinor)
        find(By.res("finance-previous")).click()
        find(By.res("expense-$oldRent"))
        assertTrue(device.wait(Until.gone(By.res("expense-$rent")), 5_000))
        filter(R.string.category_health)
        find(By.text(text(R.string.no_expenses)))
        find(By.res("finance-next")).click()
        find(By.res("expense-$checkup"))
        assertTrue(device.wait(Until.gone(By.res("expense-$rent")), 5_000))
    }

    @Test fun budgetBarsShowCategoryColorsAndProportions() = runBlocking<Unit> {
        val categories = listOf("food", "transport", "shopping", "housing", "health", "learning", "leisure", "other")
        val amounts = categories.indices.map { (it + 1) * 1000L }
        val total = amounts.sum()
        categories.zip(amounts).forEach { (category, amount) ->
            expenses += repo.saveExpense(null, today, "UI $category", amount, "CNY", category, "")
        }
        val anotherDay = today.withDayOfMonth(if (today.dayOfMonth > 1) today.dayOfMonth - 1 else 2)
        expenses += repo.saveExpense(null, anotherDay, "UI another day", total, "CNY", "food", "")
        repo.saveBudget(today, false, "CNY", total * 2)
        repo.saveBudget(today, true, "CNY", total * 4)
        openFeature(R.string.finance_title, "expense-list")
        assertBudgetBar("daily-budget-bar", amounts, total * 2, "category-budget-daily")

        click(R.string.finance_monthly)
        assertTrue(device.wait(Until.gone(By.res("daily-summary")), 5_000))
        val monthAmounts = amounts.toMutableList().apply { this[0] += total }
        assertBudgetBar("monthly-budget-bar", monthAmounts, total * 4, "category-budget-monthly")

        // Overspending and a zero budget must retain every category's share.
        repo.saveBudget(today, true, "CNY", total)
        find(By.textStartsWith(context.getString(R.string.budget_over, "")))
        assertBudgetBar("monthly-budget-bar", monthAmounts, total * 2, "category-budget-over")
        repo.saveBudget(today, true, "CNY", 0)
        assertTrue(device.wait(Until.hasObject(By.text(context.getString(R.string.budget_amount_value,
            com.december172.daytiles.domain.model.Money.format(0, "CNY", context.resources.configuration.locales[0])))), 5_000))
        assertBudgetBar("monthly-budget-bar", monthAmounts, total * 2, "category-budget-zero")

        expenses.forEach { repo.deleteExpense(it) }
        find(By.text(text(R.string.no_expenses)))
        assertBudgetBar("monthly-budget-bar", List(categories.size) { 0L }, 1, "category-budget-empty")
        repo.saveBudget(today, true, "CNY", null)
        find(By.text(text(R.string.no_budget)))
        assertTrue(device.wait(Until.gone(By.res("monthly-budget-bar")), 5_000))
    }

    private fun assertBudgetBar(tag: String, amounts: List<Long>, scale: Long, screenshot: String) {
        val bar = find(By.res(tag)).visibleBounds
        capture(screenshot)
        val frame = requireNotNull(device.takeScreenshot())
        try {
            val expectedColors = listOf(0xFFD55E00, 0xFF0072B2, 0xFF7E57C2, 0xFF009E73,
                0xFFC75B9B, 0xFF008C9E, 0xFFC28B00, 0xFF7A7A7A).map { it.toInt() }
            var start = 0L
            amounts.forEachIndexed { index, amount ->
                if (amount == 0L) return@forEachIndexed
                // Sample inside both edges of each segment to check its width as well as its color.
                for (fraction in listOf(0.2, 0.8)) {
                    val x = bar.left + (bar.width() * (start + amount * fraction) / scale).toInt()
                    val actual = frame.getPixel(x, bar.centerY())
                    val expected = expectedColors[index]
                    for (shift in listOf(0, 8, 16)) {
                        assertTrue("$tag category $index at $fraction: expected ${expected.toUInt().toString(16)}, got ${actual.toUInt().toString(16)}",
                            kotlin.math.abs(((actual shr shift) and 255) - ((expected shr shift) and 255)) <= 3)
                    }
                }
                start += amount
            }
            if (start < scale) {
                val empty = frame.getPixel(bar.left + (bar.width() * (start + (scale - start) / 2.0) / scale).toInt(), bar.centerY())
                assertFalse("Unused budget should retain the track color", empty in expectedColors)
            }
        } finally { frame.recycle() }
    }

    @Test fun budgetsCombineCurrenciesAndSettingsOwnTheRateControls() = runBlocking<Unit> {
        val rates = app.exchangeRates
        val preferences = context.getSharedPreferences("official_exchange_rates", android.content.Context.MODE_PRIVATE)
        val originalRates = preferences.all.mapValues { it.value as String }
        val monthStart = today.withDayOfMonth(1)
        suspend fun seedRate(usdRate: String) {
            val fixture = ExchangeRateRepository(context, ExchangeRateClient { source ->
                ExchangeRateTable(source, "2026-11-20 09:15", System.currentTimeMillis(),
                    mapOf("CNY" to BigDecimal.ONE, "USD" to BigDecimal(usdRate), "HKD" to BigDecimal("0.9")))
            })
            fixture.refresh("USD", "CNY")
        }
        fun money(amount: Long, code: String) = Money.format(amount, code, context.resources.configuration.locales[0])
        fun assertDaily(spent: Long, limit: Long, code: String) {
            find(By.res("daily-summary").hasDescendant(By.res("budget-spent").text(money(spent, code))))
            find(By.res("daily-summary").hasDescendant(By.text(context.getString(R.string.budget_amount_value, money(limit, code)))))
            find(By.res("daily-summary").hasDescendant(By.text(context.getString(R.string.budget_left, money(limit - spent, code)))))
        }
        try {
            rates.clearCachedRates()
            seedRate("7.25")
            val finance = db.financeDao()
            finance.deleteSharedBudget(monthStart, "SHARED_MONTH")
            finance.saveBudget(BudgetEntity(monthStart, "MONTH", "CNY", 20000, 1))
            finance.saveBudget(BudgetEntity(monthStart, "MONTH", "USD", 1000, 2))
            repo.saveBudget(today, false, "CNY", 15000)
            expenses += repo.saveExpense(null, today, "Fixture CNY expense", 1000, "CNY", "food", "")
            openFeature(R.string.finance_title, "expense-list")
            assertNull(device.findObject(By.res("open-exchange")))
            expense("Fixture USD expense", "12.34", "USD")
            val record = repo.observeExpenses(YearMonth.from(today)).first().single { it.title == "Fixture USD expense" }
            assertEquals("USD", record.currency)
            assertEquals(1234L, record.amountMinor)
            assertDaily(9947, 15000, "CNY")
            capture("combined-budget-cny")
            click(R.string.finance_monthly)
            find(By.res("monthly-summary").hasDescendant(By.text(context.getString(R.string.budget_amount_value, money(27250, "CNY")))))
            click(R.string.daily_goal)
            assertDaily(9947, 15000, "CNY")
            rates.clearCachedRates()
            find(By.res("budget-missing-rates"))
            assertNull(device.findObject(By.res("daily-budget-bar")))
            seedRate("8.00")
            assertDaily(10872, 15000, "CNY")

            openFeature(R.string.settings_title)
            find(By.res("open-exchange")).click()
            find(By.res("exchange-screen"))
            find(By.res("exchange-refresh").enabled(true))
            setText("exchange-amount", "10.00")
            find(By.res("exchange-result").text(money(125, "USD")))
            if (device.executeShellCommand("dumpsys input_method").contains("mInputShown=true")) device.pressBack()
            find(By.desc(text(R.string.back))).click()
            find(By.res("budget-display-currency")).click()
            clickCurrency("USD")
            await { app.settings.budgetCurrency == "USD" }
            openFeature(R.string.finance_title, "expense-list")
            assertDaily(1359, 1875, "USD")
            capture("combined-budget-usd")
            find(By.res("edit-day-budget")).click()
            find(By.res("budget-amount").text("150.00"))
            click(R.string.cancel)
            assertEquals(record, db.financeDao().findExpense(record.id))
        } finally {
            db.openHelper.writableDatabase.execSQL("DELETE FROM budgets WHERE periodType = 'MONTH' AND periodStart = ? AND currency IN ('CNY', 'USD')",
                arrayOf(monthStart.toString()))
            preferences.edit().clear().apply {
                originalRates.forEach { (key, value) -> putString(key, value) }
            }.commit()
        }
    }

    private fun clickCurrency(code: String) {
        device.wait(Until.hasObject(By.text(code)), 5_000)
        device.findObjects(By.text(code)).last().click()
    }

    private fun budget(tag: String, value: String) {
        scrollTo(By.res(tag), "expense-list").click()
        setText("budget-amount", value)
        find(By.res("save-budget").enabled(true)).click()
        assertTrue(device.wait(Until.gone(By.res("save-budget")), 10_000))
    }

    private fun expense(title: String, amount: String, currency: String) {
        find(By.res("add-expense")).click()
        if (currency != app.settings.budgetCurrency) {
            scrollTo(By.res("expense-currency")).click()
            clickCurrency(currency)
        }
        setText("expense-title", title)
        setText("expense-amount", amount)
        find(By.res("save-expense").enabled(true)).click()
        await { runBlocking { repo.observeExpenses(YearMonth.from(today)).first().any { it.title == title && it.currency == currency } } }
        expenses += runBlocking { repo.observeExpenses(YearMonth.from(today)).first().first { it.title == title }.id }
        assertTrue(device.wait(Until.gone(By.res("save-expense")), 10_000))
    }

    /** Crop on Android before exporting, so no personal/system chrome is included. */
    private fun capture(name: String) {
        instrumentation.waitForIdleSync()
        // Compose navigation can keep drawing after accessibility reports the next page.
        // Wait for consecutive matching frames before saving a visual reference.
        var frame = requireNotNull(device.takeScreenshot())
        var matchingFrames = 0
        val deadline = SystemClock.uptimeMillis() + 3_000
        while (matchingFrames < 2 && SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(120)
            val next = requireNotNull(device.takeScreenshot())
            matchingFrames = if (frame.sameAs(next)) matchingFrames + 1 else 0
            frame.recycle()
            frame = next
        }
        frame.recycle()
        val folder = requireNotNull(context.getExternalFilesDir("feature-screenshots"))
        folder.mkdirs()
        val original = File(folder, "$name-raw.png")
        assertTrue(device.takeScreenshot(original))
        val bitmap = requireNotNull(BitmapFactory.decodeFile(original.path))
        val windows = device.executeShellCommand("dumpsys window displays")
        val top = Regex("type=statusBars frame=\\[0,0]\\[\\d+,(\\d+)]").findAll(windows)
            .map { it.groupValues[1].toInt() }.maxOrNull()?.coerceIn(0, bitmap.height - 1) ?: 0
        // Some phones also report an empty navigation-bar source at [0,0][0,0].
        val bottom = Regex("type=navigationBars frame=\\[0,(\\d+)]\\[\\d+,(\\d+)]").findAll(windows)
            .map { it.groupValues[1].toInt() to it.groupValues[2].toInt() }
            .filter { (start, end) -> start > top && end > start }
            .minOfOrNull { it.first }?.coerceAtMost(bitmap.height) ?: bitmap.height
        val cropped = Bitmap.createBitmap(bitmap, 0, top, bitmap.width, bottom - top)
        File(folder, "$name.png").outputStream().use { assertTrue(cropped.compress(Bitmap.CompressFormat.PNG, 100, it)) }
        cropped.recycle(); bitmap.recycle()
    }
}
