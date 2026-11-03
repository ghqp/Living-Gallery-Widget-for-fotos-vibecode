package app.photowiget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import app.photowiget.data.GalleryRepository
import app.photowiget.data.WidgetSpan
import app.photowiget.data.model.GalleryData
import app.photowiget.data.model.WidgetDesign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Keeps home screen widgets in sync with the gallery and places new ones. */
class WidgetController(
    private val context: Context,
    private val repository: GalleryRepository,
    private val renderer: WidgetRenderer,
) {
    private val manager: AppWidgetManager get() = AppWidgetManager.getInstance(context)

    val canPin: Boolean get() = manager.isRequestPinAppWidgetSupported

    fun liveWidgetIds(): Set<Int> = allProviderClasses.flatMapTo(HashSet()) { provider ->
        manager.getAppWidgetIds(ComponentName(context, provider)).asList()
    }

    /** Forgets placements whose widgets are gone and returns the ids still on the home screen. */
    suspend fun reconcile(): Set<Int> = withContext(Dispatchers.Default) {
        val live = liveWidgetIds()
        repository.retainPlacements(live)
        live
    }

    suspend fun update(appWidgetIds: Collection<Int>) = withContext(Dispatchers.Default) {
        if (appWidgetIds.isEmpty()) return@withContext
        val data = repository.snapshot()
        appWidgetIds.distinct().forEach { push(it, data) }
    }

    suspend fun updateDesign(designId: String) {
        update(repository.snapshot().placements.filter { it.designId == designId }.map { it.appWidgetId })
    }

    suspend fun updateAll() {
        update(liveWidgetIds())
    }

    suspend fun requestPin(design: WidgetDesign, span: WidgetSpan): Boolean = withContext(Dispatchers.Default) {
        if (!manager.isRequestPinAppWidgetSupported) return@withContext false
        val provider = ComponentName(context, span.providerClass())
        repository.markPendingPin(design.id, provider.className)
        val mutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
        val callback = PendingIntent.getBroadcast(
            context,
            design.id.hashCode(),
            Intent(context, PinResultReceiver::class.java)
                .putExtra(PinResultReceiver.EXTRA_DESIGN_ID, design.id),
            PendingIntent.FLAG_UPDATE_CURRENT or mutable,
        )
        val extras = Bundle().apply {
            putParcelable(AppWidgetManager.EXTRA_APPWIDGET_PREVIEW, renderer.pinPreview(design, span))
        }
        manager.requestPinAppWidget(provider, extras, callback)
    }

    private fun push(appWidgetId: Int, data: GalleryData) {
        val placement = data.placements.firstOrNull { it.appWidgetId == appWidgetId }
        val design = placement?.let { found -> data.designs.firstOrNull { it.id == found.designId } }
        val options = manager.getAppWidgetOptions(appWidgetId)
        try {
            manager.updateAppWidget(appWidgetId, renderer.views(appWidgetId, design, placement, options))
        } catch (_: RuntimeException) {
            // Launchers cap the bitmap memory of a single update; a smaller bitmap still looks sharp enough.
            manager.updateAppWidget(appWidgetId, renderer.views(appWidgetId, design, placement, options, scale = 0.5f))
        }
    }
}
