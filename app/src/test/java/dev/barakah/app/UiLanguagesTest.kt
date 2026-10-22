package dev.barakah.app

import dev.barakah.app.data.ReligiousTextLanguages
import dev.barakah.app.data.UiLanguages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class UiLanguagesTest {

    @Test
    fun `device locale selects supported language`() {
        assertEquals("en", UiLanguages.forDeviceLocale(Locale.forLanguageTag("en-US")))
        assertEquals("id", UiLanguages.forDeviceLocale(Locale.forLanguageTag("in-ID")))
        assertEquals("ur", UiLanguages.forDeviceLocale(Locale.forLanguageTag("ur-PK")))
        assertEquals("de", UiLanguages.forDeviceLocale(Locale.forLanguageTag("de-DE")))
    }

    @Test
    fun `unsupported device locale falls back to arabic`() {
        assertEquals("ar", UiLanguages.forDeviceLocale(Locale.forLanguageTag("fr-FR")))
        assertEquals("ar", UiLanguages.forDeviceLocale(Locale.forLanguageTag("ar-EG")))
    }

    @Test
    fun `tafsir follows the chosen quran language`() {
        assertEquals("ur", ReligiousTextLanguages.tafsirTagForQuran("ur", "en"))
        assertEquals("ar", ReligiousTextLanguages.tafsirTagForQuran("ar", "en"))
        assertEquals("en", ReligiousTextLanguages.tafsirTagForQuran("ha", "en"))
        assertEquals("en", ReligiousTextLanguages.tafsirTagForQuran("", "en"))
        assertEquals("ar", ReligiousTextLanguages.tafsirTagForQuran("", "ar"))
    }

    @Test
    fun `bengali tafsir is attributed to Abu Bakr Zakaria`() {
        val source = ReligiousTextLanguages.bengali.tafsirSource.orEmpty()
        assertTrue(source.contains("Abu Bakr Zakaria"))
        assertFalse(source.contains("Al-Mukhtasar"))
    }
}
