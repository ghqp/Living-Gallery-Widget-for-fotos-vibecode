package app.photowiget.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PhotoMetricsTest {
    @Test
    fun sampleFittingKeepsOrdinaryPhotosWholeAndShrinksHugeOnes() {
        assertEquals(1, sampleFitting(4032, 3024, 4096))
        assertEquals(2, sampleFitting(8000, 6000, 4096))
        assertEquals(4, sampleFitting(12000, 9000, 4096))
    }

    @Test
    fun sampleForMinSideNeverDropsBelowTheRequestedSide() {
        assertEquals(1, sampleForMinSide(4032, 3024, 2000, Long.MAX_VALUE))
        assertEquals(2, sampleForMinSide(4032, 3024, 1000, Long.MAX_VALUE))
        assertEquals(4, sampleForMinSide(4032, 3024, 600, Long.MAX_VALUE))
        assertEquals(1, sampleForMinSide(400, 300, 1000, Long.MAX_VALUE))
    }

    @Test
    fun sampleForMinSideRespectsThePixelBudget() {
        assertEquals(2, sampleForMinSide(4000, 3000, 100_000, 4_000_000))
        assertEquals(4, sampleForMinSide(8000, 6000, 100_000, 4_000_000))
    }

    @Test
    fun artworkSideIsTheShorterSideOfTheLargerOrientation() {
        assertEquals(180, artworkSideDp(minWidth = 180, maxWidth = 250, minHeight = 110, maxHeight = 180))
        assertEquals(110, artworkSideDp(minWidth = 110, maxWidth = 110, minHeight = 110, maxHeight = 110))
        assertEquals(48, artworkSideDp(minWidth = 10, maxWidth = 10, minHeight = 10, maxHeight = 10))
    }

    @Test
    fun widgetSpansUseTheStandardCellFormula() {
        assertEquals(110, WidgetSpan.Cells2x2.minWidthDp)
        assertEquals(110, WidgetSpan.Cells2x2.minHeightDp)
        assertEquals(180, WidgetSpan.Cells3x2.minWidthDp)
        assertEquals(110, WidgetSpan.Cells3x2.minHeightDp)
        assertEquals(250, WidgetSpan.Cells4x3.minWidthDp)
        assertEquals(180, WidgetSpan.Cells4x3.minHeightDp)
        assertEquals("3×2", WidgetSpan.Cells3x2.label)
    }
}
