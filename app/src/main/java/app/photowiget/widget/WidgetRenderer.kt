package app.photowiget.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.util.SizeF
import android.view.View
import android.widget.RemoteViews
import androidx.core.os.BundleCompat
import app.photowiget.MainActivity
import app.photowiget.R
import app.photowiget.data.PhotoDecoder
import app.photowiget.data.PhotoFiles
import app.photowiget.data.WidgetSpan
import app.photowiget.data.artworkSideDp
import app.photowiget.data.model.Placement
import app.photowiget.data.model.TapAction
import app.photowiget.data.model.WidgetDesign
import app.photowiget.data.model.WidgetLook
import app.photowiget.render.ArtworkCanvasCache
import app.photowiget.render.ArtworkColors
import app.photowiget.render.ArtworkPainter
import app.photowiget.render.CropMath
import app.photowiget.render.CropWindow
import app.photowiget.render.toSpec
import kotlin.math.min
import kotlin.math.roundToInt

class WidgetRenderer(private val context: Context, private val files: PhotoFiles) {

    fun views(
        appWidgetId: Int,
        design: WidgetDesign?,
        placement: Placement?,
        options: Bundle?,
        scale: Float = 1f,
    ): RemoteViews {
        val side = (artworkSidePx(options) * scale).roundToInt().coerceIn(MIN_SIDE, MAX_SIDE)
        val views = RemoteViews(context.packageName, R.layout.widget_photo)
        if (design == null || design.photos.isEmpty()) {
            views.setImageViewBitmap(R.id.widget_photo, placeholder(side))
            views.setViewVisibility(R.id.widget_setup, View.VISIBLE)
            views.setContentDescription(R.id.widget_photo, context.getString(R.string.widget_setup))
            views.setOnClickPendingIntent(R.id.widget_root, setupIntent(appWidgetId))
            return views
        }
        val index = (placement?.photoIndex ?: 0).mod(design.photos.size)
        views.setImageViewBitmap(R.id.widget_photo, artwork(design, index, side))
        views.setViewVisibility(R.id.widget_setup, View.GONE)
        views.setContentDescription(
            R.id.widget_photo,
            context.getString(R.string.widget_content_description, design.name, index + 1, design.photos.size),
        )
        val rotates = design.photos.size > 1 && design.behavior.tapAction == TapAction.NextPhoto
        views.setOnClickPendingIntent(
            R.id.widget_root,
            if (rotates) nextIntent(appWidgetId) else openIntent(appWidgetId, design.id),
        )
        return views
    }

    /** What the launcher shows in its "Add widget?" dialog. */
    fun pinPreview(design: WidgetDesign, span: WidgetSpan): RemoteViews {
        val density = context.resources.displayMetrics.density
        val side = (min(span.minWidthDp, span.minHeightDp) * density).roundToInt().coerceIn(MIN_SIDE, PREVIEW_MAX_SIDE)
        return RemoteViews(context.packageName, R.layout.widget_photo).apply {
            setImageViewBitmap(R.id.widget_photo, artwork(design, 0, side))
            setViewVisibility(R.id.widget_setup, View.GONE)
        }
    }

    fun artwork(design: WidgetDesign, index: Int, side: Int): Bitmap {
        val output = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val cache = ArtworkCanvasCache()
        val item = design.photos.getOrNull(index)
        val zoom = item?.crop?.zoom?.coerceIn(1f, CropMath.MAX_ZOOM) ?: 1f
        val decoded = item?.let {
            PhotoDecoder.decode(files.photoFile(design.id, it), (side * zoom).roundToInt().coerceAtMost(MAX_DECODE_SIDE))
        }
        if (item == null || decoded == null) {
            drawPlaceholder(canvas, side, design.look, cache)
            return output
        }
        try {
            val (source, window) = downsample(decoded, CropMath.window(decoded.width, decoded.height, item.crop), side)
            val border = ArtworkColors.border(design.look.borderTint, design.look.tone, source)
            ArtworkPainter.draw(
                canvas, 0f, 0f, side.toFloat(), side.toFloat(),
                source, window, design.look.toSpec(border), PLACEHOLDER_COLOR, cache,
            )
            if (source !== decoded) source.recycle()
        } finally {
            decoded.recycle()
        }
        return output
    }

