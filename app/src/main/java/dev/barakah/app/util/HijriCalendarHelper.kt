package dev.barakah.app.util

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.util.Calendar

object HijriCalendarHelper {

    private const val TAG = "HijriCalendarHelper"

    data class HijriDate(val day: Int, val month: Int, val year: Int)

    /**
     * The Umm al-Qura conversion for an explicit date, so it can be exercised against known
     * dates instead of whenever a test happens to run.
     *
     * Uses java.time, so only call this on API 26 and up.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun getHijriDateFor(localDate: java.time.LocalDate): HijriDate {
        val hijriDate = java.time.chrono.HijrahDate.from(localDate)
        return HijriDate(
            day = hijriDate.get(java.time.temporal.ChronoField.DAY_OF_MONTH),
            month = hijriDate.get(java.time.temporal.ChronoField.MONTH_OF_YEAR),
            year = hijriDate.get(java.time.temporal.ChronoField.YEAR)
        )
    }

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
     * The one conversion the whole app goes through: Umm al-Qura where the platform has it, and
     * the arithmetic fallback on older devices.
     *
     * Keep every caller on this. The date shown on the home screen, the day an occasion reminder
     * fires and the date listed for that occasion used to use two different calendars, so the app
     * could show one Eid date and remind on another.
     *
     * [adjustmentDays] shifts the result by that many days, which is how a user lines the
     * calculated date up with the moon sighting their community follows.
     */
    private fun hijriFor(year: Int, month: Int, day: Int, adjustmentDays: Int): HijriDate {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val date = java.time.LocalDate.of(year, month, day).plusDays(adjustmentDays.toLong())
                return getHijriDateFor(date)
            } catch (e: Exception) {
                Log.e(TAG, "Falling back to the arithmetic Hijri conversion", e)
            }
        }

        return getHijriFromJulian(getJulianDay(year, month, day) + adjustmentDays)
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
        
        val monthName = if (isAr) {
            if (hijri.month in 1..12) monthNamesAr[hijri.month] else "محرم"
        } else {
            if (hijri.month in 1..12) monthNamesEn[hijri.month] else "Muharram"
        }
        
        return if (isAr) {
            "${hijri.day} $monthName ${hijri.year} هـ"
        } else {
            "${hijri.day} $monthName ${hijri.year} AH"
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
        val monthNamesEn = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val monthNamesAr = listOf("", "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")
        
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
                        return if (isAr) {
                            "$day ${monthNamesAr[month]} $year"
                        } else {
                            "$day ${monthNamesEn[month]} $year"
                        }
                    }
                }
            }
        }
        
        // Phase 2: Fallback search of the entire period (Jan 1 of current year to Dec 31 of next year)
        for (year in searchYears) {
            for (month in 1..12) {
                val daysInMonth = when (month) {
                    2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
                    4, 6, 9, 11 -> 30
                    else -> 31
                }
                for (day in 1..daysInMonth) {
                    val hijri = hijriFor(year, month, day, adjustmentDays)
                    if (hijri.month == hijriMonth && hijri.day == hijriDay) {
                        return if (isAr) {
                            "$day ${monthNamesAr[month]} $year"
                        } else {
                            "$day ${monthNamesEn[month]} $year"
                        }
                    }
                }
            }
        }
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
            if (isAr) {
                "من $startStr إلى $endStr"
            } else {
                "$startStr to $endStr"
            }
        } else {
            ""
        }
    }
}
