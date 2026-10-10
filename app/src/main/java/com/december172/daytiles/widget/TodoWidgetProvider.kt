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
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TodoWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { updateWidget(context, manager, it) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_TOGGLE) return
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, 0)
        val completed = intent.getBooleanExtra(EXTRA_COMPLETED, false)
        CoroutineScope(Dispatchers.IO).launch {
            (context.applicationContext as DayTilesApplication).repository.setTaskCompletedById(taskId, completed)
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.december172.daytiles.action.TOGGLE_TASK"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_COMPLETED = "task_completed"

        internal fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_todo)
            views.setTextViewText(R.id.todo_widget_title, context.getString(R.string.tasks_heading))
            views.setRemoteAdapter(R.id.todo_widget_list, Intent(context, TodoWidgetService::class.java))
            views.setPendingIntentTemplate(
                R.id.todo_widget_list,
                PendingIntent.getBroadcast(
                    context,
                    6000 + widgetId,
                    Intent(context, TodoWidgetProvider::class.java).setAction(ACTION_TOGGLE),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
                ),
            )
            views.setOnClickPendingIntent(
                R.id.todo_widget_title,
                DayTilesWidgetProvider.dayOpenIntent(context, LocalDate.now(), 4000 + widgetId),
            )
            manager.updateAppWidget(widgetId, views)
            manager.notifyAppWidgetViewDataChanged(widgetId, R.id.todo_widget_list)
        }
    }
}