    private fun placeholder(side: Int): Bitmap {
        val output = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        drawPlaceholder(Canvas(output), side, WidgetLook(), ArtworkCanvasCache())
        return output
    }

    private fun drawPlaceholder(canvas: Canvas, side: Int, look: WidgetLook, cache: ArtworkCanvasCache) {
        ArtworkPainter.draw(
            canvas, 0f, 0f, side.toFloat(), side.toFloat(),
            null, null, look.toSpec(PLACEHOLDER_COLOR), PLACEHOLDER_COLOR, cache,
        )
    }

    /** Halves the cropped region until it is at most twice the target, which keeps fine detail crisp. */
    private fun downsample(bitmap: Bitmap, window: CropWindow, target: Int): Pair<Bitmap, CropWindow> {
        var current = bitmap
        var currentWindow = window
        val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
        val matrix = Matrix()
        while (currentWindow.size / 2f >= target) {
            val size = (currentWindow.size / 2f).roundToInt().coerceAtLeast(1)
            val next = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            matrix.setRectToRect(
                RectF(currentWindow.left, currentWindow.top, currentWindow.left + currentWindow.size, currentWindow.top + currentWindow.size),
                RectF(0f, 0f, size.toFloat(), size.toFloat()),
                Matrix.ScaleToFit.FILL,
            )
            Canvas(next).drawBitmap(current, matrix, paint)
            if (current !== bitmap) current.recycle()
            current = next
            currentWindow = CropWindow(0f, 0f, size.toFloat())
        }
        return current to currentWindow
    }

    private fun artworkSidePx(options: Bundle?): Int {
        val density = context.resources.displayMetrics.density
        val sizes = options?.let {
            BundleCompat.getParcelableArrayList(it, AppWidgetManager.OPTION_APPWIDGET_SIZES, SizeF::class.java)
        }
        val sideDp = when {
            !sizes.isNullOrEmpty() -> sizes.maxOf { min(it.width, it.height) }.roundToInt()
            options != null -> artworkSideDp(
                minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, DEFAULT_SIDE_DP),
                maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, DEFAULT_SIDE_DP),
                minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, DEFAULT_SIDE_DP),
                maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, DEFAULT_SIDE_DP),
            )
            else -> DEFAULT_SIDE_DP
        }
        return (sideDp.coerceAtLeast(MIN_SIDE_DP) * density).roundToInt()
    }

    private fun nextIntent(appWidgetId: Int): PendingIntent = PendingIntent.getBroadcast(
        context,
        appWidgetId,
        Intent(context, PhotoWidgetProvider::class.java)
            .setAction(PhotoWidgetProvider.ACTION_NEXT_PHOTO)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            .setData(widgetUri(appWidgetId)),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun openIntent(appWidgetId: Int, designId: String): PendingIntent = PendingIntent.getActivity(
        context,
        appWidgetId,
        Intent(context, MainActivity::class.java)
            .setAction(MainActivity.ACTION_OPEN_DESIGN)
            .putExtra(MainActivity.EXTRA_DESIGN_ID, designId)
            .setData(widgetUri(appWidgetId))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun setupIntent(appWidgetId: Int): PendingIntent = PendingIntent.getActivity(
        context,
        appWidgetId,
        Intent(context, WidgetConfigureActivity::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            .setData(widgetUri(appWidgetId)),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun widgetUri(appWidgetId: Int): Uri = Uri.parse("livinggallery://widget/$appWidgetId")

    private companion object {
        const val DEFAULT_SIDE_DP = 110
        const val MIN_SIDE_DP = 48
        const val MIN_SIDE = 96
        const val MAX_SIDE = 1024
        const val PREVIEW_MAX_SIDE = 360
        const val MAX_DECODE_SIDE = 3072
        const val PLACEHOLDER_COLOR = 0xFF2C3036.toInt()
    }
}
