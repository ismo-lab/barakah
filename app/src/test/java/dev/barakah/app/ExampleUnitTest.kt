package dev.barakah.app

import org.junit.Test
import java.io.File

class ExampleUnitTest {
  @Test
  fun testLocalTafseerParsing() {
    try {
      val file1 = File("app/src/main/assets/quran/tafseer_ar.json")
      val file2 = File("src/main/assets/quran/tafseer_ar.json")
      val file = if (file1.exists()) file1 else file2
      assert(file.exists()) { "tafseer_ar.json should exist in assets!" }
      val text = file.readText()
      assert(text.contains("aya")) { "tafseer_ar.json should contain aya data!" }
    } catch (e: Exception) {
      e.printStackTrace()
      assert(false) { "Error: ${e.message}" }
    }
  }
}
