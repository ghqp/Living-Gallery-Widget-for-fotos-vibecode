package app.photowiget.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Receives the launcher's confirmation that a requested widget was placed. */
class PinResultReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        )
        val designId = intent.getStringExtra(EXTRA_DESIGN_ID)
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID || designId == null) return
        runAsync(context) { container ->
            container.repository.bind(appWidgetId, designId)
            container.widgets.update(listOf(appWidgetId))
        }
    }

    companion object {
        const val EXTRA_DESIGN_ID = "app.photowiget.extra.DESIGN_ID"
    }
}
