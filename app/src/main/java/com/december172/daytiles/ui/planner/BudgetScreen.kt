package com.december172.daytiles.ui.planner

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.december172.daytiles.DayTilesApplication
import com.december172.daytiles.R
import com.december172.daytiles.data.bestQuote
import com.december172.daytiles.data.local.BudgetEntity
import com.december172.daytiles.data.local.ExpenseEntity
import com.december172.daytiles.domain.model.Money
import com.december172.daytiles.ui.calendar.MonthPicker
import com.december172.daytiles.ui.components.PlannerTopBar
import java.math.BigDecimal
import java.math.RoundingMode
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

private val expenseCategoryColors = mapOf(
    "food" to Color(0xFFD55E00), "transport" to Color(0xFF0072B2),
    "shopping" to Color(0xFF7E57C2), "housing" to Color(0xFF009E73),
    "health" to Color(0xFFC75B9B), "learning" to Color(0xFF008C9E),
    "leisure" to Color(0xFFC28B00), "other" to Color(0xFF7A7A7A),
)

private fun expenseCategoryColor(category: String): Color =
    expenseCategoryColors.getValue(category.takeIf { it in expenseCategoryColors } ?: "other")

@Composable
private fun ExpenseCategoryDot(category: String) {
    Box(Modifier.size(10.dp).background(expenseCategoryColor(category), CircleShape))
}

@Composable
fun BudgetScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as DayTilesApplication
    val repo = app.repository
    val currency by app.settings.budgetDisplayCurrency.collectAsStateWithLifecycle()
    var dateEpoch by rememberSaveable { mutableLongStateOf(LocalDate.now().toEpochDay()) }
    var monthly by rememberSaveable { mutableStateOf(false) }
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
    val rates = app.exchangeRates
    val tables by remember(rates) { rates.observeTables() }.collectAsStateWithLifecycle(remember(rates) { rates.cachedTables() })
    val conversions = remember(currency, tables) {
        Money.currencies.associateWith { code ->
            if (code == currency) BigDecimal.ONE else tables.bestQuote(code, currency)?.rate
        }
    }
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
                Text(currency, style = MaterialTheme.typography.labelLarge)
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
            key(month) {
                val expenseRows by remember { repo.observeExpenses(month) }.collectAsStateWithLifecycle(null)
                val budgets by remember { repo.observeBudgets(month) }.collectAsStateWithLifecycle(null)
                val expenses = expenseRows.orEmpty()
                val dayExpenses = expenses.filter { it.date == date }
                val monthBudget = budgets?.monthly.orEmpty()
                val dayBudget = budgets?.daily.orEmpty()
                val currencies = remember(expenses, monthBudget, dayBudget) {
                    buildSet {
                        expenses.forEach { add(it.currency) }
                        (monthBudget + dayBudget).filter { it.amountMinor != 0L }.forEach { add(it.currency) }
                    }
                }
                if (expenseRows != null && budgets != null) {
                    LaunchedEffect(rates, currencies, currency) { rates.refreshForBudget(currencies, currency) }
                }
                val shown = if (monthly) expenses else dayExpenses
                LazyColumn(Modifier.weight(1f).testTag("expense-list"), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!monthly) item {
                        BudgetSummary(false, dayExpenses, dayBudget, currency, conversions, locale, budgets != null) {
                            failed = false; budgetMonthly = false
                        }
                    }
                    item {
                        BudgetSummary(true, expenses, monthBudget, currency, conversions, locale, budgets != null) {
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
                                    leadingContent = { ExpenseCategoryDot(expense.category) },
                                    supportingContent = { Text(stringResource(expenseCategories[expense.category] ?: R.string.category_other)) },
                                    trailingContent = { Text(Money.format(expense.amountMinor, expense.currency, locale), style = MaterialTheme.typography.titleSmall) })
                            }
                        }
                    }
                }
                val editing = expenses.firstOrNull { it.id == editingId }
                if (showEditor && (editingId == null || editing != null)) {
                    ExpenseEditor(editing, date, currency, busy, failed,
                        onClose = { if (!busy) showEditor = false },
                        onSave = { entryDate, title, amount, entryCurrency, category, note -> save {
                            repo.saveExpense(editingId, entryDate, title, amount, entryCurrency, category, note)
                            dateEpoch = entryDate.toEpochDay()
                        } },
                        onDelete = { save { editingId?.let { repo.deleteExpense(it) } } })
                }
                budgetMonthly?.let { forMonth ->
                    val limits = if (forMonth) monthBudget else dayBudget
                    val editorCurrency = limits.singleOrNull()?.currency ?: currency
                    val original = limits.singleOrNull()?.amountMinor ?: budgetTotal(limits, conversions)?.let {
                        it.takeIf { total -> total <= BigDecimal.valueOf(Money.MAX_AMOUNT_MINOR) }?.toLong()
                    }
                    if (budgets != null) BudgetEditor(forMonth, limits, original, editorCurrency, busy, failed,
                        onClose = { if (!busy) budgetMonthly = null },
                        onSave = { amount -> save { repo.saveBudget(date, forMonth, editorCurrency, amount) } })
                }
            }
        }
    }
    if (monthPicker) MonthPicker(month, onDismiss = { monthPicker = false }, onSelect = { chosen ->
        dateEpoch = chosen.atDay(minOf(date.dayOfMonth, chosen.lengthOfMonth())).toEpochDay(); monthPicker = false
    })
}

