package dev.barakah.app.notifications

import dev.barakah.app.util.ClockTime
import java.util.Calendar
import java.util.TimeZone

/**
 * When an alarm for a time of day should fire.
 *
 * Prayer times are times of day, so an alarm for one is either today's or tomorrow's, and the rule
 * for choosing between them is the one that matters most in this part of the app. It has been wrong
 * before: treating the next minute as gone meant that opening the app in the 60 seconds before a
 * prayer moved that prayer's alert to the next day, so it was never announced at all.
 *
 * Only a time that has already passed belongs to tomorrow. A double notification for the same
 * prayer is prevented separately, in the receiver, which compares the alarm's own trigger time with
 * the last one it announced.
 */
object PrayerAlarmTimes {

    /**
     * The next time [timeOfDay] comes round after [nowMillis], in [timeZone], or null when
     * [timeOfDay] is not a clock time at all.
     *
     * Seconds and milliseconds are dropped, so the alarm lands on the minute. A time that is
     * exactly now counts as passed: arming the same alarm twice in one second moves it to tomorrow
     * rather than firing it immediately.
     */
    fun nextOccurrence(timeOfDay: String, nowMillis: Long, timeZone: TimeZone): Long? {
        val (hour, minute) = ClockTime.hourAndMinute(timeOfDay) ?: return null
        val at = Calendar.getInstance(timeZone).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= nowMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        return at.timeInMillis
    }
}
