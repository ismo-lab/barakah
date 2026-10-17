package dev.barakah.app.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.github.takahirom.roborazzi.captureRoboImage
import dev.barakah.app.ui.theme.BarakahTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The Home screen's countdown card, pixel for pixel.
 *
 * The card is the one place where being a pixel out is visible without anyone reading code: it sits
 * at the top of the first screen, it is redrawn every second, and its shape and font sizes change
 * with the height of the screen. Until now the only way to know a change to it was harmless was to
 * build the app and look.
 *
 * The images are recorded on the CI machine, by dispatching the workflow with
 * `record_screenshots` set, and then committed. A normal run compares against them and fails with a
 * difference map when the card has moved.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class CountdownCardScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the countdown card in Arabic`() {
        compose.setContent {
            BarakahTheme {
                CountdownCardContent(
                    labelText = "الصلاة القادمة: الفجر",
                    countdownText = "02:15:30",
                )
            }
        }
        compose.onNodeWithTag("countdown_card")
            .captureRoboImage("src/test/screenshots/countdown_card_arabic.png")
    }

    @Test
    fun `the countdown card in English`() {
        compose.setContent {
            BarakahTheme {
                CountdownCardContent(
                    labelText = "NEXT: FAJR",
                    countdownText = "02:15:30",
                )
            }
        }
        compose.onNodeWithTag("countdown_card")
            .captureRoboImage("src/test/screenshots/countdown_card_english.png")
    }

    @Test
    fun `the countdown card before a location is chosen`() {
        compose.setContent {
            BarakahTheme {
                CountdownCardContent(
                    labelText = "NEXT: --",
                    countdownText = "--:--:--",
                )
            }
        }
        compose.onNodeWithTag("countdown_card")
            .captureRoboImage("src/test/screenshots/countdown_card_no_location.png")
    }
}
