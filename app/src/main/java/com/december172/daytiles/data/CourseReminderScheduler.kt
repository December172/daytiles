package com.december172.daytiles.data

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.december172.daytiles.data.local.CourseEntity
import com.december172.daytiles.widget.CourseReminderReceiver
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object CourseReminderScheduler {
    fun schedule(context: Context, course: CourseEntity) {
        if (course.reminderMinutes <= 0) return
        val weekday = DayOfWeek.of(course.weekday.coerceIn(1, 7))
        val now = LocalDateTime.now()
        var next = now.with(TemporalAdjusters.nextOrSame(weekday))
            .withHour(course.startMinute / 60).withMinute(course.startMinute % 60).withSecond(0).withNano(0)
            .minusMinutes(course.reminderMinutes.toLong())
        if (!next.isAfter(now)) next = next.plusWeeks(1)
        val triggerAt = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            AlarmManager.INTERVAL_DAY * 7,
            pendingIntent(context, course),
        )
    }

    fun cancel(context: Context, courseId: Long) {
        val intent = Intent(context, CourseReminderReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context, courseId.toInt(), intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        context.getSystemService(AlarmManager::class.java).cancel(pending)
    }

    private fun pendingIntent(context: Context, course: CourseEntity) = PendingIntent.getBroadcast(
        context,
        course.id.toInt(),
        Intent(context, CourseReminderReceiver::class.java)
            .putExtra(CourseReminderReceiver.EXTRA_NAME, course.name)
            .putExtra(CourseReminderReceiver.EXTRA_ID, course.id),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
