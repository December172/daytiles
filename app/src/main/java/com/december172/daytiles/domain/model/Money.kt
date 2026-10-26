package com.december172.daytiles.domain.model

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object Money {
    val currencies = listOf("CNY", "HKD", "TWD", "USD")
    const val MAX_AMOUNT_MINOR = 99_999_999_999L

    /** Accept decimal amounts exactly; never round or store money as floating point. */
    fun parseMinor(text: String, allowZero: Boolean = false): Long? {
        val clean = text.trim()
        if (!Regex("[0-9]+(?:\\.[0-9]{1,2})?").matches(clean)) return null
        val minor = try {
            BigDecimal(clean).setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact()
        } catch (_: ArithmeticException) {
            return null
        }
        return minor.takeIf { it in (if (allowZero) 0L else 1L)..MAX_AMOUNT_MINOR }
    }

    fun input(minor: Long): String = BigDecimal.valueOf(minor, 2).toPlainString()

    fun format(minor: Long, code: String, locale: Locale): String =
        NumberFormat.getCurrencyInstance(locale).apply {
            currency = Currency.getInstance(code)
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }.format(BigDecimal.valueOf(minor, 2))
}
