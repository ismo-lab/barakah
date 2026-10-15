package dev.barakah.app.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class PrayerWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerWidget()

    override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action == android.content.Intent.ACTION_LOCALE_CHANGED ||
            action == android.content.Intent.ACTION_TIME_CHANGED ||
            action == android.content.Intent.ACTION_TIMEZONE_CHANGED ||
            action == android.content.Intent.ACTION_DATE_CHANGED) {

            MainScope().launch {
                glanceAppWidget.updateAll(context)
            }
        }
    }
}

class PrayerWidgetRefreshReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.IO
        ).launch {
            try {
                try {
                    PrayerWidget().updateAll(appContext)
                } catch (error: Exception) {
                    android.util.Log.e("PrayerWidgetRefresh", "Unable to refresh PrayerWidget", error)
                }
                try {
                    PrayerRemainingWidget().updateAll(appContext)
                } catch (error: Exception) {
                    android.util.Log.e("PrayerWidgetRefresh", "Unable to refresh PrayerRemainingWidget", error)
                }
                try {
                    NawafilWidget().updateAll(appContext)
                } catch (error: Exception) {
                    android.util.Log.e("PrayerWidgetRefresh", "Unable to refresh NawafilWidget", error)
                }
                dev.barakah.app.notifications.PrayerScheduler.scheduleNextPrayers(appContext)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
