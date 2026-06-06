package dev.barakah.app

import org.junit.Test
import java.io.File
import org.json.JSONObject

class ExampleUnitTest {
  @Test
  fun testLocalTafseerParsing() {
    try {
      val file = File("src/main/assets/quran/tafseer_ar.json")
      assert(file.exists()) { "tafseer_ar.json should exist in assets!" }
      val text = file.readText()
      
      val obj = JSONObject(text)
      println("Keys in root: " + obj.keys().asSequence().toList())
      if (obj.has("surahs")) {
        val surahsArr = obj.getJSONArray("surahs")
        println("Number of surahs: ${surahsArr.length()}")
        if (surahsArr.length() > 0) {
          val firstSurah = surahsArr.getJSONObject(0)
          println("First surah keys: " + firstSurah.keys().asSequence().toList())
          println("First surah id: ${firstSurah.optInt("id")}")
          if (firstSurah.has("ayahs")) {
            val ayahsArr = firstSurah.getJSONArray("ayahs")
            println("First surah ayahs count: ${ayahsArr.length()}")
            if (ayahsArr.length() > 0) {
              println("First ayah: ${ayahsArr.getJSONObject(0)}")
            }
          }
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
      assert(false) { "Error: ${e.message}" }
    }
  }
}
