package dev.barakah.app.ui.screens

import android.app.Application
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import dev.barakah.app.data.QuranData
import dev.barakah.app.ui.BarakahViewModel
import dev.barakah.app.ui.theme.BarakahTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The window can be rotated now, so the activity is recreated and anything held in a plain
 * `remember` is discarded with it. These tests save the state the way the framework does on a
 * configuration change and restore it, then check the user is still where they were.
 *
 * They fail if the state goes back to a plain `remember`, which is exactly the regression that
 * made rotating mid-reading drop the reader and reset the tabs.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class ConfigurationChangeStateTest {

    @get:Rule
    val compose = createComposeRule()

    private fun viewModel(): BarakahViewModel {
        val application = ApplicationProvider.getApplicationContext<Application>()
        QuranData.load(application)
        return BarakahViewModel(application)
    }

    @Test
    fun `the surah being read survives a configuration change`() {
        val restorer = StateRestorationTester(compose)
        val model = viewModel()
        restorer.setContent {
            BarakahTheme {
                QuranScreen(viewModel = model)
            }
        }

        compose.onNodeWithTag("surah_item_1").performClick()
        compose.onNodeWithTag("back_to_catalog").assertExists()

        restorer.emulateSavedInstanceStateRestore()

        compose.onNodeWithTag("back_to_catalog").assertExists()
    }

    @Test
    fun `the quran search text survives a configuration change`() {
        val restorer = StateRestorationTester(compose)
        val model = viewModel()
        restorer.setContent {
            BarakahTheme {
                QuranScreen(viewModel = model)
            }
        }

        compose.onNodeWithTag("quran_search_bar").performTextInput("Baqarah")
        compose.onNodeWithTag("quran_search_bar").assertTextContains("Baqarah")

        restorer.emulateSavedInstanceStateRestore()

        compose.onNodeWithTag("quran_search_bar").assertTextContains("Baqarah")
    }

    @Test
    fun `the selected others tab survives a configuration change`() {
        val restorer = StateRestorationTester(compose)
        val model = viewModel()
        restorer.setContent {
            BarakahTheme {
                OthersScreen(viewModel = model, navController = rememberNavController())
            }
        }

        compose.onNodeWithTag("others_tab_0").assertIsSelected()
        compose.onNodeWithTag("others_tab_1").performClick()
        compose.onNodeWithTag("others_tab_1").assertIsSelected()

        restorer.emulateSavedInstanceStateRestore()

        compose.onNodeWithTag("others_tab_1").assertIsSelected()
    }
}
