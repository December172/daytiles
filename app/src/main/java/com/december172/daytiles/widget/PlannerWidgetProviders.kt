package com.december172.daytiles.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.december172.daytiles.DayTilesApplication
import com.december172.daytiles.R
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WeekWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(context, manager, it) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: android.os.Bundle) {
        updateWidget(context, manager, appWidgetId)
    }

    companion object {
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            manager.getAppWidgetIds(ComponentName(context, WeekWidgetProvider::class.java)).forEach { updateWidget(context, manager, it) }
        }

        internal fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val now = LocalDate.now()
            val monday = now.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val views = RemoteViews(context.packageName, R.layout.widget_week)
            val height = manager.getAppWidgetOptions(id).getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160)
            views.setInt(R.id.week_widget_content, "setMaxLines", if (height < 130) 3 else 8)
            views.setTextViewText(R.id.week_widget_title, context.getString(R.string.week_title))
            views.setTextViewText(R.id.week_widget_content, context.getString(R.string.widget_loading))
            views.setOnClickPendingIntent(R.id.week_widget_root, DayTilesWidgetProvider.dayOpenIntent(context, now, 7200 + id))
            manager.updateAppWidget(id, views)
            CoroutineScope(Dispatchers.IO).launch {
                val app = context.applicationContext as DayTilesApplication
                val tasks = app.repository.tasksBetween(monday, monday.plusDays(6))
                val journals = app.repository.journalsBetween(monday, monday.plusDays(6)).associate { it.date to it.content }
                val summary = (0L..6L).mapNotNull { offset ->
                    val date = monday.plusDays(offset)
                    val dailyTasks = tasks.filter { it.date == date }.joinToString { it.title }
                    val journal = journals[date]?.takeIf(String::isNotBlank)?.let { " · ${it.take(55)}" }.orEmpty()
                    if (dailyTasks.isBlank() && journal.isBlank()) null else "${date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, DayTilesWidgetProvider.locale(context))} ${date.dayOfMonth}: $dailyTasks$journal"
                }
                views.setTextViewText(R.id.week_widget_content, summary.joinToString("\n").ifBlank { context.getString(R.string.widget_no_tasks) })
                manager.updateAppWidget(id, views)
            }
        }
    }
}

class FocusWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(context, manager, it) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: android.os.Bundle) {
        updateWidget(context, manager, appWidgetId)
    }

    companion object {
        fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val preferences = context.getSharedPreferences(FocusTimerService.PREFERENCES, Context.MODE_PRIVATE)
            val endAt = preferences.getLong(FocusTimerService.KEY_END, 0L)
            val remaining = if (endAt > 0) (endAt - System.currentTimeMillis()).coerceAtLeast(0)
                else preferences.getLong(FocusTimerService.KEY_REMAINING, 0L)
            val views = RemoteViews(context.packageName, R.layout.widget_focus).apply {
                setTextViewText(R.id.focus_widget_title, context.getString(R.string.focus_title))
                setTextViewText(R.id.focus_widget_time, if (remaining > 0) "%02d:%02d".format(remaining / 60_000, remaining / 1000 % 60) else "—:—")
                setOnClickPendingIntent(
                    R.id.focus_widget_start,
                    PendingIntent.getForegroundService(
                        context, 8100 + id,
                        Intent(context, FocusTimerService::class.java).setAction(FocusTimerService.ACTION_START)
                            .putExtra(FocusTimerService.EXTRA_WORK, preferences.getInt("work", 25) * 60_000L)
                            .putExtra(FocusTimerService.EXTRA_BREAK, preferences.getInt("break", 5) * 60_000L),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
            }
            manager.updateAppWidget(id, views)
        }
    }
}
