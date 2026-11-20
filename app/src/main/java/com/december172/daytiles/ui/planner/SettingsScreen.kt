package com.december172.daytiles.ui.planner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.december172.daytiles.DayTilesApplication
import com.december172.daytiles.R
import com.december172.daytiles.domain.model.Money
import com.december172.daytiles.ui.components.PlannerTopBar

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as DayTilesApplication
    val settings = app.settings
    val showLunarDates by settings.showLunarDates.collectAsStateWithLifecycle()
    val currency by settings.budgetDisplayCurrency.collectAsStateWithLifecycle()
    var currencyMenu by remember { mutableStateOf(false) }
    var showRates by rememberSaveable { mutableStateOf(false) }
    Scaffold(
        modifier = Modifier.semantics { testTagsAsResourceId = true },
        topBar = { PlannerTopBar(stringResource(R.string.settings_title), onBack) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.calendar_display_settings), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Card {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.show_lunar_dates)) },
                    supportingContent = { Text(stringResource(R.string.show_lunar_dates_hint)) },
                    trailingContent = { Switch(checked = showLunarDates, onCheckedChange = null) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                    modifier = Modifier.testTag("lunar-date-setting")
                        .toggleable(value = showLunarDates, role = Role.Switch, onValueChange = settings::setShowLunarDates),
                )
            }
            Text(stringResource(R.string.finance_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Card {
                Box {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.budget_display_currency)) },
                        supportingContent = { Text(stringResource(R.string.finance_currency_hint)) },
                        trailingContent = { Text(currency) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                        modifier = Modifier.testTag("budget-display-currency").clickable { currencyMenu = true },
                    )
                    DropdownMenu(currencyMenu, { currencyMenu = false }) {
                        Money.currencies.forEach { code ->
                            DropdownMenuItem(text = { Text(code) }, onClick = {
                                settings.budgetCurrency = code
                                currencyMenu = false
                            })
                        }
                    }
                }
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.exchange_settings)) },
                    supportingContent = { Text(stringResource(R.string.exchange_settings_hint)) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHighest),
                    modifier = Modifier.testTag("open-exchange").clickable { showRates = true },
                )
            }
            Text(stringResource(R.string.settings_body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (showRates) ExchangeRateDialog(app.exchangeRates, currency,
        if (currency == "CNY") "USD" else "CNY", onClose = { showRates = false })
}
