package dev.barakah.app.widget

import dev.barakah.app.util.PrayerCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The countdown widget's arithmetic: which prayer is next, how long is left, and what the two lines
 * say.
 *
 * Every case here is one that used to require installing the app and staring at a widget at the
 * right minute of the day.
 */
class NextPrayerCountdownTest {

    // A plain day: 05:00, 06:20, 12:10, 15:30, 18:00, 19:30.
    private val times = PrayerCalculator.PrayerTimes(
        fajr = "05:00",
        sunrise = "06:20",
        dhuhr = "12:10",
        asr = "15:30",
        maghrib = "18:00",
        isha = "19:30",
    )

    @Test
    fun `before Fajr it counts down to Fajr today`() {
        val next = NextPrayerCountdown.next(at(4, 0), times)
        assertEquals("Fajr", next.name)
        assertFalse(next.tomorrow)
        assertEquals(3600, NextPrayerCountdown.secondsUntil(at(4, 0), next))
    }

    @Test
    fun `at the prayer itself it has already moved on`() {
        // The minute of Fajr should point at sunrise, not sit at zero.
        assertEquals("Sunrise", NextPrayerCountdown.next(at(5, 0), times).name)
        assertEquals("Dhuhr", NextPrayerCountdown.next(at(6, 20), times).name)
        assertEquals("Isha", NextPrayerCountdown.next(at(18, 0), times).name)
    }

    @Test
    fun `through the day it walks the list in order`() {
        assertEquals("Fajr", NextPrayerCountdown.next(at(4, 59), times).name)
        assertEquals("Sunrise", NextPrayerCountdown.next(at(5, 30), times).name)
        assertEquals("Dhuhr", NextPrayerCountdown.next(at(6, 21), times).name)
        assertEquals("Asr", NextPrayerCountdown.next(at(12, 10), times).name)
        assertEquals("Maghrib", NextPrayerCountdown.next(at(15, 30), times).name)
        assertEquals("Isha", NextPrayerCountdown.next(at(18, 1), times).name)
    }

    @Test
    fun `after Isha it counts down to Fajr again`() {
        val next = NextPrayerCountdown.next(at(23, 0), times)
        assertEquals("Fajr", next.name)
        assertTrue(next.tomorrow)
        // 1 hour to midnight, then 5 hours to Fajr.
        assertEquals(6 * 3600, NextPrayerCountdown.secondsUntil(at(23, 0), next))
    }

    @Test
    fun `the round trip through midnight adds up`() {
        val next = NextPrayerCountdown.next(at(19, 30), times)
        assertTrue(next.tomorrow)
        assertEquals(9 * 3600 + 30 * 60, NextPrayerCountdown.secondsUntil(at(19, 30), next))
    }

    @Test
    fun `an unreadable time counts as midnight rather than crashing`() {
        val broken = PrayerCalculator.PrayerTimes("", "", "", "", "", "")
        val next = NextPrayerCountdown.next(at(12, 0), broken)
        assertEquals("Fajr", next.name)
        assertTrue(next.tomorrow)
        assertEquals(12 * 3600, NextPrayerCountdown.secondsUntil(at(12, 0), next))
    }

    @Test
    fun `the countdown never runs backwards`() {
        val past = NextPrayerCountdown.NextPrayer("Fajr", 3600, tomorrow = false)
        assertEquals(0, NextPrayerCountdown.secondsUntil(at(9, 0), past))
    }

    @Test
    fun `the countdown line in English`() {
        assertEquals(
            "Starts in 3 hr 12 min",
            NextPrayerCountdown.describe(3 * 3600 + 12 * 60, isArabic = false, useWesternNumbers = false)
        )
        assertEquals(
            "Starts in 0 hr 0 min",
            NextPrayerCountdown.describe(0, isArabic = false, useWesternNumbers = false)
        )
        // Seconds are dropped, not rounded up: 59 minutes left reads as "0 hr 59 min".
        assertEquals(
            "Starts in 0 hr 59 min",
            NextPrayerCountdown.describe(59 * 60 + 59, isArabic = false, useWesternNumbers = false)
        )
    }

    @Test
    fun `the countdown line in Arabic`() {
        assertEquals(
            "متبقي 3 ساعة و 12 دقيقة",
            NextPrayerCountdown.describe(3 * 3600 + 12 * 60, isArabic = true, useWesternNumbers = true)
        )
    }

    @Test
    fun `the countdown line before a location is chosen`() {
        assertEquals("Starts in --", NextPrayerCountdown.describeWithoutLocation(isArabic = false))
        assertEquals("متبقي --", NextPrayerCountdown.describeWithoutLocation(isArabic = true))
    }

    @Test
    fun `prayer names in Arabic`() {
        assertEquals("الفجر", NextPrayerCountdown.displayName("Fajr", isArabic = true))
        assertEquals("الشروق", NextPrayerCountdown.displayName("Sunrise", isArabic = true))
        assertEquals("الظهر", NextPrayerCountdown.displayName("Dhuhr", isArabic = true))
        assertEquals("العصر", NextPrayerCountdown.displayName("Asr", isArabic = true))
        assertEquals("المغرب", NextPrayerCountdown.displayName("Maghrib", isArabic = true))
        assertEquals("العشاء", NextPrayerCountdown.displayName("Isha", isArabic = true))
        assertEquals("Something", NextPrayerCountdown.displayName("Something", isArabic = true))
    }

    @Test
    fun `prayer names in English are left as they are`() {
        assertEquals("Fajr", NextPrayerCountdown.displayName("Fajr", isArabic = false))
        assertEquals("Sunrise", NextPrayerCountdown.displayName("Sunrise", isArabic = false))
    }

    @Test
    fun `the time shown beside the name is that prayer own time`() {
        assertEquals("05:00", NextPrayerCountdown.timeOf(times, NextPrayerCountdown.next(at(4, 0), times)))
        assertEquals("06:20", NextPrayerCountdown.timeOf(times, NextPrayerCountdown.next(at(5, 30), times)))
        assertEquals("12:10", NextPrayerCountdown.timeOf(times, NextPrayerCountdown.next(at(7, 0), times)))
        assertEquals("15:30", NextPrayerCountdown.timeOf(times, NextPrayerCountdown.next(at(13, 0), times)))
        assertEquals("18:00", NextPrayerCountdown.timeOf(times, NextPrayerCountdown.next(at(16, 0), times)))
        assertEquals("19:30", NextPrayerCountdown.timeOf(times, NextPrayerCountdown.next(at(18, 30), times)))
        // After Isha the name is tomorrow's Fajr, and so is the time beside it.
        assertEquals("05:00", NextPrayerCountdown.timeOf(times, NextPrayerCountdown.next(at(22, 0), times)))
    }

    private fun at(hour: Int, minute: Int) = hour * 3600 + minute * 60
}
