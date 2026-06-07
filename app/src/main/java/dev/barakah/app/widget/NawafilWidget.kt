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
import java.util.*

class NawafilWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val prefs = context.getSharedPreferences("barakah_prefs", Context.MODE_PRIVATE)
        val lat = prefs.getFloat("loc_lat", 21.4225f).toDouble()
        val lng = prefs.getFloat("loc_lng", 39.8262f).toDouble()
        val label = prefs.getString("loc_label", "Mecca, KSA") ?: "Mecca, KSA"
        val isAr = prefs.getString("app_lang", "ar") == "ar"
        
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
                
                val locale = java.util.Locale.getDefault()
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
                formatted
            } catch (e: Exception) {
                timeStr
            }
        }
        
        provideContent {
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
        labelColor: ColorProvider,
        timeColor: ColorProvider,
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
