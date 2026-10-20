package dev.barakah.app.widget

import dev.barakah.app.data.UiStrings

import dev.barakah.app.util.ClockTime
import dev.barakah.app.util.PrayerCalculator
import dev.barakah.app.util.localize

/**
 * What the countdown widget says: which prayer comes next, how long is left, and how to write both
 * in the user's language.
 *
 * This arithmetic used to sit inline in the widget's composition, where the only way to check it
 * was to look at the widget. It is the part worth pinning down, because it is the part that has
 * been wrong: a countdown that reaches for tomorrow's Fajr at the wrong moment, or that formats
 * midnight as "0:00 AM", is only visible if you happen to be looking at the screen.
 *
 * Nothing here reads the clock, the device, or the preferences. Give it a time of day and the
 * calculated prayer times and the answer is fixed, which is what makes it testable.
 */
object NextPrayerCountdown {

    /** Shown in place of the name when no location has been chosen yet. */
    const val PLACEHOLDER = "--"

    /**
     * The prayer being counted down to.
     *
     * @param secondsOfDay when the prayer falls, as seconds since midnight
     * @param tomorrow true for the Fajr counted down to after Isha has passed
     */
    data class NextPrayer(
        val name: String,
        val secondsOfDay: Int,
        val tomorrow: Boolean,
    )

    /**
     * The first prayer still ahead today, or tomorrow's Fajr once Isha has passed.
     *
     * A prayer that is exactly now counts as passed, so the widget moves on to the next one rather
     * than sitting at zero for a whole minute. A time that cannot be read counts as midnight, which
     * is what the comparisons have always treated it as.
     */
    fun next(nowSecondsOfDay: Int, times: PrayerCalculator.PrayerTimes): NextPrayer {
        val today = listOf(
            "Fajr" to secondsOf(times.fajr),
            "Sunrise" to secondsOf(times.sunrise),
            "Dhuhr" to secondsOf(times.dhuhr),
            "Asr" to secondsOf(times.asr),
            "Maghrib" to secondsOf(times.maghrib),
            "Isha" to secondsOf(times.isha),
        )
        val first = today.firstOrNull { nowSecondsOfDay < it.second }
        if (first != null) {
            return NextPrayer(first.first, first.second, tomorrow = false)
        }
        return NextPrayer("Fajr", secondsOf(times.fajr), tomorrow = true)
    }

    /** A time that cannot be read counts as midnight, which is how the comparisons treat it. */
    private fun secondsOf(value: String): Int = ClockTime.secondsOfDay(value) ?: 0

    /** Whole seconds from [nowSecondsOfDay] until [next], never negative. */
    fun secondsUntil(nowSecondsOfDay: Int, next: NextPrayer): Int {
        val seconds = if (next.tomorrow) {
            (24 * 3600 - nowSecondsOfDay) + next.secondsOfDay
        } else {
            next.secondsOfDay - nowSecondsOfDay
        }
        // The choice above cannot look backwards, so this only matters if the clock does something
        // strange; it is here so a countdown can never read "-1 hr 59 min".
        return seconds.coerceAtLeast(0)
    }

    /** "3 hr 12 min" for [seconds] left, in the user's language and digits. */
    fun describe(seconds: Int, isArabic: Boolean, useWesternNumbers: Boolean): String {
        val hours = (seconds / 3600).toString().localize(isArabic, useWesternNumbers)
        val minutes = ((seconds % 3600) / 60).toString().localize(isArabic, useWesternNumbers)
        return if (isArabic) {
            "متبقي $hours ساعة و $minutes دقيقة"
        } else {
            UiStrings.format("Starts in %s hr %s min", hours, minutes)
        }
    }

    /** The countdown line before a location has been chosen. */
    fun describeWithoutLocation(isArabic: Boolean): String =
        if (isArabic) "متبقي --" else UiStrings.of("Starts in --")

    /** The prayer name in the user's language; anything unknown is passed through. */
    fun displayName(name: String, isArabic: Boolean): String {
        if (!isArabic) return UiStrings.of(name)
        return when (name) {
            "Fajr" -> "الفجر"
            "Sunrise" -> "الشروق"
            "Dhuhr" -> "الظهر"
            "Asr" -> "العصر"
            "Maghrib" -> "المغرب"
            "Isha" -> "العشاء"
            else -> name
        }
    }

    /** The clock time [next] falls at, as the calculator wrote it, or "" if the name is unknown. */
    fun timeOf(times: PrayerCalculator.PrayerTimes, next: NextPrayer): String = when (next.name) {
        "Fajr" -> times.fajr
        "Sunrise" -> times.sunrise
        "Dhuhr" -> times.dhuhr
        "Asr" -> times.asr
        "Maghrib" -> times.maghrib
        "Isha" -> times.isha
        else -> ""
    }
}
