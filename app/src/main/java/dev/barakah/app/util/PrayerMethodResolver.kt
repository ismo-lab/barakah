package dev.barakah.app.util

import android.content.SharedPreferences
import dev.barakah.app.data.AppPrefs

/**
 * One answer to "which calculation method is in force", shared by the UI, widgets and the alarm
 * scheduler. An explicit choice in Settings always wins; an untouched install follows the saved
 * country where we have a documented authority, and every other country uses MWL.
 *
 * Angles and country conventions: https://api.aladhan.com/v1/methods and
 * https://aladhan.com/calculation-methods. Only angle-based methods are listed; authorities that
 * publish tabulated times or a Maghrib adjustment are deliberately left out rather than guessed.
 */
object PrayerMethodResolver {
    val default: PrayerCalculator.CalculationMethod = PrayerCalculator.CalculationMethod.MWL

    private val countryMethods = mapOf(
        "AE" to PrayerCalculator.CalculationMethod.DUBAI,
        "BN" to PrayerCalculator.CalculationMethod.JAKIM,
        "CA" to PrayerCalculator.CalculationMethod.ISNA,
        "DZ" to PrayerCalculator.CalculationMethod.ALGERIA,
        "EG" to PrayerCalculator.CalculationMethod.EGYPT,
        "FR" to PrayerCalculator.CalculationMethod.UOIF,
        "ID" to PrayerCalculator.CalculationMethod.KEMENAG,
        "IR" to PrayerCalculator.CalculationMethod.TEHRAN,
        "KW" to PrayerCalculator.CalculationMethod.KUWAIT,
        "MA" to PrayerCalculator.CalculationMethod.MOROCCO,
        "MY" to PrayerCalculator.CalculationMethod.JAKIM,
        "PK" to PrayerCalculator.CalculationMethod.KARACHI,
        "RU" to PrayerCalculator.CalculationMethod.RUSSIA,
        "SA" to PrayerCalculator.CalculationMethod.UMM_AL_QURA,
        "SG" to PrayerCalculator.CalculationMethod.SINGAPORE,
        "TN" to PrayerCalculator.CalculationMethod.TUNISIA,
        "TR" to PrayerCalculator.CalculationMethod.TURKEY,
        "US" to PrayerCalculator.CalculationMethod.ISNA
    )

    fun resolve(prefs: SharedPreferences): PrayerCalculator.CalculationMethod {
        val chosen = prefs.getString(AppPrefs.CALC_METHOD, null)
        if (!chosen.isNullOrBlank()) {
            try {
                return PrayerCalculator.CalculationMethod.valueOf(chosen)
            } catch (_: IllegalArgumentException) {
                // A renamed enum entry must not stop the app from calculating times.
            }
        }
        return forCountry(prefs.getString(AppPrefs.LOC_COUNTRY, null))
    }

    fun forCountry(countryCode: String?): PrayerCalculator.CalculationMethod {
        val code = countryCode?.trim()?.uppercase(java.util.Locale.ROOT) ?: return default
        return countryMethods[code] ?: default
    }
}
