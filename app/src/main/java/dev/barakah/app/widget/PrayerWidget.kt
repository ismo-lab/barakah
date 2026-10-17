package dev.barakah.app.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.currentState
import androidx.datastore.preferences.core.Preferences
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
import dev.barakah.app.data.AppPrefs
import dev.barakah.app.widget.WidgetRefreshState
import dev.barakah.app.util.PrayerCalculator
import dev.barakah.app.util.localize
import java.util.*

class PrayerWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val refreshToken = currentState<Preferences>()[WidgetRefreshState.refreshToken] ?: 0L
            // Glance can reuse an active session and recompose this block without rerunning provideGlance.
            val prefs = context.getSharedPreferences(AppPrefs.FILE, Context.MODE_PRIVATE)
            val isAr = prefs.getString(AppPrefs.APP_LANG, "ar") == "ar"
            val defaultLabel = if (isAr) "الموقع غير محدد" else "No Location Selected"
            val lat = prefs.getFloat(AppPrefs.LOC_LAT, 21.4225f).toDouble()
            val lng = prefs.getFloat(AppPrefs.LOC_LNG, 39.8262f).toDouble()
            val rawLabel = prefs.getString(AppPrefs.LOC_LABEL, null)
            val label = if (rawLabel.isNullOrEmpty() || rawLabel == "Mecca, KSA" || rawLabel == "Mecca (GPS Fallback)" || rawLabel == "مكة المكرمة (تلقائي)") defaultLabel else rawLabel
            val isLocationSet = !(rawLabel.isNullOrEmpty() || rawLabel == "Mecca, KSA" || rawLabel == "Mecca (GPS Fallback)" || rawLabel == "مكة المكرمة (تلقائي)" || rawLabel == "الموقع غير محدد" || rawLabel == "No Location Selected")
            val useWesternNumbersInArabic = prefs.getBoolean(AppPrefs.USE_WESTERN_NUMBERS_IN_ARABIC, false)
        
            val isAutoLocation = prefs.getString(AppPrefs.LOCATION_METHOD, "manual") == "auto"
            val selectedTimeZoneId = if (isAutoLocation) TimeZone.getDefault().id else prefs.getString(AppPrefs.LOC_TIMEZONE, null)
            val timeZoneId = PrayerCalculator.resolveTimeZoneId(selectedTimeZoneId)
            val calendar = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId))
            val offsetHours = PrayerCalculator.getTimeZoneOffset(timeZoneId, calendar)
        
            val asrMethod = prefs.getString(AppPrefs.ASR_METHOD, "standard") ?: "standard"
            val ishaMethod = prefs.getString(AppPrefs.ISHA_METHOD, "standard") ?: "standard"
            val m = try {
                PrayerCalculator.CalculationMethod.valueOf(prefs.getString(AppPrefs.CALC_METHOD, "MWL") ?: "MWL")
            } catch(e: Exception) {
                PrayerCalculator.CalculationMethod.MWL
            }

            val adjFajr = prefs.getInt(AppPrefs.ADJ_FAJR, 0)
            val adjSunrise = prefs.getInt(AppPrefs.ADJ_SUNRISE, 0)
            val adjDhuhr = prefs.getInt(AppPrefs.ADJ_DHUHR, 0)
            val adjAsr = prefs.getInt(AppPrefs.ADJ_ASR, 0)
            val adjMaghrib = prefs.getInt(AppPrefs.ADJ_MAGHRIB, 0)
            val adjIsha = prefs.getInt(AppPrefs.ADJ_ISHA, 0)
        
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

            val is24Hour = android.text.format.DateFormat.is24HourFormat(context)

            fun formatTimeStr(timeStr: String): String {
                if (!isLocationSet) return "--:--".localize(isAr, useWesternNumbersInArabic)
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
        
        
            key(refreshToken) {
                PrayerWidgetContent(times, label, isAr, ::formatTimeStr, is24Hour)
            }
        }
    }

    @Composable
    private fun PrayerWidgetContent(
        times: PrayerCalculator.PrayerTimes, 
        label: String, 
        isAr: Boolean, 
        formatTimeStr: (String) -> String, 
        is24Hour: Boolean
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
                PrayerItem(GlanceModifier.defaultWeight(), if (isAr) "الفجر" else "Fajr", formatTimeStr(times.fajr), labelColor, timeColor, timeFontSize)
                PrayerItem(GlanceModifier.defaultWeight(), if (isAr) "الظهر" else "Dhuhr", formatTimeStr(times.dhuhr), labelColor, timeColor, timeFontSize)
                PrayerItem(GlanceModifier.defaultWeight(), if (isAr) "العصر" else "Asr", formatTimeStr(times.asr), labelColor, timeColor, timeFontSize)
                PrayerItem(GlanceModifier.defaultWeight(), if (isAr) "المغرب" else "Maghrib", formatTimeStr(times.maghrib), labelColor, timeColor, timeFontSize)
                PrayerItem(GlanceModifier.defaultWeight(), if (isAr) "العشاء" else "Isha", formatTimeStr(times.isha), labelColor, timeColor, timeFontSize)
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
        timeFontSize: androidx.compose.ui.unit.TextUnit
    ) {
        Column(
            modifier = modifier.padding(horizontal = 1.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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
