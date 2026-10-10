package com.december172.daytiles.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.december172.daytiles.DayTilesApplication
import com.december172.daytiles.data.DayRepository
import com.december172.daytiles.data.local.JournalEntryEntity
import com.december172.daytiles.data.local.TaskEntity
import com.december172.daytiles.data.local.StickerPlacementWithAsset
import com.december172.daytiles.domain.model.DayContent
import com.december172.daytiles.domain.model.JournalPreview
import com.december172.daytiles.domain.model.TaskPreview
import com.december172.daytiles.domain.model.StickerPreview
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.min
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CalendarUiState(
    val visibleMonth: YearMonth,
    val selectedDate: LocalDate,
    val today: LocalDate,
    val contentByDate: Map<LocalDate, DayContent>,
    val tasks: List<TaskEntity> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class CalendarViewModel(private val repository: DayRepository) : ViewModel() {
    private val today = LocalDate.now()
    private val visibleMonth = MutableStateFlow(YearMonth.from(today))
    private val selectedDate = MutableStateFlow(today)

    private val persistedContent: Flow<Map<LocalDate, DayContent>> = visibleMonth.flatMapLatest { month ->
        combine(
            repository.observeJournals(month),
            repository.observeTasks(month),
            repository.observeStickers(month),
        ) { journals, tasks, stickers ->
            mergeContent(journals, tasks, stickers)
        }
    }

    private val monthTasks: Flow<List<TaskEntity>> = visibleMonth.flatMapLatest(repository::observeTasks)

    val uiState: StateFlow<CalendarUiState> = combine(
        visibleMonth,
        selectedDate,
        persistedContent,
        monthTasks,
    ) { month, selected, content, tasks ->
        CalendarUiState(month, selected, today, content, tasks)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = CalendarUiState(
            visibleMonth = YearMonth.from(today),
            selectedDate = today,
            today = today,
            contentByDate = mergeContent(emptyList(), emptyList(), emptyList()),
        ),
    )

    fun showPreviousMonth() = showMonth(visibleMonth.value.minusMonths(1))

    fun showNextMonth() = showMonth(visibleMonth.value.plusMonths(1))

    fun showToday() {
        selectedDate.value = today
        visibleMonth.value = YearMonth.from(today)
    }

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }

    fun selectDateAndShow(date: LocalDate) {
        selectedDate.value = date
        visibleMonth.value = YearMonth.from(date)
    }

    fun setTaskCompleted(task: TaskEntity, completed: Boolean) {
        viewModelScope.launch { repository.setTaskCompleted(task, completed) }
    }

    private fun showMonth(month: YearMonth) {
        val selectedDay = min(selectedDate.value.dayOfMonth, month.lengthOfMonth())
        selectedDate.value = month.atDay(selectedDay)
        visibleMonth.value = month
    }

    private fun mergeContent(
        journals: List<JournalEntryEntity>,
        tasks: List<TaskEntity>,
        stickers: List<StickerPlacementWithAsset>,
    ): Map<LocalDate, DayContent> {
        val journalsByDate = journals.groupBy { it.date }
        val tasksByDate = tasks.groupBy { it.date }
        val stickersByDate = stickers.groupBy { it.date }
        return (journalsByDate.keys + tasksByDate.keys + stickersByDate.keys).associateWith { date ->
            DayContent(
                journal = journalsByDate[date]?.firstOrNull()?.let {
                    JournalPreview(it.id.toString(), it.content)
                },
                tasks = tasksByDate[date].orEmpty().map {
                    TaskPreview(it.id.toString(), it.title, it.isCompleted)
                },
                stickers = stickersByDate[date].orEmpty().map {
                    StickerPreview(
                        id = it.placementId.toString(),
                        kind = it.kind,
                        source = it.source,
                        displayName = it.displayName,
                    )
                },
            )
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as DayTilesApplication
                CalendarViewModel(application.repository)
            }
        }
    }
}
