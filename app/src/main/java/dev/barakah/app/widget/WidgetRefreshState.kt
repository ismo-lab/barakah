package dev.barakah.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.datastore.preferences.core.longPreferencesKey

/**
 * A Glance state value that changes whenever app-owned widget data is refreshed.
 * Reading it from widget composition makes that refresh observable to Glance.
 */
object WidgetRefreshState {
    val refreshToken = longPreferencesKey("widget_refresh_token")
}

suspend fun refreshWidgetInstances(
    context: Context,
    widget: GlanceAppWidget,
    glanceIds: List<GlanceId>
) {
    glanceIds.forEach { glanceId ->
        updateAppWidgetState(context, glanceId) { state ->
            state[WidgetRefreshState.refreshToken] = (state[WidgetRefreshState.refreshToken] ?: 0L) + 1L
        }
        widget.update(context, glanceId)
    }
}
