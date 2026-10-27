package dev.barakah.app

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import dev.barakah.app.data.DailyContent
import dev.barakah.app.data.UiStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DailyContentTest {

    private val context = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun `russian verse comes from the bundled translation`() {
        UiStrings.setCurrentLanguage("ru")
        val verse = DailyContent.verse(context, "ru", dayOfYear = 1)

        assertEquals(14, verse.surahId)
        assertEquals(7, verse.ayah)
        assertTrue("missing Arabic source", verse.arabic.isNotBlank())
        assertTrue("verse is not Russian: ${verse.text}", verse.text.any { it in 'А'..'я' })
    }

    @Test
    fun `language without a bundled edition gets English`() {
        UiStrings.setCurrentLanguage("en")
        val unknown = DailyContent.verse(context, "fr", dayOfYear = 1)
        val english = DailyContent.verse(context, "en", dayOfYear = 1)

        assertEquals(english.text, unknown.text)
    }

    @Test
    fun `the verse stays the same all day and rotates at midnight`() {
        UiStrings.setCurrentLanguage("en")
        val first = DailyContent.verse(context, "en", dayOfYear = 1)
        val sameDay = DailyContent.verse(context, "en", dayOfYear = 1)
        val nextDay = DailyContent.verse(context, "en", dayOfYear = 2)

        assertEquals(first.ayah, sameDay.ayah)
        assertNotEquals(first.ayah to first.surahId, nextDay.ayah to nextDay.surahId)
    }

    @Test
    fun `hadith is selected per day in the reader's language`() {
        val first = DailyContent.hadith("ru", dayOfYear = 1)
        val sameDay = DailyContent.hadith("ru", dayOfYear = 1)
        val nextDay = DailyContent.hadith("ru", dayOfYear = 2)

        assertEquals(first, sameDay)
        assertNotEquals(first.text, nextDay.text)
        assertTrue(first.text.any { it in 'А'..'я' })
        assertTrue(first.source.isNotBlank())
    }
}
