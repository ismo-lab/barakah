package dev.barakah.app.widget

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import dev.barakah.app.R
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The countdown widget as the picker shows it.
 *
 * The live widget cannot be captured here: it is a Glance composition that the launcher turns into
 * RemoteViews, so the only pixels a unit test can hold still are the static preview layout behind
 * `android:previewLayout`, which is what the widget picker draws. That layout is not UI anyone opens
 * daily, but it is the face of the widget for anyone choosing it, and it has no test of its own.
 *
 * The widget's own text - which prayer, how long is left, in which language - is covered by
 * NextPrayerCountdownTest, since it is decided by code rather than by drawing.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], qualifiers = "w411dp-h500dp")
class WidgetPreviewScreenshotTest {

    @Test
    fun `the countdown widget preview`() {
        capture(R.layout.widget_preview_prayer_remaining, "src/test/screenshots/widget_preview_countdown.png")
    }

    private fun capture(layoutId: Int, path: String) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val view = LayoutInflater.from(context).inflate(layoutId, null)
        val density = context.resources.displayMetrics.density
        val width = (220 * density).toInt()
        val height = (140 * density).toInt()
        view.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        )
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        view.captureRoboImage(path)
    }
}
