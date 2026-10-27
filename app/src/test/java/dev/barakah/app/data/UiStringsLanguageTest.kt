package dev.barakah.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the interface tables. The table is one Kotlin map per language keyed by the English copy,
 * so two entries for the same language silently replace each other and every other string in that
 * language falls back to English. That is exactly how ten languages once lost almost all of their
 * copy, and why this test checks completeness rather than three sample strings.
 */
class UiStringsLanguageTest {

    private val supported = listOf("ar", "id", "ur", "bn", "tr", "fa", "ha", "ms", "sw", "ru", "ckb", "de")

    @Test
    fun everyLanguageCoversEverySourceKey() {
        val source = UiStrings.sourceKeys()
        assertTrue("source table is empty", source.size > 250)
        for (language in supported) {
            val keys = UiStrings.keysFor(language)
            val missing = source - keys
            assertEquals("$language is missing translations: ${missing.take(8)}", emptySet<String>(), missing)
            assertEquals("$language has stale keys", emptySet<String>(), keys - source)
        }
    }

    @Test
    fun interfaceCopyIsActuallyTranslated() {
        // Names and abbreviations are legitimately identical across languages; ordinary UI copy is
        // not, so a short sample of it has to differ from the English source for every language.
        val sample = listOf(
            "Quran settings",
            "Arabic font size",
            "Translation font size",
            "Show Arabic text",
            "Done",
            "Supplication language",
            "Translation not available for this supplication.",
            "All prayers moved %s min later.",
            "Tanzil translation terms",
            "Select Language",
            "Remove bookmark",
            "Add bookmark",
            "Today's Verse",
            "Today's Hadith",
            "Text preview"
        )
        for (language in supported) {
            UiStrings.setCurrentLanguage(language)
            for (key in sample) {
                assertNotEquals("$language left '$key' untranslated", key, UiStrings.of(key))
            }
        }
        UiStrings.setCurrentLanguage("en")
    }

    @Test
    fun englishReturnsTheSourceText() {
        UiStrings.setCurrentLanguage("en")
        assertEquals("Done", UiStrings.of("Done"))
        // An unknown key falls through to itself, not to some other entry.
        assertEquals("Something with no translation", UiStrings.of("Something with no translation"))
    }

    @Test
    fun religiousTextFollowsTheInterfaceLanguage() {
        check(ReligiousTextLanguages.quranTagFor("ha") == "ha")
        check(ReligiousTextLanguages.quranTagFor("de") == "de")
        check(ReligiousTextLanguages.quranTagFor("ru") == "ru")
        check(ReligiousTextLanguages.quranTagFor("ckb") == "ckb")
        check(ReligiousTextLanguages.duaTagFor("fa") == "fa")
        check(ReligiousTextLanguages.duaTagFor("ar") == "")
    }
}
