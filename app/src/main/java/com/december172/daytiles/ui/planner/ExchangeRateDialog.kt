package com.december172.daytiles.ui.planner

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.december172.daytiles.R
import com.december172.daytiles.data.ExchangeRateRepository
import com.december172.daytiles.domain.model.ExchangeRateSource
import com.december172.daytiles.domain.model.Money
import com.december172.daytiles.ui.components.PlannerTopBar
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun ExchangeRateDialog(
    repository: ExchangeRateRepository,
    initialFrom: String,
    initialTo: String,
    onClose: () -> Unit,
) {
    var from by rememberSaveable { mutableStateOf(initialFrom) }
    var to by rememberSaveable { mutableStateOf(initialTo) }
    var amount by rememberSaveable { mutableStateOf("1.00") }
    var busy by remember(from, to) { mutableStateOf(false) }
    var failed by remember(from, to) { mutableStateOf(false) }
    var quote by remember(from, to) { mutableStateOf(repository.cachedRate(from, to)) }
    val parsed = Money.parseMinor(amount, allowZero = true)
    val converted = parsed?.let { quote?.convertMinor(it) }
    val locale = LocalConfiguration.current.locales[0]
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    suspend fun loadRate(force: Boolean) {
        busy = true; failed = false
        try { quote = repository.refresh(from, to, force) }
        catch (error: Exception) {
            if (error is CancellationException) throw error
            failed = true
        } finally { busy = false }
    }

    // Opening the converter is enough: fresh snapshots are reused without another request.
    LaunchedEffect(from, to) { loadRate(force = false) }

    Dialog(onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Scaffold(modifier = Modifier.fillMaxSize().imePadding().semantics { testTagsAsResourceId = true },
            topBar = { PlannerTopBar(stringResource(R.string.exchange_title), onClose) }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(16.dp).testTag("exchange-screen"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.exchange_provider), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.exchange_privacy), style = MaterialTheme.typography.bodySmall)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExchangeCurrencyPicker(from, R.string.exchange_from, "exchange-from", !busy, Modifier.weight(1f)) {
                        from = it
                    }
                    ExchangeCurrencyPicker(to, R.string.exchange_to, "exchange-to", !busy, Modifier.weight(1f)) {
                        to = it
                    }
                }
                OutlinedTextField(amount, { if (it.length <= 16) amount = it },
                    Modifier.fillMaxWidth().testTag("exchange-amount"),
                    label = { Text(stringResource(R.string.amount_currency, from)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = amount.isNotBlank() && parsed == null)
                quote?.let { rate ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("1 $from = ${rate.rate.stripTrailingZeros().toPlainString()} $to",
                                Modifier.testTag("exchange-rate"), style = MaterialTheme.typography.titleMedium)
                            if (converted != null) Text(Money.format(converted, to, locale),
                                Modifier.testTag("exchange-result"), style = MaterialTheme.typography.headlineMedium)
                            else if (parsed != null) Text(stringResource(R.string.exchange_amount_too_large), color = MaterialTheme.colorScheme.error)
                            if (from == to) Text(stringResource(R.string.exchange_same_currency), style = MaterialTheme.typography.bodySmall)
                            else {
                                val source = stringResource(if (rate.source == ExchangeRateSource.CFETS)
                                    R.string.exchange_source_cfets else R.string.exchange_source_hkma)
                                Text(source, Modifier.testTag("exchange-source"), style = MaterialTheme.typography.bodySmall)
                                Text(stringResource(R.string.exchange_updated, rate.updatedAt), Modifier.testTag("exchange-observation"),
                                    style = MaterialTheme.typography.bodySmall)
                                val savedAt = Instant.ofEpochMilli(rate.fetchedAt).atZone(ZoneId.systemDefault())
                                    .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(locale))
                                Text(stringResource(R.string.exchange_cached, savedAt), style = MaterialTheme.typography.bodySmall)
                                TextButton(onClick = { uriHandler.openUri(rate.source.website) }) {
                                    Text(stringResource(R.string.exchange_provider_link))
                                }
                            }
                        }
                    }
                } ?: Text(stringResource(if (busy) R.string.exchange_refreshing else R.string.exchange_no_rate),
                    Modifier.testTag("exchange-no-rate"))
                if (failed) Text(stringResource(R.string.exchange_refresh_failed), Modifier.testTag("exchange-error"),
                    color = MaterialTheme.colorScheme.error)
                Button(enabled = from != to && !busy,
                    modifier = Modifier.fillMaxWidth().testTag("exchange-refresh"),
                    onClick = { scope.launch { loadRate(force = true) } }) {
                    if (busy) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(stringResource(if (busy) R.string.exchange_refreshing else R.string.exchange_refresh))
                }
                Text(stringResource(R.string.exchange_reference_hint), style = MaterialTheme.typography.bodySmall)
                TextButton(enabled = !busy, modifier = Modifier.testTag("exchange-clear-cache"), onClick = {
                    repository.clearCachedRates(); quote = repository.cachedRate(from, to); failed = false
                }) { Text(stringResource(R.string.exchange_clear_cache)) }
            }
        }
    }
}

@Composable
private fun ExchangeCurrencyPicker(code: String, label: Int, tag: String, enabled: Boolean,
    modifier: Modifier, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        OutlinedButton(enabled = enabled, onClick = { expanded = true }, modifier = Modifier.fillMaxWidth().testTag(tag)) {
            Text(stringResource(label, code))
        }
        DropdownMenu(expanded, { expanded = false }) {
            Money.currencies.forEach { currency ->
                DropdownMenuItem(text = { Text(currency) }, modifier = Modifier.testTag("$tag-$currency"),
                    onClick = { onSelect(currency); expanded = false })
            }
        }
    }
}
