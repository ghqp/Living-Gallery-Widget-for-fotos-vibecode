package app.photowiget.render

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import app.photowiget.data.model.ColorTone
import app.photowiget.data.model.FrameShape
import app.photowiget.data.model.WidgetLook
import kotlin.math.min

/** Everything needed to draw one widget artwork. Radius and border are in design units. */
data class ArtworkSpec(
    val frame: FrameShape,
    val cornerRadius: Float,
    val borderWidth: Float,
    val borderColor: Int,
    val tone: ColorTone,
)

fun WidgetLook.toSpec(borderColor: Int) = ArtworkSpec(
    frame = frame,
    cornerRadius = cornerRadius.toFloat(),
    borderWidth = borderWidth.toFloat(),
    borderColor = borderColor,
    tone = tone,
)

/** Reusable drawing state; one instance per caller, never shared between threads. */
class ArtworkCanvasCache {
    internal val bounds = RectF()
    internal val square = RectF()
    internal val inner = RectF()
    internal val source = RectF()
    internal val outerPath = Path()
    internal val innerPath = Path()
    internal val matrix = Matrix()
    internal val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    internal val photo = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    internal var shader: BitmapShader? = null
    internal var shaderBitmap: Bitmap? = null
}

/**
 * Draws the widget artwork: the photo inside a centered square frame with an optional border.
 * The in-app preview and the home screen widget both draw through here, so they always match.
 */
object ArtworkPainter {
    /**
     * Corner radius and border width are defined on a square of this many dp and scale with the
     * artwork, so the frame keeps its proportions at every widget size.
     */
    const val DESIGN_SIDE = 96f

    fun draw(
        canvas: Canvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        source: Bitmap?,
        window: CropWindow?,
        spec: ArtworkSpec,
        placeholderColor: Int,
        cache: ArtworkCanvasCache,
    ) {
        val bounds = cache.bounds.apply { set(left, top, right, bottom) }
        val side = min(bounds.width(), bounds.height())
        if (side <= 0f) return
        val half = side / 2f
        val square = cache.square.apply {
            set(bounds.centerX() - half, bounds.centerY() - half, bounds.centerX() + half, bounds.centerY() + half)
        }
        val unit = side / DESIGN_SIDE
        val radius = spec.cornerRadius * unit
        val border = (spec.borderWidth * unit).coerceIn(0f, side / 4f)
        val outer = FrameGeometry.outline(spec.frame, square, radius, cache.outerPath)

        val photoPath = if (border > 0f) {
            cache.fill.shader = null
            cache.fill.color = spec.borderColor
            canvas.drawPath(outer, cache.fill)
            val inner = cache.inner.apply {
                set(square.left + border, square.top + border, square.right - border, square.bottom - border)
            }
            FrameGeometry.outline(spec.frame, inner, (radius - border).coerceAtLeast(0f), cache.innerPath)
        } else {
            outer
        }

        if (source == null || window == null || source.isRecycled) {
            cache.fill.color = placeholderColor
            canvas.drawPath(photoPath, cache.fill)
            return
        }

        val shader = cache.shader?.takeIf { cache.shaderBitmap === source }
            ?: BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).also {
                cache.shader = it
                cache.shaderBitmap = source
            }
        cache.source.set(window.left, window.top, window.left + window.size, window.top + window.size)
        cache.matrix.setRectToRect(cache.source, square, Matrix.ScaleToFit.FILL)
        shader.setLocalMatrix(cache.matrix)
        cache.photo.shader = shader
        cache.photo.colorFilter = ToneFilters.filter(spec.tone)
        canvas.drawPath(photoPath, cache.photo)
    }
}
