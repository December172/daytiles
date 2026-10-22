package com.december172.daytiles.widget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.december172.daytiles.R
import com.december172.daytiles.DayTilesApplication
import com.december172.daytiles.data.CourseReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

class CourseReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val app = context.applicationContext as DayTilesApplication
                    app.repository.observeCourses().first().forEach { CourseReminderScheduler.schedule(context, it) }
                } finally { pending.finish() }
            }
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, context.getString(R.string.courses_title), NotificationManager.IMPORTANCE_DEFAULT),
        )
        val builder = android.app.Notification.Builder(context, CHANNEL)
        manager.notify(
            intent.getLongExtra(EXTRA_ID, 0L).toInt(),
            builder.setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle(context.getString(R.string.course_reminder_title))
                .setContentText(intent.getStringExtra(EXTRA_NAME).orEmpty())
                .setAutoCancel(true).build(),
        )
    }

    companion object {
        const val EXTRA_NAME = "course_name"
        const val EXTRA_ID = "course_id"
        private const val CHANNEL = "course_reminders"
    }
}
