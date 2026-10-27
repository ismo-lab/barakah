package dev.barakah.app.notifications

import dev.barakah.app.data.UiStrings

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import dev.barakah.app.MainActivity
import dev.barakah.app.R
import dev.barakah.app.data.AppPrefs
import dev.barakah.app.data.DailyContent
import dev.barakah.app.data.NotifHistoryPrefs
import dev.barakah.app.data.OthersContent
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
        
        // An alarm that fired very late is not worth announcing - but "late" has to be measured in
        // hours, not minutes. Anything delayed by Doze or by the inexact fallback was dropped here
        // and the prayer was never announced at all.
        if (NotificationTiming.isTooLate(scheduledTime, now)) {
            android.util.Log.w("PrayerNotifReceiver", "Skipping stale notification for $prayerName")
            return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val appPrefs = context.getSharedPreferences(AppPrefs.FILE, Context.MODE_PRIVATE)
        val appLanguage = appPrefs.getString(AppPrefs.APP_LANG, "ar") ?: "ar"
        UiStrings.setCurrentLanguage(appLanguage)
        val isAr = appLanguage == "ar"

        if (prayerName == "Daily Verse") {
            if (!appPrefs.getBoolean(AppPrefs.NOTIFY_DAILY_VERSE, false)) return
            val verse = DailyContent.verse(context, appLanguage)
            if (verse.text.isBlank()) return
            val title = "${UiStrings.of("Today's Verse")} · ${verse.reference}"
            postDailyContent(context, notificationManager, isAr, title, verse.text, 1016)
            markNotified(prefs, prayerId, scheduledTime, now)
            return
        }
        if (prayerName == "Daily Hadith") {
            if (!appPrefs.getBoolean(AppPrefs.NOTIFY_DAILY_HADITH, false)) return
            val hadith = DailyContent.hadith(appLanguage)
            val text = "${hadith.text}\n${hadith.source}"
            postDailyContent(context, notificationManager, isAr, UiStrings.of("Today's Hadith"), text, 1017)
            markNotified(prefs, prayerId, scheduledTime, now)
            return
        }

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
            val channelName = if (isAr) "مواقيت الصلاة والذكر" else UiStrings.of("Prayer Times & Remembrance")
            notificationManager.createNotificationChannel(
                NotificationChannel(PRAYER_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH)
            )

            val adhanChannelName = if (isAr) "الأذان" else UiStrings.of("Adhan")
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
                    val dayStr = if (dayOfWeek == java.util.Calendar.MONDAY) {
                        if (isAr) "الإثنين" else UiStrings.of("Monday")
                    } else {
                        if (isAr) "الخميس" else UiStrings.of("Thursday")
                    }
                    // The day is the only part that varies, so it goes in the title: that is the
                    // line a collapsed notification always shows.
                    val title = if (isAr) "صيام $dayStr غداً" else UiStrings.format("Fasting Tomorrow: %s", dayStr)
                    val text = if (isAr) "فمن استطاع الصوم فليصم طمعاً في الأجر."
                               else UiStrings.of("If you are able to fast, earn the reward of this Sunnah.")
                    
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
                // The same adjustment the screens apply, so the reminder lands on the day the user
                // sees listed rather than the plain calculated one.
                val tomorrowHijri = dev.barakah.app.util.HijriCalendarHelper.getTomorrowHijriDate(
                    appPrefs.getInt(AppPrefs.HIJRI_ADJUSTMENT, 0)
                )
                val hDay = tomorrowHijri.day
                val hMonth = tomorrowHijri.month
                
                var occasionId: Int? = null
                var occasionName: String? = null
                var occasionNameAr: String? = null
                
                when {
                    hDay == 1 && hMonth == 1 -> {
                        occasionId = 1
                        occasionName = "Hijri New Year"
                        occasionNameAr = "رأس السنة الهجرية"
                    }
                    hDay == 10 && hMonth == 1 -> {
                        occasionId = 2
                        occasionName = "Day of Ashura"
                        occasionNameAr = "يوم عاشوراء"
                    }
                    hDay == 12 && hMonth == 3 -> {
                        occasionId = 3
                        occasionName = "Mawlid al-Nabi"
                        occasionNameAr = "المولد النبوي الشريف"
                    }
                    hDay == 27 && hMonth == 7 -> {
                        occasionId = 4
                        occasionName = "Isra' and Mi'raj"
                        occasionNameAr = "ليلة الإسراء والمعراج"
                    }
                    hDay == 1 && hMonth == 9 -> {
                        occasionId = 5
                        occasionName = "Ramadan First Day"
                        occasionNameAr = "بداية شهر رمضان المبارك"
                    }
                    hDay == 15 && hMonth == 8 -> {
                        occasionId = 6
                        occasionName = "Mid-Sha'ban Night"
                        occasionNameAr = "ليلة النصف من شعبان"
                    }
                    hDay == 26 && hMonth == 9 -> {
                        occasionId = 7
                        occasionName = "Laylat al-Qadr (27th Night)"
                        occasionNameAr = "ليلة القدر (تحري ليلة ٢٧)"
                    }
                    hDay == 1 && hMonth == 10 -> {
                        occasionId = 8
                        occasionName = "Eid al-Fitr"
                        occasionNameAr = "عيد الفطر المبارك"
                    }
                    hDay == 1 && hMonth == 11 -> {
                        occasionId = 9
                        occasionName = "Start of Hajj Season"
                        occasionNameAr = "بداية أشهر الحج المباركة"
                    }
                    hDay == 9 && hMonth == 12 -> {
                        occasionId = 10
                        occasionName = "Day of Arafah"
                        occasionNameAr = "يوم عرفة"
                    }
                    hDay == 10 && hMonth == 12 -> {
                        occasionId = 11
                        occasionName = "Eid al-Adha"
                        occasionNameAr = "عيد الأضحى المبارك"
                    }
                }
                
                if (occasionId != null && occasionName != null && occasionNameAr != null) {
                    // The occasion names the message, so it is the title; the text carries only the
                    // blessing instead of repeating the name.
                    val title = if (isAr) {
                        occasionNameAr
                    } else {
                        OthersContent.load(context).occasionName(occasionId, appLanguage, occasionName)
                    }
                    val text = if (isAr) "غداً — نسأل الله لنا ولكم القبول والبركة."
                               else UiStrings.of("Tomorrow — wishing you a blessed day.")
                    
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

            markNotified(prefs, prayerId, scheduledTime, now)
            return
        }

        var title: String
        var text: String
        if (prayerName == "Morning Adhkar") {
            title = if (isAr) "أذكار الصباح" else UiStrings.of("Morning Adhkar")
            text = if (isAr) "ابدأ يومك بالذكر" else UiStrings.of("Start your day with remembrance.")
        } else if (prayerName == "Evening Adhkar") {
            title = if (isAr) "أذكار المساء" else UiStrings.of("Evening Adhkar")
            text = if (isAr) "اختم يومك بالذكر" else UiStrings.of("End your day with remembrance.")
        } else if (prayerName == "Friday Jumuah") {
            val isFriday = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.FRIDAY
            if (!isFriday) {
                return
            }
            title = if (isAr) "يوم الجمعة المبارك" else UiStrings.of("Blessed Friday (Jumu'ah)")
            text = if (isAr) "اقترب موعد صلاة الجمعة. لا تنسَ قراءة سورة الكهف والصلاة على النبي ﷺ." else UiStrings.of("Jumu'ah prayer is approaching. Don't forget to read Surah Al-Kahf and send Salawat upon the Prophet ﷺ.")
        } else if (prayerName == "Suhur Reminder") {
            title = if (isAr) "تذكير بالسحور" else UiStrings.of("Suhur Reminder")
            text = if (isAr) "اقترب موعد الفجر، تسحروا فإن في السحور بركة." else UiStrings.of("Fajr is approaching. Have Suhur, for indeed in Suhur there is blessing.")
        } else if (prayerName == "Iftar Reminder") {
            title = if (isAr) "تذكير بالإفطار" else UiStrings.of("Iftar Reminder")
            text = if (isAr) "حان موعد أذان المغرب وإفطار الصائم. ذهب الظمأ وابتلت العروق وثبت الأجر إن شاء الله." else UiStrings.of("Maghrib Adhan is here. Time to break your fast. Thirst has gone, the arteries are moist, and the reward is sure, if Allah wills.")
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
                UiStrings.of(actName)
            }
            title = if (isAr) "اقتراب وقت صلاة $dispName" else UiStrings.format("%s Approaching", dispName)
            text = if (isAr) "متبقي ١٥ دقيقة على أذان صلاة $dispName. استعد للوضوء." 
                   else UiStrings.format("15 minutes left until %s Adhan. Prepare for wudu.", dispName)
        } else {
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
                UiStrings.of(prayerName)
            }
            // The prayer itself (and when it is) is what the user scans the shade for, so it is the
            // title; the sentence below stays as the actionable line.
            val time = if (scheduledTime > 0L) {
                android.text.format.DateFormat.getTimeFormat(context).format(java.util.Date(scheduledTime))
            } else {
                null
            }
            title = if (time != null) "$dispName · $time" else dispName
            text = if (isAr) "حان وقت صلاة $dispName." else UiStrings.format("It's time for %s prayer.", dispName)
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
            markNotified(prefs, prayerId, scheduledTime, now)
        } catch (e: Exception) {
            android.util.Log.e("PrayerNotifReceiver", "Failed to post prayer notification: ${e.message}")
        }
    }

    private fun postDailyContent(context: Context, manager: NotificationManager, isAr: Boolean, title: String, text: String, id: Int) {
        val channel = PRAYER_CHANNEL_ID
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channelName = if (isAr) "مواقيت الصلاة والذكر" else UiStrings.of("Prayer Times & Remembrance")
            manager.createNotificationChannel(NotificationChannel(channel, channelName, NotificationManager.IMPORTANCE_HIGH))
        }
        val openApp = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        manager.notify(id, NotificationCompat.Builder(context, channel).setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text)).setAutoCancel(true).setContentIntent(openApp).build())
    }

    /**
     * Records which scheduled time was announced for this prayer.
     *
     * This is written only once a notification has actually been posted. Recording it beforehand
     * meant a notification that was skipped still consumed the record, so the alert could never be
     * delivered later.
     */
    private fun markNotified(
        prefs: android.content.SharedPreferences,
        prayerId: Int,
        scheduledTime: Long,
        now: Long
    ) {
        prefs.edit {
            putLong(NotifHistoryPrefs.lastNotified(prayerId), scheduledTime)
            putLong(NotifHistoryPrefs.lastNotifiedAt(prayerId), now)
        }
    }
}
