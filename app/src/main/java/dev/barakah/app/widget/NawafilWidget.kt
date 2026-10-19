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
import dev.barakah.app.data.LocationDefaults
import dev.barakah.app.widget.WidgetRefreshState
import dev.barakah.app.util.ClockTime
import dev.barakah.app.util.NawafilTimes
import dev.barakah.app.util.PrayerCalculator
import dev.barakah.app.util.localize
import java.util.*

class NawafilWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val refreshToken = currentState<Preferences>()[WidgetRefreshState.refreshToken] ?: 0L
            // Glance can reuse an active session and recompose this block without rerunning provideGlance.
            val prefs = context.getSharedPreferences(AppPrefs.FILE, Context.MODE_PRIVATE)
            val isAr = prefs.getString(AppPrefs.APP_LANG, "ar") == "ar"
            val defaultLabel = LocationDefaults.unsetLabel(isAr)
            val lat = prefs.getFloat(AppPrefs.LOC_LAT, LocationDefaults.LATITUDE.toFloat()).toDouble()
            val lng = prefs.getFloat(AppPrefs.LOC_LNG, LocationDefaults.LONGITUDE.toFloat()).toDouble()
            val rawLabel = prefs.getString(AppPrefs.LOC_LABEL, null)
            val label = rawLabel?.takeIf { !LocationDefaults.isPlaceholder(it) } ?: defaultLabel
            val isLocationSet = LocationDefaults.isSet(rawLabel)
            val useWesternNumbersInArabic = prefs.getBoolean(AppPrefs.USE_WESTERN_NUMBERS_IN_ARABIC, false)
        
            val isAutoLocation = prefs.getString(AppPrefs.LOCATION_METHOD, "manual") == "auto"
            val selectedTimeZoneId = if (isAutoLocation) TimeZone.getDefault().id else prefs.getString(AppPrefs.LOC_TIMEZONE, null)
            val timeZoneId = PrayerCalculator.resolveTimeZoneId(selectedTimeZoneId)
            val calendar = Calendar.getInstance(TimeZone.getTimeZone(timeZoneId))
            val offsetHours = PrayerCalculator.getTimeZoneOffset(timeZoneId, calendar)
        
            val asrMethod = prefs.getString(AppPrefs.ASR_METHOD, "standard") ?: "standard"
            val ishaMethod = prefs.getString(AppPrefs.ISHA_METHOD, "standard") ?: "standard"
        
            // Custom offsets (in minutes)
            val adjFajr = prefs.getInt(AppPrefs.ADJ_FAJR, 0)
            val adjSunrise = prefs.getInt(AppPrefs.ADJ_SUNRISE, 0)
            val adjDhuhr = prefs.getInt(AppPrefs.ADJ_DHUHR, 0)
            val adjAsr = prefs.getInt(AppPrefs.ADJ_ASR, 0)
            val adjMaghrib = prefs.getInt(AppPrefs.ADJ_MAGHRIB, 0)
            val adjIsha = prefs.getInt(AppPrefs.ADJ_ISHA, 0)
            val clockOffset = prefs.getInt(AppPrefs.ADJ_GLOBAL, 0)

            val m = try {
                PrayerCalculator.CalculationMethod.valueOf(prefs.getString(AppPrefs.CALC_METHOD, "MWL") ?: "MWL")
            } catch(e: Exception) {
                PrayerCalculator.CalculationMethod.MWL
            }
        
            val times = PrayerCalculator.calculate(
                latitude = lat,
                longitude = lng,
                timezoneOffset = offsetHours,
                calendar = calendar, 
                method = m,
                asrMethod = asrMethod,
                ishaMethod = ishaMethod,
                adjFajr = adjFajr,
                adjSunrise = adjSunrise,
                adjDhuhr = adjDhuhr,
                adjAsr = adjAsr,
                adjMaghrib = adjMaghrib,
                adjIsha = adjIsha,
                clockOffsetMinutes = clockOffset
            )

            fun calculateOffsetTime(timeStr: String, offsetMinutes: Int): String =
                ClockTime.offset(timeStr, offsetMinutes, TimeZone.getTimeZone(timeZoneId))

            // Calculate 4 Nawafil times
            val duhaRaw = calculateOffsetTime(times.sunrise, NawafilTimes.DUHA_AFTER_SUNRISE)
            val qiyamRaw = calculateOffsetTime(times.fajr, NawafilTimes.QIYAM_BEFORE_FAJR)
            val tahajjudRaw = calculateOffsetTime(times.fajr, NawafilTimes.TAHAJJUD_BEFORE_FAJR)
            val witrRaw = calculateOffsetTime(times.isha, NawafilTimes.WITR_AFTER_ISHA)

            val is24Hour = android.text.format.DateFormat.is24HourFormat(context)

            fun formatTimeStr(timeStr: String): String {
                if (!isLocationSet) return "--:--".localize(isAr, useWesternNumbersInArabic)
                // The four-column layout drops the leading zero to fit.
                return ClockTime.format(timeStr, is24Hour, isAr, useWesternNumbersInArabic, padHour = false)
            }
        
        
            key(refreshToken) {
            NawafilWidgetContent(
                label = label,
                isAr = isAr,
                duha = formatTimeStr(duhaRaw),
                qiyam = formatTimeStr(qiyamRaw),
                tahajjud = formatTimeStr(tahajjudRaw),
                witr = formatTimeStr(witrRaw),
                is24Hour = is24Hour
            )
            }
        }
    }

    @Composable
    private fun NawafilWidgetContent(
        label: String,
        isAr: Boolean,
        duha: String,
        qiyam: String,
        tahajjud: String,
        witr: String,
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
                NawafilItem(GlanceModifier.defaultWeight(), if (isAr) "الضحى" else "Duha", duha, labelColor, timeColor, timeFontSize)
                NawafilItem(GlanceModifier.defaultWeight(), if (isAr) "القيام" else "Qiyam", qiyam, labelColor, timeColor, timeFontSize)
                NawafilItem(GlanceModifier.defaultWeight(), if (isAr) "التهجد" else "Tahajjud", tahajjud, labelColor, timeColor, timeFontSize)
                NawafilItem(GlanceModifier.defaultWeight(), if (isAr) "الوتر" else "Witr", witr, labelColor, timeColor, timeFontSize)
            }
        }
    }

    @Composable
    private fun NawafilItem(
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
