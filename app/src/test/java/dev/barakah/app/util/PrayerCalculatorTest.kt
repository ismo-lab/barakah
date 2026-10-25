package dev.barakah.app.util

import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

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
    fun `umm al qura isha follows the published interval`() {
        // Umm al-Qura gives Isha as an interval after Maghrib: 90 minutes normally and 120 through
        // Ramadan. The Makkah calendar for 11 March 2024 (1 Ramadan 1445) lists 20:29 against an
        // 18:29 maghrib, and 21 June 2024 lists 20:36 against 19:06.
        val ramadan = PrayerCalculator.calculate(
            latitude = 21.4225,
            longitude = 39.8262,
            timezoneOffset = 3.0,
            calendar = day(2024, 3, 11),
            method = PrayerCalculator.CalculationMethod.UMM_AL_QURA
        )
        assertEquals(minutes(ramadan.maghrib) + 120, minutes(ramadan.isha))

        val ordinary = PrayerCalculator.calculate(
            latitude = 21.4225,
            longitude = 39.8262,
            timezoneOffset = 3.0,
            calendar = day(2024, 6, 21),
            method = PrayerCalculator.CalculationMethod.UMM_AL_QURA
        )
        assertEquals(minutes(ordinary.maghrib) + 90, minutes(ordinary.isha))
    }

    @Test
    fun `makkah matches the published umm al qura calendar`() {
        // Reference: api.aladhan.com method 4 (Umm al-Qura) at 21.3890824/39.8579118, for a Ramadan
        // day and an ordinary one. The app solves the same solar geometry with a low-precision solar
        // position, so a minute or two is the expected agreement. This catches a formula drifting
        // by more than that; it does not claim the authority's own rounding or criteria.
        val reference = linkedMapOf(
            day(2024, 3, 11) to listOf("05:17", "06:33", "12:31", "15:54", "18:29", "20:29"),
            day(2024, 6, 21) to listOf("04:11", "05:39", "12:23", "15:42", "19:06", "20:36")
        )

        for ((date, expected) in reference) {
            val times = PrayerCalculator.calculate(
                latitude = 21.3890824,
                longitude = 39.8579118,
                timezoneOffset = 3.0,
                calendar = date,
                method = PrayerCalculator.CalculationMethod.UMM_AL_QURA
            )
            val actual = listOf(times.fajr, times.sunrise, times.dhuhr, times.asr, times.maghrib, times.isha)

            for (i in expected.indices) {
                assertTrue(
                    "expected ~${expected[i]}, got ${actual[i]} for ${date.time}",
                    abs(minutes(actual[i]) - minutes(expected[i])) <= 3
                )
            }
        }
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
    fun `the high latitude fallbacks keep every time in order`() {
        // Above the Arctic circle the sun does not cross the horizon on either solstice: sunrise and
        // maghrib have to be borrowed from a lower latitude, and in June the twilight angles fall
        // back as well.
        for (date in listOf(day(2024, 6, 21), day(2024, 12, 21))) {
            val times = PrayerCalculator.calculate(69.65, 18.96, 1.0, date)
            val ordered = listOf(times.fajr, times.sunrise, times.dhuhr, times.asr, times.maghrib, times.isha)

            for (i in 0 until ordered.size - 1) {
                assertTrue(
                    "expected ${ordered[i]} before ${ordered[i + 1]} in $ordered",
                    minutes(ordered[i]) < minutes(ordered[i + 1])
                )
            }
        }
    }

    @Test
    fun `an unreachable twilight angle stays inside the day`() {
        // London in June: the sun rises and sets, but it never dips 18 degrees, so Fajr falls back.
        // The fixed offsets this replaced put Fajr five hours before midday, at 08:03, against a
        // sunrise at 04:44 - and put Isha at 19:15, before a 21:22 maghrib.
        val times = PrayerCalculator.calculate(51.5074, -0.1278, 1.0, day(2024, 6, 21))

        assertTrue(
            "fajr ${times.fajr} should precede sunrise ${times.sunrise}",
            minutes(times.fajr) < minutes(times.sunrise)
        )
        assertTrue(
            "isha ${times.isha} should follow maghrib ${times.maghrib}",
            minutes(times.isha) > minutes(times.maghrib)
        )
        // A seventh of a seven hour night, so Fajr belongs within an hour or so of sunrise.
        assertTrue(
            "fajr ${times.fajr} drifted too far before sunrise ${times.sunrise}",
            minutes(times.sunrise) - minutes(times.fajr) < 180
        )
    }

    @Test
    fun `a reachable twilight angle is still held inside a seventh of the night`() {
        // London on the June solstice with ISNA's 15 degree angles. The sun does dip 15 degrees, so
        // the raw hour angle resolves - and used to put Fajr at 01:16 and Isha at 00:49, Isha after
        // midnight and before the next Fajr. A seventh of the night is the cap every authority
        // applies, and what adhan 4.4.3 returns for the same day (03:40 and 22:25).
        val times = PrayerCalculator.calculate(
            51.5074, -0.1278, 1.0, day(2024, 6, 21),
            method = PrayerCalculator.CalculationMethod.ISNA
        )

        assertTrue("fajr was ${times.fajr}, expected about 03:40", abs(minutes(times.fajr) - 220) <= 10)
        assertTrue("isha was ${times.isha}, expected about 22:25", abs(minutes(times.isha) - 1345) <= 10)
        assertTrue(
            "fajr ${times.fajr} should precede sunrise ${times.sunrise}",
            minutes(times.fajr) < minutes(times.sunrise)
        )
        assertTrue(
            "isha ${times.isha} should follow maghrib ${times.maghrib}",
            minutes(times.isha) > minutes(times.maghrib)
        )
    }

    @Test
    fun `the seventh of the night cap leaves an ordinary day alone`() {
        // Makkah in March: both twilight angles resolve well inside the cap, so it must change
        // nothing. If this ever fails, the bound is being applied where it was not needed.
        val standard = PrayerCalculator.calculate(21.4225, 39.8262, 3.0, day(2024, 3, 11))

        assertTrue("fajr was ${standard.fajr}, expected about 05:19", abs(minutes(standard.fajr) - 319) <= 2)
        assertTrue("isha was ${standard.isha}, expected about 19:38", abs(minutes(standard.isha) - 1178) <= 2)
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

    @Test
    fun `phone clock offset moves every prayer by the same amount`() {
        val calendar = day(2024, 3, 11)
        val base = PrayerCalculator.calculate(21.4225, 39.8262, 3.0, calendar)
        val shifted = PrayerCalculator.calculate(
            21.4225, 39.8262, 3.0, calendar, clockOffsetMinutes = -60
        )

        val pairs = listOf(
            "fajr" to (base.fajr to shifted.fajr),
            "sunrise" to (base.sunrise to shifted.sunrise),
            "dhuhr" to (base.dhuhr to shifted.dhuhr),
            "asr" to (base.asr to shifted.asr),
            "maghrib" to (base.maghrib to shifted.maghrib),
            "isha" to (base.isha to shifted.isha)
        )
        for ((name, pair) in pairs) {
            assertEquals("$name moved by the wrong amount: ${pair.first} to ${pair.second}",
                minutes(pair.first) - 60, minutes(pair.second))
        }
    }

    @Test
    fun `phone clock offset adds to the per prayer adjustment`() {
        val calendar = day(2024, 3, 11)
        val base = PrayerCalculator.calculate(21.4225, 39.8262, 3.0, calendar)
        val shifted = PrayerCalculator.calculate(
            21.4225, 39.8262, 3.0, calendar,
            adjFajr = 5,
            clockOffsetMinutes = -60
        )

        assertEquals(minutes(base.fajr) - 55, minutes(shifted.fajr))
        assertEquals(minutes(base.dhuhr) - 60, minutes(shifted.dhuhr))
    }
}
