package dev.barakah.app.ui.theme

import androidx.compose.ui.graphics.Color

// Bold Typography Theme Colors (High contrast, forest greens, lime accents, off-white background)
val GreenPrimaryLight = Color(0xFF1B6E3D)       // Icon green: the deep end of the launcher gradient
val GreenOnPrimaryLight = Color(0xFFFFFFFF)     // Soft light off-white
val GreenPrimaryContainerLight = Color(0xFFE2F0E7) // Vibrant Lime Green
val GreenOnPrimaryContainerLight = Color(0xFF072B17) // Very Dark Forest Green

val GoldSecondaryLight = Color(0xFF414941)       // Muted Dark Slate Green
val GoldOnSecondaryLight = Color(0xFFFFFFFF)
val GoldSecondaryContainerLight = Color(0xFFD7E8CD) // Light Sage Accent
val GoldOnSecondaryContainerLight = Color(0xFF111F0E) // Dark Forest text

val SurfaceLight = Color(0xFFFBFDF8)            // Pristine off-white
val OnSurfaceLight = Color(0xFF191C19)          // Off-black
val BackgroundLight = Color(0xFFFBFDF8)         // Pristine off-white
val OnBackgroundLight = Color(0xFF191C19)

val SurfaceVariantLight = Color(0xFFF0F5EB)     // Warm light minty-green
val OnSurfaceVariantLight = Color(0xFF5C635C)   // Secondary text gray-green

// Outline roles. The Material 3 baseline for these is a purple-tinted grey, which is visible
// wherever this app draws a divider, a card border or an unselected icon.
val OutlineLight = Color(0xFF6F776F)
val OutlineVariantLight = Color(0xFFC6CEC3)

// Tertiary is the amber accent (the Morning Adhkar card). Baseline tertiary is pink.
val TertiaryLight = Color(0xFF7A5900)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFFFDFA0)
val OnTertiaryContainerLight = Color(0xFF271900)

// Steps between BackgroundLight and SurfaceVariantLight. Material 3 1.3 moved NavigationBar,
// NavigationRail and AlertDialog off Surface/TonalElevation and onto these roles, so leaving
// them unset paints those components with the baseline neutral instead of this palette.
val SurfaceDimLight = Color(0xFFDBDED6)
val SurfaceBrightLight = Color(0xFFFBFDF8)
val SurfaceContainerLowestLight = Color(0xFFFFFFFF)
val SurfaceContainerLowLight = Color(0xFFF5F8F2)
val SurfaceContainerLight = Color(0xFFEFF2EC)
val SurfaceContainerHighLight = Color(0xFFE9EDE6)
val SurfaceContainerHighestLight = Color(0xFFE3E7E0)

// Dark Theme Colors supporting same theme vibe (High-contrast bold neon accent)
val GreenPrimaryDark = Color(0xFF81D7A1)        // Lime accent as primary key in dark
val GreenOnPrimaryDark = Color(0xFF003816)
val GreenPrimaryContainerDark = Color(0xFF1B6E3D)  // Same icon green, for a container that must not read as a lighter green
val GreenOnPrimaryContainerDark = Color(0xFFE2F0E7)

val GoldSecondaryDark = Color(0xFFD7E8CD)
val GoldOnSecondaryDark = Color(0xFF111F0E)
val GoldSecondaryContainerDark = Color(0xFF414941)
val GoldOnSecondaryContainerDark = Color(0xFFF0F5EB)

val SurfaceDark = Color(0xFF121411)
val OnSurfaceDark = Color(0xFFFCFDF9)
val BackgroundDark = Color(0xFF121411)
val OnBackgroundDark = Color(0xFFFCFDF9)

val SurfaceVariantDark = Color(0xFF191C19)
val OnSurfaceVariantDark = Color(0xFFDCDEC0)

val OutlineDark = Color(0xFF8B938A)
val OutlineVariantDark = Color(0xFF3E453E)

val TertiaryDark = Color(0xFFE9C46A)
val OnTertiaryDark = Color(0xFF412D00)
val TertiaryContainerDark = Color(0xFF5C4200)
val OnTertiaryContainerDark = Color(0xFFFFDFA0)

val SurfaceDimDark = Color(0xFF121411)
val SurfaceBrightDark = Color(0xFF383B36)
val SurfaceContainerLowestDark = Color(0xFF0C0F0C)
val SurfaceContainerLowDark = Color(0xFF191C19)
val SurfaceContainerDark = Color(0xFF1D201C)
val SurfaceContainerHighDark = Color(0xFF282B27)
val SurfaceContainerHighestDark = Color(0xFF333631)
