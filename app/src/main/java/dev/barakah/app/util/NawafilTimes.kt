package dev.barakah.app.util

import java.util.Locale

/**
 * The voluntary prayer times the home screen, the widget and the alarm scheduler share.
 *
 * Duha and Witr are fixed offsets because the sunnah gives a window rather than an instant.
 * Tahajjud is not: it belongs to the last third of the night, and Qiyam al-Layl is timed from the
 * Islamic midnight that begins the second half. Both are derived from the Maghrib-to-Fajr night,
 * so they move with the season instead of sitting at a fixed distance before Fajr.
 */
object NawafilTimes {
    /** Duha, after sunrise. */
    const val DUHA_AFTER_SUNRISE = 20

    /** Witr, after Isha. */
    const val WITR_AFTER_ISHA = 30

    private const val DAY_SECONDS = 24 * 60 * 60

    data class Schedule(
        val duha: String,
        val witr: String,
        val firstThird: String,
        /** Islamic midnight, where Qiyam al-Layl starts. */
        val qiyam: String,
        /** Start of the last third, the preferred time for Tahajjud. */
        val tahajjud: String
    )

    /** Returns null when the prayer times are placeholders, so callers can show "--:--". */
    fun schedule(times: PrayerCalculator.PrayerTimes): Schedule? {
        val fajr = ClockTime.secondsOfDay(times.fajr) ?: return null
        val sunrise = ClockTime.secondsOfDay(times.sunrise) ?: return null
        val maghrib = ClockTime.secondsOfDay(times.maghrib) ?: return null
        val isha = ClockTime.secondsOfDay(times.isha) ?: return null

        val nightEnd = if (fajr <= maghrib) fajr + DAY_SECONDS else fajr
        val night = nightEnd - maghrib
        if (night <= 0) return null

        return Schedule(
            duha = format(sunrise + DUHA_AFTER_SUNRISE * 60),
            witr = format(isha + WITR_AFTER_ISHA * 60),
            firstThird = format(maghrib + night / 3),
            qiyam = format(maghrib + night / 2),
            tahajjud = format(maghrib + (night * 2) / 3)
        )
    }

    private fun format(seconds: Int): String {
        val wrapped = ((seconds % DAY_SECONDS) + DAY_SECONDS) % DAY_SECONDS
        return String.format(Locale.US, "%02d:%02d", wrapped / 3600, (wrapped % 3600) / 60)
    }
}
