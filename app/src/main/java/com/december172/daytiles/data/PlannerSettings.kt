package com.december172.daytiles.data

import android.content.Context
import androidx.core.content.edit
import com.december172.daytiles.domain.model.Money
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Display preferences are independent of journals, tasks, stickers, and the Room schema. */
class PlannerSettings(context: Context) {
    private val preferences = context.getSharedPreferences("planner_settings", Context.MODE_PRIVATE)
    private val displayCurrency = MutableStateFlow(
        preferences.getString("ledger_currency", null)?.takeIf { it in Money.currencies } ?: "CNY",
    )
    val budgetDisplayCurrency = displayCurrency.asStateFlow()

    var budgetCurrency: String
        get() = displayCurrency.value
        set(value) {
            require(value in Money.currencies)
            preferences.edit { putString("ledger_currency", value) }
            displayCurrency.value = value
        }

    private val lunarDates = MutableStateFlow(preferences.getBoolean("show_lunar_dates", true))
    val showLunarDates = lunarDates.asStateFlow()

    fun setShowLunarDates(show: Boolean) {
        preferences.edit { putBoolean("show_lunar_dates", show) }
        lunarDates.value = show
    }
}
