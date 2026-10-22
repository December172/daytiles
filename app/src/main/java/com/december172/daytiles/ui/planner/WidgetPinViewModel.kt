package com.december172.daytiles.ui.planner

import android.app.Application
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.december172.daytiles.R
import com.december172.daytiles.widget.WidgetPinnedReceiver
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class WidgetPinStatus { IDLE, WAITING, ADDED, UNSUPPORTED, FAILED, UNCONFIRMED }

data class WidgetPinState(
    val status: WidgetPinStatus = WidgetPinStatus.IDLE,
    val provider: ComponentName? = null,
    val name: Int = R.string.widgets_tab,
    val showHelp: Boolean = false,
)

class WidgetPinViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val manager = AppWidgetManager.getInstance(app)
    private val results = app.getSharedPreferences(WidgetPinnedReceiver.RESULTS, Context.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(WidgetPinState())
    val state = mutableState.asStateFlow()
    private var requestToken: String? = null
    private var timeout: Job? = null
    private var helpOffered = false
    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == WidgetPinnedReceiver.COMPLETED_REQUEST) confirmAdded()
    }

    init { results.registerOnSharedPreferenceChangeListener(listener) }

    fun request(provider: ComponentName, name: Int) {
        timeout?.cancel()
        helpOffered = false
        val token = UUID.randomUUID().toString()
        requestToken = token
        mutableState.value = WidgetPinState(WidgetPinStatus.WAITING, provider, name)
        val accepted = try {
            if (!manager.isRequestPinAppWidgetSupported) false else {
                val intent = Intent(app, WidgetPinnedReceiver::class.java)
                    .setAction(WidgetPinnedReceiver.ACTION_PINNED)
                    .setData("daytiles://widget-pin/$token".toUri())
                    .putExtra(WidgetPinnedReceiver.EXTRA_REQUEST, token)
                    .putExtra(WidgetPinnedReceiver.EXTRA_PROVIDER, provider.flattenToString())
                // The launcher fills in EXTRA_APPWIDGET_ID. The intent has an explicit private target.
                val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
                val callback = PendingIntent.getBroadcast(app, 0, intent, flags)
                manager.requestPinAppWidget(provider, null, callback)
            }
        } catch (_: IllegalStateException) {
            mutableState.value = mutableState.value.copy(status = WidgetPinStatus.FAILED, showHelp = true)
            return
        } catch (_: SecurityException) {
            mutableState.value = mutableState.value.copy(status = WidgetPinStatus.FAILED, showHelp = true)
            return
        }
        if (!accepted) {
            mutableState.value = mutableState.value.copy(status = WidgetPinStatus.UNSUPPORTED, showHelp = true)
        } else {
            timeout = viewModelScope.launch {
                delay(8_000)
                if (mutableState.value.status == WidgetPinStatus.WAITING) {
                    mutableState.value = mutableState.value.copy(status = WidgetPinStatus.UNCONFIRMED)
                }
            }
        }
    }

    private fun confirmAdded() {
        if (requestToken == null || results.getString(WidgetPinnedReceiver.COMPLETED_REQUEST, null) != requestToken) return
        val id = results.getInt(WidgetPinnedReceiver.COMPLETED_WIDGET, AppWidgetManager.INVALID_APPWIDGET_ID)
        if (manager.getAppWidgetInfo(id)?.provider != mutableState.value.provider) return
        timeout?.cancel()
        mutableState.value = mutableState.value.copy(status = WidgetPinStatus.ADDED, showHelp = false)
    }

    fun retry() { state.value.provider?.let { request(it, state.value.name) } }
    fun offerUnconfirmedHelp() {
        if (state.value.status == WidgetPinStatus.UNCONFIRMED && !helpOffered) {
            helpOffered = true
            showHelp()
        }
    }
    fun showHelp() { mutableState.value = mutableState.value.copy(showHelp = true) }
    fun dismissHelp() { mutableState.value = mutableState.value.copy(showHelp = false) }

    override fun onCleared() {
        results.unregisterOnSharedPreferenceChangeListener(listener)
    }
}
