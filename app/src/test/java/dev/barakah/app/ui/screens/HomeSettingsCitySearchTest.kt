package dev.barakah.app.ui.screens

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import dev.barakah.app.data.UiStrings
import dev.barakah.app.ui.BarakahViewModel
import dev.barakah.app.ui.theme.BarakahTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HomeSettingsCitySearchTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `manual city search shows results`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = BarakahViewModel(context)
        UiStrings.setCurrentLanguage("en")
        viewModel.setAppLanguage("en")
        compose.setContent {
            BarakahTheme { HomeSettingsSection(viewModel, onDismiss = {}) }
        }
        compose.waitForIdle()
        compose.onRoot().printToLog("SETTINGS")

        compose.onNodeWithText("Search city name...").performTextInput("London")
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        compose.onRoot().printToLog("AFTER_SEARCH")
        compose.onNodeWithText("London", substring = true).assertExists()
    }
}
