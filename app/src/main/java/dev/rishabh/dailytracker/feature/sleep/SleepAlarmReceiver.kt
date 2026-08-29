package dev.rishabh.dailytracker.feature.sleep

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Receives the exact alarm and hands off to the foreground service.
 *
 * A setAlarmClock alarm briefly allowlists the app, which is what lets this start a
 * foreground service from the background on Android 14. The receiver does no work itself —
 * the service owns the ringtone and the full-screen intent.
 */
class SleepAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: return
        val service = Intent(context, SleepAlarmService::class.java).apply {
            putExtra(EXTRA_SESSION_ID, sessionId)
        }
        context.startForegroundService(service)
    }

    companion object {
        const val ACTION_ALARM = "dev.rishabh.dailytracker.action.WAKE_ALARM"
        const val EXTRA_SESSION_ID = "session_id"
    }
}
