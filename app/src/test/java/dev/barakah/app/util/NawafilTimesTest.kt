package dev.barakah.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NawafilTimesTest {

    private fun times(
        fajr: String = "05:00",
        sunrise: String = "06:00",
        maghrib: String = "18:00",
        isha: String = "20:00"
    ) = PrayerCalculator.PrayerTimes(
        fajr = fajr,
        sunrise = sunrise,
        dhuhr = "12:00",
        asr = "15:00",
        maghrib = maghrib,
        isha = isha
    )

    @Test
    fun `night prayers follow the maghrib to fajr night`() {
        val schedule = NawafilTimes.schedule(times())!!

        assertEquals("06:20", schedule.duha)
        assertEquals("20:30", schedule.witr)
        assertEquals("21:40", schedule.firstThird)
        assertEquals("23:30", schedule.qiyam)
        assertEquals("01:20", schedule.tahajjud)
    }

    @Test
    fun `tahajjud moves with the night length instead of a fixed offset`() {
        val shortNight = NawafilTimes.schedule(times(fajr = "04:00", maghrib = "20:00"))!!
        val longNight = NawafilTimes.schedule(times(fajr = "06:00", maghrib = "17:00"))!!

        assertEquals("01:20", shortNight.tahajjud)
        assertEquals("01:40", longNight.tahajjud)
    }

    @Test
    fun `witr wraps past midnight`() {
        assertEquals("00:20", NawafilTimes.schedule(times(isha = "23:50"))!!.witr)
    }

    @Test
    fun `placeholders produce no schedule`() {
        assertNull(NawafilTimes.schedule(times(fajr = "--:--")))
    }
}
