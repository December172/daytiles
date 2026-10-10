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

    companion object {
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            listOf(
                DayTilesWidgetProvider::class.java,
                TodoWidgetProvider::class.java,
                MonthWidgetProvider::class.java,
            ).forEach { provider ->
                manager.getAppWidgetIds(ComponentName(context, provider)).forEach { id ->
                    when (provider) {
                        DayTilesWidgetProvider::class.java -> updateWidget(context, manager, id)
                        TodoWidgetProvider::class.java -> TodoWidgetProvider.updateWidget(context, manager, id)
                        MonthWidgetProvider::class.java -> MonthWidgetProvider.updateWidget(context, manager, id)
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

        private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_daytiles)
            val today = LocalDate.now()
            val locale = locale(context)
            views.setTextViewText(R.id.widget_month, today.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)))
            views.setTextViewText(R.id.widget_today, today.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)))
            views.setTextViewText(R.id.widget_tasks, context.getString(R.string.widget_loading))
            views.setOnClickPendingIntent(R.id.widget_root, dayOpenIntent(context, today, 1000 + widgetId))
            views.setOnClickPendingIntent(R.id.widget_add_task, dayOpenIntent(context, today, 3000 + widgetId))
            manager.updateAppWidget(widgetId, views)
            CoroutineScope(Dispatchers.IO).launch {
                runCatching { (context.applicationContext as DayTilesApplication).repository.recentOpenTasks() }
                    .onSuccess { tasks ->
                        val taskText = tasks.take(3).joinToString("\n") { task ->
                            val date = task.date.format(DateTimeFormatter.ofPattern("MMM d", locale))
                            "○  $date  ${task.title}"
                        }.ifEmpty { context.getString(R.string.widget_no_tasks) }
                        views.setTextViewText(R.id.widget_tasks, taskText)
                        manager.updateAppWidget(widgetId, views)
                    }
            }
        }

        internal fun locale(context: Context): Locale =
            context.resources.configuration.locales[0] ?: Locale.getDefault()

        internal fun taskSummary(context: Context, task: TaskEntity): String {
            val locale = locale(context)
            val date = task.date.format(DateTimeFormatter.ofPattern("MMM d", locale))
            val reminder = task.reminderAt?.let {
                DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
                    .format(java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneId.systemDefault()))
            }
            return if (reminder == null) "$date · ${task.title}"
            else "$date · $reminder · ${task.title}"
        }
    }
}
