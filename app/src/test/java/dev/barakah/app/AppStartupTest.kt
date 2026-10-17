package dev.barakah.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The one failure a release must never ship: the app crashing on launch.
 *
 * The activity start used to sit in a try/catch that printed the stack trace and let the test pass
 * either way, which is the same as not testing it.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppStartupTest {

  @Test
  fun `main activity starts`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    dev.barakah.app.data.QuranData.load(context)
    assertEquals("Barakah", context.getString(R.string.app_name))

    val activity = Robolectric.buildActivity(MainActivity::class.java).create().start().resume().get()
    assertFalse("MainActivity was finishing straight after start", activity.isFinishing)
  }
}