private fun budgetTotal(limits: List<BudgetEntity>, conversions: Map<String, BigDecimal?>): BigDecimal? {
    if (limits.isEmpty()) return null
    return limits.sumOf { convert(it.amountMinor, it.currency, conversions) ?: return null }
        .setScale(0, RoundingMode.HALF_UP)
}

private fun convert(amount: Long, currency: String, conversions: Map<String, BigDecimal?>): BigDecimal? =
    if (amount == 0L) BigDecimal.ZERO else conversions[currency]?.multiply(BigDecimal.valueOf(amount))

@Composable
private fun BudgetSummary(
    monthly: Boolean, expenses: List<ExpenseEntity>, limits: List<BudgetEntity>, currency: String,
    conversions: Map<String, BigDecimal?>, locale: Locale, limitsLoaded: Boolean, onEdit: () -> Unit,
) {
    val missingSpending = expenses.map { it.currency }.filter { conversions[it] == null }.toSet()
    val missingLimits = limits.filter { it.amountMinor != 0L && conversions[it.currency] == null }.map { it.currency }.toSet()
    val categoryTotals = if (missingSpending.isNotEmpty()) emptyMap() else expenses
        .groupBy { it.category.takeIf(expenseCategories::containsKey) ?: "other" }
        .mapValues { (_, records) ->
            records.sumOf { requireNotNull(convert(it.amountMinor, it.currency, conversions)) }
        }
    val exactSpent = categoryTotals.values.sumOf { it }
    val spent = exactSpent.setScale(0, RoundingMode.HALF_UP)
    val budget = budgetTotal(limits, conversions)
    Card(Modifier.fillMaxWidth().testTag(if (monthly) "monthly-summary" else "daily-summary"),
        colors = CardDefaults.cardColors(containerColor = if (monthly) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(if (monthly) R.string.month_spending else R.string.day_spending), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
                TextButton(onClick = onEdit, enabled = limitsLoaded,
                    modifier = Modifier.testTag(if (monthly) "edit-month-budget" else "edit-day-budget")) {
                    Text(stringResource(if (limits.isEmpty()) R.string.set_budget else R.string.edit_budget))
                }
            }
            if (!monthly) Text(stringResource(R.string.daily_budget_hint), style = MaterialTheme.typography.bodySmall)
            if (missingSpending.isEmpty()) Text(Money.format(spent, currency, locale), Modifier.testTag("budget-spent"),
                style = MaterialTheme.typography.headlineMedium)
            val missing = missingSpending + missingLimits
            if (missing.isNotEmpty()) Text(stringResource(R.string.budget_missing_rates, missing.sorted().joinToString(", ")),
                modifier = Modifier.testTag("budget-missing-rates"), style = MaterialTheme.typography.bodySmall)
            if (limitsLoaded && limits.isEmpty()) Text(stringResource(R.string.no_budget), style = MaterialTheme.typography.bodySmall)
            if (budget != null) Text(stringResource(R.string.budget_amount_value, Money.format(budget, currency, locale)),
                style = MaterialTheme.typography.bodySmall)
            if (budget != null && missingSpending.isEmpty()) {
                val remaining = budget - spent
                val unused = (budget - exactSpent).max(BigDecimal.ZERO)
                val barTotal = maxOf(budget, exactSpent)
                val progress = if (barTotal.signum() == 0) 0f else (exactSpent.toDouble() / barTotal.toDouble()).toFloat()
                Row(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag(if (monthly) "monthly-budget-bar" else "daily-budget-bar")
                    .semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f) }) {
                    // Scale an overspent bar to total spending so every category remains visible.
                    if (barTotal.signum() > 0) {
                        expenseCategories.keys.forEach { category ->
                            val amount = categoryTotals[category] ?: BigDecimal.ZERO
                            if (amount.signum() > 0) Box(Modifier.weight((amount.toDouble() / barTotal.toDouble()).toFloat())
                                .fillMaxHeight().background(expenseCategoryColor(category)))
                        }
                        if (unused.signum() > 0) Spacer(Modifier.weight((unused.toDouble() / barTotal.toDouble()).toFloat()))
                    }
                }
                expenseCategories.forEach { (category, label) ->
                    categoryTotals[category]?.let { amount ->
                        Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ExpenseCategoryDot(category)
                            Text(stringResource(label), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                            Text(Money.format(amount.setScale(0, RoundingMode.HALF_UP), currency, locale), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Text(stringResource(if (remaining.signum() < 0) R.string.budget_over else R.string.budget_left,
                    Money.format(remaining.abs(), currency, locale)),
                    color = if (remaining.signum() < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun BudgetEditor(monthly: Boolean, limits: List<BudgetEntity>, original: Long?, currency: String, busy: Boolean, failed: Boolean,
    onClose: () -> Unit, onSave: (Long?) -> Unit) {
    var changedAmount by rememberSaveable { mutableStateOf<String?>(null) }
    val amount = changedAmount ?: original?.let(Money::input).orEmpty()
    val parsed = Money.parseMinor(amount, allowZero = true)
    AlertDialog(onDismissRequest = { if (!busy) onClose() }, modifier = Modifier.semantics { testTagsAsResourceId = true },
        title = { Text(stringResource(if (monthly) R.string.month_budget else R.string.day_budget)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!monthly) Text(stringResource(R.string.daily_budget_hint))
                if (limits.size > 1) Text(stringResource(R.string.budget_legacy_combined))
                OutlinedTextField(amount, { if (it.length <= 16) changedAmount = it }, label = { Text(stringResource(R.string.amount_currency, currency)) },
                    modifier = Modifier.testTag("budget-amount"), singleLine = true, enabled = !busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amount.isNotBlank() && parsed == null,
                    supportingText = { Text(stringResource(R.string.budget_amount_hint)) })
                if (failed) Text(stringResource(R.string.save_failed), color = MaterialTheme.colorScheme.error)
                if (limits.isNotEmpty()) TextButton(enabled = !busy, onClick = { onSave(null) }) { Text(stringResource(R.string.remove_budget)) }
            }
        },
        confirmButton = { TextButton(enabled = parsed != null && !busy, onClick = { parsed?.let(onSave) },
            modifier = Modifier.testTag("save-budget")) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(enabled = !busy, onClick = onClose) { Text(stringResource(R.string.cancel)) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseEditor(expense: ExpenseEntity?, selectedDate: LocalDate, currency: String, busy: Boolean, failed: Boolean,
    onClose: () -> Unit, onSave: (LocalDate, String, Long, String, String, String) -> Unit, onDelete: () -> Unit) {
    var title by rememberSaveable(expense?.id) { mutableStateOf(expense?.title.orEmpty()) }
    var amount by rememberSaveable(expense?.id) { mutableStateOf(expense?.let { Money.input(it.amountMinor) }.orEmpty()) }
    var note by rememberSaveable(expense?.id) { mutableStateOf(expense?.note.orEmpty()) }
    var category by rememberSaveable(expense?.id) { mutableStateOf(expense?.category ?: "other") }
    var dateEpoch by rememberSaveable(expense?.id) { mutableLongStateOf((expense?.date ?: selectedDate).toEpochDay()) }
    var categories by remember { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }
    var entryCurrency by rememberSaveable(expense?.id) { mutableStateOf(expense?.currency ?: currency) }
    var currenciesExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val date = LocalDate.ofEpochDay(dateEpoch)
    val parsed = Money.parseMinor(amount)
    Dialog(onDismissRequest = { if (!busy) onClose() }, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Scaffold(modifier = Modifier.fillMaxSize().imePadding().semantics { testTagsAsResourceId = true },
            topBar = { PlannerTopBar(stringResource(if (expense == null) R.string.add_expense else R.string.edit_expense), onClose) {
                TextButton(enabled = !busy && title.isNotBlank() && parsed != null,
                    onClick = { parsed?.let { onSave(date, title, it, entryCurrency, category, note) } }, modifier = Modifier.testTag("save-expense")) {
                    Text(stringResource(R.string.save))
                }
            } }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth().testTag("expense-title"), label = { Text(stringResource(R.string.expense_title)) }, singleLine = true, enabled = !busy)
                OutlinedTextField(amount, { if (it.length <= 16) amount = it }, Modifier.fillMaxWidth().testTag("expense-amount"),
                    label = { Text(stringResource(R.string.amount_currency, entryCurrency)) }, singleLine = true, enabled = !busy,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), isError = amount.isNotBlank() && parsed == null,
                    supportingText = { Text(stringResource(R.string.expense_amount_hint)) })
                if (expense == null) Box {
                    OutlinedButton(enabled = !busy, onClick = { currenciesExpanded = true }, modifier = Modifier.testTag("expense-currency")) {
                        Text(stringResource(R.string.expense_currency, entryCurrency))
                    }
                    DropdownMenu(currenciesExpanded, { currenciesExpanded = false }) {
                        Money.currencies.forEach { code ->
                            DropdownMenuItem(text = { Text(code) }, onClick = {
                                entryCurrency = code
                                currenciesExpanded = false
                            })
                        }
                    }
                }
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
