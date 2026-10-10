package com.december172.daytiles.ui.day

import android.content.Context
import android.net.Uri
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
import com.december172.daytiles.data.local.StickerAssetEntity
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
    val stickers: List<StickerPlacementWithAsset>,
    val stickerAssets: List<StickerAssetEntity>,
    val remoteStickerState: RemoteStickerState,
)

enum class RemoteStickerState { IDLE, DOWNLOADING, ERROR }

class DayEditorViewModel(
    private val date: LocalDate,
    private val repository: DayRepository,
) : ViewModel() {
    private val journalText = MutableStateFlow("")
    private val tasks = repository.observeTasks(date)
    private val stickers = repository.observeStickers(date)
    private val stickerAssets = repository.observeStickerAssets()
    private val remoteStickerState = MutableStateFlow(RemoteStickerState.IDLE)
    private val saveMutex = Mutex()
    private var currentJournal: JournalEntryEntity? = null
    private var hasEditedJournal = false

    val uiState: StateFlow<DayEditorUiState> = combine(
        journalText,
        tasks,
        stickers,
        stickerAssets,
        remoteStickerState,
    ) { text, dayTasks, dayStickers, assets, downloadState ->
        DayEditorUiState(date, text, dayTasks, dayStickers, assets, downloadState)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DayEditorUiState(
            date,
            "",
            emptyList(),
            emptyList(),
            emptyList(),
            RemoteStickerState.IDLE,
        ),
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

    fun importImageSticker(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching { repository.importImageSticker(context, date, uri) }
        }
    }

    fun importRemoteImageSticker(context: Context, url: String) {
        if (remoteStickerState.value == RemoteStickerState.DOWNLOADING) return
        remoteStickerState.value = RemoteStickerState.DOWNLOADING
        viewModelScope.launch {
            remoteStickerState.value = runCatching {
                repository.importRemoteImageSticker(context, date, url)
            }.fold(
                onSuccess = { RemoteStickerState.IDLE },
                onFailure = { RemoteStickerState.ERROR },
            )
        }
    }

    fun clearRemoteStickerError() {
        if (remoteStickerState.value == RemoteStickerState.ERROR) {
            remoteStickerState.value = RemoteStickerState.IDLE
        }
    }

    fun addStickerAsset(asset: StickerAssetEntity) {
        viewModelScope.launch { repository.placeStickerAsset(date, asset.id) }
    }

    fun deleteSticker(sticker: StickerPlacementWithAsset) {
        viewModelScope.launch { repository.deleteSticker(sticker.placementId) }
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
