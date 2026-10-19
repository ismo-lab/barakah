package dev.barakah.app

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import dev.barakah.app.ui.BarakahViewModel
import dev.barakah.app.ui.theme.BarakahTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * What the shell looks like at each window width, which is the part of "tablet support" that has
 * only ever been asserted on and never drawn.
 *
 * `WindowWidth.of` is unit tested, and `AppStartupTest` composes a tablet window without looking at
 * it, so the rail could be the wrong size or in the wrong place, and the reader could be too narrow
 * to read beside the surah list, with every test still passing. Each width is captured as well as
 * asserted on, for the reason `QuranWindowWidthTest` gives: an assertion can prove a thing exists,
 * only the picture shows whether it is usable.
 *
 * The widths sit either side of a decision rather than on a round number:
 *
 * - 700dp: medium, so the rail replaces the bottom bar while the content keeps its width cap;
 * - 850dp: ten dp past the expanded breakpoint, the narrowest window that splits the reader away
 *   from the surah list, which is where a split would be too tight if it ever is;
 * - 1000dp: expanded and landscape, for the other screen that splits.
 *
 * The app defaults to Arabic, and the layout direction follows the language, so these references
 * are right-to-left, the way a device shows them. The left-to-right arrangement is the mirror of
 * the same geometry.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class AppShellWidthTest {

    @get:Rule
    val compose = createComposeRule()

    private fun openShell() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        val viewModel = BarakahViewModel(application)
        compose.setContent {
            val appLanguage by viewModel.appLanguage.collectAsStateWithLifecycle()
            BarakahTheme {
                // MainActivity decides the layout direction from the language, and Arabic is this
                // app's default. Composing the shell without that draws a frame the app never shows
                // for either language - Arabic strings laid out left to right, with the rail on the
                // wrong side - so the images would say nothing about what a user sees, and nothing
                // about the direction that is actually the default.
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (appLanguage == "ar") {
                        LayoutDirection.Rtl
                    } else {
                        LayoutDirection.Ltr
                    }
                ) {
                    MainNavigationContainer(
                        viewModel = viewModel,
                        navController = rememberNavController()
                    )
                }
            }
        }
    }

    @Test
    @Config(sdk = [36], qualifiers = "w700dp-h1000dp")
    fun `a medium window gets the rail and not the bottom bar`() {
        openShell()

        compose.onNodeWithTag("side_navigation_rail").assertIsDisplayed()
        compose.onNodeWithTag("bottom_nav_bar").assertDoesNotExist()

        compose.onRoot().captureRoboImage("src/test/screenshots/app_shell_rail_medium.png")
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp")
    fun `a compact window keeps the bottom bar`() {
        openShell()

        compose.onNodeWithTag("bottom_nav_bar").assertIsDisplayed()
        compose.onNodeWithTag("side_navigation_rail").assertDoesNotExist()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w850dp-h1000dp")
    fun `the narrowest window that splits the quran shows both panes`() {
        openShell()

        compose.onNodeWithTag("tab_quran").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("surah_item_1").performClick()
        compose.waitForIdle()

        compose.onNodeWithTag("side_navigation_rail").assertIsDisplayed()
        compose.onNodeWithTag("surah_item_1").assertIsDisplayed()

        compose.onRoot().captureRoboImage("src/test/screenshots/quran_two_pane_850dp.png")
    }

    @Test
    @Config(sdk = [36], qualifiers = "w914dp-h411dp-land")
    fun `a short landscape window still shows every rail item`() {
        openShell()

        compose.onNodeWithTag("side_navigation_rail").assertIsDisplayed()
        compose.onNodeWithTag("tab_duas").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [36], qualifiers = "w1000dp-h800dp-land")
    fun `the supplications split at an expanded width`() {
        openShell()

        compose.onNodeWithTag("tab_duas").performClick()
        compose.waitForIdle()

        compose.onNodeWithTag("dua_search_bar").assertIsDisplayed()

        compose.onRoot().captureRoboImage("src/test/screenshots/duas_two_pane_1000dp.png")
    }
}
