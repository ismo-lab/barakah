package dev.barakah.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dev.barakah.app.util.PrayerCalculator
import java.util.Calendar
import java.util.TimeZone

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import dev.barakah.app.data.AppDatabase
import dev.barakah.app.data.AppRepository

object PrayerScheduler {
    fun scheduleNextPrayers(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = context.getSharedPreferences("barakah_prefs", Context.MODE_PRIVATE)
            val lat = prefs.getFloat("loc_lat", 21.4225f).toDouble()
            val lng = prefs.getFloat("loc_lng", 39.8262f).toDouble()
            
            val calendar = Calendar.getInstance()
            val offsetHours = dev.barakah.app.util.PrayerCalculator.getEffectiveTimezoneOffset(lat, lng)
            
            val asrMethod = prefs.getString("asr_method", "standard") ?: "standard"
            val ishaMethod = prefs.getString("isha_method", "standard") ?: "standard"
            val m = try {
                PrayerCalculator.CalculationMethod.valueOf(prefs.getString("calc_method", "MWL") ?: "MWL")
            } catch(e: Exception) {
                PrayerCalculator.CalculationMethod.MWL
            }
            
            val times = PrayerCalculator.calculate(
                lat, lng, offsetHours, calendar, 
                method = m,
                asrMethod = asrMethod,
                ishaMethod = ishaMethod
            )
            
            val db = AppDatabase.getDatabase(context)
            val settings = db.prayerAlertDao().getAllAlertSettings().first()
            val isEnabled = { name: String -> settings.find { it.prayerName == name }?.isEnabled != false }
            val showNawafil = prefs.getBoolean("show_nawafil", false)
            
            // Primary Prayers
            scheduleAlarm(context, "Fajr", times.fajr, 1, isEnabled("Fajr"))
            scheduleAlarm(context, "Dhuhr", times.dhuhr, 2, isEnabled("Dhuhr"))
            scheduleAlarm(context, "Asr", times.asr, 3, isEnabled("Asr"))
            scheduleAlarm(context, "Maghrib", times.maghrib, 4, isEnabled("Maghrib"))
            scheduleAlarm(context, "Isha", times.isha, 5, isEnabled("Isha"))

            // Pre-Adhan alerts (15 minutes before)
            val notifyBeforeAdhan = prefs.getBoolean("notify_before_adhan", true)
            scheduleAlarm(context, "Pre-Fajr", calculateOffsetTime(times.fajr, -15), 1011, notifyBeforeAdhan && isEnabled("Fajr"))
            scheduleAlarm(context, "Pre-Dhuhr", calculateOffsetTime(times.dhuhr, -15), 1012, notifyBeforeAdhan && isEnabled("Dhuhr"))
            scheduleAlarm(context, "Pre-Asr", calculateOffsetTime(times.asr, -15), 1013, notifyBeforeAdhan && isEnabled("Asr"))
            scheduleAlarm(context, "Pre-Maghrib", calculateOffsetTime(times.maghrib, -15), 1014, notifyBeforeAdhan && isEnabled("Maghrib"))
            scheduleAlarm(context, "Pre-Isha", calculateOffsetTime(times.isha, -15), 1015, notifyBeforeAdhan && isEnabled("Isha"))

            if (showNawafil) {
                // Secondary / Nawafil
                val duhaTime = calculateOffsetTime(times.sunrise, 20)
                val witrTime = calculateOffsetTime(times.isha, 45)
                val tahajjudTime = calculateOffsetTime(times.fajr, -90)
                val qiyamTime = calculateOffsetTime(times.fajr, -150)
                
                scheduleAlarm(context, "Duha (Nafilah)", duhaTime, 6, isEnabled("Duha (Nafilah)"))
                scheduleAlarm(context, "Witr (Nafilah)", witrTime, 7, isEnabled("Witr (Nafilah)"))
                scheduleAlarm(context, "Tahajjud (Nafilah)", tahajjudTime, 8, isEnabled("Tahajjud (Nafilah)"))
                scheduleAlarm(context, "Qiyam-ul-Layl (Nafilah)", qiyamTime, 9, isEnabled("Qiyam-ul-Layl (Nafilah)"))
            }

            // Morning and Evening Adhkar Notifications
            val notifyMorning = prefs.getBoolean("notify_morning_adhkar", true)
            val notifyEvening = prefs.getBoolean("notify_evening_adhkar", true)

            val morningAdhkarTime = calculateOffsetTime(times.fajr, 30)
            val eveningAdhkarTime = calculateOffsetTime(times.asr, 30)

            scheduleAlarm(context, "Morning Adhkar", morningAdhkarTime, 10, notifyMorning)
            scheduleAlarm(context, "Evening Adhkar", eveningAdhkarTime, 11, notifyEvening)

            // Daily Check at 20:00 for tomorrow's fasting / occasions (a day before)
            val notifyOccasions = prefs.getBoolean("notify_occasions", true)
            val notifyFasting = prefs.getBoolean("notify_fasting", true)
            val dailyEnabled = notifyOccasions || notifyFasting
            scheduleAlarm(context, "Daily Support Reminder", "20:00", 12, dailyEnabled)

            val notifyJumuah = prefs.getBoolean("notify_jumuah", true)
            scheduleAlarm(context, "Friday Jumuah", calculateOffsetTime(times.dhuhr, -45), 13, notifyJumuah)

            val notifySuhur = prefs.getBoolean("notify_suhur", false)
            scheduleAlarm(context, "Suhur Reminder", calculateOffsetTime(times.fajr, -45), 14, notifySuhur)

            val notifyIftar = prefs.getBoolean("notify_iftar", false)
            scheduleAlarm(context, "Iftar Reminder", times.maghrib, 15, notifyIftar)
        }
    }

    private fun calculateOffsetTime(timeStr: String, offsetMinutes: Int): String {
        return try {
            val parts = timeStr.trim().split(":")
            val h = parts[0].toInt()
            val m = parts[1].split(" ")[0].trim().toInt()
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
                add(Calendar.MINUTE, offsetMinutes)
            }
            String.format(java.util.Locale.US, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        } catch (e: Exception) {
            ""
        }
    }

    private fun scheduleAlarm(context: Context, name: String, timeStr: String, id: Int, enabled: Boolean) {
        if (timeStr.isEmpty()) return
        val parts = timeStr.split(":")
        if (parts.size < 2) return
        val h = parts[0].toIntOrNull() ?: return
        val m = parts[1].split(" ")[0].toIntOrNull() ?: return
        
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // Ensure alarm is in the future
        if (cal.timeInMillis <= now) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        
        val intent = Intent(context, PrayerNotificationReceiver::class.java).apply {
            action = "dev.barakah.app.ACTION_PRAYER_NOTIFICATION"
            putExtra("PRAYER_NAME", name)
            putExtra("PRAYER_ID", id)
            putExtra("SCHEDULED_TIME", cal.timeInMillis)
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        
        if (!enabled) {
            alarmManager.cancel(pendingIntent)
            return
        }
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
        }
    }
}
