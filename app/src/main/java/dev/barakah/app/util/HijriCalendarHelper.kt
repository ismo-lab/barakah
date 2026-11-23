package dev.barakah.app.util

import dev.barakah.app.data.UiStrings

import android.os.Build
import android.util.Log
import java.time.LocalDate
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

object HijriCalendarHelper {

    private const val TAG = "HijriCalendarHelper"
    private const val MILLIS_PER_DAY = 86_400_000L

    data class HijriDate(val day: Int, val month: Int, val year: Int)

    /** The Umm al-Qura conversion for an explicit date, from the table bundled in the app. */
    fun getHijriDateFor(localDate: LocalDate): HijriDate = fromEpochDay(localDate.toEpochDay())

    /**
     * True when the calculated Umm al-Qura date falls in Ramadan. Umm al-Qura lengthens its Isha
     * interval for the month, so the prayer calculator needs this too.
     */
    fun isRamadan(year: Int, month: Int, day: Int): Boolean =
        hijriFor(year, month, day, 0).month == 9

    /**
     * The shift that matches the month start most often in [countryCode]. Countries that wait for
     * a local sighting usually begin a day after the Umm al-Qura table this app computes from.
     *
     * Only a default, not a claim of the actual sighting: any single month can go either way, so
     * the manual adjustment stays as the override and an unknown country keeps the plain date.
     */
    fun countryAdjustmentDays(countryCode: String?): Int {
        val code = countryCode?.trim()?.uppercase(java.util.Locale.ROOT) ?: return 0
        return if (code in ONE_DAY_LATER_COUNTRIES) -1 else 0
    }

    private val ONE_DAY_LATER_COUNTRIES =
        setOf("BD", "DZ", "ID", "IN", "LK", "MA", "MY", "PK", "TN")

    /**
     * The one conversion the whole app goes through: the bundled Umm al-Qura table on every
     * device, with the arithmetic fallback only for dates outside the table.
     *
     * Keep every caller on this. The date shown on the home screen, the day an occasion reminder
     * fires and the date listed for that occasion used to use two different calendars, so the app
     * could show one Eid date and remind on another.
     *
     * [adjustmentDays] shifts the result by that many days, which is how a user lines the
     * calculated date up with the moon sighting their community follows.
     */
    private fun hijriFor(year: Int, month: Int, day: Int, adjustmentDays: Int): HijriDate {
        val calendar = utcCalendar()
        calendar.set(year, month - 1, day)
        calendar.add(Calendar.DAY_OF_MONTH, adjustmentDays)
        return fromEpochDay(Math.floorDiv(calendar.timeInMillis, MILLIS_PER_DAY))
    }

    /** UTC only: the table counts civil days, so device timezone must not enter the conversion. */
    private fun utcCalendar(): GregorianCalendar =
        GregorianCalendar(TimeZone.getTimeZone("UTC")).apply { clear() }

    private fun fromEpochDay(epochDay: Long): HijriDate {
        val starts = UmmAlQuraTable.monthStartEpochDays
        if (epochDay < starts[0] || epochDay >= UmmAlQuraTable.END_EPOCH_DAY) {
            val calendar = utcCalendar().apply { timeInMillis = epochDay * MILLIS_PER_DAY }
            return getHijriFromJulian(
                getJulianDay(
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
            )
        }

        var low = 0
        var high = starts.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (starts[mid] <= epochDay) low = mid + 1 else high = mid
        }
        val index = low - 1
        return HijriDate(
            (epochDay - starts[index] + 1).toInt(),
            index % 12 + 1,
            UmmAlQuraTable.FIRST_YEAR + index / 12
        )
    }

