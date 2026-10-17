package dev.barakah.app.widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class PrayerRemainingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PrayerRemainingWidget()

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

        if (action == android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            // A widget can be added while the app is not running, so the minute tick that keeps the
            // countdown counting down is started from here and not only from the app itself.
            dev.barakah.app.notifications.PrayerScheduler.armWidgetTick(context.applicationContext)
        }
    }
}
