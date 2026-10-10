package com.december172.daytiles.ui.calendar

import java.time.DayOfWeek
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MonthGridTest {
    @Test
    fun `four-week month has four complete rows`() {
        val weeks = buildMonthWeeks(YearMonth.of(2021, 2))

        assertEquals(4, weeks.size)
        assertEquals(7, weeks.first().size)
        assertEquals(1, weeks.first().first()?.dayOfMonth)
        assertEquals(28, weeks.last().last()?.dayOfMonth)
    }

    @Test
    fun `six-week month keeps leading and trailing cells empty`() {
        val weeks = buildMonthWeeks(YearMonth.of(2021, 8))

        assertEquals(6, weeks.size)
        assertNull(weeks.first().first())
        assertEquals(1, weeks.first().last()?.dayOfMonth)
        assertEquals(31, weeks.last()[1]?.dayOfMonth)
        assertNull(weeks.last()[2])
    }

    @Test
    fun `custom week start changes leading cells`() {
        val weeks = buildMonthWeeks(
            month = YearMonth.of(2026, 10),
            firstDayOfWeek = DayOfWeek.SUNDAY,
        )

        assertEquals(4, weeks.first().indexOfFirst { it?.dayOfMonth == 1 })
    }
}

