package dev.barakah.app.util

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * The clock arithmetic the widgets and the alarm scheduler share.
 *
 * Prayer times reach both of them as "HH:mm" strings from [PrayerCalculator], and each place used
 * to parse and format them on its own. Gathering it here means the three widgets and the scheduler
 * cannot drift apart, and that the parsing has one answer instead of five.
 *
 * Nothing here reads the clock or the device: given a string and a time zone the result is fixed,
 * which is what makes it testable.
 */
object ClockTime {

    /** Splits "05:01" or "5:01 AM" into an hour and a minute, or null if it is neither. */
    fun hourAndMinute(value: String): Pair<Int, Int>? {
        val parts = value.trim().split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: return null
        val minute = parts.getOrNull(1)?.substringBefore(" ")?.trim()?.toIntOrNull() ?: return null
        return hour to minute
    }

    /** Seconds since midnight for [value], or null if it is not a clock time. */
    fun secondsOfDay(value: String): Int? {
        val (hour, minute) = hourAndMinute(value) ?: return null
        return hour * 3600 + minute * 60
    }

    /**
     * [value] shifted by [offsetMinutes], as "HH:mm", wrapping across midnight.
     *
     * Returns an empty string when [value] is not a clock time. Callers treat that as "skip this
     * one" rather than scheduling something they could not work out.
     */
    fun offset(value: String, offsetMinutes: Int, timeZone: TimeZone): String {
        val (hour, minute) = hourAndMinute(value) ?: return ""
        val shifted = Calendar.getInstance(timeZone).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            add(Calendar.MINUTE, offsetMinutes)
        }
        return String.format(
            Locale.US,
            "%02d:%02d",
            shifted.get(Calendar.HOUR_OF_DAY),
            shifted.get(Calendar.MINUTE)
        )
    }

    /**
     * [value] ready for display: 24-hour or 12-hour, Western or Arabic-Indic digits.
     *
     * [padHour] exists because the narrow widgets drop the leading zero to fit ("6:15 AM" rather
     * than "06:15 AM") while the others keep it. Anything that cannot be parsed comes back
     * localized but otherwise untouched, so a placeholder like "--:--" survives.
     */
    fun format(
        value: String,
        is24Hour: Boolean,
        isArabic: Boolean,
        useWesternNumbers: Boolean,
        padHour: Boolean = true,
    ): String {
        val (hour, minute) = hourAndMinute(value)
            ?: return value.localize(isArabic, useWesternNumbers)

        val formatted = if (is24Hour) {
            String.format(Locale.US, "%02d:%02d", hour, minute)
        } else {
            val hour12 = if (hour % 12 == 0) 12 else hour % 12
            val amPm = if (hour < 12) {
                if (isArabic) "ص" else "AM"
            } else {
                if (isArabic) "م" else "PM"
            }
            String.format(Locale.US, if (padHour) "%02d:%02d %s" else "%d:%02d %s", hour12, minute, amPm)
        }
        return formatted.localize(isArabic, useWesternNumbers)
    }
}
