package com.december172.daytiles.widget

import android.content.Intent
import android.content.Context
import android.appwidget.AppWidgetManager
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.december172.daytiles.DayTilesApplication
import com.december172.daytiles.R
import com.december172.daytiles.data.local.TaskEntity
import kotlinx.coroutines.runBlocking

class TodoWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        TodoViewsFactory(this, intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID))
}

private class TodoViewsFactory(
    private val service: TodoWidgetService,
    private val widgetId: Int,
) : RemoteViewsService.RemoteViewsFactory {
    private var rows = emptyList<TaskEntity>()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        rows = runBlocking {
            val height = AppWidgetManager.getInstance(service).getAppWidgetOptions(widgetId)
                .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 180)
            (service.application as DayTilesApplication).repository.recentOpenTasks(TodoWidgetContent.limitForHeight(height))
        }
    }

    override fun onDestroy() {
        rows = emptyList()
    }

    override fun getCount(): Int = if (rows.isEmpty()) 1 else rows.size

    override fun getViewAt(position: Int): RemoteViews = TodoWidgetContent.row(service, rows.getOrNull(position))

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = rows.getOrNull(position)?.id ?: -1
    override fun hasStableIds(): Boolean = true
}

/** Shared rows keep the API 31 collection and the older service adapter identical. */
internal object TodoWidgetContent {
    fun limitForHeight(height: Int): Int = when {
        height < 130 -> 2
        height < 220 -> 5
        else -> 1000
    }

    fun row(context: Context, task: TaskEntity?): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_todo_row).apply {
            setViewVisibility(R.id.todo_row_checkbox, if (task == null) View.GONE else View.VISIBLE)
            setTextViewText(R.id.todo_row_title, task?.title ?: context.getString(R.string.widget_no_tasks))
            setTextViewText(R.id.todo_row_meta, task?.let { DayTilesWidgetProvider.taskSummary(context, it) }.orEmpty())
            setContentDescription(R.id.todo_row_root, if (task == null) context.getString(R.string.widget_no_tasks)
                else context.getString(R.string.todo_checkbox_accessibility, task.title))
            val click = Intent()
            if (task != null) click.putExtra(TodoWidgetProvider.EXTRA_TASK_ID, task.id)
                .putExtra(TodoWidgetProvider.EXTRA_COMPLETED, true)
            setOnClickFillInIntent(R.id.todo_row_root, click)
        }
}
