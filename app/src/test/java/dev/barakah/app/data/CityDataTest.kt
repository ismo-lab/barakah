package dev.barakah.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CityDataTest {

    @Before
    fun cleanCatalog() {
        CityData.resetForTests()
    }

    /** Extraction was the only thing that changed when the catalog was gzipped. */
    @Test
    fun `offline search finds a known city`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val results = CityData.search(context, "London", limit = 5)
        assertTrue("expected London in $results", results.any {
            it.name.equals("London", ignoreCase = true) && it.countryCode == "GB"
        })
    }

    /** Old releases wrote straight to the final path; an interrupted write must self-heal. */
    @Test
    fun `corrupt installed catalog is replaced`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val version = context.assets.open("cities500.version").bufferedReader().use { it.readText().trim() }
        val file = context.getDatabasePath("geonames-cities500-$version.sqlite")
        file.parentFile?.mkdirs()
        file.writeBytes(byteArrayOf(1, 2, 3, 4))

        val results = CityData.search(context, "London", limit = 5)
        assertTrue("expected London in $results", results.any {
            it.name.equals("London", ignoreCase = true) && it.countryCode == "GB"
        })
    }
}
