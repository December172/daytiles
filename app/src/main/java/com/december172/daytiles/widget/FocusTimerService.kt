package com.december172.daytiles.widget

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.edit
import com.december172.daytiles.MainActivity
import com.december172.daytiles.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FocusTimerService : Service() {
    private val prefs by lazy { getSharedPreferences(PREFERENCES, MODE_PRIVATE) }
    private val scope = CoroutineScope(Dispatchers.Main)
    private var ticker: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        ensureChannel()
        when (intent?.action) {
            ACTION_START -> {
                val work = intent.getLongExtra(EXTRA_WORK, 25 * 60_000L)
                val rest = intent.getLongExtra(EXTRA_BREAK, 5 * 60_000L)
                prefs.edit {
                    putString(KEY_PHASE, PHASE_WORK)
                    putLong(KEY_BREAK, rest)
                    putLong(KEY_END, System.currentTimeMillis() + work)
                    putString(KEY_TASK, intent.getStringExtra(EXTRA_TASK_TITLE))
                    putLong(KEY_TASK_ID, intent.getLongExtra(EXTRA_TASK_ID, 0L))
                    remove(KEY_REMAINING)
                }
                startForegroundCompat(notification(work, false))
                refreshWidgets()
                tick()
            }
            ACTION_PAUSE -> {
                val remaining = (prefs.getLong(KEY_END, 0L) - System.currentTimeMillis()).coerceAtLeast(0)
                prefs.edit {
                    putLong(KEY_REMAINING, remaining)
                    remove(KEY_END)
                }
                ticker?.cancel()
                updateNotification(notification(remaining, true))
                refreshWidgets()
            }
            ACTION_RESUME -> {
                val remaining = prefs.getLong(KEY_REMAINING, 0L)
                prefs.edit {
                    putLong(KEY_END, System.currentTimeMillis() + remaining)
                    remove(KEY_REMAINING)
                }
                updateNotification(notification(remaining, false))
                refreshWidgets()
                tick()
            }
            ACTION_STOP -> {
                ticker?.cancel()
                prefs.edit {
                    remove(KEY_END)
                    remove(KEY_REMAINING)
                    remove(KEY_PHASE)
                    remove(KEY_TASK)
                    remove(KEY_TASK_ID)
                }
                stopForeground(STOP_FOREGROUND_REMOVE)
                refreshWidgets()
                stopSelf()
            }
            else -> {
                val remaining = (prefs.getLong(KEY_END, 0L) - System.currentTimeMillis()).coerceAtLeast(0)
                if (remaining > 0) {
                    startForegroundCompat(notification(remaining, false))
                    tick()
                } else stopSelf()
            }
        }
        return START_STICKY
    }

    private fun tick() {
        ticker?.cancel()
        ticker = scope.launch {
            while (true) {
                val remaining = (prefs.getLong(KEY_END, 0L) - System.currentTimeMillis()).coerceAtLeast(0)
                if (remaining == 0L) {
                    if (prefs.getString(KEY_PHASE, PHASE_WORK) == PHASE_WORK && prefs.getLong(KEY_BREAK, 0L) > 0) {
                        val breakDuration = prefs.getLong(KEY_BREAK, 0L)
                        prefs.edit {
                            putString(KEY_PHASE, PHASE_BREAK)
                            putLong(KEY_END, System.currentTimeMillis() + breakDuration)
                        }
                        updateNotification(notification(breakDuration, false))
                        refreshWidgets()
                    } else {
                        prefs.edit {
                            remove(KEY_END)
                            remove(KEY_PHASE)
                        }
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        refreshWidgets()
                        stopSelf()
                        return@launch
                    }
                } else updateNotification(notification(remaining, false))
                if (remaining % 60_000L < 1_000L) refreshWidgets()
                delay(1_000)
            }
        }
    }

    private fun notification(remaining: Long, paused: Boolean) =
        android.app.Notification.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(prefs.getString(KEY_TASK, null)?.let { getString(R.string.focus_linked_task, it) }
                ?: getString(if (prefs.getString(KEY_PHASE, PHASE_WORK) == PHASE_BREAK) R.string.focus_break else R.string.focus_title))
            .setContentText(if (paused) getString(R.string.focus_paused) else "%02d:%02d".format(remaining / 60_000, remaining / 1000 % 60))
            .setContentIntent(PendingIntent.getActivity(this, 31, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            .setOngoing(true)
            .addAction(
                android.app.Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(this, R.drawable.ic_launcher),
                    getString(R.string.focus_stop),
                    serviceIntent(ACTION_STOP),
                ).build(),
            )
            .build()

    private fun serviceIntent(action: String) = PendingIntent.getForegroundService(
        this, action.hashCode(), Intent(this, FocusTimerService::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun ensureChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, getString(R.string.focus_title), NotificationManager.IMPORTANCE_LOW),
        )
    }

    private fun startForegroundCompat(notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= 34) startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(NOTIFICATION_ID, notification)
    }

    private fun updateNotification(notification: android.app.Notification) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    private fun refreshWidgets() {
        val manager = getSystemService(android.appwidget.AppWidgetManager::class.java)
        val ids = manager.getAppWidgetIds(android.content.ComponentName(this, FocusWidgetProvider::class.java))
        ids.forEach { FocusWidgetProvider.updateWidget(this, manager, it) }
    }

    companion object {
        const val ACTION_START = "com.december172.daytiles.focus.START"
        const val ACTION_PAUSE = "com.december172.daytiles.focus.PAUSE"
        const val ACTION_RESUME = "com.december172.daytiles.focus.RESUME"
        const val ACTION_STOP = "com.december172.daytiles.focus.STOP"
        const val EXTRA_WORK = "work_ms"
        const val EXTRA_BREAK = "break_ms"
        const val EXTRA_TASK_TITLE = "task_title"
        const val EXTRA_TASK_ID = "task_id"
        const val KEY_TASK = "linked_task"
        const val KEY_TASK_ID = "linked_task_id"
        const val PREFERENCES = "focus_clock"
        const val KEY_END = "end_at"
        const val KEY_REMAINING = "remaining"
        const val KEY_BREAK = "break_ms"
        const val KEY_PHASE = "phase"
        const val PHASE_WORK = "work"
        const val PHASE_BREAK = "break"
        private const val CHANNEL = "focus_clock"
        private const val NOTIFICATION_ID = 4101
    }
}
