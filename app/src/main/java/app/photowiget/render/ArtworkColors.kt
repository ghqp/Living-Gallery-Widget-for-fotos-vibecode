package app.photowiget.render

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.ColorUtils
import app.photowiget.data.model.BorderTint
import app.photowiget.data.model.ColorTone

object ArtworkColors {
    private const val FALLBACK_BORDER = 0xFFE1E2E8.toInt()

    fun border(tint: BorderTint, tone: ColorTone, photo: Bitmap?): Int =
        tint.argb ?: photo?.let { fromPhoto(it, tone) } ?: FALLBACK_BORDER

    /** A light tint of the photo's average color, passed through the same tone as the photo. */
    fun fromPhoto(photo: Bitmap, tone: ColorTone): Int {
        if (photo.isRecycled || photo.config == Bitmap.Config.HARDWARE) return FALLBACK_BORDER
        val steps = 12
        var red = 0L
        var green = 0L
        var blue = 0L
        for (y in 0 until steps) {
            for (x in 0 until steps) {
                val pixel = photo.getPixel(
                    ((x + 0.5f) * photo.width / steps).toInt().coerceIn(0, photo.width - 1),
                    ((y + 0.5f) * photo.height / steps).toInt().coerceIn(0, photo.height - 1),
                )
                red += Color.red(pixel)
                green += Color.green(pixel)
                blue += Color.blue(pixel)
            }
        }
        val count = steps * steps
        val toned = ToneMatrices.apply(
            ToneMatrices.of(tone),
            red.toFloat() / count,
            green.toFloat() / count,
            blue.toFloat() / count,
        )
        val hsl = FloatArray(3)
        ColorUtils.RGBToHSL(toned[0].toInt(), toned[1].toInt(), toned[2].toInt(), hsl)
        hsl[1] = hsl[1].coerceAtMost(0.5f)
        hsl[2] = 0.84f
        return ColorUtils.HSLToColor(hsl)
    }
}
