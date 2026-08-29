package dev.rishabh.dailytracker.feature.sleep

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules the wake alarm with the OS alarm-clock API.
 *
 * [AlarmManager.setAlarmClock] is the strongest guarantee a normal app has: it survives Doze
 * and shows in the system's next-alarm slot. There is at most one wake alarm, so a fixed
 * request code means a new bedtime replaces the previous alarm rather than stacking.
 */
@Singleton
open class SleepAlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    open fun schedule(sessionId: String, triggerAtMillis: Long) {
        val info = AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent())
        alarmManager.setAlarmClock(info, alarmIntent(sessionId))
    }

    open fun cancel(sessionId: String) {
        alarmManager.cancel(alarmIntent(sessionId))
    }

    /** Fires [SleepAlarmReceiver] at the wake time. */
    private fun alarmIntent(sessionId: String): PendingIntent {
        val intent = Intent(context, SleepAlarmReceiver::class.java).apply {
            action = SleepAlarmReceiver.ACTION_ALARM
            putExtra(SleepAlarmReceiver.EXTRA_SESSION_ID, sessionId)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_ALARM,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    /** Where the system's alarm icon leads if tapped before it fires — just opens the app. */
    private fun showIntent(): PendingIntent {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        return PendingIntent.getActivity(
            context,
            REQUEST_SHOW,
            launch,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private companion object {
        const val REQUEST_ALARM = 7001
        const val REQUEST_SHOW = 7002
    }
}
