package dev.barakah.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_TIME_CHANGED &&
            action != Intent.ACTION_TIMEZONE_CHANGED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }

        // Alarms do not survive a reboot, a clock change or an app update, so they all have to be
        // set again here. This is the point where the app is most likely to be killed before it
        // finishes: the process was just started, and it can go away the moment onReceive()
        // returns. goAsync() keeps it alive until the scheduling has actually completed.
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                PrayerScheduler.scheduleNextPrayersAndWait(appContext)
            } catch (error: Exception) {
                android.util.Log.e("BootReceiver", "Failed to reschedule prayer alarms", error)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
