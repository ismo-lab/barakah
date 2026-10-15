package dev.barakah.app.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
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
    private const val ACTION_REFRESH_WIDGETS = "dev.barakah.app.ACTION_REFRESH_WIDGETS"
    private const val WIDGET_REFRESH_REQUEST_CODE = 2001

    fun scheduleNextPrayers(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = context.getSharedPreferences("barakah_prefs", Context.MODE_PRIVATE)
            val rawLabel = prefs.getString("loc_label", null)
            val isLocationSet = !(rawLabel.isNullOrEmpty() || rawLabel == "Mecca, KSA" || rawLabel == "Mecca (GPS Fallback)" || rawLabel == "مكة المكرمة (تلقائي)" || rawLabel == "الموقع غير محدد" || rawLabel == "No Location Selected")

            if (!isLocationSet) {
                cancelWidgetRefresh(context)
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
                if (alarmManager != null) {
                    val ids = listOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 1011, 1012, 1013, 1014, 1015)
                    for (id in ids) {
                        val intent = Intent(context, PrayerNotificationReceiver::class.java).apply {
                            action = "dev.barakah.app.ACTION_PRAYER_NOTIFICATION"
                        }
                        val pendingIntent = PendingIntent.getBroadcast(
                            context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        alarmManager.cancel(pendingIntent)
                    }
                }
                return@launch
            }

            val lat = prefs.getFloat("loc_lat", 21.4225f).toDouble()
            val lng = prefs.getFloat("loc_lng", 39.8262f).toDouble()
            
            val isAutoLocation = prefs.getString("location_method", "manual") == "auto"
            val selectedTimeZoneId = if (isAutoLocation) TimeZone.getDefault().id else prefs.getString("loc_timezone", null)
            val timeZoneId = PrayerCalculator.resolveTimeZoneId(selectedTimeZoneId)
            val timeZone = TimeZone.getTimeZone(timeZoneId)
            val calendar = Calendar.getInstance(timeZone)
            val offsetHours = PrayerCalculator.getTimeZoneOffset(timeZoneId, calendar)
            
            val asrMethod = prefs.getString("asr_method", "standard") ?: "standard"
            val ishaMethod = prefs.getString("isha_method", "standard") ?: "standard"
            val m = try {
                PrayerCalculator.CalculationMethod.valueOf(prefs.getString("calc_method", "MWL") ?: "MWL")
            } catch(e: Exception) {
                PrayerCalculator.CalculationMethod.MWL
            }

            val adjFajr = prefs.getInt("adj_fajr", 0)
            val adjSunrise = prefs.getInt("adj_sunrise", 0)
            val adjDhuhr = prefs.getInt("adj_dhuhr", 0)
            val adjAsr = prefs.getInt("adj_asr", 0)
            val adjMaghrib = prefs.getInt("adj_maghrib", 0)
            val adjIsha = prefs.getInt("adj_isha", 0)
            
            val times = PrayerCalculator.calculate(
                lat, lng, offsetHours, calendar, 
                method = m,
                asrMethod = asrMethod,
                ishaMethod = ishaMethod,
                adjFajr = adjFajr,
                adjSunrise = adjSunrise,
                adjDhuhr = adjDhuhr,
                adjAsr = adjAsr,
                adjMaghrib = adjMaghrib,
                adjIsha = adjIsha
            )
            
            scheduleWidgetRefresh(
                context,
                listOf(times.fajr, times.sunrise, times.dhuhr, times.asr, times.maghrib, times.isha),
                timeZone
            )

            val db = AppDatabase.getDatabase(context)
            val settings = db.prayerAlertDao().getAllAlertSettings().first()
            val isEnabled = { name: String -> settings.find { it.prayerName == name }?.isEnabled != false }
            val showNawafil = prefs.getBoolean("show_nawafil", false)
            val notifyNawafil = prefs.getBoolean("notify_nawafil", true)
            
            // Primary Prayers
            scheduleAlarm(context, "Fajr", times.fajr, 1, isEnabled("Fajr"), timeZone)
            scheduleAlarm(context, "Dhuhr", times.dhuhr, 2, isEnabled("Dhuhr"), timeZone)
            scheduleAlarm(context, "Asr", times.asr, 3, isEnabled("Asr"), timeZone)
            scheduleAlarm(context, "Maghrib", times.maghrib, 4, isEnabled("Maghrib"), timeZone)
            scheduleAlarm(context, "Isha", times.isha, 5, isEnabled("Isha"), timeZone)

            // Pre-Adhan alerts (15 minutes before)
            val notifyBeforeAdhan = prefs.getBoolean("notify_before_adhan", true)
            scheduleAlarm(context, "Pre-Fajr", calculateOffsetTime(times.fajr, -15, timeZone), 1011, notifyBeforeAdhan && isEnabled("Fajr"), timeZone)
            scheduleAlarm(context, "Pre-Dhuhr", calculateOffsetTime(times.dhuhr, -15, timeZone), 1012, notifyBeforeAdhan && isEnabled("Dhuhr"), timeZone)
            scheduleAlarm(context, "Pre-Asr", calculateOffsetTime(times.asr, -15, timeZone), 1013, notifyBeforeAdhan && isEnabled("Asr"), timeZone)
            scheduleAlarm(context, "Pre-Maghrib", calculateOffsetTime(times.maghrib, -15, timeZone), 1014, notifyBeforeAdhan && isEnabled("Maghrib"), timeZone)
            scheduleAlarm(context, "Pre-Isha", calculateOffsetTime(times.isha, -15, timeZone), 1015, notifyBeforeAdhan && isEnabled("Isha"), timeZone)

            if (showNawafil) {
                // Secondary / Nawafil
                val duhaTime = calculateOffsetTime(times.sunrise, 20, timeZone)
                val witrTime = calculateOffsetTime(times.isha, 30, timeZone)
                val tahajjudTime = calculateOffsetTime(times.fajr, -90, timeZone)
                val qiyamTime = calculateOffsetTime(times.fajr, -150, timeZone)
                
                scheduleAlarm(context, "Duha (Nafilah)", duhaTime, 6, notifyNawafil && isEnabled("Duha (Nafilah)"), timeZone)
                scheduleAlarm(context, "Witr (Nafilah)", witrTime, 7, notifyNawafil && isEnabled("Witr (Nafilah)"), timeZone)
                scheduleAlarm(context, "Tahajjud (Nafilah)", tahajjudTime, 8, notifyNawafil && isEnabled("Tahajjud (Nafilah)"), timeZone)
                scheduleAlarm(context, "Qiyam-ul-Layl (Nafilah)", qiyamTime, 9, notifyNawafil && isEnabled("Qiyam-ul-Layl (Nafilah)"), timeZone)
            }

            // Morning and Evening Adhkar Notifications
            val notifyMorning = prefs.getBoolean("notify_morning_adhkar", true)
            val notifyEvening = prefs.getBoolean("notify_evening_adhkar", true)

            val morningOffset = prefs.getInt("morning_adhkar_offset", 30)
            val eveningOffset = prefs.getInt("evening_adhkar_offset", 30)

            val morningAdhkarTime = calculateOffsetTime(times.fajr, morningOffset, timeZone)
            val eveningAdhkarTime = calculateOffsetTime(times.asr, eveningOffset, timeZone)

            scheduleAlarm(context, "Morning Adhkar", morningAdhkarTime, 10, notifyMorning, timeZone)
            scheduleAlarm(context, "Evening Adhkar", eveningAdhkarTime, 11, notifyEvening, timeZone)

            // Daily Check at 20:00 for tomorrow's fasting / occasions (a day before)
            val notifyOccasions = prefs.getBoolean("notify_occasions", true)
            val notifyFasting = prefs.getBoolean("notify_fasting", true)
            val dailyEnabled = notifyOccasions || notifyFasting
            scheduleAlarm(context, "Daily Support Reminder", "20:00", 12, dailyEnabled, timeZone)

            val notifyJumuah = prefs.getBoolean("notify_jumuah", true)
            scheduleAlarm(context, "Friday Jumuah", calculateOffsetTime(times.dhuhr, -45, timeZone), 13, notifyJumuah, timeZone)

            val notifySuhur = prefs.getBoolean("notify_suhur", false)
            scheduleAlarm(context, "Suhur Reminder", calculateOffsetTime(times.fajr, -45, timeZone), 14, notifySuhur, timeZone)

            val notifyIftar = prefs.getBoolean("notify_iftar", false)
            scheduleAlarm(context, "Iftar Reminder", times.maghrib, 15, notifyIftar, timeZone)
        }
    }

    private fun widgetRefreshPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, dev.barakah.app.widget.PrayerWidgetRefreshReceiver::class.java).apply {
            action = ACTION_REFRESH_WIDGETS
        }
        return PendingIntent.getBroadcast(
            context,
            WIDGET_REFRESH_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun cancelWidgetRefresh(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.cancel(widgetRefreshPendingIntent(context))
    }

    private fun scheduleWidgetRefresh(context: Context, prayerTimes: List<String>, timeZone: TimeZone) {
        val remainingWidget = ComponentName(
            context,
            dev.barakah.app.widget.PrayerRemainingWidgetReceiver::class.java
        )
        if (AppWidgetManager.getInstance(context).getAppWidgetIds(remainingWidget).isEmpty()) {
            cancelWidgetRefresh(context)
            return
        }

        val now = System.currentTimeMillis()
        val nextRefreshAt = prayerTimes.mapNotNull { timeString ->
            val parts = timeString.trim().split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null
            val minute = parts.getOrNull(1)?.substringBefore(" ")?.toIntOrNull() ?: return@mapNotNull null
            Calendar.getInstance(timeZone).apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
            }.timeInMillis
        }.minOrNull() ?: return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = widgetRefreshPendingIntent(context)
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
                !alarmManager.canScheduleExactAlarms()
            ) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextRefreshAt, pendingIntent)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextRefreshAt, pendingIntent)
            }
        } catch (error: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextRefreshAt, pendingIntent)
        } catch (error: Exception) {
            android.util.Log.e("PrayerScheduler", "Failed to schedule widget refresh", error)
        }
    }

    private fun calculateOffsetTime(timeStr: String, offsetMinutes: Int, timeZone: TimeZone): String {
        return try {
            val parts = timeStr.trim().split(":")
            val h = parts[0].toInt()
            val m = parts[1].split(" ")[0].trim().toInt()
            val cal = Calendar.getInstance(timeZone).apply {
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
                add(Calendar.MINUTE, offsetMinutes)
            }
            String.format(java.util.Locale.US, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        } catch (e: Exception) {
            ""
        }
    }

    private fun scheduleAlarm(context: Context, name: String, timeStr: String, id: Int, enabled: Boolean, timeZone: TimeZone) {
        if (timeStr.isEmpty()) return
        val parts = timeStr.split(":")
        if (parts.size < 2) return
        val h = parts[0].toIntOrNull() ?: return
        val m = parts[1].split(" ")[0].toIntOrNull() ?: return
        
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance(timeZone).apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // Ensure alarm is in the future (add 60s buffer to prevent duplicate firing when alarm triggers)
        if (cal.timeInMillis <= now + 60_000L) {
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
        
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            try {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
            } catch (ex: Exception) {
                try {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
                } catch (ignored: Exception) {}
            }
        } catch (e: Exception) {
            android.util.Log.e("PrayerScheduler", "Failed to schedule alarm for $name: ${e.message}")
        }
    }
}
