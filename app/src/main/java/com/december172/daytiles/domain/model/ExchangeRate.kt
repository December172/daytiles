package com.december172.daytiles.domain.model

import java.math.BigDecimal
import java.math.RoundingMode

enum class ExchangeRateSource(val website: String) {
    CFETS("https://www.chinamoney.com.cn/chinese/bkccpr/"),
    HKMA("https://apidocs.hkma.gov.hk/documentation/market-data-and-statistics/monthly-statistical-bulletin/er-ir/er-eeri-daily/"),
}

/** A reference quote: one unit of [from] buys [rate] units of [to]. */
data class ExchangeRate(
    val from: String,
    val to: String,
    val rate: BigDecimal,
    val updatedAt: String,
    val fetchedAt: Long,
    val source: ExchangeRateSource = ExchangeRateSource.CFETS,
) {
    init {
        require(from in Money.currencies && to in Money.currencies)
        require(rate > BigDecimal.ZERO && rate <= BigDecimal("1000000"))
        require(rate.precision() <= 24 && rate.scale() in 0..12)
        require(updatedAt.length <= 64 && fetchedAt >= 0)
    }

    /** Round only the final converted amount, using decimal arithmetic throughout. */
    fun convertMinor(amountMinor: Long): Long? {
        if (amountMinor !in 0..Money.MAX_AMOUNT_MINOR) return null
        return try {
            BigDecimal.valueOf(amountMinor).multiply(rate).setScale(0, RoundingMode.HALF_UP)
                .longValueExact().takeIf { it in 0..Money.MAX_AMOUNT_MINOR }
        } catch (_: ArithmeticException) { null }
    }
}
