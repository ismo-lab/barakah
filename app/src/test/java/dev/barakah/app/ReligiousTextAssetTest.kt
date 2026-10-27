package dev.barakah.app

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

/**
 * The bundled religious-text assets are added by a script, not by the compiler, so a missing or
 * truncated file would only show up as an empty screen. These checks keep the fallback path from
 * silently becoming the only path.
 */
@RunWith(RobolectricTestRunner::class)
class ReligiousTextAssetTest {

    private fun asset(path: String): File =
        listOf(File("src/main/$path"), File("app/src/main/$path"))
            .firstOrNull { it.exists() }
            ?: error("Missing asset: $path")

    @Test
    fun quranTranslationsAndTafsirCoverEveryVerse() {
        val translations = listOf("id", "ur", "bn", "tr", "fa", "ha", "ms", "sw", "ru")
        for (language in translations) {
            var verses = 0
            for (surah in 1..114) {
                val root = JSONObject(asset("assets/quran/translations/$language/surah_$surah.json").readText())
                val list = root.getJSONArray("verses")
                assertTrue("empty $language surah $surah", list.length() > 0)
                verses += list.length()
            }
            assertEquals("$language translation verse count", 6236, verses)
        }

        for (language in listOf("id", "bn", "tr", "fa", "ur", "ru", "ckb")) {
            var ayahs = 0
            for (surah in 1..114) {
                val root = JSONObject(asset("assets/quran/tafsir/$language/$surah.json").readText())
                ayahs += root.getJSONArray("ayahs").length()
            }
            assertEquals("$language tafsir ayah count", 6236, ayahs)
        }
    }

    @Test
    fun supplicationTranslationsArePresent() {
        for (language in listOf("ru", "fa", "ckb")) {
            val root = JSONObject(asset("res/raw/duas_$language.json").readText())
            assertTrue("$language supplication translations", root.length() > 0)
        }
    }

    @Test
    fun supplicationTitlesCoverEveryBundledLanguage() {
        val entries = JSONArray(asset("res/raw/hisn_en.json").readText())
        val titles = linkedSetOf<String>()
        for (i in 0 until entries.length()) {
            val title = entries.getJSONObject(i)
                .optString("title", "")
                .replace("Chapter: ", "")
                .trim()
            if (title.isNotEmpty()) titles.add(title)
        }
        val translated = JSONObject(asset("res/raw/dua_titles.json").readText())
        for (language in listOf("id", "de", "ur", "bn", "tr", "fa", "ha", "ms", "sw", "ru", "ckb")) {
            val map = translated.getJSONObject(language)
            for (title in titles) {
                assertTrue(
                    "$language missing supplication title: $title",
                    map.optString(title, "").isNotBlank()
                )
            }
        }
    }
}
