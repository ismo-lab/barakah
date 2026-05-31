package dev.barakah.app

import org.junit.Test
import java.net.URL

class ExampleUnitTest {
  @Test
  fun testDownloadQuranSchema() {
    try {
      val url = URL("https://raw.githubusercontent.com/rn0x/Quran-Data/refs/heads/version-2.0/data/mainDataQuran.json")
      val text = url.readText()
      println("DOWNLOAD_SUCCESS: Length is ${text.length}")
      println("FIRST_1000_CHARS:")
      println(text.take(1500))
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }
}
