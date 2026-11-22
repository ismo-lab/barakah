package dev.barakah.app.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The Hijri date is shown on the home screen and used to schedule the occasion reminders,
 * so the calendar conversion and the day-to-day rollover are worth pinning down.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HijriCalendarHelperTest {

    @Test
    fun `known dates land on the umm al qura calendar`() {
        // 1 Ramadan 1445 was 11 March 2024, and 1 Muharram 1446 was 7 July 2024.
        val ramadan = HijriCalendarHelper.getHijriDateFor(LocalDate.of(2024, 3, 11))
        assertEquals(1, ramadan.day)
        assertEquals(9, ramadan.month)
        assertEquals(1445, ramadan.year)

        val newYear = HijriCalendarHelper.getHijriDateFor(LocalDate.of(2024, 7, 7))
        assertEquals(1, newYear.day)
        assertEquals(1, newYear.month)
        assertEquals(1446, newYear.year)
    }

    @Test
    fun `every day advances the date by exactly one`() {
        val start = LocalDate.of(2024, 2, 1)
        var previous = HijriCalendarHelper.getHijriDateFor(start)

        for (offset in 1..400L) {
            val current = HijriCalendarHelper.getHijriDateFor(start.plusDays(offset))

            val nextDayInMonth = current.month == previous.month &&
                current.year == previous.year &&
                current.day == previous.day + 1
            val startOfNextMonth = current.day == 1 && current.year == previous.year &&
                current.month == previous.month + 1
            val startOfNextYear = current.day == 1 && current.month == 1 &&
                current.year == previous.year + 1

            assertTrue("$previous followed by $current", nextDayInMonth || startOfNextMonth || startOfNextYear)
            previous = current
        }
    }

    @Test
    fun `today and tomorrow differ by a day`() {
        val today = HijriCalendarHelper.getTodayHijriDate()
        val tomorrow = HijriCalendarHelper.getTomorrowHijriDate()

        val nextDayInMonth = tomorrow.month == today.month &&
            tomorrow.year == today.year &&
            tomorrow.day == today.day + 1
        val startOfMonth = tomorrow.day == 1

        assertTrue("$today followed by $tomorrow", nextDayInMonth || startOfMonth)
    }

    @Test
    fun `the formatted date carries the month name and the year`() {
        val today = HijriCalendarHelper.getTodayHijriDate()

        val english = HijriCalendarHelper.getTodayHijriDateString(isAr = false)
        assertTrue(english, english.endsWith(" AH"))
        assertTrue(english, english.contains(" ${today.year} "))

        val arabic = HijriCalendarHelper.getTodayHijriDateString(isAr = true)
        assertTrue(arabic, arabic.endsWith(" هـ"))
    }

    @Test
    fun `a real occasion resolves to a gregorian date`() {
        val date = HijriCalendarHelper.getGregorianEquivalent(9, 1, false)

        assertTrue("1 Ramadan resolved to '$date'", Regex("""^\d{1,2} [A-Z][a-z]{2} \d{4}$""").matches(date))

        val range = HijriCalendarHelper.getGregorianEquivalentRange(9, 1, 3, false)
        assertTrue("range was '$range'", range.contains(" to "))
    }

    @Test
    fun `the date shown for an occasion converts back to that same occasion`() {
        // The occasions list and the occasion reminders used to convert with two different
        // calendars, so the Eid date on screen could be a day away from the day reminded about.
        // Whatever date the list shows has to convert back to the day it claims -- with and
        // without the user's adjustment.
        val occasions = listOf(
            1 to 1, 1 to 10, 3 to 12, 7 to 27, 8 to 15,
            9 to 1, 9 to 26, 10 to 1, 11 to 1, 12 to 9, 12 to 10
        )

        for (adjustment in listOf(0, 1, -1)) {
            for ((month, day) in occasions) {
                val shown = HijriCalendarHelper.getGregorianEquivalent(month, day, isAr = false, adjustmentDays = adjustment)
                assertTrue("$day/$month ($adjustment) showed '$shown'", shown.isNotEmpty())

                val gregorian = LocalDate.parse(shown, DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH))
                val hijri = HijriCalendarHelper.getHijriDateFor(gregorian.plusDays(adjustment.toLong()))

                assertEquals("$day/$month ($adjustment) showed $shown", day, hijri.day)
                assertEquals("$day/$month ($adjustment) showed $shown", month, hijri.month)
            }
        }
    }

    @Test
    fun `an adjustment moves the date by exactly those days`() {
        // The shift is applied by moving the day the conversion is done for, so +1 has to read
        // exactly like tomorrow and -1 exactly like today.
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
    fun `the last ten nights resolve even when ramadan is short`() {
        // Ramadan is 29 days long in some years, so the 30th night does not exist that year and
        // the range has to end on the last night that does.
        val range = HijriCalendarHelper.getGregorianEquivalentRange(9, 21, 30, false)

        assertTrue("range was '$range'", range.contains(" to "))
    }

    @Test
    fun `ramadan is recognised from the calculated date`() {
        assertTrue(HijriCalendarHelper.isRamadan(2024, 3, 11))
        assertFalse(HijriCalendarHelper.isRamadan(2024, 6, 21))
    }

    @Test
    fun `countries that sight locally default a day behind`() {
        // Local sighting usually starts the month after the Umm al-Qura table this app computes.
        assertEquals(-1, HijriCalendarHelper.countryAdjustmentDays("pk"))
        assertEquals(-1, HijriCalendarHelper.countryAdjustmentDays(" PK "))
        assertEquals(0, HijriCalendarHelper.countryAdjustmentDays("SA"))
        assertEquals(0, HijriCalendarHelper.countryAdjustmentDays(null))
    }

    @Test
    fun `an impossible occasion resolves to nothing`() {
        assertEquals("", HijriCalendarHelper.getGregorianEquivalent(13, 1, false))
        assertEquals("", HijriCalendarHelper.getGregorianEquivalentRange(13, 1, 1, false))
    }
}
