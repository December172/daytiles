package com.december172.daytiles.data.sample

import com.december172.daytiles.domain.model.DayContent
import com.december172.daytiles.domain.model.JournalPreview
import com.december172.daytiles.domain.model.StickerPreview
import com.december172.daytiles.domain.model.TaskPreview
import java.time.LocalDate
import java.time.YearMonth

/**
 * Milestone 1 preview data only. Nothing in this object is production persistence.
 */
object SampleMonthContent {
    fun forMonth(month: YearMonth): Map<LocalDate, DayContent> = mapOf(
        month.atDay(2) to DayContent(
            journal = JournalPreview("sample-note-1", "慢慢来，也是在前进。"),
            stickers = listOf(StickerPreview("sample-sticker-sun", "☀️", "晴天贴纸")),
        ),
        month.atDay(5) to DayContent(
            tasks = listOf(
                TaskPreview("sample-task-1", "买花和牛奶", true),
                TaskPreview("sample-task-2", "整理书桌", false),
            ),
        ),
        month.atDay(9) to DayContent(
            journal = JournalPreview("sample-note-2", "傍晚散步，风里已经有秋天的味道。"),
            stickers = listOf(StickerPreview("sample-sticker-leaf", "🍂", "落叶贴纸")),
        ),
        month.atDay(13) to DayContent(
            tasks = listOf(TaskPreview("sample-task-3", "给家人打电话", false)),
            stickers = listOf(StickerPreview("sample-sticker-heart", "💛", "爱心贴纸")),
        ),
        month.atDay(18) to DayContent(
            journal = JournalPreview("sample-note-3", "读完一本很温柔的书。"),
            tasks = listOf(TaskPreview("sample-task-4", "归还图书", false)),
        ),
        month.atDay(21) to DayContent(
            stickers = listOf(
                StickerPreview("sample-sticker-coffee", "☕", "咖啡贴纸"),
                StickerPreview("sample-sticker-star", "✨", "星星贴纸"),
            ),
        ),
        month.atDay(27) to DayContent(
            journal = JournalPreview("sample-note-4", "留一点空白给自己。"),
            tasks = listOf(
                TaskPreview("sample-task-5", "周计划复盘", true),
                TaskPreview("sample-task-6", "准备下个月手账", false),
            ),
            stickers = listOf(StickerPreview("sample-sticker-moon", "🌙", "月亮贴纸")),
        ),
    )
}

