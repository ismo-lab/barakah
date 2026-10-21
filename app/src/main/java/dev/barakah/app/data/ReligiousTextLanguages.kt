package dev.barakah.app.data

/**
 * A language offered for religious text. The UI language is deliberately separate: a reader can
 * keep the interface in English while reading a translation or tafsir in another language.
 *
 * [quranEdition] null means no Quran translation is bundled; [tafsirSource] null means no tafsir
 * is bundled. Both cases fall back to English at runtime and say so on screen.
 */
data class ReligiousLanguage(
    val tag: String,
    val nameEn: String,
    val nameAr: String,
    val quranEdition: String? = null,
    val tafsirSource: String? = null,
    val duaTranslations: Boolean = false
)

object ReligiousTextLanguages {

    val english = ReligiousLanguage(
        tag = "en",
        nameEn = "English",
        nameAr = "الإنجليزية",
        quranEdition = "Bundled English translation",
        tafsirSource = "Al-Mukhtasar (Tafsir Center for Quranic Studies)",
        duaTranslations = true
    )

    val arabic = ReligiousLanguage(
        tag = "ar",
        nameEn = "Arabic",
        nameAr = "العربية",
        tafsirSource = "تفسير الجلالين (QUL)"
    )

    val indonesian = ReligiousLanguage(
        tag = "id",
        nameEn = "Indonesian",
        nameAr = "الإندونيسية",
        quranEdition = "Kementerian Agama RI (QuranEnc)",
        tafsirSource = "Al-Mukhtasar (Tarteel QUL)"
    )

    val urdu = ReligiousLanguage(
        tag = "ur",
        nameEn = "Urdu",
        nameAr = "الأردية",
        quranEdition = "Muhammad Junagarhi (Tanzil)",
        tafsirSource = "As-Saadi Urdu (QuranEnc, V1.0.0)"
    )

    val bengali = ReligiousLanguage(
        tag = "bn",
        nameEn = "Bengali",
        nameAr = "البنغالية",
        quranEdition = "Abu Bakr Zakaria (King Fahd Complex)",
        tafsirSource = "Tafsir Abu Bakr Zakaria (Tarteel QUL)"
    )

    val turkish = ReligiousLanguage(
        tag = "tr",
        nameEn = "Turkish",
        nameAr = "التركية",
        quranEdition = "Muslim Shahin (QuranEnc)",
        tafsirSource = "Turkish Al-Mukhtasar (Tarteel QUL)"
    )

    val persian = ReligiousLanguage(
        tag = "fa",
        nameEn = "Persian",
        nameAr = "الفارسية",
        quranEdition = "Islamhouse.com Persian Team (QuranEnc)",
        tafsirSource = "Persian Al-Mukhtasar (Tarteel QUL)"
    )

    val hausa = ReligiousLanguage(
        tag = "ha",
        nameEn = "Hausa",
        nameAr = "الهوسا",
        quranEdition = "Abubakar Mahmood Jummi (QuranEnc)"
    )

    val malay = ReligiousLanguage(
        tag = "ms",
        nameEn = "Malay",
        nameAr = "الملايوية",
        quranEdition = "Abdullah Muhammad Basmeih (Tanzil)"
    )

    val swahili = ReligiousLanguage(
        tag = "sw",
        nameEn = "Swahili",
        nameAr = "السواحلية",
        quranEdition = "Ali Muhsin Al Barwani (Tanzil)"
    )

    val russian = ReligiousLanguage(
        tag = "ru",
        nameEn = "Russian",
        nameAr = "الروسية",
        tafsirSource = "Russian Al-Mukhtasar (Tarteel QUL)",
        duaTranslations = true
    )

    val sorani = ReligiousLanguage(
        tag = "ckb",
        nameEn = "Sorani Kurdish",
        nameAr = "الكردية السورانية",
        tafsirSource = "Kurdish Al-Mukhtasar (Tarteel QUL)",
        duaTranslations = true
    )

    val all = listOf(
        english, arabic, indonesian, urdu, bengali, turkish, persian,
        hausa, malay, swahili, russian, sorani
    )

    fun byTag(tag: String?): ReligiousLanguage? = all.firstOrNull { it.tag == tag }

    /** The Quran text setting that follows [uiTag]. Empty means Arabic-only, per the reader. */
    fun quranTagFor(uiTag: String): String =
        if (uiTag == "ar") "" else byTag(uiTag)?.takeIf { it.quranEdition != null }?.tag ?: "en"

    /** The tafsir setting that follows [uiTag], with English when no tafsir is bundled. */
    fun tafsirTagFor(uiTag: String): String =
        byTag(uiTag)?.takeIf { it.tafsirSource != null }?.tag ?: "en"

    /**
     * The tafsir setting that follows the reader's Quran choice. Arabic-only falls back to
     * [uiTag], and a translation without a bundled tafsir falls back to English.
     */
    fun tafsirTagForQuran(quranTag: String, uiTag: String): String =
        if (quranTag.isEmpty()) {
            tafsirTagFor(uiTag)
        } else {
            byTag(quranTag)?.takeIf { it.tafsirSource != null }?.tag ?: "en"
        }

    /** The dua setting that follows [uiTag]. Empty means the Arabic text is the content. */
    fun duaTagFor(uiTag: String): String =
        if (uiTag == "ar") "" else byTag(uiTag)?.takeIf { it.duaTranslations }?.tag ?: "en"

    fun name(tag: String, isAr: Boolean): String {
        val language = byTag(tag) ?: return if (isAr) "الإنجليزية" else UiStrings.of("English")
        return if (isAr) language.nameAr else language.nameEn
    }

    /** Arabic is excluded: the Arabic text is already shown above every translation. */
    fun quranOptions(): List<ReligiousLanguage> = all.filter { it.tag != "ar" }

    /** Only languages with a bundled tafsir, so a pick cannot look like a no-op. */
    fun tafsirOptions(): List<ReligiousLanguage> = all.filter { it.tafsirSource != null }

    fun duaOptions(): List<ReligiousLanguage> = all.filter { it.duaTranslations }
}
