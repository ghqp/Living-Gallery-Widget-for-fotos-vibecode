package app.photowiget.render

import app.photowiget.data.model.ColorTone
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToneMatricesTest {
    @Test
    fun originalLeavesColorsUntouched() {
        assertArrayEquals(floatArrayOf(10f, 120f, 240f), ToneMatrices.apply(ToneMatrices.of(ColorTone.Original), 10f, 120f, 240f), 1e-3f)
    }

    @Test
    fun monoAndNoirProduceGray() {
        listOf(ColorTone.Mono, ColorTone.Noir).forEach { tone ->
            val (r, g, b) = ToneMatrices.apply(ToneMatrices.of(tone), 200f, 40f, 90f).toList()
            assertEquals(r, g, 0.5f)
            assertEquals(g, b, 0.5f)
        }
    }

    @Test
    fun monoKeepsPerceivedBrightness() {
        val gray = ToneMatrices.apply(ToneMatrices.of(ColorTone.Mono), 255f, 255f, 255f)
        assertArrayEquals(floatArrayOf(255f, 255f, 255f), gray, 0.5f)
    }

    @Test
    fun concatAppliesTheSecondMatrixFirst() {
        val double = ToneMatrices.channels(2f, 2f, 2f, 0f, 0f, 0f)
        val shift = ToneMatrices.channels(1f, 1f, 1f, 10f, 10f, 10f)
        val shiftThenDouble = ToneMatrices.concat(double, shift)
        assertArrayEquals(floatArrayOf(40f, 40f, 40f), ToneMatrices.apply(shiftThenDouble, 10f, 10f, 10f), 1e-3f)
    }

    @Test
    fun warmAndCoolPullInOppositeDirections() {
        val warm = ToneMatrices.apply(ToneMatrices.of(ColorTone.Warm), 128f, 128f, 128f)
        val cool = ToneMatrices.apply(ToneMatrices.of(ColorTone.Cool), 128f, 128f, 128f)
        assertTrue(warm[0] > warm[2])
        assertTrue(cool[2] > cool[0])
    }

    @Test
    fun everyToneIsAFreshCopy() {
        val first = ToneMatrices.of(ColorTone.Vivid)
        first[0] = 99f
        assertTrue(ToneMatrices.of(ColorTone.Vivid)[0] != 99f)
    }
}
