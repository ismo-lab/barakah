package dev.barakah.app

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.barakah.app.data.AppPrefs
import dev.barakah.app.ui.BarakahViewModel
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TafsirLanguageFollowTest {

    @Test
    fun `quran pick re-syncs tafsir after a manual tafsir choice`() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        application.getSharedPreferences(AppPrefs.FILE, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        val viewModel = BarakahViewModel(application)

        viewModel.setQuranTranslationLanguage("ur")
        assertEquals("ur", viewModel.tafsirLanguage.value)

        viewModel.setTafsirLanguage("ar")
        assertEquals("ar", viewModel.tafsirLanguage.value)

        viewModel.setQuranTranslationLanguage("bn")
        assertEquals("bn", viewModel.tafsirLanguage.value)

        viewModel.setQuranTranslationLanguage("sw")
        assertEquals("sw", viewModel.tafsirLanguage.value)

        viewModel.setQuranTranslationLanguage("ha")
        assertEquals("en", viewModel.tafsirLanguage.value)
    }
}
