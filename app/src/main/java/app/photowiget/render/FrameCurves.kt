package app.photowiget.render

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

/** Closed outlines as flat (x, y) arrays that span exactly [-1, 1] on their longer axis. */
internal object FrameCurves {

    fun superellipse(exponent: Float, segments: Int): FloatArray {
        val power = 2.0 / exponent
        val points = FloatArray(segments * 2)
        for (i in 0 until segments) {
            val t = 2.0 * PI * i / segments
            val c = cos(t)
            val s = sin(t)
            points[2 * i] = (sign(c) * abs(c).pow(power)).toFloat()
            points[2 * i + 1] = (sign(s) * abs(s).pow(power)).toFloat()
        }
        return normalize(points)
    }

    /** A circle whose radius dips by [depth] between [lobes] evenly spaced bulges. */
    fun lobed(lobes: Int, depth: Float, rotation: Float, segments: Int): FloatArray {
        val points = FloatArray(segments * 2)
        for (i in 0 until segments) {
            val t = 2.0 * PI * i / segments
            val radius = 1.0 - depth * (1.0 - cos(lobes * (t - rotation))) / 2.0
            points[2 * i] = (radius * cos(t)).toFloat()
            points[2 * i + 1] = (radius * sin(t)).toFloat()
        }
        return normalize(points)
    }

    /** Centers the bounding box on the origin and scales it uniformly to fit [-1, 1]. */
    fun normalize(points: FloatArray): FloatArray {
        var minX = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        for (i in points.indices step 2) {
            minX = minOf(minX, points[i])
            maxX = maxOf(maxX, points[i])
            minY = minOf(minY, points[i + 1])
            maxY = maxOf(maxY, points[i + 1])
        }
        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f
        val half = max(maxX - minX, maxY - minY) / 2f
        if (half <= 0f) return points
        return FloatArray(points.size) { i ->
            if (i % 2 == 0) (points[i] - centerX) / half else (points[i] - centerY) / half
        }
    }
}
