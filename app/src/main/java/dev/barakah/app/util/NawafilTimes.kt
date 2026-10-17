package dev.barakah.app.util

/**
 * The voluntary prayer times are fixed offsets from a prayer time. The four numbers live here so
 * the home screen, the widget and the alarm scheduler cannot drift apart.
 *
 * The offsets themselves are unchanged; whether they are the right ones is a separate question.
 */
object NawafilTimes {
    /** Duha, after sunrise. */
    const val DUHA_AFTER_SUNRISE = 20

    /** Witr, after Isha. */
    const val WITR_AFTER_ISHA = 30

    /** Tahajjud, before Fajr. */
    const val TAHAJJUD_BEFORE_FAJR = -90

    /** Qiyam al-Layl, before Fajr. */
    const val QIYAM_BEFORE_FAJR = -150
}
