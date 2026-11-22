package dev.barakah.app.util

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.barakah.app.data.AppPrefs
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PrayerMethodResolverTest {

    private fun prefs() = ApplicationProvider.getApplicationContext<Application>()
        .getSharedPreferences(AppPrefs.FILE, Context.MODE_PRIVATE)
        .also { it.edit().clear().commit() }

    @Test
    fun `unknown country and no choice default to mwl`() {
        val prefs = prefs()
        assertEquals(PrayerCalculator.CalculationMethod.MWL, PrayerMethodResolver.resolve(prefs))
        prefs.edit().putString(AppPrefs.LOC_COUNTRY, "ZZ").commit()
        assertEquals(PrayerCalculator.CalculationMethod.MWL, PrayerMethodResolver.resolve(prefs))
    }

    @Test
    fun `morocco uses its own documented angles`() {
        val prefs = prefs().also { it.edit().putString(AppPrefs.LOC_COUNTRY, " ma ").commit() }
        assertEquals(PrayerCalculator.CalculationMethod.MOROCCO, PrayerMethodResolver.resolve(prefs))
        assertEquals(19.0, PrayerCalculator.CalculationMethod.MOROCCO.fajrAngle, 0.0)
        assertEquals(17.0, PrayerCalculator.CalculationMethod.MOROCCO.ishaAngle, 0.0)
    }

    @Test
    fun `an explicit choice wins over the country`() {
        val prefs = prefs().also {
            it.edit()
                .putString(AppPrefs.LOC_COUNTRY, "MA")
                .putString(AppPrefs.CALC_METHOD, "ISNA")
                .commit()
        }
        assertEquals(PrayerCalculator.CalculationMethod.ISNA, PrayerMethodResolver.resolve(prefs))
    }

    @Test
    fun `a stale stored name falls back to the country`() {
        val prefs = prefs().also {
            it.edit()
                .putString(AppPrefs.LOC_COUNTRY, "EG")
                .putString(AppPrefs.CALC_METHOD, "REMOVED_METHOD")
                .commit()
        }
        assertEquals(PrayerCalculator.CalculationMethod.EGYPT, PrayerMethodResolver.resolve(prefs))
    }
}
