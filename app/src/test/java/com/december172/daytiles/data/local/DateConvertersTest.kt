package com.december172.daytiles.data.local

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DateConvertersTest {
    private val converters = DateConverters()

    @Test
    fun `local date round trips through ISO storage`() {
        val date = LocalDate.of(2026, 10, 10)

        assertEquals(date, converters.toLocalDate(converters.fromLocalDate(date)))
    }

    @Test
    fun `stored dates retain chronological text ordering`() {
        val earlier = converters.fromLocalDate(LocalDate.of(2026, 2, 28))
        val later = converters.fromLocalDate(LocalDate.of(2026, 10, 1))

        assert(earlier < later)
    }
}

