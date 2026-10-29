package com.december172.daytiles.ui.planner

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.december172.daytiles.DayTilesApplication
import com.december172.daytiles.R
import com.december172.daytiles.data.local.BudgetEntity
import com.december172.daytiles.data.local.ExpenseEntity
import com.december172.daytiles.domain.model.Money
import com.december172.daytiles.ui.calendar.MonthPicker
import com.december172.daytiles.ui.components.PlannerTopBar
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private val expenseCategories = linkedMapOf(
    "food" to R.string.category_food, "transport" to R.string.category_transport,
    "shopping" to R.string.category_shopping, "housing" to R.string.category_housing,
    "health" to R.string.category_health, "learning" to R.string.category_learning,
    "leisure" to R.string.category_leisure, "other" to R.string.category_other,
)

@Composable
fun BudgetScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as DayTilesApplication
    val repo = app.repository
    var currency by rememberSaveable { mutableStateOf(app.settings.ledgerCurrency) }
    var dateEpoch by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var monthly by rememberSaveable { mutableStateOf(false) }
    var currencyMenu by remember { mutableStateOf(false) }
    var monthPicker by rememberSaveable { mutableStateOf(false) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var budgetMonthly by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var busy by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val date = LocalDate.ofEpochDay(dateEpoch)
    val month = YearMonth.from(date)
    val locale = LocalConfiguration.current.locales[0]
    val scope = rememberCoroutineScope()
    fun save(action: suspend () -> Unit) {
        if (busy) return
        busy = true; failed = false
        scope.launch {
            try { action(); showEditor = false; budgetMonthly = null }
            catch (error: Exception) {
                if (error is CancellationException) throw error
                failed = true
            } finally { busy = false }
        }
    }
    Scaffold(
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        topBar = { PlannerTopBar(stringResource(R.string.finance_title), onBack) },
        floatingActionButton = { FloatingActionButton(onClick = { editingId = null; failed = false; showEditor = true },
            modifier = Modifier.testTag("add-expense")) {
            Icon(painterResource(R.drawable.ic_planner_add), stringResource(R.string.add_expense))
        } },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(!monthly, { monthly = false }, label = { Text(stringResource(R.string.daily_goal)) })
                Spacer(Modifier.width(8.dp))
                FilterChip(monthly, { monthly = true }, label = { Text(stringResource(R.string.finance_monthly)) })
                Spacer(Modifier.weight(1f))
                Box {
                    TextButton(onClick = { currencyMenu = true }, modifier = Modifier.testTag("ledger-currency")) { Text(currency) }
                    DropdownMenu(currencyMenu, { currencyMenu = false }) {
                        Money.currencies.forEach { code ->
                            DropdownMenuItem(text = { Text(code) }, onClick = {
                                currency = code; app.settings.ledgerCurrency = code; currencyMenu = false
                            })
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { dateEpoch = (if (monthly) date.minusMonths(1) else date.minusDays(1)).toEpochDay() },
                    modifier = Modifier.testTag("finance-previous")) {
                    Icon(painterResource(R.drawable.ic_planner_chevron_left), stringResource(R.string.previous_period))
                }
                TextButton(onClick = {
                    if (monthly) monthPicker = true
                    else DatePickerDialog(context, { _, y, m, d -> dateEpoch = LocalDate.of(y, m + 1, d).toEpochDay() },
                        date.year, date.monthValue - 1, date.dayOfMonth).show()
                }, modifier = Modifier.weight(1f).testTag("finance-date")) {
                    Text(if (monthly) month.format(DateTimeFormatter.ofPattern(if (locale.language == "zh") "yyyy年M月" else "MMMM yyyy", locale))
                        else date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)))
                }
                TextButton(onClick = { dateEpoch = LocalDate.now().toEpochDay() }) { Text(stringResource(R.string.today)) }
                IconButton(onClick = { dateEpoch = (if (monthly) date.plusMonths(1) else date.plusDays(1)).toEpochDay() },
                    modifier = Modifier.testTag("finance-next")) {
                    Icon(painterResource(R.drawable.ic_planner_chevron_right), stringResource(R.string.next_period))
                }
            }
            Text(stringResource(R.string.finance_currency_hint), Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            // Give every currency/month its own collected state; never flash another ledger's totals.
            key(month, currency) {
                val expenses by remember { repo.observeExpenses(month, currency) }.collectAsStateWithLifecycle(emptyList())
                val budgets by remember { repo.observeBudgets(month, currency) }.collectAsStateWithLifecycle(emptyList())
                val dayExpenses = expenses.filter { it.date == date }
                val monthBudget = budgets.firstOrNull { it.periodType == "MONTH" }
                val dayBudget = budgets.firstOrNull { it.periodType == "DAY" && it.periodStart == date }
                val shown = if (monthly) expenses else dayExpenses
                LazyColumn(Modifier.weight(1f).testTag("expense-list"), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!monthly) item {
                        BudgetSummary(false, dayExpenses.sumOf { it.amountMinor }, dayBudget, currency, locale) {
                            failed = false; budgetMonthly = false
                        }
                    }
                    item {
                        BudgetSummary(true, expenses.sumOf { it.amountMinor }, monthBudget, currency, locale) {
                            failed = false; budgetMonthly = true
                        }
                    }
                    if (shown.isEmpty()) item { FeatureEmptyState(stringResource(R.string.no_expenses)) }
                    shown.groupBy { it.date }.forEach { (day, records) ->
                        if (monthly) item(key = "date-$day") {
                            Text(day.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)),
                                style = MaterialTheme.typography.titleSmall)
                        }
                        items(records, key = ExpenseEntity::id) { expense ->
                            OutlinedCard(Modifier.fillMaxWidth().testTag("expense-${expense.id}").clickable {
                                editingId = expense.id; failed = false; showEditor = true
                            }) {
                                ListItem(headlineContent = { Text(expense.title) },
                                    supportingContent = { Text(stringResource(expenseCategories[expense.category] ?: R.string.category_other)) },
                                    trailingContent = { Text(Money.format(expense.amountMinor, currency, locale), style = MaterialTheme.typography.titleSmall) })
                            }
                        }
                    }
                }
                val editing = expenses.firstOrNull { it.id == editingId }
                if (showEditor && (editingId == null || editing != null)) {
                    ExpenseEditor(editing, date, currency, busy, failed,
                        onClose = { if (!busy) showEditor = false },
                        onSave = { entryDate, title, amount, category, note -> save {
                            repo.saveExpense(editingId, entryDate, title, amount, currency, category, note)
                            dateEpoch = entryDate.toEpochDay()
                        } },
                        onDelete = { save { editingId?.let { repo.deleteExpense(it) } } })
                }
                budgetMonthly?.let { forMonth ->
                    BudgetEditor(forMonth, if (forMonth) monthBudget else dayBudget, currency, busy, failed,
                        onClose = { if (!busy) budgetMonthly = null },
                        onSave = { amount -> save { repo.saveBudget(date, forMonth, currency, amount) } })
                }
            }
        }
    }
    if (monthPicker) MonthPicker(month, onDismiss = { monthPicker = false }, onSelect = { chosen ->
        dateEpoch = chosen.atDay(minOf(date.dayOfMonth, chosen.lengthOfMonth())).toEpochDay(); monthPicker = false
    })
}

