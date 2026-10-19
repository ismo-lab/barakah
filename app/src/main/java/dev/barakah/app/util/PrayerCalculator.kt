package dev.barakah.app.util

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.*

object PrayerCalculator {
    /** Latitude borrowed where the sun does not cross the horizon. Forty-eight always has a sunrise. */
    private const val SUNRISE_FALLBACK_LATITUDE = 48.0

    /** Daylight assumed in the last resort, if even the borrowed latitude cannot be solved. */
    private const val ASSUMED_DAYLIGHT_HOURS = 12.0

    private val availableTimeZoneIds by lazy(LazyThreadSafetyMode.PUBLICATION) {
        TimeZone.getAvailableIDs().toHashSet()
    }

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

        // Sunrise and Sunset (Maghrib). Both share one hour angle: the sun sits the same distance
        // below the horizon at each. Where it never crosses the horizon - inside the polar circles -
        // the day is borrowed from the nearest latitude that has one, which keeps sunrise and sunset
        // plausible and leaves the twilight angles to the fallback below.
        val sunriseAngle = -0.833
        val sunHourAngle = hourAngle(sunriseAngle, latitude, declination)
            ?: hourAngle(
                sunriseAngle,
                latitude.coerceIn(-SUNRISE_FALLBACK_LATITUDE, SUNRISE_FALLBACK_LATITUDE),
                declination
            )
            ?: (ASSUMED_DAYLIGHT_HOURS / 2.0)

        val sunriseTime = dhuhrTime - sunHourAngle
        val maghribTime = dhuhrTime + sunHourAngle

        // A seventh of the night, the usual estimate for Fajr and Isha where their angles are out of
        // reach - ordinary mid-latitude summers included, where the sun never dips 15 to 18 degrees.
        val nightSeventh = (24.0 - 2.0 * sunHourAngle) / 7.0

        // Fajr. The fixed offset this replaces ignored the day it was supposed to sit in: five hours
        // before midday against a sunrise at six put Fajr after sunrise, at London in June and above
        // the Arctic circle alike.
        val fajrHourAngle = hourAngle(-method.fajrAngle, latitude, declination)
            ?: (sunHourAngle + nightSeventh)
        val fajrTime = dhuhrTime - fajrHourAngle

        // Isha, which had the mirror image of the same bug: a flat 6.2 hours after midday landed it
        // before maghrib wherever the evening twilight angle could not be reached.
        val ishaTime = if (method == CalculationMethod.UMM_AL_QURA) {
            // Umm al-Qura publishes Isha as an interval after Maghrib: 120 minutes through Ramadan
            // and 90 otherwise. A flat 90 put Ramadan Isha half an hour before the Makkah calendar.
            maghribTime + if (HijriCalendarHelper.isRamadan(year, month, day)) 2.0 else 1.5
        } else {
            val angle = if (ishaMethod == "hanafi") 18.0 else method.ishaAngle
            val ishaHourAngle = hourAngle(-angle, latitude, declination)
                ?: (sunHourAngle + nightSeventh)
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
        return String.format(java.util.Locale.US, "%02d:%02d", hh, m)
    }

    private fun scaleTo360(angle: Double): Double {
        var a = angle % 360.0
        if (a < 0) a += 360.0
        return a
    }

    fun resolveTimeZoneId(timeZoneId: String?, fallback: String = "Asia/Riyadh"): String {
        return timeZoneId?.takeIf { it in availableTimeZoneIds }
            ?: fallback.takeIf { it in availableTimeZoneIds }
            ?: "UTC"
    }

    fun getTimeZoneOffset(timeZoneId: String, calendar: Calendar): Double {
        val timeZone = TimeZone.getTimeZone(resolveTimeZoneId(timeZoneId))
        return timeZone.getOffset(calendar.timeInMillis) / 3_600_000.0
    }
}
