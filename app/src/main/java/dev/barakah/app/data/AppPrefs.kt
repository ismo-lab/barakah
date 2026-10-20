package dev.barakah.app.data

/**
 * Every SharedPreferences key the app reads or writes, in one place.
 *
 * The values are exactly the strings that used to be written inline at each call site, so
 * settings already saved on a device keep working. Going through these constants is what
 * stops a typo in one call site from silently falling back to a default.
 *
 * Defaults deliberately stay at the call sites: the phrasing differs between screens
 * ("ar" here, a numeric 0 there) and moving them silently would change behaviour.
 */
object AppPrefs {
    /** The settings file itself. */
    const val FILE = "barakah_prefs"

    // Language and appearance
    const val APP_LANG = "app_lang"
    const val APP_THEME = "app_theme"
    const val USE_DYNAMIC_COLOR = "use_dynamic_color"
    const val AMOLED_DARK = "amoled_dark"
    const val ARABIC_FONT_SIZE = "arabic_font_size"
    const val ENGLISH_FONT_SIZE = "english_font_size"
    const val QURAN_FONT = "quran_font"
    const val USE_WESTERN_NUMBERS_IN_ARABIC = "use_western_numbers_in_arabic"
    const val ENABLE_TASBIH_HAPTICS = "enable_tasbih_haptics"
    const val IS_FIRST_RUN = "is_first_run"

    /**
     * Language of the religious text itself, kept apart from [APP_LANG] so a reader can keep the
     * interface in one language and the translation or tafsir in another. An empty value means
     * no translation is shown.
     */
    const val QURAN_TRANSLATION_LANG = "quran_translation_lang"
    const val TAFSIR_LANG = "tafsir_lang"
    const val DUA_TRANSLATION_LANG = "dua_translation_lang"

    /**
     * True once the reader has chosen a religious-text language for one area. Until then the
     * area follows the interface: Arabic for Arabic, English for every other interface.
     */
    const val QURAN_TRANSLATION_EXPLICIT = "quran_translation_explicit"
    const val TAFSIR_EXPLICIT = "tafsir_explicit"
    const val DUA_TRANSLATION_EXPLICIT = "dua_translation_explicit"

    // Location and prayer calculation
    const val LOCATION_METHOD = "location_method"
    const val LOC_LAT = "loc_lat"
    const val LOC_LNG = "loc_lng"
    const val LOC_LABEL = "loc_label"
    const val LOC_TIMEZONE = "loc_timezone"

    /** ISO code of the saved location's country, which sets the default Hijri shift. */
    const val LOC_COUNTRY = "loc_country"
    const val CALC_METHOD = "calc_method"
    const val ASR_METHOD = "asr_method"
    const val ISHA_METHOD = "isha_method"
    const val ADJ_FAJR = "adj_fajr"
    const val ADJ_SUNRISE = "adj_sunrise"
    const val ADJ_DHUHR = "adj_dhuhr"
    const val ADJ_ASR = "adj_asr"
    const val ADJ_MAGHRIB = "adj_maghrib"
    const val ADJ_ISHA = "adj_isha"

    /**
     * Minutes added to every calculated time at once, sunrise included. This exists for phones
     * whose time zone rules are out of date: a country that changes its lawful hour leaves the
     * device reporting the old offset, which moves every prayer by the same amount. It is kept
     * apart from the per-prayer adjustments so it can be reset to 0 on its own.
     */
    const val ADJ_GLOBAL = "adj_global"

    /**
     * Days to shift the calculated Hijri date by, so the calendar can be lined up with the moon
     * sighting the user's community follows. 0 keeps the plain Umm al-Qura date.
     */
    const val HIJRI_ADJUSTMENT = "hijri_adjustment"

    // Adhan and notifications
    const val ADHAN_AUDIO_ENABLED = "adhan_audio_enabled"
    const val ADHAN_AUDIO_MODE = "adhan_audio_mode"
    const val ADHAN_AUDIO_STREAM = "adhan_audio_stream"
    const val SHOW_NAWAFIL = "show_nawafil"
    const val NOTIFY_NAWAFIL = "notify_nawafil"
    const val NOTIFY_BEFORE_ADHAN = "notify_before_adhan"
    const val NOTIFY_MORNING_ADHKAR = "notify_morning_adhkar"
    const val NOTIFY_EVENING_ADHKAR = "notify_evening_adhkar"
    const val MORNING_ADHKAR_OFFSET = "morning_adhkar_offset"
    const val EVENING_ADHKAR_OFFSET = "evening_adhkar_offset"
    const val NOTIFY_OCCASIONS = "notify_occasions"
    const val NOTIFY_FASTING = "notify_fasting"
    const val NOTIFY_JUMUAH = "notify_jumuah"
    const val NOTIFY_SUHUR = "notify_suhur"
    const val NOTIFY_IFTAR = "notify_iftar"
}

/** Bookkeeping that keeps a prayer from notifying twice for the same scheduled time. */
object NotifHistoryPrefs {
    const val FILE = "notif_history"

    /** Keyed by the prayer id the alarm was scheduled with. */
    fun lastNotified(prayerId: Int) = "last_notified_$prayerId"

    fun lastNotifiedAt(prayerId: Int) = "last_notified_ts_$prayerId"
}
