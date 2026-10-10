package com.december172.daytiles.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.december172.daytiles.DayTilesApplication
import com.december172.daytiles.MainActivity
import com.december172.daytiles.R
import com.december172.daytiles.data.local.TaskEntity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DayTilesWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(context, manager, it) }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: android.os.Bundle) {
        updateWidget(context, manager, appWidgetId)
    }

    companion object {
        fun refreshJournal(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, DayTilesWidgetProvider::class.java)
            manager.getAppWidgetIds(component).forEach { updateWidget(context, manager, it) }
        }

        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            listOf(
                DayTilesWidgetProvider::class.java,
                TodoWidgetProvider::class.java,
                MonthWidgetProvider::class.java,
                WeekWidgetProvider::class.java,
                FocusWidgetProvider::class.java,
            ).forEach { provider ->
                manager.getAppWidgetIds(ComponentName(context, provider)).forEach { id ->
                    when (provider) {
                        DayTilesWidgetProvider::class.java -> updateWidget(context, manager, id)
                        TodoWidgetProvider::class.java -> TodoWidgetProvider.updateWidget(context, manager, id)
                        MonthWidgetProvider::class.java -> MonthWidgetProvider.updateWidget(context, manager, id)
                        WeekWidgetProvider::class.java -> WeekWidgetProvider.updateWidget(context, manager, id)
                        FocusWidgetProvider::class.java -> FocusWidgetProvider.updateWidget(context, manager, id)
                    }
                }
            }
        }

        internal fun dayOpenIntent(context: Context, date: LocalDate, requestCode: Int): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_DATE, date.toString())
            }
            return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        internal fun journalOpenIntent(context: Context, date: LocalDate, requestCode: Int): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_DATE, date.toString())
                putExtra(MainActivity.EXTRA_OPEN_JOURNAL, true)
            }
            return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_daytiles)
            val widgetHeight = manager.getAppWidgetOptions(widgetId)
                .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 160)
            views.setInt(R.id.journal_widget_preview, "setMaxLines", if (widgetHeight < 130) 2 else 6)
            views.setTextViewTextSize(R.id.journal_widget_preview, android.util.TypedValue.COMPLEX_UNIT_SP, if (widgetHeight < 130) 11f else 13f)
            val today = LocalDate.now()
            val locale = locale(context)
            views.setTextViewText(R.id.journal_widget_title, context.getString(R.string.journal_heading))
            views.setTextViewText(R.id.journal_widget_date, today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)))
            views.setTextViewText(R.id.journal_widget_preview, context.getString(R.string.widget_loading))
            val openJournal = journalOpenIntent(context, today, 1000 + widgetId)
            views.setOnClickPendingIntent(R.id.journal_widget_root, openJournal)
            views.setOnClickPendingIntent(R.id.journal_widget_edit, openJournal)
            manager.updateAppWidget(widgetId, views)
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { (context.applicationContext as DayTilesApplication).repository.journalFor(today) }
                    .onSuccess { journal ->
                        views.setTextViewText(
                            R.id.journal_widget_preview,
                            journal.ifBlank { context.getString(R.string.journal_widget_empty) },
                        )
                        manager.updateAppWidget(widgetId, views)
                    }
            }
        }

        internal fun locale(context: Context): Locale =
            context.resources.configuration.locales[0] ?: Locale.getDefault()

        internal fun taskSummary(context: Context, task: TaskEntity): String {
            val locale = locale(context)
            val date = task.date.format(DateTimeFormatter.ofPattern("MMM d", locale))
            val dateState = when {
                task.date.isBefore(LocalDate.now()) -> context.getString(R.string.task_overdue)
                task.date == LocalDate.now() -> context.getString(R.string.task_due_today)
                else -> null
            }
            val reminder = task.reminderAt?.let {
                DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
                    .format(java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()))
            }
            val summary = listOfNotNull(dateState, date, reminder).joinToString(" · ")
            return "$summary · ${task.title}"
        }
    }
}
