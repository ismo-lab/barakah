package dev.barakah.app.util

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The prayer times decide when the alarms fire and when the adhan plays, so these lock down
 * the properties that must not drift: the order of the times through a day, a midday that
 * lands where the sun actually is, the Umm al-Qura Isha offset, the two asr and isha
 * conventions, the per-prayer minute adjustments, and the high-latitude fallbacks.
 */
class PrayerCalculatorTest {

    private fun day(year: Int, month: Int, date: Int): Calendar =
        GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(year, month - 1, date)
        }

    private fun minutes(time: String): Int {
        val parts = time.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }

    @Test
    fun `times are in order through a normal day`() {
        // Makkah, 11 March 2024, Umm al-Qura zone offset.
        val times = PrayerCalculator.calculate(21.4225, 39.8262, 3.0, day(2024, 3, 11))

        val ordered = listOf(times.fajr, times.sunrise, times.dhuhr, times.asr, times.maghrib, times.isha)
        for (i in 0 until ordered.size - 1) {
            assertTrue(
                "expected ${ordered[i]} before ${ordered[i + 1]} in $ordered",
                minutes(ordered[i]) < minutes(ordered[i + 1])
            )
        }
    }

    @Test
    fun `midday follows local solar noon`() {
        // Both offsets are real: Makkah is UTC+3 at longitude 39.83, London UTC+1 (BST) at
        // -0.13. Solar noon is therefore around 12:30 and 13:03 respectively.
        val makkah = PrayerCalculator.calculate(21.4225, 39.8262, 3.0, day(2024, 3, 11))
        assertTrue("makkah dhuhr was ${makkah.dhuhr}", minutes(makkah.dhuhr) in 740..760)

        val london = PrayerCalculator.calculate(51.5074, -0.1278, 1.0, day(2024, 6, 21))
        assertTrue("london dhuhr was ${london.dhuhr}", minutes(london.dhuhr) in 770..795)
    }

    @Test
    fun `an equinox day at the equator lasts about twelve hours`() {
        val times = PrayerCalculator.calculate(0.0, 0.0, 0.0, day(2024, 3, 20))
        val daylight = minutes(times.maghrib) - minutes(times.sunrise)

        // Refraction and the sun's radius stretch the astronomical day slightly past 12h.
        assertTrue("daylight was $daylight minutes", daylight in 710..745)
    }

    @Test
    fun `umm al qura isha is ninety minutes after maghrib`() {
        val times = PrayerCalculator.calculate(
            latitude = 21.4225,
            longitude = 39.8262,
            timezoneOffset = 3.0,
            calendar = day(2024, 3, 11),
            method = PrayerCalculator.CalculationMethod.UMM_AL_QURA
        )

        assertEquals(minutes(times.maghrib) + 90, minutes(times.isha))
    }

    @Test
    fun `a larger fajr angle starts the fast earlier`() {
        val mwl = PrayerCalculator.calculate(
            21.4225, 39.8262, 3.0, day(2024, 3, 11),
            method = PrayerCalculator.CalculationMethod.MWL
        )
        val isna = PrayerCalculator.calculate(
            21.4225, 39.8262, 3.0, day(2024, 3, 11),
            method = PrayerCalculator.CalculationMethod.ISNA
        )

        assertTrue("MWL fajr ${mwl.fajr} should precede ISNA fajr ${isna.fajr}", minutes(mwl.fajr) < minutes(isna.fajr))
    }

    @Test
    fun `the hanafi conventions come later than the standard ones`() {
        val standard = PrayerCalculator.calculate(21.4225, 39.8262, 3.0, day(2024, 3, 11))
        val hanafi = PrayerCalculator.calculate(
            21.4225, 39.8262, 3.0, day(2024, 3, 11),
            asrMethod = "hanafi",
            ishaMethod = "hanafi"
        )

        assertTrue("hanafi asr ${hanafi.asr} should follow ${standard.asr}", minutes(hanafi.asr) > minutes(standard.asr))
        assertTrue("hanafi isha ${hanafi.isha} should follow ${standard.isha}", minutes(hanafi.isha) > minutes(standard.isha))
    }

    @Test
    fun `per prayer adjustments shift only their own time`() {
        val plain = PrayerCalculator.calculate(21.4225, 39.8262, 3.0, day(2024, 3, 11))
        val adjusted = PrayerCalculator.calculate(
            21.4225, 39.8262, 3.0, day(2024, 3, 11),
            adjFajr = -7,
            adjDhuhr = 7,
            adjIsha = 3
        )

        assertEquals(minutes(plain.fajr) - 7, minutes(adjusted.fajr))
        assertEquals(minutes(plain.dhuhr) + 7, minutes(adjusted.dhuhr))
        assertEquals(minutes(plain.isha) + 3, minutes(adjusted.isha))
        assertEquals(plain.sunrise, adjusted.sunrise)
        assertEquals(plain.asr, adjusted.asr)
        assertEquals(plain.maghrib, adjusted.maghrib)
    }

    @Test
    fun `a polar summer still yields usable times`() {
        // 70 degrees north in June: the sun never sets, so the hour angles cannot be solved
        // and the fallbacks have to keep every time inside a day.
        val times = PrayerCalculator.calculate(70.0, 20.0, 1.0, day(2024, 6, 21))

        val formatted = Regex("""^\d{2}:\d{2}$""")
        for (time in listOf(times.fajr, times.sunrise, times.dhuhr, times.asr, times.maghrib, times.isha)) {
            assertTrue("unexpected time '$time'", formatted.matches(time))
            assertTrue("out of range '$time'", minutes(time) in 0 until 24 * 60)
        }
    }

    @Test
    fun `time zone ids fall back when missing or unknown`() {
        assertEquals("Asia/Riyadh", PrayerCalculator.resolveTimeZoneId(null))
        assertEquals("UTC", PrayerCalculator.resolveTimeZoneId(null, "UTC"))
        assertEquals("Asia/Riyadh", PrayerCalculator.resolveTimeZoneId("Not/AnActualZone"))
        assertEquals("Europe/London", PrayerCalculator.resolveTimeZoneId("Europe/London"))
    }

    @Test
    fun `time zone offsets come from the calendar`() {
        val january = day(2024, 1, 15)
        val july = day(2024, 7, 15)

        // Riyadh has no daylight saving; London does.
        assertEquals(3.0, PrayerCalculator.getTimeZoneOffset("Asia/Riyadh", january), 0.001)
        assertEquals(3.0, PrayerCalculator.getTimeZoneOffset("Asia/Riyadh", july), 0.001)
        assertEquals(0.0, PrayerCalculator.getTimeZoneOffset("UTC", january), 0.001)
        assertEquals(0.0, PrayerCalculator.getTimeZoneOffset("Europe/London", january), 0.001)
        assertEquals(1.0, PrayerCalculator.getTimeZoneOffset("Europe/London", july), 0.001)
    }
}
