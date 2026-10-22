package com.december172.daytiles.ui.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

/** Builds complete Monday-to-Sunday rows while leaving adjacent-month cells empty. */
fun buildMonthWeeks(
    month: YearMonth,
    firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
): List<List<LocalDate?>> {
    val firstDate = month.atDay(1)
    val leadingEmptyCells = Math.floorMod(
        firstDate.dayOfWeek.value - firstDayOfWeek.value,
        DayOfWeek.entries.size,
    )
    val occupiedCellCount = leadingEmptyCells + month.lengthOfMonth()
    val trailingEmptyCells = Math.floorMod(-occupiedCellCount, DayOfWeek.entries.size)
    val cellCount = occupiedCellCount + trailingEmptyCells

    return List(cellCount) { index ->
        val day = index - leadingEmptyCells + 1
        if (day in 1..month.lengthOfMonth()) month.atDay(day) else null
    }.chunked(DayOfWeek.entries.size)
}


/** Continuous dates for the edge-to-edge month view, including adjacent months. */
fun buildDisplayMonthWeeks(
    month: YearMonth,
    firstDayOfWeek: DayOfWeek = DayOfWeek.SUNDAY,
): List<List<LocalDate>> {
    val first = month.atDay(1)
    val leading = Math.floorMod(first.dayOfWeek.value - firstDayOfWeek.value, 7)
    val count = ((leading + month.lengthOfMonth() + 6) / 7) * 7
    val start = first.minusDays(leading.toLong())
    return List(count) { start.plusDays(it.toLong()) }.chunked(7)
}
