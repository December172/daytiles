package com.december172.daytiles.domain.model

import java.util.Locale
import org.junit.Assert.*
import org.junit.Test

class MoneyTest {
    @Test fun decimalInputIsStoredAsExactMinorUnits() {
        assertEquals(1L, Money.parseMinor("0.01"))
        assertEquals(1230L, Money.parseMinor("12.3"))
        assertEquals(30L, Money.parseMinor("0.10")!! + Money.parseMinor("0.20")!!)
        assertEquals("0.30", Money.input(30))
    }

    @Test fun invalidAmountsAreRejectedInsteadOfRounded() {
        listOf("", "0", "-1", "12.345", "1,000", "NaN", "1e3", "999999999999999999999").forEach {
            assertNull(it, Money.parseMinor(it))
        }
    }

    @Test fun zeroBudgetsAreSupportedWithoutPermittingZeroExpenses() {
        assertEquals(0L, Money.parseMinor("0.00", allowZero = true))
        assertNull(Money.parseMinor("0.00"))
    }

    @Test fun amountBoundsAreEnforcedExactly() {
        assertEquals(Money.MAX_AMOUNT_MINOR, Money.parseMinor("999999999.99"))
        assertNull(Money.parseMinor("1000000000.00"))
    }

    @Test fun moneyInputRoundTripsAndAllSupportedCurrenciesFormat() {
        listOf(1L, 10L, 123456789L, Money.MAX_AMOUNT_MINOR).forEach {
            assertEquals(it, Money.parseMinor(Money.input(it)))
        }
        Money.currencies.forEach { code ->
            assertTrue(Money.format(12345L, code, Locale.US).contains("123.45"))
        }
    }
}
