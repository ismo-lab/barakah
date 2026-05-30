package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.util.PrayerCalculator
import java.util.*

class PrayerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("barakah_prefs", Context.MODE_PRIVATE)
        val lat = prefs.getFloat("loc_lat", 21.4225f).toDouble()
        val lng = prefs.getFloat("loc_lng", 39.8262f).toDouble()
        val label = prefs.getString("loc_label", "Mecca, KSA") ?: "Mecca, KSA"
        
        val calendar = Calendar.getInstance()
        val tz = TimeZone.getDefault()
        val offsetHours = tz.getOffset(calendar.timeInMillis) / 3600000.0
        
        val times = PrayerCalculator.calculate(lat, lng, offsetHours, calendar)
        
        provideContent {
            PrayerWidgetContent(times, label)
        }
    }

    @Composable
    private fun PrayerWidgetContent(times: PrayerCalculator.PrayerTimes, label: String) {
        val bgColor = androidx.glance.color.ColorProvider(day = Color(0xEBFAFAFA), night = Color(0xCC1A1C1E))
        val titleColor = androidx.glance.color.ColorProvider(day = Color(0xFF44474E), night = Color(0xFFC4C6D0))
        val labelColor = androidx.glance.color.ColorProvider(day = Color(0xFF74777F), night = Color(0xFF8E9099))
        val timeColor = androidx.glance.color.ColorProvider(day = Color(0xFF1A1C1E), night = Color(0xFFE2E2E6))

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bgColor)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = TextStyle(
                    color = titleColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            
            Spacer(modifier = GlanceModifier.height(8.dp))
            
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PrayerItem("Fajr", times.fajr, labelColor, timeColor)
                PrayerItem("Dhuhr", times.dhuhr, labelColor, timeColor)
                PrayerItem("Asr", times.asr, labelColor, timeColor)
                PrayerItem("Maghrib", times.maghrib, labelColor, timeColor)
                PrayerItem("Isha", times.isha, labelColor, timeColor)
            }
        }
    }

    @Composable
    private fun PrayerItem(name: String, time: String, labelColor: ColorProvider, timeColor: ColorProvider) {
        Column(
            modifier = GlanceModifier.padding(horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name.take(3),
                style = TextStyle(color = labelColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            )
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = time,
                style = TextStyle(color = timeColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            )
        }
    }
}
