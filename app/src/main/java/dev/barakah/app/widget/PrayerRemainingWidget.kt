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

class PrayerRemainingWidget : GlanceAppWidget() {
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
                method = m, asrMethod = asrMethod, ishaMethod = ishaMethod,
                adjFajr = adjFajr, adjSunrise = adjSunrise, adjDhuhr = adjDhuhr,
                adjAsr = adjAsr, adjMaghrib = adjMaghrib, adjIsha = adjIsha
            )
        
            val currentTimeInSec = calendar.get(Calendar.HOUR_OF_DAY) * 3600 + calendar.get(Calendar.MINUTE) * 60 + calendar.get(Calendar.SECOND)
        
            fun parseTimeToSec(timeStr: String): Int {
                return try {
                    val parts = timeStr.split(":")
                    val h = parts[0].trim().toInt()
                    val mPart = parts[1].split(" ")[0].trim().toInt()
                    h * 3600 + mPart * 60
                } catch (e: Exception) {
                    0
                }
            }
        
            val fajrSec = parseTimeToSec(times.fajr)
            val sunriseSec = parseTimeToSec(times.sunrise)
            val dhuhrSec = parseTimeToSec(times.dhuhr)
            val asrSec = parseTimeToSec(times.asr)
            val maghribSec = parseTimeToSec(times.maghrib)
            val ishaSec = parseTimeToSec(times.isha)

            val nextName: String
            val nextSec: Int
            var isNextDay = false

            if (currentTimeInSec < fajrSec) {
                nextName = "Fajr"
                nextSec = fajrSec
            } else if (currentTimeInSec < sunriseSec) {
                nextName = "Sunrise"
                nextSec = sunriseSec
            } else if (currentTimeInSec < dhuhrSec) {
                nextName = "Dhuhr"
                nextSec = dhuhrSec
            } else if (currentTimeInSec < asrSec) {
                nextName = "Asr"
                nextSec = asrSec
            } else if (currentTimeInSec < maghribSec) {
                nextName = "Maghrib"
                nextSec = maghribSec
            } else if (currentTimeInSec < ishaSec) {
                nextName = "Isha"
                nextSec = ishaSec
            } else {
                nextName = "Fajr"
                nextSec = fajrSec
                isNextDay = true
            }

            var diff = if (isNextDay) {
                (24 * 3600 - currentTimeInSec) + nextSec
            } else {
                nextSec - currentTimeInSec
            }
            if (diff < 0) diff = 0

            val h = diff / 3600
            val mn = (diff % 3600) / 60

            val locale = java.util.Locale.US
            val formattedH = h.toString().localize(isAr, useWesternNumbersInArabic)
            val formattedM = mn.toString().localize(isAr, useWesternNumbersInArabic)
            val diffStr = if (!isLocationSet) {
                if (isAr) "متبقي --" else "Starts in --"
            } else {
                if (isAr) "متبقي $formattedH ساعة و $formattedM دقيقة" else "Starts in $formattedH hr $formattedM min"
            }

            val dispName = if (!isLocationSet) {
                "--"
            } else {
                if (isAr) {
                    when (nextName) {
                        "Fajr" -> "الفجر"
                        "Dhuhr" -> "الظهر"
                        "Asr" -> "العصر"
                        "Maghrib" -> "المغرب"
                        "Isha" -> "العشاء"
                        "Sunrise" -> "الشروق"
                        else -> nextName
                    }
                } else {
                    nextName
                }
            }

            val nextTimeStr = when(nextName) {
                "Fajr" -> times.fajr
                "Sunrise" -> times.sunrise
                "Dhuhr" -> times.dhuhr
                "Asr" -> times.asr
                "Maghrib", "المغرب" -> times.maghrib
                "Isha", "العشاء" -> times.isha
                else -> ""
            }

            val is24Hour = android.text.format.DateFormat.is24HourFormat(context)

            fun formatTimeStr(timeStr: String): String {
                if (!isLocationSet) return "--:--".localize(isAr, useWesternNumbersInArabic)
                return try {
                    val parts = timeStr.trim().split(":")
                    val hh = parts[0].toInt()
                    val min = parts[1].split(" ")[0].trim().toInt()
                
                    val locale = java.util.Locale.US
                    val formatted = if (is24Hour) {
                        java.lang.String.format(locale, "%02d:%02d", hh, min)
                    } else {
                        val hour12 = if (hh % 12 == 0) 12 else hh % 12
                        val amPm = if (hh < 12) {
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
                PrayerRemainingWidgetContent(dispName, formatTimeStr(nextTimeStr), diffStr, isAr, is24Hour)
            }
        }
    }

    @Composable
    private fun PrayerRemainingWidgetContent(nextName: String, nextTime: String, diffStr: String, isAr: Boolean, is24Hour: Boolean) {
        val bgColor = androidx.glance.color.ColorProvider(day = Color(0xCCFFFFFF), night = Color(0xB3000000))
        val primaryColor = androidx.glance.color.ColorProvider(day = Color(0xFF44474E), night = Color(0xFFC4C6D0))
        val textColor = androidx.glance.color.ColorProvider(day = Color(0xFF1A1C1E), night = Color(0xFFE2E2E6))
        val subTextColor = androidx.glance.color.ColorProvider(day = Color(0xFF74777F), night = Color(0xFF8E9099))

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bgColor)
                .padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isAr) "الصلاة القادمة" else "Next Prayer",
                style = TextStyle(color = primaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            Spacer(modifier = GlanceModifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = nextName,
                    style = TextStyle(color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.width(8.dp))
                val timeFontSize = if (is24Hour) 16.sp else 13.5.sp
                Text(
                    text = nextTime,
                    style = TextStyle(color = subTextColor, fontSize = timeFontSize, fontWeight = FontWeight.Medium),
                    maxLines = 1
                )
            }
            Spacer(modifier = GlanceModifier.height(8.dp))
            Box(
                modifier = GlanceModifier
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = diffStr,
                    style = TextStyle(color = primaryColor, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1
                )
            }
        }
    }
}
