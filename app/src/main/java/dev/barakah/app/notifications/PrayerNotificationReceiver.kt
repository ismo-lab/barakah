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
import dev.barakah.app.data.AppPrefs
import dev.barakah.app.data.NotifHistoryPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PrayerNotificationReceiver : BroadcastReceiver() {
    companion object {
        const val ID_PRAYER = 1000
        const val ID_ADHKAR = 1001
        const val ID_DAILY = 1002

        private const val PRAYER_CHANNEL_ID = "prayer_channel"
        private const val ADHAN_CHANNEL_ID = "adhan_channel"
        private val PRIMARY_PRAYERS = setOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
    }

    override fun onReceive(context: Context, intent: Intent) {
        // Posting the notification itself is quick, but the rescheduling that follows it must not be
        // abandoned when this receiver returns: the process is eligible to be killed the moment
        // onReceive() does. goAsync() hands the work back to the system and keeps the process alive
        // until finish() is called.
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                postForPrayerNotification(appContext, intent)
            } catch (error: Exception) {
                android.util.Log.e("PrayerNotifReceiver", "Failed to handle prayer notification", error)
            } finally {
                try {
                    PrayerScheduler.scheduleNextPrayersAndWait(appContext)
                } catch (error: Exception) {
                    android.util.Log.e("PrayerNotifReceiver", "Failed to reschedule prayer alarms", error)
                }
                pendingResult.finish()
            }
        }
    }

    private fun postForPrayerNotification(context: Context, intent: Intent) {
        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: "Prayer"
        val prayerId = intent.getIntExtra("PRAYER_ID", prayerName.hashCode())
        val scheduledTime = intent.getLongExtra("SCHEDULED_TIME", 0L)
        
        // Prevent duplicate firing by checking if we already notified for this exact scheduled time
        val prefs = context.getSharedPreferences(NotifHistoryPrefs.FILE, Context.MODE_PRIVATE)
        val lastNotifiedTime = prefs.getLong(NotifHistoryPrefs.lastNotified(prayerId), 0L)
        val lastNotifiedTs = prefs.getLong(NotifHistoryPrefs.lastNotifiedAt(prayerId), 0L)
        val now = System.currentTimeMillis()
        
        // If scheduledTime is provided and matches last notified, skip to avoid duplicates
        if (scheduledTime != 0L && scheduledTime == lastNotifiedTime) {
            return
        }

        // Prevent rapid duplicate notifications for the same prayer ID within 2 minutes
        if (now - lastNotifiedTs < 2 * 60 * 1000L) {
            return
        }
        
        // Also skip if current time is way off from scheduled time (e.g. system re-running old alarms)
        if (scheduledTime != 0L && kotlin.math.abs(now - scheduledTime) > 30 * 60 * 1000L) { // 30 minutes threshold
            return
        }

        // Persist notified time and timestamp
        prefs.edit()
            .putLong(NotifHistoryPrefs.lastNotified(prayerId), scheduledTime)
            .putLong(NotifHistoryPrefs.lastNotifiedAt(prayerId), now)
            .apply()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val appPrefs = context.getSharedPreferences(AppPrefs.FILE, Context.MODE_PRIVATE)
        val isAr = appPrefs.getString(AppPrefs.APP_LANG, "ar") == "ar"

        // Only the five prayers play the Adhan, and only when adhan audio is enabled.
        val playsAdhan = prayerName in PRIMARY_PRAYERS &&
            appPrefs.getBoolean(AppPrefs.ADHAN_AUDIO_ENABLED, false)

        if (playsAdhan) {
            val playbackIntent = Intent(context, AdhanPlaybackService::class.java).apply {
                putExtra(AdhanPlaybackService.EXTRA_PRAYER_NAME, prayerName)
            }
            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(playbackIntent)
                } else {
                    context.startService(playbackIntent)
                }
            } catch (error: Exception) {
                android.util.Log.e("PrayerNotifReceiver", "Could not start Adhan playback", error)
            }
        }

        // A channel owns its sound on Android 8+, so the prayers that play the Adhan are posted to a
        // second, silent channel: the Adhan is the alert, and the notification must not add a tone on
        // top of it. Every other reminder keeps the audible channel.
        val channelId = if (playsAdhan) ADHAN_CHANNEL_ID else PRAYER_CHANNEL_ID
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channelName = if (isAr) "مواقيت الصلاة والذكر" else "Prayer Times & Remembrance"
            notificationManager.createNotificationChannel(
                NotificationChannel(PRAYER_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH)
            )

            val adhanChannelName = if (isAr) "الأذان" else "Adhan"
            notificationManager.createNotificationChannel(
                NotificationChannel(ADHAN_CHANNEL_ID, adhanChannelName, NotificationManager.IMPORTANCE_HIGH)
                    .apply { setSound(null, null) }
            )
        }
        
        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, activityIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (prayerName == "Daily Support Reminder") {
            val notifyOccasions = appPrefs.getBoolean(AppPrefs.NOTIFY_OCCASIONS, true)
            val notifyFasting = appPrefs.getBoolean(AppPrefs.NOTIFY_FASTING, true)
            
            if (notifyFasting) {
                val tomorrow = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, 1) }
                val dayOfWeek = tomorrow.get(java.util.Calendar.DAY_OF_WEEK)
                if (dayOfWeek == java.util.Calendar.MONDAY || dayOfWeek == java.util.Calendar.THURSDAY) {
                    val title = if (isAr) "تذكير بصيام السنة" else "Sunnah Fasting Reminder"
                    val dayStr = if (dayOfWeek == java.util.Calendar.MONDAY) {
                        if (isAr) "الإثنين" else "Monday"
                    } else {
                        if (isAr) "الخميس" else "Thursday"
                    }
                    val text = if (isAr) "تذكير: غداً هو يوم $dayStr، فمن استطاع الصوم فليصم طمعاً في الأجر." 
                               else "Reminder: Tomorrow is $dayStr. If you are able to fast, earn the reward of this Sunnah."
                    
                    val builder = NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        
                    try {
                        notificationManager.notify(ID_DAILY, builder.build())
                    } catch (e: Exception) {
                        android.util.Log.e("PrayerNotifReceiver", "Failed to post daily notification: ${e.message}")
                    }
                }
            }
            
            if (notifyOccasions) {
                val tomorrowHijri = dev.barakah.app.util.HijriCalendarHelper.getTomorrowHijriDate()
                val hDay = tomorrowHijri.day
                val hMonth = tomorrowHijri.month
                
                var occasionName: String? = null
                var occasionNameAr: String? = null
                
                when {
                    hDay == 1 && hMonth == 1 -> {
                        occasionName = "Hijri New Year"
                        occasionNameAr = "رأس السنة الهجرية"
                    }
                    hDay == 10 && hMonth == 1 -> {
                        occasionName = "Day of Ashura"
                        occasionNameAr = "يوم عاشوراء"
                    }
                    hDay == 12 && hMonth == 3 -> {
                        occasionName = "Mawlid al-Nabi"
                        occasionNameAr = "المولد النبوي الشريف"
                    }
                    hDay == 27 && hMonth == 7 -> {
                        occasionName = "Isra' and Mi'raj"
                        occasionNameAr = "ليلة الإسراء والمعراج"
                    }
                    hDay == 1 && hMonth == 9 -> {
                        occasionName = "Ramadan First Day"
                        occasionNameAr = "بداية شهر رمضان المبارك"
                    }
                    hDay == 15 && hMonth == 8 -> {
                        occasionName = "Mid-Sha'ban Night"
                        occasionNameAr = "ليلة النصف من شعبان"
                    }
                    hDay == 26 && hMonth == 9 -> {
                        occasionName = "Laylat al-Qadr (27th Night)"
                        occasionNameAr = "ليلة القدر (تحري ليلة ٢٧)"
                    }
                    hDay == 1 && hMonth == 10 -> {
                        occasionName = "Eid al-Fitr"
                        occasionNameAr = "عيد الفطر المبارك"
                    }
                    hDay == 1 && hMonth == 11 -> {
                        occasionName = "Start of Hajj Season"
                        occasionNameAr = "بداية أشهر الحج المباركة"
                    }
                    hDay == 9 && hMonth == 12 -> {
                        occasionName = "Day of Arafah"
                        occasionNameAr = "يوم عرفة"
                    }
                    hDay == 10 && hMonth == 12 -> {
                        occasionName = "Eid al-Adha"
                        occasionNameAr = "عيد الأضحى المبارك"
                    }
                }
                
                if (occasionName != null && occasionNameAr != null) {
                    val title = if (isAr) "تذكير بالمناسبة الإسلامية" else "Islamic Occasion Reminder"
                    val text = if (isAr) "تذكير: غداً هو $occasionNameAr. نسأل الله لنا ولكم القبول والبركة."
                               else "Reminder: Tomorrow is $occasionName. Wishing you a blessed day."
                    
                    val builder = NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(pendingIntent)
                        
                    try {
                        notificationManager.notify(ID_DAILY, builder.build())
                    } catch (e: Exception) {
                        android.util.Log.e("PrayerNotifReceiver", "Failed to post occasion notification: ${e.message}")
                    }
                }
            }
            
            return
        }

        var title: String
        var text: String
        if (prayerName == "Morning Adhkar") {
            title = if (isAr) "أذكار الصباح" else "Morning Adhkar"
            text = if (isAr) "ابدأ يومك بالذكر" else "Start your day with remembrance."
        } else if (prayerName == "Evening Adhkar") {
            title = if (isAr) "أذكار المساء" else "Evening Adhkar"
            text = if (isAr) "اختم يومك بالذكر" else "End your day with remembrance."
        } else if (prayerName == "Friday Jumuah") {
            val isFriday = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.FRIDAY
            if (!isFriday) {
                return
            }
            title = if (isAr) "يوم الجمعة المبارك" else "Blessed Friday (Jumu'ah)"
            text = if (isAr) "اقترب موعد صلاة الجمعة. لا تنسَ قراءة سورة الكهف والصلاة على النبي ﷺ." else "Jumu'ah prayer is approaching. Don't forget to read Surah Al-Kahf and send Salawat upon the Prophet ﷺ."
        } else if (prayerName == "Suhur Reminder") {
            title = if (isAr) "تذكير بالسحور" else "Suhur Reminder"
            text = if (isAr) "اقترب موعد الفجر، تسحروا فإن في السحور بركة." else "Fajr is approaching. Have Suhur, for indeed in Suhur there is blessing."
        } else if (prayerName == "Iftar Reminder") {
            title = if (isAr) "تذكير بالإفطار" else "Iftar Reminder"
            text = if (isAr) "حان موعد أذان المغرب وإفطار الصائم. ذهب الظمأ وابتلت العروق وثبت الأجر إن شاء الله." else "Maghrib Adhan is here. Time to break your fast. Thirst has gone, the arteries are moist, and the reward is sure, if Allah wills."
        } else if (prayerName.startsWith("Pre-")) {
            val actName = prayerName.substringAfter("Pre-")
            val dispName = if (isAr) {
                when (actName) {
                    "Fajr" -> "الفجر"
                    "Dhuhr" -> "الظهر"
                    "Asr" -> "العصر"
                    "Maghrib" -> "المغرب"
                    "Isha" -> "العشاء"
                    else -> actName
                }
            } else {
                actName
            }
            title = if (isAr) "اقتراب وقت صلاة $dispName" else "$dispName Approaching"
            text = if (isAr) "متبقي ١٥ دقيقة على أذان صلاة $dispName. استعد للوضوء." 
                   else "15 minutes left until $dispName Adhan. Prepare for wudu."
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
            text = if (isAr) "حان وقت صلاة $dispName." else "It's time for $dispName prayer."
        }

        // The notification strings are either fully Arabic (app in Arabic mode) or fully
        // English, so the platform's own bidirectional algorithm renders them correctly and
        // BidiFormatter.unicodeWrap() is unnecessary. getInstance() picks the *system*
        // locale, which is usually LTR, so it wrapped the Arabic in left-to-right override
        // characters for no benefit.

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            
        val notifId = when (prayerName) {
            "Morning Adhkar", "Evening Adhkar", "Friday Jumuah" -> ID_ADHKAR
            else -> ID_PRAYER
        }
            
        try {
            notificationManager.notify(notifId, builder.build())
        } catch (e: Exception) {
            android.util.Log.e("PrayerNotifReceiver", "Failed to post prayer notification: ${e.message}")
        }
    }
}