@Composable
private fun BudgetSummary(monthly: Boolean, spent: Long, budget: BudgetEntity?, currency: String, locale: Locale, onEdit: () -> Unit) {
    Card(Modifier.fillMaxWidth().testTag(if (monthly) "monthly-summary" else "daily-summary"),
        colors = CardDefaults.cardColors(containerColor = if (monthly) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(if (monthly) R.string.month_spending else R.string.day_spending), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = onEdit, modifier = Modifier.testTag(if (monthly) "edit-month-budget" else "edit-day-budget")) {
                    Text(stringResource(if (budget == null) R.string.set_budget else R.string.edit_budget))
                }
            }
            Text(Money.format(spent, currency, locale), style = MaterialTheme.typography.headlineMedium)
            if (budget == null) Text(stringResource(R.string.no_budget), style = MaterialTheme.typography.bodySmall)
            else {
                val remaining = budget.amountMinor - spent
                Text(stringResource(R.string.budget_amount_value, Money.format(budget.amountMinor, currency, locale)), style = MaterialTheme.typography.bodySmall)
                LinearProgressIndicator(progress = {
                    if (budget.amountMinor == 0L) { if (spent == 0L) 0f else 1f }
                    else (spent.toFloat() / budget.amountMinor).coerceIn(0f, 1f)
                }, modifier = Modifier.fillMaxWidth(), color = if (remaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                Text(stringResource(if (remaining < 0) R.string.budget_over else R.string.budget_left,
                    Money.format(kotlin.math.abs(remaining), currency, locale)),
                    color = if (remaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun BudgetEditor(monthly: Boolean, budget: BudgetEntity?, currency: String, busy: Boolean, failed: Boolean,
    onClose: () -> Unit, onSave: (Long?) -> Unit) {
    var amount by rememberSaveable(monthly, budget?.periodStart, currency) { mutableStateOf(budget?.let { Money.input(it.amountMinor) }.orEmpty()) }
    val parsed = Money.parseMinor(amount, allowZero = true)
    AlertDialog(onDismissRequest = { if (!busy) onClose() }, modifier = Modifier.semantics { testTagsAsResourceId = true },
        title = { Text(stringResource(if (monthly) R.string.month_budget else R.string.day_budget)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(amount, { if (it.length <= 16) amount = it }, label = { Text(stringResource(R.string.amount_currency, currency)) },
                    modifier = Modifier.testTag("budget-amount"), singleLine = true, enabled = !busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amount.isNotBlank() && parsed == null,
                    supportingText = { Text(stringResource(R.string.budget_amount_hint)) })
                if (failed) Text(stringResource(R.string.save_failed), color = MaterialTheme.colorScheme.error)
                if (budget != null) TextButton(enabled = !busy, onClick = { onSave(null) }) { Text(stringResource(R.string.remove_budget)) }
            }
        },
        confirmButton = { TextButton(enabled = parsed != null && !busy, onClick = { parsed?.let(onSave) },
            modifier = Modifier.testTag("save-budget")) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(enabled = !busy, onClick = onClose) { Text(stringResource(R.string.cancel)) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseEditor(expense: ExpenseEntity?, selectedDate: LocalDate, currency: String, busy: Boolean, failed: Boolean,
    onClose: () -> Unit, onSave: (LocalDate, String, Long, String, String) -> Unit, onDelete: () -> Unit) {
    var title by rememberSaveable(expense?.id) { mutableStateOf(expense?.title.orEmpty()) }
    var amount by rememberSaveable(expense?.id) { mutableStateOf(expense?.let { Money.input(it.amountMinor) }.orEmpty()) }
    var note by rememberSaveable(expense?.id) { mutableStateOf(expense?.note.orEmpty()) }
    var category by rememberSaveable(expense?.id) { mutableStateOf(expense?.category ?: "other") }
    var dateEpoch by rememberSaveable(expense?.id) { mutableLongStateOf((expense?.date ?: selectedDate).toEpochDay()) }
    var categories by remember { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val date = LocalDate.ofEpochDay(dateEpoch)
    val parsed = Money.parseMinor(amount)
    Dialog(onDismissRequest = { if (!busy) onClose() }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Scaffold(modifier = Modifier.fillMaxSize().imePadding().semantics { testTagsAsResourceId = true },
            topBar = { PlannerTopBar(stringResource(if (expense == null) R.string.add_expense else R.string.edit_expense), onClose) {
                TextButton(enabled = !busy && title.isNotBlank() && parsed != null,
                    onClick = { parsed?.let { onSave(date, title, it, category, note) } }, modifier = Modifier.testTag("save-expense")) {
                    Text(stringResource(R.string.save))
                }
            } }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth().testTag("expense-title"), label = { Text(stringResource(R.string.expense_title)) }, singleLine = true, enabled = !busy)
                OutlinedTextField(amount, { if (it.length <= 16) amount = it }, Modifier.fillMaxWidth().testTag("expense-amount"),
                    label = { Text(stringResource(R.string.amount_currency, currency)) }, singleLine = true, enabled = !busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), isError = amount.isNotBlank() && parsed == null,
                    supportingText = { Text(stringResource(R.string.expense_amount_hint)) })
                OutlinedButton(enabled = !busy, onClick = {
                    DatePickerDialog(context, { _, y, m, d -> dateEpoch = LocalDate.of(y, m + 1, d).toEpochDay() }, date.year, date.monthValue - 1, date.dayOfMonth).show()
                }, modifier = Modifier.fillMaxWidth().testTag("expense-date")) {
                    Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)))
                }
                ExposedDropdownMenuBox(categories, { if (!busy) categories = it }) {
                    OutlinedTextField(stringResource(expenseCategories[category] ?: R.string.category_other), {}, readOnly = true,
                        label = { Text(stringResource(R.string.expense_category)) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categories) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(), enabled = !busy)
                    ExposedDropdownMenu(categories, { categories = false }) {
                        expenseCategories.forEach { (key, label) ->
                            DropdownMenuItem(text = { Text(stringResource(label)) }, onClick = { category = key; categories = false })
                        }
                    }
                }
                OutlinedTextField(note, { note = it }, Modifier.fillMaxWidth().testTag("expense-note"), label = { Text(stringResource(R.string.expense_note)) }, minLines = 2, enabled = !busy)
                if (failed) Text(stringResource(R.string.save_failed), color = MaterialTheme.colorScheme.error)
                if (expense != null) TextButton(enabled = !busy, onClick = { deleting = true }, modifier = Modifier.testTag("delete-expense")) {
                    Text(stringResource(R.string.delete_expense), color = MaterialTheme.colorScheme.error)
                }
            }
        }
        if (deleting) AlertDialog(onDismissRequest = { if (!busy) deleting = false },
            title = { Text(stringResource(R.string.delete_expense)) }, text = { Text(stringResource(R.string.delete_expense_confirmation)) },
            confirmButton = { TextButton(enabled = !busy, onClick = onDelete) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(enabled = !busy, onClick = { deleting = false }) { Text(stringResource(R.string.cancel)) } })
    }
}
