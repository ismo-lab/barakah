package com.example.util

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.*

object PrayerCalculator {

    enum class CalculationMethod(val fajrAngle: Double, val ishaAngle: Double) {
        MWL(18.0, 17.0),          // Muslim World League
        ISNA(15.0, 15.0),         // Islamic Society of North America
        EGYPT(19.5, 17.5),        // Egyptian General Authority of Survey
        UMM_AL_QURA(18.5, 90.0),  // Umm al-Qura (Isha is 90 mins after Maghrib, solved specially)
        KARACHI(18.0, 18.0)       // University of Islamic Sciences, Karachi
    }

    data class PrayerTimes(
        val fajr: String,
        val sunrise: String,
        val dhuhr: String,
        val asr: String,
        val maghrib: String,
        val isha: String
    )

    fun calculate(
        latitude: Double,
        longitude: Double,
        timezoneOffset: Double,
        calendar: Calendar = Calendar.getInstance(),
        method: CalculationMethod = CalculationMethod.MWL,
        asrMethod: String = "standard",
        ishaMethod: String = "standard",
        adjFajr: Int = 0,
        adjSunrise: Int = 0,
        adjDhuhr: Int = 0,
        adjAsr: Int = 0,
        adjMaghrib: Int = 0,
        adjIsha: Int = 0
    ): PrayerTimes {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        // 1. Calculate Julian Date
        val jd = julianDate(year, month, day)

        // 2. Solar calculations
        val d = jd - 2451545.0
        val g = scaleTo360(357.529 + 0.98560028 * d)
        val q = scaleTo360(280.459 + 0.98564736 * d)
        val gRad = Math.toRadians(g)
        val qRad = Math.toRadians(q)

        val l = scaleTo360(q + 1.915 * sin(gRad) + 0.020 * sin(2.0 * gRad))
        val lRad = Math.toRadians(l)

        val r = 1.00014 - 0.01671 * cos(gRad) - 0.00014 * cos(2.0 * gRad)
        val obliq = 23.439 - 0.00000036 * d
        val obliqRad = Math.toRadians(obliq)

        val ra = scaleTo360(Math.toDegrees(atan2(cos(obliqRad) * sin(lRad), cos(lRad))))
        val declination = Math.toDegrees(asin(sin(obliqRad) * sin(lRad)))
        
        var eqDiff = q - ra
        while (eqDiff < -180.0) eqDiff += 360.0
        while (eqDiff > 180.0) eqDiff -= 360.0
        val equationOfTime = eqDiff / 15.0 // in hours

        // Midday (Dhuhr)
        val timezoneDiff = timezoneOffset - longitude / 15.0
        var dhuhrTime = 12.0 + timezoneDiff - equationOfTime

        // Sunrise and Sunset (Maghrib)
        val sunriseAngle = -0.833
        val sunriseHourAngle = hourAngle(sunriseAngle, latitude, declination) ?: 6.0
        val sunsetHourAngle = hourAngle(sunriseAngle, latitude, declination) ?: 6.0

        val sunriseTime = dhuhrTime - sunriseHourAngle
        val maghribTime = dhuhrTime + sunsetHourAngle

        // Fajr
        val fajrHourAngle = hourAngle(-method.fajrAngle, latitude, declination) ?: 5.0
        val fajrTime = dhuhrTime - fajrHourAngle

        // Isha
        val ishaTime = if (method == CalculationMethod.UMM_AL_QURA) {
            maghribTime + 1.5 // 90 minutes after Maghrib
        } else {
            val angle = if (ishaMethod == "hanafi") 18.0 else method.ishaAngle
            val ishaHourAngle = hourAngle(-angle, latitude, declination) ?: 6.2
            dhuhrTime + ishaHourAngle
        }

        // Asr (Shafi/Standard method vs Hanafi shadow factors)
        val declRad = Math.toRadians(declination)
        val latRad = Math.toRadians(latitude)
        val shadowFactor = if (asrMethod == "hanafi") 2.0 else 1.0
        val altitudeAsrRad = atan(1.0 / (shadowFactor + tan(abs(latRad - declRad))))
        val altitudeAsr = Math.toDegrees(altitudeAsrRad)
        val asrHourAngle = hourAngle(altitudeAsr, latitude, declination) ?: 3.5
        val asrTime = dhuhrTime + asrHourAngle

        return PrayerTimes(
            fajr = formatTime(fajrTime, adjFajr),
            sunrise = formatTime(sunriseTime, adjSunrise),
            dhuhr = formatTime(dhuhrTime, adjDhuhr),
            asr = formatTime(asrTime, adjAsr),
            maghrib = formatTime(maghribTime, adjMaghrib),
            isha = formatTime(ishaTime, adjIsha)
        )
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2.0 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun hourAngle(angle: Double, latitude: Double, declination: Double): Double? {
        val angleRad = Math.toRadians(angle)
        val latRad = Math.toRadians(latitude)
        val declRad = Math.toRadians(declination)
        val cosH = (sin(angleRad) - sin(latRad) * sin(declRad)) / (cos(latRad) * cos(declRad))
        if (cosH < -1.0 || cosH > 1.0) return null
        return Math.toDegrees(acos(cosH)) / 15.0
    }

    private fun formatTime(hours: Double, adjMinutes: Int = 0): String {
        var h = hours + (adjMinutes / 60.0)
        while (h < 0) h += 24.0
        while (h >= 24) h -= 24.0
        val totalMinutes = (h * 60.0).roundToInt()
        val m = totalMinutes % 60
        val hh = (totalMinutes / 60) % 24
        return String.format("%02d:%02d", hh, m)
    }

    private fun scaleTo360(angle: Double): Double {
        var a = angle % 360.0
        if (a < 0) a += 360.0
        return a
    }
}
