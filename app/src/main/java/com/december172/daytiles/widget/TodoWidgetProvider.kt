package com.december172.daytiles.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import androidx.core.net.toUri
import com.december172.daytiles.DayTilesApplication
import com.december172.daytiles.R
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TodoWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ids.forEach { renderWidget(context, manager, it) }
            } finally {
                pending.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, appWidgetId: Int, newOptions: android.os.Bundle) {
        onUpdate(context, manager, intArrayOf(appWidgetId))
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_TOGGLE) return
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, 0)
        if (taskId <= 0) return
        val completed = intent.getBooleanExtra(EXTRA_COMPLETED, false)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                (context.applicationContext as DayTilesApplication).repository.setTaskCompletedById(taskId, completed)
                val manager = AppWidgetManager.getInstance(context)
                val component = ComponentName(context, TodoWidgetProvider::class.java)
                manager.getAppWidgetIds(component).forEach { renderWidget(context, manager, it) }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.december172.daytiles.action.TOGGLE_TASK"
        const val EXTRA_TASK_ID = "task_id"
        const val EXTRA_COMPLETED = "task_completed"

        internal fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            CoroutineScope(Dispatchers.IO).launch { renderWidget(context, manager, widgetId) }
        }

        private suspend fun renderWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_todo)
            val height = manager.getAppWidgetOptions(widgetId)
                .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180)
            views.setTextViewTextSize(R.id.todo_widget_title, android.util.TypedValue.COMPLEX_UNIT_SP, if (height < 130) 12f else 15f)
            views.setTextViewText(R.id.todo_widget_title, context.getString(R.string.tasks_heading))
            if (Build.VERSION.SDK_INT >= 31) {
                val tasks = (context.applicationContext as DayTilesApplication).repository
                    .recentOpenTasks(TodoWidgetContent.limitForHeight(height))
                val items = RemoteViews.RemoteCollectionItems.Builder()
                    .setHasStableIds(true)
                    .setViewTypeCount(1)
                if (tasks.isEmpty()) items.addItem(-1, TodoWidgetContent.row(context, null))
                else tasks.forEach { items.addItem(it.id, TodoWidgetContent.row(context, it)) }
                views.setRemoteAdapter(R.id.todo_widget_list, items.build())
            } else {
                setLegacyAdapter(context, views, widgetId)
            }
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
            if (Build.VERSION.SDK_INT < 31) refreshLegacyRows(manager, widgetId)
        }

        // Android 8–11 require the service-backed collection API.
        @Suppress("DEPRECATION")
        private fun setLegacyAdapter(context: Context, views: RemoteViews, widgetId: Int) {
            val service = Intent(context, TodoWidgetService::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                .setData("daytiles://todo-widget/$widgetId".toUri())
            views.setRemoteAdapter(R.id.todo_widget_list, service)
        }

        @Suppress("DEPRECATION") // Paired with setLegacyAdapter, only below API 31.
        private fun refreshLegacyRows(manager: AppWidgetManager, widgetId: Int) {
            manager.notifyAppWidgetViewDataChanged(widgetId, R.id.todo_widget_list)
        }
    }
}
