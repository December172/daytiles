package com.december172.daytiles.ui.calendar

import androidx.lifecycle.ViewModel
import com.december172.daytiles.data.sample.SampleMonthContent
import com.december172.daytiles.domain.model.DayContent
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.min
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CalendarUiState(
    val visibleMonth: YearMonth,
    val selectedDate: LocalDate,
    val today: LocalDate,
    val contentByDate: Map<LocalDate, DayContent>,
)

class CalendarViewModel : ViewModel() {
    private val today = LocalDate.now()

    private val _uiState = MutableStateFlow(
        CalendarUiState(
            visibleMonth = YearMonth.from(today),
            selectedDate = today,
            today = today,
            contentByDate = SampleMonthContent.forMonth(YearMonth.from(today)),
        ),
    )
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    fun showPreviousMonth() = showMonth(_uiState.value.visibleMonth.minusMonths(1))

    fun showNextMonth() = showMonth(_uiState.value.visibleMonth.plusMonths(1))

    fun showToday() {
        val month = YearMonth.from(today)
        _uiState.update {
            it.copy(
                visibleMonth = month,
                selectedDate = today,
                contentByDate = SampleMonthContent.forMonth(month),
            )
        }
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
    }

    private fun showMonth(month: YearMonth) {
        val currentSelection = _uiState.value.selectedDate
        val selectedDay = min(currentSelection.dayOfMonth, month.lengthOfMonth())
        _uiState.update {
            it.copy(
                visibleMonth = month,
                selectedDate = month.atDay(selectedDay),
                contentByDate = SampleMonthContent.forMonth(month),
            )
        }
    }
}

