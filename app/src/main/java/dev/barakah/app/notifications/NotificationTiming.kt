package dev.barakah.app.notifications

/**
 * How late a prayer notification may be and still be worth showing.
 *
 * The exact-alarm path can be delayed while the device sits in Doze, and the inexact fallback used
 * when the user withholds the alarm permission can be delayed further, so a lateness rule measured
 * in minutes turns a merely delayed alarm into no notification at all. A prayer announced an hour
 * or two late is still useful; one that turns up the next day is not.
 */
object NotificationTiming {
    const val MAX_LATE_MS = 2 * 60 * 60 * 1000L

    /** True when [scheduledTimeMs] is so far behind [nowMs] that the reminder is stale. */
    fun isTooLate(scheduledTimeMs: Long, nowMs: Long): Boolean =
        scheduledTimeMs != 0L && nowMs - scheduledTimeMs > MAX_LATE_MS
}
