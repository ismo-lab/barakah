package dev.barakah.app.data

/** Interface languages. Religious-text languages are separate; German has no bundled text yet. */
data class UiLanguage(val tag: String, val nativeName: String)

object UiLanguages {
    val all = listOf(
        UiLanguage("en", "English"),
        UiLanguage("ar", "العربية"),
        UiLanguage("id", "Bahasa Indonesia"),
        UiLanguage("ur", "اردو"),
        UiLanguage("bn", "বাংলা"),
        UiLanguage("tr", "Türkçe"),
        UiLanguage("fa", "فارسی"),
        UiLanguage("ha", "Hausa"),
        UiLanguage("ms", "Bahasa Melayu"),
        UiLanguage("sw", "Kiswahili"),
        UiLanguage("ru", "Русский"),
        UiLanguage("ckb", "کوردی"),
        UiLanguage("de", "Deutsch")
    )

    fun byTag(tag: String?): UiLanguage? = all.firstOrNull { it.tag == tag }

    fun isRtl(tag: String?): Boolean = tag in setOf("ar", "ur", "fa", "ckb")
}
