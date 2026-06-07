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

class NawafilWidget : GlanceAppWidget() {
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
        
        // Custom offsets (in minutes)
        val adjFajr = prefs.getInt("adj_fajr", 0)
        val adjSunrise = prefs.getInt("adj_sunrise", 0)
        val adjDhuhr = prefs.getInt("adj_dhuhr", 0)
        val adjAsr = prefs.getInt("adj_asr", 0)
        val adjMaghrib = prefs.getInt("adj_maghrib", 0)
        val adjIsha = prefs.getInt("adj_isha", 0)

        val m = try {
            PrayerCalculator.CalculationMethod.valueOf(prefs.getString("calc_method", "MWL") ?: "MWL")
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
            adjIsha = adjIsha
        )

        fun calculateOffsetTime(timeStr: String, offsetMinutes: Int): String {
            return try {
                val parts = timeStr.trim().split(":")
                val h = parts[0].toInt()
                val min = parts[1].split(" ")[0].trim().toInt()
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, h)
                    set(Calendar.MINUTE, min)
                    add(Calendar.MINUTE, offsetMinutes)
                }
                String.format(Locale.US, "%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
            } catch (e: Exception) {
                ""
            }
        }

        // Calculate 4 Nawafil times
        val duhaRaw = calculateOffsetTime(times.sunrise, 20)
        val qiyamRaw = calculateOffsetTime(times.fajr, -150)
        val tahajjudRaw = calculateOffsetTime(times.fajr, -90)
        val witrRaw = calculateOffsetTime(times.isha, 45)

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
                    java.lang.String.format(locale, "%d:%02d %s", hour12, min, amPm)
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

            val dMin = toMin(duhaRaw)
            val qMin = toMin(qiyamRaw)
            val tMin = toMin(tahajjudRaw)
            val wMin = toMin(witrRaw)
            val dhuhrMin = toMin(times.dhuhr)
            val fajrMin = toMin(times.fajr)

            fun isCurrentNawafil(nName: String): Boolean {
                return when(nName) {
                    "Duha", "الضحى" -> nowMin in dMin until dhuhrMin
                    "Qiyam", "القيام" -> nowMin in qMin until tMin
                    "Tahajjud", "التهجد" -> nowMin in tMin until fajrMin
                    "Witr", "الوتر" -> {
                        // Witr usually from Isha/Witr start until Qiyam or Fajr
                        if (wMin < fajrMin) { // rare but possible depending on lat
                             nowMin in wMin until fajrMin
                        } else {
                             nowMin >= wMin || nowMin < fajrMin
                        }
                    }
                    else -> false
                }
            }

            NawafilWidgetContent(
                label = label,
                isAr = isAr,
                duha = formatTimeStr(duhaRaw),
                qiyam = formatTimeStr(qiyamRaw),
                tahajjud = formatTimeStr(tahajjudRaw),
                witr = formatTimeStr(witrRaw),
                is24Hour = is24Hour,
                isCurrent = ::isCurrentNawafil
            )
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
                val n1 = if (isAr) "الضحى" else "Duha"
                val n2 = if (isAr) "القيام" else "Qiyam"
                val n3 = if (isAr) "التهجد" else "Tahajjud"
                val n4 = if (isAr) "الوتر" else "Witr"
                
                NawafilItem(GlanceModifier.defaultWeight(), n1, duha, labelColor, timeColor, timeFontSize, isCurrent(n1))
                NawafilItem(GlanceModifier.defaultWeight(), n2, qiyam, labelColor, timeColor, timeFontSize, isCurrent(n2))
                NawafilItem(GlanceModifier.defaultWeight(), n3, tahajjud, labelColor, timeColor, timeFontSize, isCurrent(n3))
                NawafilItem(GlanceModifier.defaultWeight(), n4, witr, labelColor, timeColor, timeFontSize, isCurrent(n4))
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
        timeFontSize: androidx.compose.ui.unit.TextUnit,
        isCurrent: Boolean
    ) {
        val activeDotColor = androidx.glance.color.ColorProvider(day = Color(0xFF00668B), night = Color(0xFF81CFFF))
        Column(
            modifier = modifier.padding(horizontal = 1.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = GlanceModifier.height(8.dp), contentAlignment = Alignment.Center) {
                if (isCurrent) {
                    Spacer(
                        modifier = GlanceModifier
                            .size(4.dp)
                            .background(activeDotColor)
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
