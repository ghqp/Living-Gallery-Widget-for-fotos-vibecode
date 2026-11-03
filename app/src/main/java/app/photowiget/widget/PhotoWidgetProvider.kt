package app.photowiget.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle

open class PhotoWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val provider = javaClass.name
        runAsync(context) { container ->
            appWidgetIds.forEach { container.repository.claimPendingPin(it, provider) }
            container.widgets.update(appWidgetIds.toList())
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        runAsync(context) { container -> container.widgets.update(listOf(appWidgetId)) }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        runAsync(context) { container -> container.repository.unbind(appWidgetIds.toList()) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_NEXT_PHOTO) {
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID,
            )
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                runAsync(context) { container ->
                    container.repository.advance(appWidgetId)
                    container.widgets.update(listOf(appWidgetId))
                }
            }
            return
        }
        super.onReceive(context, intent)
    }

    companion object {
        const val ACTION_NEXT_PHOTO = "app.photowiget.action.NEXT_PHOTO"
    }
}
