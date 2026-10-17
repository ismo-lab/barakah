package dev.barakah.app.notifications

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Choosing between today's prayer and tomorrow's.
 *
 * The case at the bottom of this file is the one that shipped broken: a prayer 60 seconds away was
 * read as passed, moved to the next day, and never announced.
 */
class PrayerAlarmTimesTest {

    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun `a time later today is today`() {
        val now = at(2026, 10, 17, 4, 30)
        assertEquals("2026-10-17 05:00:00.000", local(next("05:00", now)))
    }

    @Test
    fun `a time that has already passed is tomorrow`() {
        val now = at(2026, 10, 17, 6, 0)
        assertEquals("2026-10-18 05:00:00.000", local(next("05:00", now)))
    }

    @Test
    fun `a time that is exactly now is tomorrow`() {
        // Arming twice within the same second must not fire the prayer again.
        val now = at(2026, 10, 17, 5, 0)
        assertEquals("2026-10-18 05:00:00.000", local(next("05:00", now)))
    }

    @Test
    fun `the next minute is not treated as gone`() {
        // The regression: the alarm for 05:00 set at 04:59:59.999 has to be today's 05:00.
        val now = at(2026, 10, 17, 4, 59, second = 59, millis = 999)
        val occurrence = next("05:00", now)
        assertEquals("2026-10-17 05:00:00.000", local(occurrence))
        assertEquals(1, occurrence - now)
    }

    @Test
    fun `seconds are dropped so the alarm lands on the minute`() {
        val now = at(2026, 10, 17, 4, 0)
        assertEquals("2026-10-17 05:00:00.000", local(next("05:00", now)))
    }

    @Test
    fun `midnight is a time like any other`() {
        val now = at(2026, 10, 17, 12, 0)
        assertEquals("2026-10-18 00:00:00.000", local(next("00:00", now)))
        assertEquals("2026-10-17 23:59:00.000", local(next("23:59", now)))
    }

    @Test
    fun `every occurrence is in the future`() {
        val now = at(2026, 10, 17, 13, 37, second = 42)
        for (time in listOf("00:00", "05:00", "12:10", "15:30", "18:00", "19:30", "23:59")) {
            val occurrence = next(time, now)
            assertTrue("$time should be in the future", occurrence > now)
            // ...and no further away than a day.
            assertTrue("$time should be within a day", occurrence - now <= 24 * 60 * 60 * 1000L)
        }
    }

    @Test
    fun `a time that cannot be read has no occurrence`() {
        val now = at(2026, 10, 17, 4, 30)
        assertNull(PrayerAlarmTimes.nextOccurrence("", now, utc))
        assertNull(PrayerAlarmTimes.nextOccurrence("--:--", now, utc))
        assertNull(PrayerAlarmTimes.nextOccurrence("noon", now, utc))
        assertNull(PrayerAlarmTimes.nextOccurrence("05:", now, utc))
    }

    private fun next(timeOfDay: String, nowMillis: Long) =
        requireNotNull(PrayerAlarmTimes.nextOccurrence(timeOfDay, nowMillis, utc)) {
            "$timeOfDay should have had an occurrence"
        }

    private fun at(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int = 0, millis: Int = 0): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(year, month - 1, day, hour, minute, second)
            set(Calendar.MILLISECOND, millis)
        }.timeInMillis

    private fun local(millis: Long): String {
        val moment = Calendar.getInstance(utc).apply { timeInMillis = millis }
        return String.format(
            Locale.US,
            "%04d-%02d-%02d %02d:%02d:%02d.%03d",
            moment.get(Calendar.YEAR),
            moment.get(Calendar.MONTH) + 1,
            moment.get(Calendar.DAY_OF_MONTH),
            moment.get(Calendar.HOUR_OF_DAY),
            moment.get(Calendar.MINUTE),
            moment.get(Calendar.SECOND),
            moment.get(Calendar.MILLISECOND)
        )
    }
}
