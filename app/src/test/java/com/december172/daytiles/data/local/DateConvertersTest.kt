package com.december172.daytiles.data.local

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DateConvertersTest {
    private val converters = DateConverters()

    @Test
    fun localDateRoundTripsThroughISOStorage() {
        val date = LocalDate.of(2026, 10, 10)

        assertEquals(date, converters.toLocalDate(converters.fromLocalDate(date)))
    }

    @Test
    fun storedDatesRetainChronologicalTextOrdering() {
        val earlier = converters.fromLocalDate(LocalDate.of(2026, 2, 28))
        val later = converters.fromLocalDate(LocalDate.of(2026, 10, 1))

        assert(earlier < later)
    }
}

