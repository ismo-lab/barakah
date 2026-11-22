package dev.barakah.app

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.barakah.app.data.AppPrefs
import dev.barakah.app.ui.BarakahViewModel
import dev.barakah.app.util.HijriCalendarHelper
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RemovedModifiersTest {

    @Test
    fun `stored hijri override is ignored in favour of the country default`() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val prefs = application.getSharedPreferences(AppPrefs.FILE, Context.MODE_PRIVATE)
        prefs.edit().clear().commit()
        prefs.edit()
            .putString(AppPrefs.LOC_COUNTRY, "SA")
            .putInt(AppPrefs.HIJRI_ADJUSTMENT, 2)
            .putInt(AppPrefs.ADJ_GLOBAL, 60)
            .commit()

        val viewModel = BarakahViewModel(application)

        assertEquals(
            HijriCalendarHelper.countryAdjustmentDays("SA"),
            viewModel.hijriAdjustment.value
        )
    }
}
