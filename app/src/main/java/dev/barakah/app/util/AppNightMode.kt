package dev.barakah.app.util

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import dev.barakah.app.data.AppPrefs

/**
 * Hands the app's own light/dark choice to the system, so that what the system draws *for* us agrees
 * with what Compose draws *in* us.
 *
 * The one place it shows is the Android 12+ splash screen. That is a starting window the system builds
 * from the manifest theme before MainActivity exists, and it picks its -night resources from the
 * phone's night mode, so a dark app on a light phone flashed a light splash (and the reverse) whatever
 * the appearance setting said. UiModeManager#setApplicationNightMode is the API meant for this, and
 * the system persists the value, so the next launch is right as well. It is API 31+, which is the
 * version the splash resources were written for anyway.
 */
object AppNightMode {
    /** Applies the saved preference. Safe on every launch: a call that changes nothing is a no-op. */
    fun applyStored(context: Context) {
        val theme = context.getSharedPreferences(AppPrefs.FILE, Context.MODE_PRIVATE)
            .getString(AppPrefs.APP_THEME, "system") ?: "system"
        apply(context, theme)
    }

    /** [theme] is one of the app's three values: "dark", "light", or anything else for the phone's. */
    fun apply(context: Context, theme: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val mode = when (theme) {
            "dark" -> UiModeManager.MODE_NIGHT_YES
            "light" -> UiModeManager.MODE_NIGHT_NO
            // There is no "drop the app-level override" value in the API. UiModeManagerService maps
            // MODE_NIGHT_AUTO to Configuration.UI_MODE_NIGHT_UNDEFINED, which is that case, so this
            // is what following the phone has to pass.
            else -> UiModeManager.MODE_NIGHT_AUTO
        }
        // A repeat call with the value already in effect reports no change and recreates nothing.
        context.getSystemService(UiModeManager::class.java)?.setApplicationNightMode(mode)
    }
}
