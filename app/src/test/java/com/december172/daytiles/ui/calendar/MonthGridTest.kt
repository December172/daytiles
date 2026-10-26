package com.december172.daytiles.ui.calendar

import java.time.DayOfWeek
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MonthGridTest {
    @Test
    fun fourWeekMonthHasFourCompleteRows() {
        val weeks = buildMonthWeeks(YearMonth.of(2021, 2))

        assertEquals(4, weeks.size)
        assertEquals(7, weeks.first().size)
        assertEquals(1, weeks.first().first()?.dayOfMonth)
        assertEquals(28, weeks.last().last()?.dayOfMonth)
    }

    @Test
    fun sixWeekMonthKeepsLeadingAndTrailingCellsEmpty() {
        val weeks = buildMonthWeeks(YearMonth.of(2021, 8))

        assertEquals(6, weeks.size)
        assertNull(weeks.first().first())
        assertEquals(1, weeks.first().last()?.dayOfMonth)
        assertEquals(31, weeks.last()[1]?.dayOfMonth)
        assertNull(weeks.last()[2])
    }

    @Test
    fun customWeekStartChangesLeadingCells() {
        val weeks = buildMonthWeeks(
            month = YearMonth.of(2026, 10),
            firstDayOfWeek = DayOfWeek.SUNDAY,
        )

        assertEquals(4, weeks.first().indexOfFirst { it?.dayOfMonth == 1 })
    }

    @Test
    fun displayMonthFillsAdjacentDaysAcrossAYearBoundary() {
        val weeks = buildDisplayMonthWeeks(YearMonth.of(2027, 1))
        assertEquals(java.time.LocalDate.of(2026, 12, 27), weeks.first().first())
        assertEquals(java.time.LocalDate.of(2027, 2, 6), weeks.last().last())
        assertEquals(31, weeks.flatten().count { it.monthValue == 1 })
        assertEquals(6, weeks.size)
    }

    @Test
    fun displayMonthStaysContinuousForLeapDayAndAllWeekStarts() {
        for (firstDay in DayOfWeek.entries) {
            val dates = buildDisplayMonthWeeks(YearMonth.of(2024, 2), firstDay).flatten()
            assertEquals(firstDay, dates.first().dayOfWeek)
            assertEquals(29, dates.count { it.monthValue == 2 })
            dates.zipWithNext().forEach { (a, b) -> assertEquals(a.plusDays(1), b) }
            assertEquals(0, dates.size % 7)
        }
    }

    @Test
    fun sundayAlignedFourWeekDisplayDoesNotAddAnEmptyRow() {
        val weeks = buildDisplayMonthWeeks(YearMonth.of(2026, 2))
        assertEquals(4, weeks.size)
        assertEquals(java.time.LocalDate.of(2026, 2, 1), weeks.first().first())
        assertEquals(java.time.LocalDate.of(2026, 2, 28), weeks.last().last())
    }
}

