package dev.barakah.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The window-width breakpoints, at the boundaries that matter.
 *
 * These are the numbers the whole adaptive layout hangs off, and a one-off error here would either
 * put a rail on a narrow phone or leave a tablet with a bottom bar, neither of which a screenshot
 * test would necessarily catch.
 */
class WindowWidthTest {

    @Test
    fun `phones are compact`() {
        assertEquals(WindowWidth.Compact, WindowWidth.of(320))
        assertEquals(WindowWidth.Compact, WindowWidth.of(411))
        assertEquals(WindowWidth.Compact, WindowWidth.of(599))
    }

    @Test
    fun `the medium breakpoint starts at 600dp`() {
        assertEquals(WindowWidth.Medium, WindowWidth.of(WindowWidth.MEDIUM_DP))
        assertEquals(WindowWidth.Medium, WindowWidth.of(673))   // foldable, unfolded
        assertEquals(WindowWidth.Medium, WindowWidth.of(839))
    }

    @Test
    fun `the expanded breakpoint starts at 840dp`() {
        assertEquals(WindowWidth.Expanded, WindowWidth.of(WindowWidth.EXPANDED_DP))
        assertEquals(WindowWidth.Expanded, WindowWidth.of(1024))
        assertEquals(WindowWidth.Expanded, WindowWidth.of(1280)) // tablet, landscape
    }

    @Test
    fun `only compact keeps the bottom bar`() {
        assertFalse(WindowWidth.Compact.showsNavigationRail)
        assertTrue(WindowWidth.Medium.showsNavigationRail)
        assertTrue(WindowWidth.Expanded.showsNavigationRail)
    }

    @Test
    fun `only expanded shows two panes`() {
        assertFalse(WindowWidth.Compact.showsTwoPanes)
        assertFalse(WindowWidth.Medium.showsTwoPanes)
        assertTrue(WindowWidth.Expanded.showsTwoPanes)
    }
}
