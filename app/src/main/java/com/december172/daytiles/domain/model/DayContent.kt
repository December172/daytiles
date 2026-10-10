package com.december172.daytiles.domain.model

data class DayContent(
    val journal: JournalPreview? = null,
    val tasks: List<TaskPreview> = emptyList(),
    val stickers: List<StickerPreview> = emptyList(),
)

data class JournalPreview(
    val id: String,
    val text: String,
)

data class TaskPreview(
    val id: String,
    val title: String,
    val isCompleted: Boolean,
)

data class StickerPreview(
    val id: String,
    val symbol: String,
)
