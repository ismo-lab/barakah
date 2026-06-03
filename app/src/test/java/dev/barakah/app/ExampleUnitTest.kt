package dev.barakah.app

import org.junit.Test
import java.net.URL

class ExampleUnitTest {
  @Test
  fun testLocalTafseerParsing() {
    try {
      val file = java.io.File("src/main/assets/quran/tafseer.json")
      assert(file.exists()) { "tafseer.json should exist in assets!" }
      val text = file.readText()
      
      // Let's inspect the first 20 lines to see if there are other keys
      val lines = text.lineSequence().take(40).toList()
      println("FIRST 40 LINES IN TAFSEER.JSON:")
      lines.forEach { println(it) }
      
      // Let's check if there are keys like "description" or "intro" in the file!
      val hasIntro = text.contains("intro", ignoreCase = true)
      val hasDesc = text.contains("description", ignoreCase = true)
      val hasSummary = text.contains("summary", ignoreCase = true)
      println("CONTAINS_KEYS_CHECK: hasIntro=$hasIntro, hasDesc=$hasDesc, hasSummary=$hasSummary")
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}
