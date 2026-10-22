package com.december172.daytiles.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.content.edit

/** Only the explicit PendingIntent created by DayTiles reaches this private receiver. */
class WidgetPinnedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_PINNED) return
        val request = intent.getStringExtra(EXTRA_REQUEST) ?: return
        val provider = intent.getStringExtra(EXTRA_PROVIDER)?.let(ComponentName::unflattenFromString) ?: return
        if (provider.packageName != context.packageName) return
        val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val manager = AppWidgetManager.getInstance(context)
        if (manager.getAppWidgetInfo(id)?.provider != provider) return
        context.getSharedPreferences(RESULTS, Context.MODE_PRIVATE).edit {
            putString(COMPLETED_REQUEST, request)
            putInt(COMPLETED_WIDGET, id)
        }
        context.sendBroadcast(Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).setComponent(provider)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, intArrayOf(id)))
    }

    companion object {
        const val ACTION_PINNED = "com.december172.daytiles.WIDGET_PINNED"
        const val EXTRA_REQUEST = "pin_request"
        const val EXTRA_PROVIDER = "pin_provider"
        const val RESULTS = "widget_pin_results"
        const val COMPLETED_REQUEST = "completed_request"
        const val COMPLETED_WIDGET = "completed_widget"
    }
}
