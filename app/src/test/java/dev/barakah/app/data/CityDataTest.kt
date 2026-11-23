package dev.barakah.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CityDataTest {

    /** Extraction was the only thing that changed when the catalog was gzipped. */
    @Test
    fun `offline search finds a known city`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val results = CityData.search(context, "London", limit = 5)
        assertTrue("expected London in $results", results.any {
            it.name.equals("London", ignoreCase = true) && it.countryCode == "GB"
        })
    }
}
