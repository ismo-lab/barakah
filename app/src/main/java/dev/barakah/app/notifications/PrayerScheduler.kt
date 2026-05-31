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
            val tz = TimeZone.getDefault()
            val offsetHours = tz.getOffset(calendar.timeInMillis) / 3600000.0
            
            val times = PrayerCalculator.calculate(lat, lng, offsetHours, calendar, PrayerCalculator.CalculationMethod.MWL)
            
            val db = AppDatabase.getDatabase(context)
            val settings = db.prayerAlertDao().getAllAlertSettings().first()
            val isEnabled = { name: String -> settings.find { it.prayerName == name }?.isEnabled != false }
            
            scheduleAlarm(context, "Fajr", times.fajr, 1, isEnabled("Fajr"))
            scheduleAlarm(context, "Dhuhr", times.dhuhr, 2, isEnabled("Dhuhr"))
            scheduleAlarm(context, "Asr", times.asr, 3, isEnabled("Asr"))
            scheduleAlarm(context, "Maghrib", times.maghrib, 4, isEnabled("Maghrib"))
            scheduleAlarm(context, "Isha", times.isha, 5, isEnabled("Isha"))
        }
    }

    private fun scheduleAlarm(context: Context, name: String, timeStr: String, id: Int, enabled: Boolean) {
        if (timeStr.isEmpty()) return
        val parts = timeStr.split(":")
        if (parts.size < 2) return
        val h = parts[0].toIntOrNull() ?: return
        val m = parts[1].split(" ")[0].toIntOrNull() ?: return
        
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
        }
        
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1) // Next day if time already passed
        }
        
        val intent = Intent(context, PrayerNotificationReceiver::class.java).apply {
            putExtra("PRAYER_NAME", name)
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
