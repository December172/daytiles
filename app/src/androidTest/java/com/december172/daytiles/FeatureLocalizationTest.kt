package com.december172.daytiles

import android.content.res.Configuration
import android.os.LocaleList
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeatureLocalizationTest {
    @Test fun newFeaturesResolveInEverySupportedChineseLocale() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val ids = listOf(R.string.full_schedule_title, R.string.recycle_bin_title, R.string.finance_title,
            R.string.restore_task, R.string.delete_forever, R.string.day_budget, R.string.month_budget,
            R.string.add_expense, R.string.expense_title, R.string.expense_category, R.string.budget_over,
            R.string.delete_task_confirmation, R.string.empty_bin, R.string.save_failed)
        fun resources(tag: String) = context.createConfigurationContext(Configuration(context.resources.configuration).apply {
            setLocales(LocaleList.forLanguageTags(tag))
        }).resources
        val english = resources("en-US")
        for (tag in listOf("zh-CN", "zh-HK", "zh-TW", "zh-Hant", "zh-Hant-HK", "zh-Hant-TW")) {
            val localized = resources(tag)
            ids.forEach { assertNotEquals("$tag fell back to English", english.getString(it), localized.getString(it)) }
            assertTrue(localized.getString(R.string.amount_currency, "CNY").contains("CNY"))
            assertTrue(localized.getString(R.string.budget_over, "12.50").contains("12.50"))
        }
        assertEquals("回收站", resources("zh-HK").getString(R.string.recycle_bin_title))
        assertEquals("回收筒", resources("zh-TW").getString(R.string.recycle_bin_title))
    }
}
