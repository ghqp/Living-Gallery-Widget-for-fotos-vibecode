package app.photowiget.render

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot

class FrameCurvesTest {
    @Test
    fun curvesFillTheUnitSquare() {
        listOf(
            FrameCurves.superellipse(4f, 256),
            FrameCurves.lobed(9, 0.1f, (-PI / 2).toFloat(), 360),
            FrameCurves.lobed(4, 0.34f, (PI / 4).toFloat(), 360),
        ).forEach { points ->
            val xs = points.filterIndexed { i, _ -> i % 2 == 0 }
            val ys = points.filterIndexed { i, _ -> i % 2 == 1 }
            val spanX = xs.max() - xs.min()
            val spanY = ys.max() - ys.min()
            assertEquals(2f, maxOf(spanX, spanY), 1e-4f)
            assertTrue(xs.all { abs(it) <= 1.0001f })
            assertTrue(ys.all { abs(it) <= 1.0001f })
        }
    }

    @Test
    fun superellipseIsRounderThanASquareButFullerThanACircle() {
        val points = FrameCurves.superellipse(4f, 256)
        val diagonal = (0 until points.size / 2).maxOf { hypot(points[2 * it], points[2 * it + 1]) }
        assertTrue(diagonal > 1.05f)
        assertTrue(diagonal < hypot(1f, 1f))
    }

    @Test
    fun lobedCurveDipsBetweenLobes() {
        val points = FrameCurves.lobed(4, 0.34f, 0f, 360)
        val radii = (0 until points.size / 2).map { hypot(points[2 * it], points[2 * it + 1]) }
        assertTrue(radii.min() < radii.max() * 0.7f)
    }
}
