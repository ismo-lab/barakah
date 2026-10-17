package dev.barakah.app.util

import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The clock arithmetic the widgets and the alarm scheduler share.
 *
 * These are the pieces that decide what a widget prints and when an alarm is set. They used to be
 * written out five times over, with the same edges wrong in more than one of them, so the cases
 * worth keeping are the edges: midnight, noon, and a time that cannot be read at all.
 */
class ClockTimeTest {

    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun `reads the times the calculator emits`() {
        assertEquals(5 to 1, ClockTime.hourAndMinute("05:01"))
        assertEquals(0 to 0, ClockTime.hourAndMinute("00:00"))
        assertEquals(23 to 59, ClockTime.hourAndMinute("23:59"))
    }

    @Test
    fun `tolerates the spacing a stored value can pick up`() {
        assertEquals(5 to 1, ClockTime.hourAndMinute(" 05:01 "))
        assertEquals(5 to 1, ClockTime.hourAndMinute("5:01 AM"))
        assertEquals(13 to 45, ClockTime.hourAndMinute("13:45 "))
    }

    @Test
    fun `anything that is not a clock time is rejected, not guessed at`() {
        assertNull(ClockTime.hourAndMinute(""))
        assertNull(ClockTime.hourAndMinute("--:--"))
        assertNull(ClockTime.hourAndMinute("noon"))
        assertNull(ClockTime.hourAndMinute("05:"))
        assertNull(ClockTime.hourAndMinute("half:past"))
    }

    @Test
    fun `seconds since midnight`() {
        assertEquals(0, ClockTime.secondsOfDay("00:00"))
        assertEquals(5 * 3600 + 60, ClockTime.secondsOfDay("05:01"))
        assertEquals(24 * 3600 - 60, ClockTime.secondsOfDay("23:59"))
        assertNull(ClockTime.secondsOfDay("oops"))
    }

    @Test
    fun `an offset moves a time forwards and backwards`() {
        assertEquals("05:15", ClockTime.offset("05:00", 15, utc))
        assertEquals("04:45", ClockTime.offset("05:00", -15, utc))
        assertEquals("05:00", ClockTime.offset("05:00", 0, utc))
    }

    @Test
    fun `an offset wraps past midnight in both directions`() {
        assertEquals("00:05", ClockTime.offset("23:50", 15, utc))
        assertEquals("23:50", ClockTime.offset("00:05", -15, utc))
        assertEquals("02:30", ClockTime.offset("05:00", -150, utc))
    }

    @Test
    fun `an offset of something unreadable is empty, so the caller skips it`() {
        assertEquals("", ClockTime.offset("", -15, utc))
        assertEquals("", ClockTime.offset("--:--", -15, utc))
    }

    @Test
    fun `24 hour times keep both fields padded`() {
        assertEquals("06:15", format24("06:15"))
        assertEquals("00:05", format24("0:05"))
        // Even the narrow widget pads in 24-hour form: the columns have to line up.
        assertEquals("06:15", ClockTime.format("06:15", is24Hour = true, isArabic = false, useWesternNumbers = false, padHour = false))
    }

    @Test
    fun `12 hour times know midnight and noon`() {
        assertEquals("12:00 AM", format12("00:00"))
        assertEquals("12:00 PM", format12("12:00"))
        assertEquals("01:05 AM", format12("01:05"))
        assertEquals("12:30 PM", format12("12:30"))
        assertEquals("11:59 PM", format12("23:59"))
    }

    @Test
    fun `12 hour times in Arabic use the Arabic markers`() {
        assertEquals("06:15 ص", ClockTime.format("06:15", is24Hour = false, isArabic = true, useWesternNumbers = true))
        assertEquals("06:15 م", ClockTime.format("18:15", is24Hour = false, isArabic = true, useWesternNumbers = true))
    }

    @Test
    fun `the four column widget can drop the leading zero`() {
        assertEquals("6:15 AM", ClockTime.format("06:15", is24Hour = false, isArabic = false, useWesternNumbers = false, padHour = false))
    }

    @Test
    fun `Arabic-Indic digits are used when the device is set up for them`() {
        val original = Locale.getDefault()
        try {
            Locale.setDefault(Locale.forLanguageTag("ar"))
            assertEquals("٠٦:١٥ ص", ClockTime.format("06:15", is24Hour = false, isArabic = true, useWesternNumbers = false))
            // The user's own preference wins over the device.
            assertEquals("06:15 ص", ClockTime.format("06:15", is24Hour = false, isArabic = true, useWesternNumbers = true))
        } finally {
            Locale.setDefault(original)
        }
    }

    @Test
    fun `a value that cannot be read is passed through untouched`() {
        assertEquals("--:--", format24("--:--"))
        assertEquals("", format24(""))
    }

    private fun format24(value: String) =
        ClockTime.format(value, is24Hour = true, isArabic = false, useWesternNumbers = false)

    private fun format12(value: String) =
        ClockTime.format(value, is24Hour = false, isArabic = false, useWesternNumbers = false)
}
