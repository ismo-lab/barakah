package dev.barakah.app.util

import android.content.Context
import android.text.format.DateFormat
import java.util.*
import java.text.SimpleDateFormat

object TimeUtils {
    fun formatDisplayTime(context: Context, timeStr: String, isAr: Boolean = false, useWesternNumbersInArabic: Boolean = false): String {
        if (timeStr.contains("-")) {
            val parts = timeStr.split("-")
            if (parts.size == 2) {
                val start = formatDisplayTime(context, parts[0].trim(), isAr, useWesternNumbersInArabic)
                val end = formatDisplayTime(context, parts[1].trim(), isAr, useWesternNumbersInArabic)
                return "$start - $end"
            }
        }
        return try {
            val is24Hour = DateFormat.is24HourFormat(context)
            val parts = timeStr.trim().split(":")
            val h = parts[0].toInt()
            val m = parts[1].split(" ")[0].trim().toInt()
            
            val locale = Locale.US
            val formatted = if (is24Hour) {
                java.lang.String.format(locale, "%02d:%02d", h, m)
            } else {
                val hour12 = if (h % 12 == 0) 12 else h % 12
                val amPm = if (h < 12) {
                    if (isAr) "ص" else "AM"
                } else {
                    if (isAr) "م" else "PM"
                }
                java.lang.String.format(locale, "%02d:%02d %s", hour12, m, amPm)
            }
            formatted.localize(isAr, useWesternNumbersInArabic)
        } catch (e: Exception) {
            timeStr.localize(isAr, useWesternNumbersInArabic)
        }
    }

    data class FormattedTime(
        val digits: String,
        val suffix: String = ""
    )

    fun parseDisplayTime(context: Context, timeStr: String, isAr: Boolean = false, useWesternNumbersInArabic: Boolean = false): FormattedTime {
        if (timeStr.contains("-")) {
            val parts = timeStr.split("-")
            if (parts.size == 2) {
                val start = formatDisplayTime(context, parts[0].trim(), isAr, useWesternNumbersInArabic)
                val end = formatDisplayTime(context, parts[1].trim(), isAr, useWesternNumbersInArabic)
                return FormattedTime("$start - $end", "")
            }
        }
        return try {
            val is24Hour = DateFormat.is24HourFormat(context)
            val parts = timeStr.trim().split(":")
            val h = parts[0].toInt()
            val m = parts[1].split(" ")[0].trim().toInt()
            
            val locale = Locale.US
            val digitsRaw = if (is24Hour) {
                java.lang.String.format(locale, "%02d:%02d", h, m)
            } else {
                val hour12 = if (h % 12 == 0) 12 else h % 12
                java.lang.String.format(locale, "%02d:%02d", hour12, m)
            }
            val suffixRaw = if (is24Hour) {
                ""
            } else {
                if (h < 12) {
                    if (isAr) "ص" else "AM"
                } else {
                    if (isAr) "م" else "PM"
                }
            }
            FormattedTime(
                digits = digitsRaw.localize(isAr, useWesternNumbersInArabic),
                suffix = suffixRaw.localize(isAr, useWesternNumbersInArabic)
            )
        } catch (e: Exception) {
            FormattedTime(timeStr.localize(isAr, useWesternNumbersInArabic), "")
        }
    }

    fun calculateOffsetTime(timeStr: String, offsetMinutes: Int): String {
        return try {
            val parts = timeStr.trim().split(":")
            val h = parts[0].toInt()
            val m = parts[1].split(" ")[0].trim().toInt()
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, h)
                set(Calendar.MINUTE, m)
                add(Calendar.MINUTE, offsetMinutes)
            }
            val nh = cal.get(Calendar.HOUR_OF_DAY)
            val nm = cal.get(Calendar.MINUTE)
            String.format(Locale.US, "%02d:%02d", nh, nm)
        } catch (e: Exception) {
            timeStr
        }
    }
}
