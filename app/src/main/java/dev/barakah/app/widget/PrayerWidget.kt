package dev.barakah.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import dev.barakah.app.MainActivity
import dev.barakah.app.util.PrayerCalculator
import dev.barakah.app.util.localize
import java.util.*

class PrayerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("barakah_prefs", Context.MODE_PRIVATE)
        val lat = prefs.getFloat("loc_lat", 21.4225f).toDouble()
        val lng = prefs.getFloat("loc_lng", 39.8262f).toDouble()
        val label = prefs.getString("loc_label", "Mecca, KSA") ?: "Mecca, KSA"
        val isAr = prefs.getString("app_lang", "ar") == "ar"
        val useWesternNumbersInArabic = prefs.getBoolean("use_western_numbers_in_arabic", false)
        
        val calendar = Calendar.getInstance()
        val tz = TimeZone.getDefault()
        val offsetHours = tz.getOffset(calendar.timeInMillis) / 3600000.0
        
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

        val is24Hour = android.text.format.DateFormat.is24HourFormat(context)

        fun formatTimeStr(timeStr: String): String {
            return try {
                val parts = timeStr.trim().split(":")
                val h = parts[0].toInt()
                val min = parts[1].split(" ")[0].trim().toInt()
                
                val locale = java.util.Locale.US
                val formatted = if (is24Hour) {
                    java.lang.String.format(locale, "%02d:%02d", h, min)
                } else {
                    val hour12 = if (h % 12 == 0) 12 else h % 12
                    val amPm = if (h < 12) {
                        if (isAr) "ص" else "AM"
                    } else {
                        if (isAr) "م" else "PM"
                    }
                    java.lang.String.format(locale, "%02d:%02d %s", hour12, min, amPm)
                }
                formatted.localize(isAr, useWesternNumbersInArabic)
            } catch (e: Exception) {
                timeStr.localize(isAr, useWesternNumbersInArabic)
            }
        }
        
        provideContent {
            val now = Calendar.getInstance()
            val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)

            fun toMin(t: String): Int {
                return try {
                    val p = t.split(":")
                    p[0].toInt() * 60 + p[1].split(" ")[0].toInt()
                } catch(e: Exception) { 0 }
            }

            val f = toMin(times.fajr)
            val d = toMin(times.dhuhr)
            val a = toMin(times.asr)
            val m = toMin(times.maghrib)
            val i = toMin(times.isha)

            fun isCurrent(pName: String): Boolean {
                return when(pName) {
                    "Fajr", "الفجر" -> nowMin in f until d
                    "Dhuhr", "الظهر" -> nowMin in d until a
                    "Asr", "العصر" -> nowMin in a until m
                    "Maghrib", "المغرب" -> nowMin in m until i
                    "Isha", "العشاء" -> nowMin >= i || nowMin < f
                    else -> false
                }
            }

            PrayerWidgetContent(times, label, isAr, ::formatTimeStr, is24Hour, ::isCurrent)
        }
    }

    @Composable
    private fun PrayerWidgetContent(
        times: PrayerCalculator.PrayerTimes, 
        label: String, 
        isAr: Boolean, 
        formatTimeStr: (String) -> String, 
        is24Hour: Boolean,
        isCurrent: (String) -> Boolean
    ) {
        val bgColor = androidx.glance.color.ColorProvider(day = Color(0xCCFFFFFF), night = Color(0xB3000000))
        val titleColor = androidx.glance.color.ColorProvider(day = Color(0xFF44474E), night = Color(0xFFC4C6D0))
        val labelColor = androidx.glance.color.ColorProvider(day = Color(0xFF74777F), night = Color(0xFF8E9099))
        val timeColor = androidx.glance.color.ColorProvider(day = Color(0xFF1A1C1E), night = Color(0xFFE2E2E6))

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bgColor)
                .padding(14.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = label,
                    style = TextStyle(
                        color = titleColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
            }
            
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                val timeFontSize = if (is24Hour) 11.sp else 9.5.sp
                val p1 = if (isAr) "الفجر" else "Fajr"
                val p2 = if (isAr) "الظهر" else "Dhuhr"
                val p3 = if (isAr) "العصر" else "Asr"
                val p4 = if (isAr) "المغرب" else "Maghrib"
                val p5 = if (isAr) "العشاء" else "Isha"
                
                PrayerItem(GlanceModifier.defaultWeight(), p1, formatTimeStr(times.fajr), labelColor, timeColor, timeFontSize, isCurrent(p1))
                PrayerItem(GlanceModifier.defaultWeight(), p2, formatTimeStr(times.dhuhr), labelColor, timeColor, timeFontSize, isCurrent(p2))
                PrayerItem(GlanceModifier.defaultWeight(), p3, formatTimeStr(times.asr), labelColor, timeColor, timeFontSize, isCurrent(p3))
                PrayerItem(GlanceModifier.defaultWeight(), p4, formatTimeStr(times.maghrib), labelColor, timeColor, timeFontSize, isCurrent(p4))
                PrayerItem(GlanceModifier.defaultWeight(), p5, formatTimeStr(times.isha), labelColor, timeColor, timeFontSize, isCurrent(p5))
            }
        }
    }

    @Composable
    private fun PrayerItem(
        modifier: GlanceModifier, 
        name: String, 
        time: String, 
        labelColor: androidx.glance.unit.ColorProvider, 
        timeColor: androidx.glance.unit.ColorProvider, 
        timeFontSize: androidx.compose.ui.unit.TextUnit,
        isCurrent: Boolean
    ) {
        Column(
            modifier = modifier.padding(horizontal = 1.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = GlanceModifier.height(8.dp), contentAlignment = Alignment.Center) {
                if (isCurrent) {
                    androidx.glance.Image(
                        provider = androidx.glance.ImageProvider(dev.barakah.app.R.drawable.dot_circle),
                        contentDescription = "Active Indicator",
                        modifier = GlanceModifier.size(6.dp)
                    )
                }
            }
            Spacer(modifier = GlanceModifier.height(2.dp))
            Text(
                text = name,
                style = TextStyle(color = labelColor, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Text(
                text = time,
                style = TextStyle(color = timeColor, fontSize = timeFontSize, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
        }
    }
}
