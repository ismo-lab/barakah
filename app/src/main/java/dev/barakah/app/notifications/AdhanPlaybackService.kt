package dev.barakah.app.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import dev.barakah.app.MainActivity
import dev.barakah.app.R

class AdhanPlaybackService : Service() {
    companion object {
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val ACTION_STOP = "dev.barakah.app.action.STOP_ADHAN"
        private const val CHANNEL_ID = "adhan_playback"
        private const val NOTIFICATION_ID = 2101
        private const val SHORT_PLAYBACK_MS = 15_000L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var player: MediaPlayer? = null
    private val stopShortPlayback = Runnable { stopPlayback() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopPlayback()
            return START_NOT_STICKY
        }

        val prayerName = intent?.getStringExtra(EXTRA_PRAYER_NAME) ?: "Dhuhr"
        if (prayerName !in setOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val prefs = getSharedPreferences("barakah_prefs", MODE_PRIVATE)
        if (!prefs.getBoolean("adhan_audio_enabled", false)) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        createNotificationChannel()
        val isAr = prefs.getString("app_lang", "ar") == "ar"
        try {
            startForeground(NOTIFICATION_ID, createNotification(prayerName, isAr))
            releasePlayer()
            val audioResource = if (prayerName == "Fajr") R.raw.adhan_fajr else R.raw.adhan_regular
            val newPlayer = MediaPlayer()
            player = newPlayer
            newPlayer.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
            // Which system volume slider controls the Adhan. "notification" (default) routes to
            // STREAM_NOTIFICATION, so the Adhan is suppressed by silent mode and Do Not Disturb.
            // "alarm" routes to STREAM_ALARM, which plays through both.
            val useAlarmStream = prefs.getString("adhan_audio_stream", "notification") == "alarm"
            newPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(
                        if (useAlarmStream) AudioAttributes.USAGE_ALARM
                        else AudioAttributes.USAGE_NOTIFICATION_EVENT
                    )
                    .setContentType(
                        if (useAlarmStream) AudioAttributes.CONTENT_TYPE_MUSIC
                        else AudioAttributes.CONTENT_TYPE_SONIFICATION
                    )
                    .build()
            )
            resources.openRawResourceFd(audioResource).use { descriptor ->
                newPlayer.setDataSource(
                    descriptor.fileDescriptor,
                    descriptor.startOffset,
                    descriptor.length
                )
            }
            newPlayer.prepare()
            newPlayer.setOnCompletionListener { stopPlayback() }
            newPlayer.start()

            if (prefs.getString("adhan_audio_mode", "short") != "full") {
                handler.postDelayed(stopShortPlayback, SHORT_PLAYBACK_MS)
            }
        } catch (error: Exception) {
            android.util.Log.e("AdhanPlaybackService", "Adhan playback failed", error)
            stopPlayback()
        }

        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                if (getSharedPreferences("barakah_prefs", MODE_PRIVATE).getString("app_lang", "ar") == "ar")
                    "تشغيل الأذان"
                else
                    "Adhan playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setSound(null, null)
                description = "Shows while the Adhan audio is playing"
            }
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(prayerName: String, isAr: Boolean): Notification {
        val stopIntent = PendingIntent.getService(
            this,
            NOTIFICATION_ID,
            Intent(this, AdhanPlaybackService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openAppIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val prayerLabel = if (isAr) {
            when (prayerName) {
                "Fajr" -> "الفجر"
                "Dhuhr" -> "الظهر"
                "Asr" -> "العصر"
                "Maghrib" -> "المغرب"
                else -> "العشاء"
            }
        } else {
            prayerName
        }
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(if (isAr) "أذان صلاة $prayerLabel" else "$prayerLabel Adhan")
            .setContentText(if (isAr) "يُشغّل الأذان" else "Adhan is playing")
            .setContentIntent(openAppIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setSilent(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                if (isAr) "إيقاف" else "Stop",
                stopIntent
            )
            .build()
    }

    private fun releasePlayer() {
        handler.removeCallbacks(stopShortPlayback)
        player?.runCatching {
            if (isPlaying) stop()
            release()
        }
        player = null
    }

    private fun stopPlayback() {
        releasePlayer()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    override fun onDestroy() {
        releasePlayer()
        super.onDestroy()
    }
}
