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

class PrayerNotificationReceiver : BroadcastReceiver() {
    companion object {
        const val ID_PRAYER = 1000
        const val ID_ADHKAR = 1001
        const val ID_DAILY = 1002
        
        private var activeAdhanPlayer: android.media.MediaPlayer? = null
        
        fun stopActiveAdhan() {
            try {
                activeAdhanPlayer?.let {
                    if (it.isPlaying) {
                        it.stop()
                    }
                    it.release()
                }
                activeAdhanPlayer = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        private fun playAdhanSound(context: Context, isFajr: Boolean) {
            try {
                stopActiveAdhan()
                val resId = if (isFajr) R.raw.adhan_fajr else R.raw.adhan_regular
                val player = android.media.MediaPlayer.create(context, resId)
                if (player != null) {
                    player.setAudioStreamType(android.media.AudioManager.STREAM_MUSIC)
                    player.start()
                    activeAdhanPlayer = player
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (action == "ACTION_STOP_ADHAN") {
            stopActiveAdhan()
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(ID_PRAYER)
            return
        }

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
        
        val appPrefs = context.getSharedPreferences("barakah_prefs", Context.MODE_PRIVATE)
        val isAr = appPrefs.getString("app_lang", "ar") == "ar"

        val channelId = "prayer_channel"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channelName = if (isAr) "مواقيت الصلاة والذكر" else "Prayer Times & Remembrance"
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        
        val activityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, activityIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (prayerName == "Daily Support Reminder") {
            val notifyOccasions = appPrefs.getBoolean("notify_occasions", true)
            val notifyFasting = appPrefs.getBoolean("notify_fasting", true)
            
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
                        
                    notificationManager.notify(ID_DAILY, builder.build())
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
                        
                    notificationManager.notify(ID_DAILY, builder.build())
                }
            }
            
            PrayerScheduler.scheduleNextPrayers(context)
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
                PrayerScheduler.scheduleNextPrayers(context)
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

        // Use BidiFormatter to correctly format RTL strings without layout clipping on Android 16/15
        if (isAr) {
            val bidi = androidx.core.text.BidiFormatter.getInstance()
            title = bidi.unicodeWrap(title)
            text = bidi.unicodeWrap(text)
        }

        val isFardPrayer = prayerName == "Fajr" || prayerName == "Dhuhr" || prayerName == "Asr" || prayerName == "Maghrib" || prayerName == "Isha"
        var isPlayingAdhan = false
        if (isFardPrayer) {
            val enableAdhan = appPrefs.getBoolean("enable_adhan_sound", false)
            if (enableAdhan) {
                val adhanFajrOnly = appPrefs.getBoolean("adhan_fajr_only", false)
                val isFajr = prayerName == "Fajr"
                if (!adhanFajrOnly || isFajr) {
                    playAdhanSound(context, isFajr)
                    isPlayingAdhan = true
                }
            }
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (isPlayingAdhan) {
            val stopIntent = Intent(context, PrayerNotificationReceiver::class.java).apply {
                setAction("ACTION_STOP_ADHAN")
            }
            val stopPendingIntent = PendingIntent.getBroadcast(
                context, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val btnTitle = if (isAr) "إيقاف الأذان" else "Stop Adhan"
            builder.addAction(R.drawable.ic_notification, btnTitle, stopPendingIntent)
        }
            
        val notifId = when (prayerName) {
            "Morning Adhkar", "Evening Adhkar", "Friday Jumuah" -> ID_ADHKAR
            else -> ID_PRAYER
        }
            
        notificationManager.notify(notifId, builder.build())

        // Reschedule for next days
        PrayerScheduler.scheduleNextPrayers(context)
    }
}
