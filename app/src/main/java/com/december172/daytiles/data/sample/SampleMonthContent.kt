package com.december172.daytiles.data.sample

import com.december172.daytiles.domain.model.StickerPreview
import java.time.LocalDate
import java.time.YearMonth

/**
 * Sticker preview data only. Notes and tasks always come from Room.
 */
object SampleMonthContent {
    fun stickersForMonth(month: YearMonth): Map<LocalDate, List<StickerPreview>> = mapOf(
        month.atDay(2) to listOf(StickerPreview("sample-sticker-sun", "☀️")),
        month.atDay(9) to listOf(StickerPreview("sample-sticker-leaf", "🍂")),
        month.atDay(13) to listOf(StickerPreview("sample-sticker-heart", "💛")),
        month.atDay(21) to listOf(
                StickerPreview("sample-sticker-coffee", "☕"),
                StickerPreview("sample-sticker-star", "✨"),
            ),
        month.atDay(27) to listOf(StickerPreview("sample-sticker-moon", "🌙")),
    )
}
