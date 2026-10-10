package com.december172.daytiles.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.december172.daytiles.data.local.TaskEntity
import com.december172.daytiles.widget.TaskReminderReceiver

object TaskReminderScheduler {
    fun schedule(context: Context, task: TaskEntity) {
        val reminderAt = task.reminderAt ?: return
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val alarm = pendingIntent(context, task.id, task.title)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminderAt, alarm)
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, reminderAt, alarm)
        }
    }

    fun cancel(context: Context, taskId: Long) {
        context.getSystemService(AlarmManager::class.java)
            .cancel(pendingIntent(context, taskId, ""))
    }

    private fun pendingIntent(context: Context, taskId: Long, title: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            Intent(context, TaskReminderReceiver::class.java)
                .putExtra(TaskReminderReceiver.EXTRA_TITLE, title),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
