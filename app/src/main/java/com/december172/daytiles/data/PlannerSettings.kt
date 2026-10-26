package com.december172.daytiles.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Display preferences are independent of journals, tasks, stickers, and the Room schema. */
class PlannerSettings(context: Context) {
    private val preferences = context.getSharedPreferences("planner_settings", Context.MODE_PRIVATE)
    var ledgerCurrency: String
        get() = preferences.getString("ledger_currency", "CNY") ?: "CNY"
        set(value) { preferences.edit { putString("ledger_currency", value) } }

    private val lunarDates = MutableStateFlow(preferences.getBoolean("show_lunar_dates", true))
    val showLunarDates = lunarDates.asStateFlow()

    fun setShowLunarDates(show: Boolean) {
        preferences.edit { putBoolean("show_lunar_dates", show) }
        lunarDates.value = show
    }
}
