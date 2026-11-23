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
    fun `manual city search works while location method is auto`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = BarakahViewModel(context)
        UiStrings.setCurrentLanguage("en")
        viewModel.setAppLanguage("en")
        viewModel.setLocationMethod("auto")
        compose.setContent {
            BarakahTheme { HomeSettingsSection(viewModel, onDismiss = {}) }
        }
        compose.waitForIdle()
        val root = compose.onRoot().fetchSemanticsNode()
        println("ROOT_CHILDREN=${root.children.size}")
        root.children.take(8).forEach { println("CHILD=${it.config}") }

        compose.onNodeWithText("Search city name...").performTextInput("London")
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        compose.onNodeWithText("London", substring = true).assertExists()
    }
}