    fun getTodayHijriDate(adjustmentDays: Int = 0): HijriDate {
        val today = Calendar.getInstance()
        return hijriFor(
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH) + 1,
            today.get(Calendar.DAY_OF_MONTH),
            adjustmentDays
        )
    }

    fun getTodayHijriDateString(isAr: Boolean = false, adjustmentDays: Int = 0): String {
        val hijri = getTodayHijriDate(adjustmentDays)
        val language = if (isAr) "ar" else UiStrings.language
        val monthName = hijriMonthName(hijri.month, language)

        return when {
            isAr -> "${hijri.day} $monthName ${hijri.year} هـ"
            language == "id" -> "${hijri.day} $monthName ${hijri.year} H"
            else -> "${hijri.day} $monthName ${hijri.year} AH"
        }
    }

    /**
     * A Hijri month name in [language]. The platform's locale data carries Islamic calendar month
     * names for the bundled interface languages, so switching the app language does not leave the
     * date on the home screen and in settings in English. Older releases fall back to the built-in
     * Arabic, English and Indonesian names.
     */
    fun hijriMonthName(month: Int, language: String = UiStrings.language): String {
        if (month !in 1..12) return ""
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val locale = Locale.forLanguageTag(language.ifBlank { "en" })
                val formatter = java.time.format.DateTimeFormatter.ofPattern("MMMM", locale)
                return formatter.format(java.time.chrono.HijrahDate.of(1445, month, 1))
            } catch (e: Exception) {
                Log.e(TAG, "Falling back to the built-in Hijri month names", e)
            }
        }

        val monthNamesAr = listOf(
            "", "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
            "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
            "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
        )
        val monthNamesEn = listOf(
            "", "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
            "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Sha'ban",
            "Ramadan", "Shawwal", "Dhul-Qi'dah", "Dhul-Hijjah"
        )
        val monthNamesId = listOf(
            "", "Muharam", "Safar", "Rabiul Awal", "Rabiul Akhir",
            "Jumadil Awal", "Jumadil Akhir", "Rajab", "Syaban",
            "Ramadan", "Syawal", "Zulkaidah", "Zulhijah"
        )
        return when (language) {
            "ar" -> monthNamesAr[month]
            "id" -> monthNamesId[month]
            else -> monthNamesEn[month]
        }
    }

    /** The short Gregorian month name in [language], falling back to English. */
    private fun gregorianMonthShort(month: Int, language: String): String {
        val english = java.text.DateFormatSymbols(Locale.ENGLISH)
        if (month !in 1..12) return ""
        if (language.isBlank() || language == "en") return english.shortMonths[month - 1]
        return try {
            val symbols = java.text.DateFormatSymbols(Locale.forLanguageTag(language))
            symbols.shortMonths.getOrNull(month - 1)?.takeIf { it.isNotBlank() }
                ?: english.shortMonths[month - 1]
        } catch (e: Exception) {
            Log.e(TAG, "Falling back to English Gregorian month names", e)
            english.shortMonths[month - 1]
        }
    }

    fun getTomorrowHijriDate(adjustmentDays: Int = 0): HijriDate {
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        return hijriFor(
            tomorrow.get(Calendar.YEAR),
            tomorrow.get(Calendar.MONTH) + 1,
            tomorrow.get(Calendar.DAY_OF_MONTH),
            adjustmentDays
        )
    }

    private fun getJulianDay(year: Int, month: Int, day: Int): Int {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = y / 100
        val b = 2 - a + (a / 4)
        val jd = (365.25 * (y + 4716)).toInt() + (30.6001 * (m + 1)).toInt() + day + b - 1524
        return jd
    }

    private fun getHijriFromJulian(RemJD: Int): HijriDate {
        val epochDiff = RemJD - 1948439
        val cycle = epochDiff / 10631
        var rem = epochDiff % 10631
        
        var yearInCycle = 0
        var totalDays = 0
        // Detect Hijri year in the 30-year cycle
        val leapYears = listOf(2, 5, 7, 10, 13, 16, 18, 21, 24, 26, 29)
        for (i in 1..30) {
            val isLeap = i in leapYears
            val daysInYear = if (isLeap) 355 else 354
            if (rem < totalDays + daysInYear) {
                yearInCycle = i - 1
                rem -= totalDays
                break
            }
            totalDays += daysInYear
        }
        
        val hijriYear = cycle * 30 + yearInCycle + 1
        
        var hijriMonth = 1
        var daysAccumulated = 0
        for (m in 1..12) {
            val daysInMonth = if (m % 2 == 1) 30 else 29
            val actualDaysInMonth = if (m == 12 && (yearInCycle + 1) in leapYears) 30 else daysInMonth
            if (rem < daysAccumulated + actualDaysInMonth) {
                hijriMonth = m
                rem -= daysAccumulated
                break
            }
            daysAccumulated += actualDaysInMonth
        }
        
        val hijriDay = rem + 1
        return HijriDate(hijriDay, hijriMonth, hijriYear)
    }

    fun getGregorianEquivalent(hijriMonth: Int, hijriDay: Int, isAr: Boolean, adjustmentDays: Int = 0): String {
        val today = Calendar.getInstance()
        val currentYear = today.get(Calendar.YEAR)
        val currentMonth = today.get(Calendar.MONTH) + 1
        val currentDay = today.get(Calendar.DAY_OF_MONTH)
        
        val searchYears = listOf(currentYear, currentYear + 1)
        val language = if (isAr) "ar" else UiStrings.language
        
        // Phase 1: Search from today onwards (current or upcoming occurrence)
        for (year in searchYears) {
            val startMonth = if (year == currentYear) currentMonth else 1
            for (month in startMonth..12) {
                val startDayVal = if (year == currentYear && month == currentMonth) currentDay else 1
                val daysInMonth = when (month) {
                    2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
                    4, 6, 9, 11 -> 30
                    else -> 31
                }
                for (day in startDayVal..daysInMonth) {
                    val hijri = hijriFor(year, month, day, adjustmentDays)
                    if (hijri.month == hijriMonth && hijri.day == hijriDay) {
                        return "$day ${gregorianMonthShort(month, language)} $year"
                    }
                }
            }
        }
        // No second pass over the past months: an occasion without an occurrence left in the
        // search window has no upcoming date. Searching from January again used to pair a future
        // start with a past end when a Hijri month had no 30th day.
        return ""
    }

    fun getGregorianEquivalentRange(hijriMonth: Int, startDay: Int, endDay: Int, isAr: Boolean, adjustmentDays: Int = 0): String {
        val startStr = getGregorianEquivalent(hijriMonth, startDay, isAr, adjustmentDays)

        // A Hijri month can be 29 days long, in which case the requested last day does not exist
        // that year. End the range on the last day that does, instead of dropping the range.
        var lastDayValue = endDay
        var endStr = getGregorianEquivalent(hijriMonth, lastDayValue, isAr, adjustmentDays)
        while (endStr.isEmpty() && lastDayValue > startDay) {
            lastDayValue -= 1
            endStr = getGregorianEquivalent(hijriMonth, lastDayValue, isAr, adjustmentDays)
        }

        return if (startStr.isNotEmpty() && endStr.isNotEmpty()) {
            UiStrings.format("%s to %s", startStr, endStr)
        } else {
            ""
        }
    }
}
