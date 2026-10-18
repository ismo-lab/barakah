package dev.barakah.app.ui.screens

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import dev.barakah.app.data.QuranData
import dev.barakah.app.ui.BarakahViewModel
import dev.barakah.app.ui.theme.BarakahTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Quran screen at two window widths, which is what "tablet support" means for this screen.
 *
 * The list and the reader used to be two branches of one AnimatedContent, so a wide window could
 * only ever show one of them. Now that they are separate panes, the thing worth asserting is that
 * both are on screen at once, and that a phone still shows one.
 *
 * The tablet image is captured as well as asserted on: an assertion can prove both panes exist, but
 * only the picture shows whether they are the right size and in the right place.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class QuranWindowWidthTest {

    @get:Rule
    val compose = createComposeRule()

    private fun openQuranScreen() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        QuranData.load(application)
        val viewModel = BarakahViewModel(application)
        compose.setContent {
            BarakahTheme {
                QuranScreen(viewModel = viewModel)
            }
        }
    }

    @Test
    @Config(sdk = [36], qualifiers = "w1280dp-h800dp-land")
    fun `a tablet shows the surah list and the reader pane together`() {
        openQuranScreen()

        compose.onNodeWithTag("surah_item_1").assertIsDisplayed()
        compose.onNodeWithTag("reader_placeholder").assertIsDisplayed()

        compose.onRoot().captureRoboImage("src/test/screenshots/quran_tablet_two_pane.png")
    }

    @Test
    @Config(sdk = [36], qualifiers = "w411dp-h891dp")
    fun `a phone shows the surah list on its own`() {
        openQuranScreen()

        compose.onNodeWithTag("surah_item_1").assertIsDisplayed()
        compose.onNodeWithTag("reader_placeholder").assertDoesNotExist()
    }
}
