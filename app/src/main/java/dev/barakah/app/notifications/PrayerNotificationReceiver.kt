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
        if (scheduledTime != 0L && kotlin.math.abs(now - scheduledTime) > 30 * 60 * 1000) { // 30 minutes threshold
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
        
        val appPrefs = context.getSharedPreferences("barakah_prefs", Context.MODE_PRIVATE)
        val isAr = appPrefs.getString("app_lang", "en") == "ar"

        var title: String
        var text: String
        if (prayerName == "Morning Adhkar") {
            title = if (isAr) "أذكار الصباح" else "Morning Adhkar"
            text = if (isAr) "ابدأ يومك بالذكر" else "Start your day with remembrance."
        } else if (prayerName == "Evening Adhkar") {
            title = if (isAr) "أذكار المساء" else "Evening Adhkar"
            text = if (isAr) "اختم يومك بالذكر" else "End your day with remembrance."
        } else {
            title = if (isAr) "مواقيت الصلاة" else "Prayer Time"
            val dispName = if (isAr) {
                when (prayerName) {
                    "Fajr" -> "الفجر"
                    "Dhuhr" -> "الظهر"
                    "Asr" -> "العصر"
                    "Maghrib" -> "المغرب"
                    "Isha" -> "العشاء"
                    "Sunrise" -> "الشروق"
                    "Duha (Nafilah)" -> "الضحى"
                    "Witr (Nafilah)" -> "الوتر"
                    "Tahajjud (Nafilah)" -> "التهجد"
                    "Qiyam-ul-Layl (Nafilah)" -> "قيام الليل"
                    else -> prayerName
                }
            } else {
                prayerName
            }
            text = if (isAr) "صلاة $dispName" else "$dispName prayer."
        }

        // Use RLM (Right-to-Left Mark) to force correct layout direction rendering in standard system templates,
        // and append a trailing space for title and a newline with zero-width joiner for description text
        // to prevent glyph/baseline cutting off/clipping issues at different system font scales.
        val displayTitle = if (isAr) "\u200F" + title + " " else title
        val displayText = if (isAr) "\u200F" + text + "\n\u200D" else text

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(displayTitle)
            .setContentText(displayText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            
        notificationManager.notify(prayerId, builder.build())
        
        // Reschedule for next days
        PrayerScheduler.scheduleNextPrayers(context)
    }
}
