package com.december172.daytiles.ui.day

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.december172.daytiles.DayTilesApplication
import com.december172.daytiles.data.DayRepository
import com.december172.daytiles.data.local.JournalEntryEntity
import com.december172.daytiles.data.local.TaskEntity
import com.december172.daytiles.data.local.StickerPlacementEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class DayEditorUiState(
    val date: LocalDate,
    val journalText: String,
    val tasks: List<TaskEntity>,
    val stickers: List<StickerPlacementEntity>,
)

class DayEditorViewModel(
    private val date: LocalDate,
    private val repository: DayRepository,
) : ViewModel() {
    private val journalText = MutableStateFlow("")
    private val tasks = repository.observeTasks(date)
    private val stickers = repository.observeStickers(date)
    private val saveMutex = Mutex()
    private var currentJournal: JournalEntryEntity? = null
    private var hasEditedJournal = false

    val uiState: StateFlow<DayEditorUiState> = combine(
        journalText,
        tasks,
        stickers,
    ) { text, dayTasks, dayStickers ->
        DayEditorUiState(date, text, dayTasks, dayStickers)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DayEditorUiState(date, "", emptyList(), emptyList()),
    )

    init {
        repository.observeJournals(date).onEach { entries ->
            currentJournal = entries.firstOrNull()
            if (!hasEditedJournal) journalText.value = currentJournal?.content.orEmpty()
        }.launchIn(viewModelScope)
    }

    fun updateJournal(text: String) {
        hasEditedJournal = true
        journalText.value = text
        viewModelScope.launch {
            saveMutex.withLock {
                val now = System.currentTimeMillis()
                val existing = currentJournal
                val entry = existing?.copy(content = text, updatedAt = now)
                    ?: JournalEntryEntity(
                        date = date,
                        content = text,
                        createdAt = now,
                        updatedAt = now,
                    )
                val savedId = repository.saveJournal(entry)
                currentJournal = if (savedId == 0L) null else entry.copy(id = savedId)
            }
        }
    }

    fun addTask(title: String) {
        viewModelScope.launch { repository.addTask(date, title) }
    }

    fun setTaskCompleted(task: TaskEntity, completed: Boolean) {
        viewModelScope.launch { repository.setTaskCompleted(task, completed) }
    }

    fun updateTaskTitle(task: TaskEntity, title: String) {
        viewModelScope.launch { repository.updateTaskTitle(task, title) }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch { repository.deleteTask(task.id) }
    }

    fun addSticker(symbol: String) {
        viewModelScope.launch { repository.addSticker(date, symbol) }
    }

    fun deleteSticker(sticker: StickerPlacementEntity) {
        viewModelScope.launch { repository.deleteSticker(sticker.id) }
    }

    companion object {
        fun factory(date: LocalDate) = viewModelFactory {
            initializer {
                val application = this[APPLICATION_KEY] as DayTilesApplication
                DayEditorViewModel(date, application.repository)
            }
        }
    }
}
