package dev.barakah.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/**
 * How much width the app window has, in the three sizes Material 3 names.
 *
 * This used to be decided from the orientation, which asks the wrong question twice over: a tablet
 * held upright is wider than a landscape phone but reports PORTRAIT, and an unfolded foldable is
 * wider still and also reports PORTRAIT. What determines whether there is room for a rail, or for a
 * list beside its detail, is how many dp the window actually got.
 *
 * Breakpoints are Material 3's: 600dp for "medium" and 840dp for "expanded".
 */
enum class WindowWidth {
    /** A phone in portrait, or a narrow split-screen window. One pane, bottom navigation. */
    Compact,

    /** A landscape phone, a foldable, or a tablet upright. Navigation rail, still one pane. */
    Medium,

    /** A tablet, or a foldable unfolded into its widest posture. Rail and two panes. */
    Expanded;

    /** True where a navigation rail fits better than a bottom bar. */
    val showsNavigationRail: Boolean get() = this != Compact

    /** True where a screen with a detail view can show the list and the detail at once. */
    val showsTwoPanes: Boolean get() = this == Expanded

    companion object {
        /** Material 3's medium breakpoint. */
        const val MEDIUM_DP = 600

        /** Material 3's expanded breakpoint. */
        const val EXPANDED_DP = 840

        fun of(widthDp: Int): WindowWidth = when {
            widthDp >= EXPANDED_DP -> Expanded
            widthDp >= MEDIUM_DP -> Medium
            else -> Compact
        }
    }
}

/** The [WindowWidth] of the window this composable is drawing into. */
@Composable
fun currentWindowWidth(): WindowWidth = WindowWidth.of(LocalConfiguration.current.screenWidthDp)
