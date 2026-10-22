package dev.barakah.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ReligiousAssetIntegrityTest {

    private val quranDir: File = listOf(
        File("src/main/assets/quran"),
        File("app/src/main/assets/quran")
    ).first { it.isDirectory }

    @Test
    fun `quran assets contain no control characters`() {
        val control = Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F-\\x9F]")
        val bad = quranDir.walkTopDown()
            .filter { it.isFile && it.extension == "json" }
            .filter { control.containsMatchIn(it.readText()) }
            .map { it.relativeTo(quranDir).path }
            .toList()

        assertTrue("control characters found in: $bad", bad.isEmpty())
    }

    @Test
    fun `quran assets contain no replacement characters`() {
        val bad = quranDir.walkTopDown()
            .filter { it.isFile && it.extension == "json" }
            .filter { it.readText().contains('\uFFFD') }
            .map { it.relativeTo(quranDir).path }
            .toList()

        assertEquals("U+FFFD corrupts rendered scripture: $bad", emptyList<String>(), bad)
    }

    @Test
    fun `bengali tafsir and hausa translation contain no legacy font glyphs`() {
        val legacy = Regex("[æÐŸ¡¯ƒ¤œ²¦øÍñ‚›]")
        val paths = listOf(
            quranDir.resolve("tafsir/bn"),
            quranDir.resolve("translations/ha")
        )

        val bad = paths.flatMap { dir ->
            dir.walkTopDown()
                .filter { it.isFile && it.extension == "json" }
                .filter { legacy.containsMatchIn(it.readText()) }
                .map { it.relativeTo(quranDir).path }
                .toList()
        }

        assertTrue("legacy font glyphs found in: $bad", bad.isEmpty())
    }

    @Test
    fun `bengali assets use Bengali letters not Assamese variants`() {
        val assamese = Regex("[ৰৱ৺]")
        val paths = listOf(
            quranDir.resolve("tafsir/bn"),
            quranDir.resolve("translations/bn")
        )

        val bad = paths.flatMap { dir ->
            dir.walkTopDown()
                .filter { it.isFile && it.extension == "json" }
                .filter { assamese.containsMatchIn(it.readText()) }
                .map { it.relativeTo(quranDir).path }
                .toList()
        }

        assertTrue("Assamese letters found in Bengali assets: $bad", bad.isEmpty())
    }
}
