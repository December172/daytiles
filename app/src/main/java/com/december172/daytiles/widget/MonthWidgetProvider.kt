package com.december172.daytiles.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.december172.daytiles.R
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class MonthWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(context, manager, it) }
    }

    companion object {
        internal fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val now = LocalDate.now()
            val month = YearMonth.from(now)
            val firstOffset = month.atDay(1).dayOfWeek.value - 1
            val rows = ((firstOffset + month.lengthOfMonth() + 6) / 7)
            val numbers = MutableList(rows * 7) { " " }
            for (day in 1..month.lengthOfMonth()) numbers[firstOffset + day - 1] = day.toString()
            val weeks = numbers.chunked(7).mapIndexed { weekIndex, week ->
                week.mapIndexed { dayIndex, value ->
                    val day = weekIndex * 7 + dayIndex - firstOffset + 1
                    if (day == now.dayOfMonth) "[$value]" else value.padStart(2)
                }.joinToString(" ")
            }.joinToString("\n")
            val views = RemoteViews(context.packageName, R.layout.widget_month)
            val locale = DayTilesWidgetProvider.locale(context)
            views.setTextViewText(R.id.month_widget_title, month.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)))
            val weekdays = context.resources.getStringArray(R.array.widget_weekdays).joinToString("  ")
            views.setTextViewText(R.id.month_widget_grid, "$weekdays\n$weeks")
            views.setOnClickPendingIntent(
                R.id.month_widget_root,
                DayTilesWidgetProvider.dayOpenIntent(context, now, 5000 + widgetId),
            )
            manager.updateAppWidget(widgetId, views)
        }
    }
}
