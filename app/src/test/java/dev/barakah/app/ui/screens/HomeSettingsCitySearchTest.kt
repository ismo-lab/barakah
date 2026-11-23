package dev.barakah.app.ui.screens

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
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
    fun `manual city search works for successive queries`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = BarakahViewModel(context)
        UiStrings.setCurrentLanguage("en")
        viewModel.setAppLanguage("en")
        viewModel.setLocationMethod("auto")
        compose.setContent {
            BarakahTheme { HomeSettingsSection(viewModel, onDismiss = {}) }
        }

        compose.onNodeWithTag("settings_list").performScrollToIndex(2)
        compose.waitForIdle()
        val field = compose.onNodeWithTag("city_search_field")
        field.performTextInput("London")
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        compose.onNodeWithText("London", substring = true).assertExists()

        field.performTextReplacement("Paris")
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        compose.onNodeWithText("Paris", substring = true).assertExists()
    }
}
