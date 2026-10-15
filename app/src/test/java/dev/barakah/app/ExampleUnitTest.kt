package dev.barakah.app

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {
  @Test
  fun testLocalTafseerParsing() {
    val file = listOf(
      File("src/main/assets/quran/tafseer_ar.json"),
      File("app/src/main/assets/quran/tafseer_ar.json")
    ).firstOrNull { it.exists() }

    assertTrue("tafseer_ar.json should exist in assets!", file != null && file.exists())
    val reader = android.util.JsonReader(file!!.reader())
    reader.beginArray()
    assertTrue(reader.hasNext())
    reader.beginObject()
    var hasNumber = false
    var hasAya = false
    var hasText = false
    while (reader.hasNext()) {
      when (reader.nextName()) {
        "number" -> { reader.nextString(); hasNumber = true }
        "aya" -> { reader.nextString(); hasAya = true }
        "text" -> { reader.nextString(); hasText = true }
        else -> reader.skipValue()
      }
    }
    reader.endObject()
    reader.close()
    assertTrue("Should have number field", hasNumber)
    assertTrue("Should have aya field", hasAya)
    assertTrue("Should have text field", hasText)
  }
}

