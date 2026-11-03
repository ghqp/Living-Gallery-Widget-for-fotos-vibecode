package app.photowiget.data

import kotlin.math.max
import kotlin.math.min

/** Largest power-of-two sample size that keeps the longest side within [maxSide]. */
internal fun sampleFitting(width: Int, height: Int, maxSide: Int): Int {
    if (width <= 0 || height <= 0 || maxSide <= 0) return 1
    var sample = 1
    val longest = max(width, height).toLong()
    while (sample < 256 && longest / sample > maxSide) {
        sample *= 2
    }
    return sample
}

/**
 * Largest power-of-two sample size whose result still has a shortest side of at least [minSide]
 * and no more than [maxPixels] pixels.
 */
internal fun sampleForMinSide(width: Int, height: Int, minSide: Int, maxPixels: Long): Int {
    if (width <= 0 || height <= 0) return 1
    val shortest = min(width, height).toLong()
    var sample = 1
    while (sample < 256 && shortest / (sample * 2L) >= minSide) {
        sample *= 2
    }
    while (sample < 256 && (width.toLong() / sample) * (height.toLong() / sample) > maxPixels) {
        sample *= 2
    }
    return sample
}

/** Side of the square artwork for a widget cell: the shorter side of the larger orientation. */
internal fun artworkSideDp(minWidth: Int, maxWidth: Int, minHeight: Int, maxHeight: Int): Int {
    val portrait = min(minWidth, maxHeight)
    val landscape = min(maxWidth, minHeight)
    return max(portrait, landscape).coerceAtLeast(48)
}
