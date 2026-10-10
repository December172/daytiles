package com.december172.daytiles.widget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.december172.daytiles.R
import com.december172.daytiles.DayTilesApplication
import com.december172.daytiles.data.TaskReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val app = context.applicationContext as DayTilesApplication
                    app.repository.allOpenTasks().filter { it.reminderAt != null }.forEach {
                        TaskReminderScheduler.schedule(context, it)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, context.getString(R.string.task_reminders), NotificationManager.IMPORTANCE_DEFAULT),
            )
        }
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            android.app.Notification.Builder(context, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            android.app.Notification.Builder(context)
        }
        manager.notify(
            System.currentTimeMillis().toInt(),
            notification
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(context.getString(R.string.task_reminder_title))
                .setContentText(intent.getStringExtra(EXTRA_TITLE).orEmpty())
                .setAutoCancel(true)
                .build(),
        )
    }

    companion object {
        const val EXTRA_TITLE = "task_title"
        private const val CHANNEL_ID = "task_reminders"
    }
}
