package dev.rishabh.dailytracker.feature.sleep

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat

/**
 * The ringing alarm: a foreground service that owns the ringtone and pushes the full-screen
 * mission over the lockscreen.
 *
 * It plays on the alarm stream forced to max, holds a wake lock, and posts a full-screen
 * intent so the mission activity shows even on a locked, dozing phone. Nothing here can end
 * the alarm — only solving the mission ([AlarmActivity]) stops the service.
 */
class SleepAlarmService : Service() {

    private var player: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val sessionId = intent?.getStringExtra(SleepAlarmReceiver.EXTRA_SESSION_ID)
        startForeground(NOTIFICATION_ID, buildNotification(sessionId), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        acquireWakeLock()
        startRingtone()
        // START_STICKY so the OS restarts the ring if it kills us before the user wakes.
        return START_STICKY
    }

    override fun onDestroy() {
        stopRingtone()
        releaseWakeLock()
        super.onDestroy()
    }

    private fun buildNotification(sessionId: String?): android.app.Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Wake alarm", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "The full-screen wake-up alarm."
                setSound(null, null) // the service plays the ringtone itself
                enableVibration(true)
            },
        )
        val fullScreen = PendingIntent.getActivity(
            this,
            0,
            Intent(this, AlarmActivity::class.java).apply {
                putExtra(SleepAlarmReceiver.EXTRA_SESSION_ID, sessionId)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Wake up")
            .setContentText("Solve the mission to dismiss")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreen, true)
            .setContentIntent(fullScreen)
            .build()
    }

    private fun startRingtone() {
        val audio = getSystemService(AudioManager::class.java)
        audio.setStreamVolume(
            AudioManager.STREAM_ALARM,
            audio.getStreamMaxVolume(AudioManager.STREAM_ALARM),
            0,
        )
        val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)
            ?: return
        player = MediaPlayer().apply {
            setDataSource(this@SleepAlarmService, uri)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            isLooping = true
            prepare()
            start()
        }
    }

    private fun stopRingtone() {
        runCatching { player?.stop() }
        player?.release()
        player = null
    }

    private fun acquireWakeLock() {
        val power = getSystemService(PowerManager::class.java)
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "dailytracker:wake_alarm").apply {
            acquire(WAKE_LOCK_TIMEOUT_MS)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    companion object {
        private const val CHANNEL_ID = "wake_alarm"
        private const val NOTIFICATION_ID = 9101
        private const val ACTION_STOP = "dev.rishabh.dailytracker.action.STOP_WAKE_ALARM"
        private const val WAKE_LOCK_TIMEOUT_MS = 10L * 60 * 1000

        /** Solving the mission calls this to silence the alarm. */
        fun stop(context: Context) {
            context.startService(Intent(context, SleepAlarmService::class.java).setAction(ACTION_STOP))
        }
    }
}
