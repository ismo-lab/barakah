package dev.barakah.app.ui.screens

import android.app.Application
import androidx.compose.ui.semantics.SemanticsNode
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

    private fun dump(node: SemanticsNode, depth: Int = 0) {
        if (depth > 8) return
        println("${"  ".repeat(depth)}NODE=${node.config}")
        node.children.forEach { dump(it, depth + 1) }
    }

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
        dump(compose.onRoot().fetchSemanticsNode())

        compose.onNodeWithText("Search city name...").performTextInput("London")
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        compose.onNodeWithText("London", substring = true).assertExists()
    }
}
