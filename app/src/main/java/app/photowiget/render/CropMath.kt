package app.photowiget.render

import app.photowiget.data.model.PhotoCrop
import kotlin.math.min

/** Square region of the source photo, in source pixels. */
data class CropWindow(val left: Float, val top: Float, val size: Float)

object CropMath {
    const val MAX_ZOOM = 5f

    fun window(width: Int, height: Int, crop: PhotoCrop): CropWindow {
        val zoom = crop.zoom.coerceIn(1f, MAX_ZOOM)
        val size = min(width, height) / zoom
        val half = size / 2f
        val centerX = (crop.centerX * width).coerceIn(half, width - half)
        val centerY = (crop.centerY * height).coerceIn(half, height - half)
        return CropWindow(centerX - half, centerY - half, size)
    }

    /** Pulls the crop center back inside the photo so that the window never leaves it. */
    fun clamp(crop: PhotoCrop, width: Int, height: Int): PhotoCrop {
        if (width <= 0 || height <= 0) return PhotoCrop()
        val window = window(width, height, crop)
        val half = window.size / 2f
        return PhotoCrop(
            zoom = crop.zoom.coerceIn(1f, MAX_ZOOM),
            centerX = (window.left + half) / width,
            centerY = (window.top + half) / height,
        )
    }

    /**
     * Moves the photo by ([dx], [dy]) viewport pixels and scales it by [zoomBy] around the viewport
     * center, for a square viewport [viewport] pixels wide.
     */
    fun transform(
        crop: PhotoCrop,
        width: Int,
        height: Int,
        viewport: Float,
        dx: Float,
        dy: Float,
        zoomBy: Float,
    ): PhotoCrop {
        if (width <= 0 || height <= 0 || viewport <= 0f) return crop
        val current = clamp(crop, width, height)
        val scale = viewport / window(width, height, current).size
        val moved = current.copy(
            zoom = (current.zoom * zoomBy).coerceIn(1f, MAX_ZOOM),
            centerX = current.centerX - dx / scale / width,
            centerY = current.centerY - dy / scale / height,
        )
        return clamp(moved, width, height)
    }
}
