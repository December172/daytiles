package com.december172.daytiles.domain.model

import java.math.BigDecimal
import org.junit.Assert.*
import org.junit.Test

class ExchangeRateTest {
    private fun quote(rate: String) = ExchangeRate("USD", "CNY", BigDecimal(rate), "2026-11-20 12:00:00", 1)

    @Test fun conversionUsesExactDecimalArithmeticAndRoundsOnlyFinalResult() {
        assertEquals(87L, quote("7.234567").convertMinor(12))
        assertEquals(3L, quote("0.1").convertMinor(30))
        assertEquals(101L, quote("1.005").convertMinor(100))
        assertEquals(100L, quote("1.004999").convertMinor(100))
    }

    @Test fun zeroAndSubCentEstimatesAreSupportedWithoutNegativeAmounts() {
        assertEquals(0L, quote("0.01").convertMinor(1))
        assertEquals(0L, quote("7.2").convertMinor(0))
        assertNull(quote("7.2").convertMinor(-1))
    }

    @Test fun limitsAreCheckedAfterConversionWithoutOverflow() {
        assertEquals(Money.MAX_AMOUNT_MINOR, quote("1").convertMinor(Money.MAX_AMOUNT_MINOR))
        assertNull(quote("1.01").convertMinor(Money.MAX_AMOUNT_MINOR))
        assertNull(quote("1000000").convertMinor(Money.MAX_AMOUNT_MINOR))
        assertNull(quote("0.5").convertMinor(Long.MAX_VALUE))
    }

    @Test fun invalidRatesAndUnboundedPrecisionAreRejected() {
        for (rate in listOf("0", "-1", "1000001", "0.0000000000001", "1E+999999")) {
            assertThrows(IllegalArgumentException::class.java) { quote(rate) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            ExchangeRate("EUR", "CNY", BigDecimal.ONE, "", 0)
        }
    }

    @Test fun identityQuotePreservesEverySupportedAmount() {
        for (code in Money.currencies) {
            val rate = ExchangeRate(code, code, BigDecimal.ONE, "", 0)
            for (amount in listOf(0L, 1L, 12345L, Money.MAX_AMOUNT_MINOR)) assertEquals(amount, rate.convertMinor(amount))
        }
    }
}
