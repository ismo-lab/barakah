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
        tafsirSource = "Tafsir al-Jalalayn (Altafsir)",
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
        tafsirSource = "Al-Mukhtasar (Tarteel QUL)",
        duaTranslations = true
    )

    val urdu = ReligiousLanguage(
        tag = "ur",
        nameEn = "Urdu",
        nameAr = "الأردية",
        quranEdition = "Muhammad Junagarhi (Tanzil)"
    )

    val bengali = ReligiousLanguage(
        tag = "bn",
        nameEn = "Bengali",
        nameAr = "البنغالية",
        quranEdition = "Abu Bakr Zakaria (King Fahd Complex)",
        tafsirSource = "Bengali Al-Mukhtasar (Tarteel QUL)"
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
        duaTranslations = true
    )

    val sorani = ReligiousLanguage(
        tag = "ckb",
        nameEn = "Sorani Kurdish",
        nameAr = "الكردية السورانية",
        duaTranslations = true
    )

    val all = listOf(
        english, arabic, indonesian, urdu, bengali, turkish, persian,
        hausa, malay, swahili, russian, sorani
    )

    fun byTag(tag: String?): ReligiousLanguage? = all.firstOrNull { it.tag == tag }

    fun name(tag: String, isAr: Boolean): String {
        val language = byTag(tag) ?: return if (isAr) "الإنجليزية" else UiStrings.of("English")
        return if (isAr) language.nameAr else language.nameEn
    }

    /** Arabic is excluded: the Arabic text is already shown above every translation. */
    fun quranOptions(): List<ReligiousLanguage> = all.filter { it.tag != "ar" }

    /** Every language is selectable; ones without a bundled tafsir say so and show English. */
    fun tafsirOptions(): List<ReligiousLanguage> = all

    fun duaOptions(): List<ReligiousLanguage> = all.filter { it.duaTranslations }
}
