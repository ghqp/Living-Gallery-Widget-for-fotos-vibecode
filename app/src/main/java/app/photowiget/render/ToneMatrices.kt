package app.photowiget.render

import android.graphics.ColorFilter
import android.graphics.ColorMatrixColorFilter
import app.photowiget.data.model.ColorTone
import java.util.concurrent.ConcurrentHashMap

/** 4×5 color matrices in the [android.graphics.ColorMatrix] layout, offsets in 0..255. */
object ToneMatrices {
    val Identity = floatArrayOf(
        1f, 0f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f, 0f,
        0f, 0f, 1f, 0f, 0f,
        0f, 0f, 0f, 1f, 0f,
    )

    private const val LUMA_R = 0.2126f
    private const val LUMA_G = 0.7152f
    private const val LUMA_B = 0.0722f

    fun of(tone: ColorTone): FloatArray = when (tone) {
        ColorTone.Original -> Identity
        ColorTone.Mono -> saturation(0f)
        ColorTone.Vivid -> concat(contrast(1.08f), saturation(1.45f))
        ColorTone.Dramatic -> concat(contrast(1.35f, brightness = -12f), saturation(0.85f))
        ColorTone.Warm -> concat(channels(1.08f, 1.01f, 0.86f, 10f, 4f, 0f), saturation(1.08f))
        ColorTone.Cool -> concat(channels(0.9f, 1f, 1.1f, 0f, 4f, 12f), saturation(0.95f))
        ColorTone.Sepia -> floatArrayOf(
            0.393f, 0.769f, 0.189f, 0f, 0f,
            0.349f, 0.686f, 0.168f, 0f, 0f,
            0.272f, 0.534f, 0.131f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )
        ColorTone.Fade -> concat(contrast(0.82f, brightness = 18f), saturation(0.7f))
        ColorTone.Noir -> concat(contrast(1.5f, brightness = -6f), saturation(0f))
    }.copyOf()

    fun saturation(amount: Float): FloatArray {
        val inverse = 1f - amount
        val r = LUMA_R * inverse
        val g = LUMA_G * inverse
        val b = LUMA_B * inverse
        return floatArrayOf(
            r + amount, g, b, 0f, 0f,
            r, g + amount, b, 0f, 0f,
            r, g, b + amount, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )
    }

    fun contrast(amount: Float, brightness: Float = 0f): FloatArray {
        val offset = 128f * (1f - amount) + brightness
        return floatArrayOf(
            amount, 0f, 0f, 0f, offset,
            0f, amount, 0f, 0f, offset,
            0f, 0f, amount, 0f, offset,
            0f, 0f, 0f, 1f, 0f,
        )
    }

    fun channels(r: Float, g: Float, b: Float, offsetR: Float, offsetG: Float, offsetB: Float) = floatArrayOf(
        r, 0f, 0f, 0f, offsetR,
        0f, g, 0f, 0f, offsetG,
        0f, 0f, b, 0f, offsetB,
        0f, 0f, 0f, 1f, 0f,
    )

    /** The matrix that applies [second] first and then [first]. */
    fun concat(first: FloatArray, second: FloatArray): FloatArray {
        val out = FloatArray(20)
        for (row in 0 until 4) {
            for (column in 0 until 5) {
                var value = if (column == 4) first[row * 5 + 4] else 0f
                for (k in 0 until 4) {
                    value += first[row * 5 + k] * second[k * 5 + column]
                }
                out[row * 5 + column] = value
            }
        }
        return out
    }

    /** Applies [matrix] to one opaque RGB color, with channels in 0..255. */
    fun apply(matrix: FloatArray, r: Float, g: Float, b: Float): FloatArray = FloatArray(3) { row ->
        (matrix[row * 5] * r + matrix[row * 5 + 1] * g + matrix[row * 5 + 2] * b + matrix[row * 5 + 3] * 255f +
            matrix[row * 5 + 4]).coerceIn(0f, 255f)
    }
}

object ToneFilters {
    private val cache = ConcurrentHashMap<ColorTone, ColorFilter>()

    fun filter(tone: ColorTone): ColorFilter? {
        if (tone == ColorTone.Original) return null
        return cache.getOrPut(tone) { ColorMatrixColorFilter(ToneMatrices.of(tone)) }
    }
}
