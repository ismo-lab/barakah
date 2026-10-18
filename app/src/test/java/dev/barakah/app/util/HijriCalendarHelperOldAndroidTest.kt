package dev.barakah.app.util

import android.os.Build
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The same helper on a device too old for `java.time`.
 *
 * `hijriFor` holds two implementations of one conversion: Umm al-Qura through `java.time` where the
 * platform has it, and the arithmetic fallback everywhere else. Every other test in the suite runs
 * on API 36, so the fallback — the one that runs on every Android 7 device, which the app still
 * supports — has never been executed by a test.
 *
 * This does not claim the fallback agrees with Umm al-Qura. It is an approximation and can sit a
 * day either side. What it pins down is what the app actually depends on: that a day is followed by
 * the next day rather than by two days or a month, that the days of a month come out as consecutive
 * Gregorian days with no gaps or repeats, and that an occasion still resolves to a date instead of
 * quietly disappearing from the list.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24])
class HijriCalendarHelperOldAndroidTest {

    private val datePattern = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

    @Test
    fun `this runs on the android version the fallback is for`() {
        // The class is only worth anything while `hijriFor` takes its fallback branch, which is
        // decided by SDK_INT. A Robolectric default or an @Config edit could move this class to a
        // newer Android, where the java.time path would run and every test below would still pass
        // while testing the implementation they claim to be an alternative to. So assert the
        // premise rather than trusting the annotation.
        assertEquals(24, Build.VERSION.SDK_INT)
    }

    @Test
    fun `a day is followed by the next day`() {
        val today = HijriCalendarHelper.getTodayHijriDate()
        val tomorrow = HijriCalendarHelper.getTomorrowHijriDate()

        assertTrue("day out of range: $today", today.day in 1..30)
        assertTrue("month out of range: $today", today.month in 1..12)
        assertTrue("year looks wrong: $today", today.year in 1440..1460)

        val nextDayInMonth = tomorrow.month == today.month &&
            tomorrow.year == today.year &&
            tomorrow.day == today.day + 1
        val startOfMonth = tomorrow.day == 1 && tomorrow.month == today.month + 1 &&
            tomorrow.year == today.year
        val startOfYear = tomorrow.day == 1 && tomorrow.month == 1 &&
            tomorrow.year == today.year + 1

        assertTrue("$today followed by $tomorrow", nextDayInMonth || startOfMonth || startOfYear)
    }

    @Test
    fun `the formatted date agrees with the numbers`() {
        val today = HijriCalendarHelper.getTodayHijriDate()

        val english = HijriCalendarHelper.getTodayHijriDateString(isAr = false)
        assertTrue(english, english.endsWith(" AH"))
        assertTrue(english, english.startsWith("${today.day} "))
        assertTrue(english, english.contains(" ${today.year} "))

        val arabic = HijriCalendarHelper.getTodayHijriDateString(isAr = true)
        assertTrue(arabic, arabic.endsWith(" هـ"))
        assertTrue(arabic, arabic.startsWith("${today.day} "))
    }

    @Test
    fun `an adjustment moves the date by exactly those days`() {
        assertEquals(
            HijriCalendarHelper.getTomorrowHijriDate(),
            HijriCalendarHelper.getTodayHijriDate(adjustmentDays = 1)
        )
        assertEquals(
            HijriCalendarHelper.getTodayHijriDate(),
            HijriCalendarHelper.getTomorrowHijriDate(adjustmentDays = -1)
        )
    }

    @Test
    fun `every occasion still resolves to a gregorian date`() {
        val occasions = listOf(
            1 to 1, 1 to 10, 3 to 12, 7 to 27, 8 to 15,
            9 to 1, 9 to 26, 10 to 1, 11 to 1, 12 to 9, 12 to 10
        )

        for ((month, day) in occasions) {
            val shown = HijriCalendarHelper.getGregorianEquivalent(month, day, isAr = false)
            assertTrue("$day/$month resolved to nothing", shown.isNotEmpty())
            // Throws rather than passing if the fallback produced something that is not a date.
            LocalDate.parse(shown, datePattern)
        }

        assertTrue(
            HijriCalendarHelper.getGregorianEquivalentRange(9, 1, 3, false).contains(" to ")
        )
        assertTrue(
            HijriCalendarHelper.getGregorianEquivalentRange(9, 21, 30, false).contains(" to ")
        )
    }

    @Test
    fun `the days of a month are consecutive gregorian days`() {
        // Ramadan and Dhul-Hijjah: the two months whose days the occasion list actually shows, so a
        // gap, a repeat or a day that never resolves is something a user would see.
        for (month in listOf(9, 12)) {
            var previous: LocalDate? = null
            for (day in 1..29) {
                val shown = HijriCalendarHelper.getGregorianEquivalent(month, day, isAr = false)
                assertTrue("$day/$month resolved to nothing", shown.isNotEmpty())

                val gregorian = LocalDate.parse(shown, datePattern)
                val expected = previous?.plusDays(1)
                if (expected != null) {
                    assertEquals("$day/$month resolved to $shown", expected, gregorian)
                }
                previous = gregorian
            }
        }
    }
}
