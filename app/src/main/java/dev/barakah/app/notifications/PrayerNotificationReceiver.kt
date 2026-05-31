package dev.barakah.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import dev.barakah.app.MainActivity
import dev.barakah.app.R
import dev.barakah.app.notifications.PrayerScheduler

class PrayerNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "Prayer"
        val prayerId = intent.getIntExtra("PRAYER_ID", prayerName.hashCode())
        val scheduledTime = intent.getLongExtra("SCHEDULED_TIME", 0L)
        
        // Prevent duplicate firing by checking if we already notified for this exact scheduled time
        val prefs = context.getSharedPreferences("notif_history", Context.MODE_PRIVATE)
        val lastNotifiedTime = prefs.getLong("last_notified_$prayerId", 0L)
        
        // If scheduledTime is provided and matches last notified, skip to avoid duplicates
        if (scheduledTime != 0L && scheduledTime == lastNotifiedTime) {
            return
        }
        
        // Also skip if current time is way off from scheduled time (e.g. system re-running old alarms)
        val now = System.currentTimeMillis()
        if (scheduledTime != 0L && Math.abs(now - scheduledTime) > 30 * 60 * 1000) { // 30 minutes threshold
            return
        }

        // Persist notified time
        prefs.edit().putLong("last_notified_$prayerId", scheduledTime).apply()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val channelId = "prayer_channel"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Prayer Times", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        
        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, activityIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Prayer Time")
            .setContentText("It's time for $prayerName prayer.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            
        notificationManager.notify(prayerId, builder.build())
        
        // Play the standard DEFAULT notification sound
        try {
            val notificationUri = android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
            val r = android.media.RingtoneManager.getRingtone(context, notificationUri)
            r.play()
        } catch (e: Exception) {}
        
        // Reschedule for next days
        PrayerScheduler.scheduleNextPrayers(context)
    }
}
