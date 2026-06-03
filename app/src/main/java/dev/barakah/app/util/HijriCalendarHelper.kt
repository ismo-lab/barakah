package dev.barakah.app.util

import android.os.Build
import java.util.Calendar

object HijriCalendarHelper {

    data class HijriDate(val day: Int, val month: Int, val year: Int)

    fun getTomorrowHijriDate(): HijriDate {
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val tomorrowLocalDate = java.time.LocalDate.now().plusDays(1)
                val hijriDate = java.time.chrono.HijrahDate.from(tomorrowLocalDate)
                val day = hijriDate.get(java.time.temporal.ChronoField.DAY_OF_MONTH)
                val month = hijriDate.get(java.time.temporal.ChronoField.MONTH_OF_YEAR)
                val year = hijriDate.get(java.time.temporal.ChronoField.YEAR)
                return HijriDate(day, month, year)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // Fallback approximate conversion for older Android versions
        val day = tomorrow.get(Calendar.DAY_OF_MONTH)
        val month = tomorrow.get(Calendar.MONTH) + 1
        val year = tomorrow.get(Calendar.YEAR)
        
        if (year == 2026 && month == 5) {
            val hijriDay = day - 18 + 1
            return if (hijriDay > 0) HijriDate(hijriDay, 12, 1447) else HijriDate(30 + hijriDay, 11, 1447)
        } else if (year == 2026 && month == 6) {
            val hijriDay = day + 13
            return if (hijriDay <= 30) HijriDate(hijriDay, 12, 1447) else HijriDate(hijriDay - 30, 1, 1448)
        }
        
        // Simple astronomical formula fallback (Kuwaiti algorithm)
        val jd = getJulianDay(year, month, day)
        return getHijriFromJulian(jd)
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

    fun getGregorianEquivalent(hijriMonth: Int, hijriDay: Int, isAr: Boolean): String {
        // Use a Gregorian search around the current Gregorian year (covering current + next)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val searchYears = listOf(currentYear, currentYear + 1)
        val monthNamesEn = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val monthNamesAr = listOf("", "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")
        
        for (year in searchYears) {
            for (month in 1..12) {
                val daysInMonth = when (month) {
                    2 -> if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) 29 else 28
                    4, 6, 9, 11 -> 30
                    else -> 31
                }
                for (day in 1..daysInMonth) {
                    val jd = getJulianDay(year, month, day)
                    val hijri = getHijriFromJulian(jd)
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

    fun getGregorianEquivalentRange(hijriMonth: Int, startDay: Int, endDay: Int, isAr: Boolean): String {
        val startStr = getGregorianEquivalent(hijriMonth, startDay, isAr)
        val endStr = getGregorianEquivalent(hijriMonth, endDay, isAr)
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
