package dev.barakah.app.util

import java.time.LocalDate
import org.junit.Assert.assertEquals
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
    fun `an impossible occasion resolves to nothing`() {
        assertEquals("", HijriCalendarHelper.getGregorianEquivalent(13, 1, false))
        assertEquals("", HijriCalendarHelper.getGregorianEquivalentRange(13, 1, 1, false))
    }
}
