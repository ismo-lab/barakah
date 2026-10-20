package dev.barakah.app.data

/**
 * The place the app shows before one has been chosen, and the labels that mean "nothing chosen yet".
 *
 * A stored label is the only record of whether a location was ever set, so every reader of it has to
 * recognise all of these: a placeholder that one reader knows and another does not is a location
 * that looks set to one screen and unset to the next.
 */
object LocationDefaults {
    const val LATITUDE = 21.4225
    const val LONGITUDE = 39.8262

    /** What the manual picker calls Makkah, and what older versions stored for it. */
    const val MECCA_LABEL = "Mecca, KSA"

    private val placeholderLabels = setOf(
        MECCA_LABEL,
        "Mecca (GPS Fallback)",
        "مكة المكرمة (تلقائي)",
        "No Location Selected",
        "الموقع غير محدد"
    )

    /** The label a location gets when nothing has been chosen, in the app's language. */
    fun unsetLabel(isArabic: Boolean): String =
        if (isArabic) "الموقع غير محدد" else UiStrings.of("No Location Selected")

    /** True for the default coordinates, the "unset" texts, and anything empty. */
    fun isPlaceholder(label: String?): Boolean =
        label.isNullOrEmpty() || label in placeholderLabels ||
            label == UiStrings.of("No Location Selected")

    fun isSet(label: String?): Boolean = !isPlaceholder(label)
}
