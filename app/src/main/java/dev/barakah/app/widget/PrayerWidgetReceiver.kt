package dev.barakah.app.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import dev.barakah.app.notifications.PrayerScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class PrayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == Intent.ACTION_LOCALE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_DATE_CHANGED) {

            MainScope().launch {
                glanceAppWidget.updateAll(context)
            }
        }
    }
}

class PrayerWidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (intent.action == PrayerScheduler.ACTION_WIDGET_TICK) {
                    // The tick exists only to keep the countdown honest, so it redraws that one widget
                    // and arms nothing but the next tick. Redrawing all three widgets and rebuilding
                    // every prayer alarm once a minute would be a lot of work for no benefit.
                    updateWidget("PrayerRemainingWidget") {
                        PrayerRemainingWidget().updateAll(appContext)
                    }
                    PrayerScheduler.armWidgetTick(appContext)
                } else {
                    updateWidget("PrayerWidget") {
                        PrayerWidget().updateAll(appContext)
                    }
                    updateWidget("PrayerRemainingWidget") {
                        PrayerRemainingWidget().updateAll(appContext)
                    }
                    updateWidget("NawafilWidget") {
                        NawafilWidget().updateAll(appContext)
                    }
                    PrayerScheduler.scheduleNextPrayersAndWait(appContext)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun updateWidget(name: String, update: suspend () -> Unit) {
        try {
            update()
        } catch (error: Exception) {
            android.util.Log.e("PrayerWidgetRefresh", "Unable to refresh $name", error)
        }
    }
}
