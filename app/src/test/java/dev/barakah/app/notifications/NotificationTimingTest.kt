package dev.barakah.app.notifications

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationTimingTest {
    private val now = 1_700_000_000_000L

    @Test
    fun `an on-time notification is never stale`() {
        assertFalse(NotificationTiming.isTooLate(now, now))
    }

    @Test
    fun `forty-five minutes late is still announced`() {
        // The case the old 30 minute rule dropped: a Doze wakeup routinely delays an alarm this far.
        assertFalse(NotificationTiming.isTooLate(now - 45 * 60 * 1000L, now))
    }

    @Test
    fun `one millisecond inside the window is still announced`() {
        assertFalse(NotificationTiming.isTooLate(now - NotificationTiming.MAX_LATE_MS + 1, now))
    }

    @Test
    fun `one millisecond past the window is stale`() {
        assertTrue(NotificationTiming.isTooLate(now - NotificationTiming.MAX_LATE_MS - 1, now))
    }

    @Test
    fun `a scheduled time in the future is not stale`() {
        assertFalse(NotificationTiming.isTooLate(now + 60_000L, now))
    }

    @Test
    fun `an absent scheduled time has nothing to judge`() {
        assertFalse(NotificationTiming.isTooLate(0L, now))
    }
}
