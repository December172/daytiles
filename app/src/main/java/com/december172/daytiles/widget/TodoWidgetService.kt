package com.december172.daytiles.widget

import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.december172.daytiles.DayTilesApplication
import com.december172.daytiles.R
import kotlinx.coroutines.runBlocking

class TodoWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory = TodoViewsFactory(this)
}

private class TodoViewsFactory(
    private val service: TodoWidgetService,
) : RemoteViewsService.RemoteViewsFactory {
    private var rows = emptyList<com.december172.daytiles.data.local.TaskEntity>()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        rows = runBlocking {
            (service.application as DayTilesApplication).repository.allOpenTasks()
        }
    }

    override fun onDestroy() {
        rows = emptyList()
    }

    override fun getCount(): Int = if (rows.isEmpty()) 1 else rows.size

    override fun getViewAt(position: Int): RemoteViews {
        if (rows.isEmpty()) {
            return RemoteViews(service.packageName, R.layout.widget_todo_row).apply {
                setViewVisibility(R.id.todo_row_checkbox, android.view.View.GONE)
                setTextViewText(R.id.todo_row_title, service.getString(R.string.widget_no_tasks))
                setTextViewText(R.id.todo_row_meta, "")
            }
        }
        val task = rows[position]
        return RemoteViews(service.packageName, R.layout.widget_todo_row).apply {
            setTextViewText(R.id.todo_row_checkbox, "○")
            setContentDescription(
                R.id.todo_row_root,
                service.getString(R.string.todo_checkbox_accessibility, task.title),
            )
            setTextViewText(R.id.todo_row_title, task.title)
            setTextViewText(R.id.todo_row_meta, DayTilesWidgetProvider.taskSummary(service, task))
            setOnClickFillInIntent(
                R.id.todo_row_root,
                Intent()
                    .putExtra(TodoWidgetProvider.EXTRA_TASK_ID, task.id)
                    .putExtra(TodoWidgetProvider.EXTRA_COMPLETED, true),
            )
        }
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = rows.getOrNull(position)?.id ?: -1
    override fun hasStableIds(): Boolean = true
}
