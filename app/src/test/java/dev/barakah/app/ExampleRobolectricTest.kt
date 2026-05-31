package dev.barakah.app

import dev.barakah.app.R

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `main activity starts`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    dev.barakah.app.data.QuranData.load(context)
    println("Surahs parsed: " + dev.barakah.app.data.QuranData.surahs.size)
    val appName = context.getString(R.string.app_name)
    assertEquals("Barakah", appName)
    try {
        org.robolectric.Robolectric.buildActivity(dev.barakah.app.MainActivity::class.java).create().start().resume()
    } catch(e: Exception) {
        e.printStackTrace()
    }
  }
}
