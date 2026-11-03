package app.photowiget.render

import android.graphics.Path
import android.graphics.RectF
import app.photowiget.data.model.FrameShape
import kotlin.math.PI
import kotlin.math.min

object FrameGeometry {
    private val rounded by lazy { FrameCurves.superellipse(exponent = 4f, segments = 256) }
    private val cookie by lazy {
        FrameCurves.lobed(lobes = 9, depth = 0.1f, rotation = (-PI / 2).toFloat(), segments = 360)
    }
    private val clover by lazy {
        FrameCurves.lobed(lobes = 4, depth = 0.34f, rotation = (PI / 4).toFloat(), segments = 360)
    }

    /** Writes the outline of [frame] filling [rect] into [path]; [cornerRadius] is in pixels. */
    fun outline(frame: FrameShape, rect: RectF, cornerRadius: Float, path: Path): Path {
        path.rewind()
        when (frame) {
            FrameShape.Circle -> path.addOval(rect, Path.Direction.CW)
            FrameShape.Square -> {
                val radius = cornerRadius.coerceIn(0f, min(rect.width(), rect.height()) / 2f)
                path.addRoundRect(rect, radius, radius, Path.Direction.CW)
            }
            FrameShape.Rounded -> polygon(path, rounded, rect)
            FrameShape.Arch -> arch(path, rect, cornerRadius)
            FrameShape.Cookie -> polygon(path, cookie, rect)
            FrameShape.Clover -> polygon(path, clover, rect)
        }
        return path
    }

    private fun polygon(path: Path, unit: FloatArray, rect: RectF) {
        val centerX = rect.centerX()
        val centerY = rect.centerY()
        val halfWidth = rect.width() / 2f
        val halfHeight = rect.height() / 2f
        path.moveTo(centerX + unit[0] * halfWidth, centerY + unit[1] * halfHeight)
        for (i in 2 until unit.size step 2) {
            path.lineTo(centerX + unit[i] * halfWidth, centerY + unit[i + 1] * halfHeight)
        }
        path.close()
    }

    private fun arch(path: Path, rect: RectF, cornerRadius: Float) {
        val top = rect.width() / 2f
        val bottom = cornerRadius.coerceIn(0f, min(rect.width() / 2f, rect.height() - top))
        path.moveTo(rect.left, rect.top + top)
        path.arcTo(rect.left, rect.top, rect.right, rect.top + 2f * top, 180f, 180f, false)
        path.lineTo(rect.right, rect.bottom - bottom)
        if (bottom > 0f) {
            path.arcTo(rect.right - 2f * bottom, rect.bottom - 2f * bottom, rect.right, rect.bottom, 0f, 90f, false)
        }
        path.lineTo(rect.left + bottom, rect.bottom)
        if (bottom > 0f) {
            path.arcTo(rect.left, rect.bottom - 2f * bottom, rect.left + 2f * bottom, rect.bottom, 90f, 90f, false)
        }
        path.close()
    }
}
