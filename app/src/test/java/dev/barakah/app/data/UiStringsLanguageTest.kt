package dev.barakah.app.data

fun main() {
    val supported = listOf("en", "ar", "id", "ur", "bn", "tr", "fa", "ha", "ms", "sw", "ru", "ckb", "de")
    for (language in supported) {
        UiStrings.setCurrentLanguage(language)
        check(UiStrings.of("Today's Verse") != "Today's Verse")
        check(UiStrings.of("Today's Hadith") != "Today's Hadith")
        check(UiStrings.of("Text preview") != "Text preview")
    }
    check(ReligiousTextLanguages.quranTagFor("ha") == "ha")
    check(ReligiousTextLanguages.quranTagFor("de") == "en")
}
