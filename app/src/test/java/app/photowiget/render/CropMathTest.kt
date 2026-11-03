package app.photowiget.render

import app.photowiget.data.model.PhotoCrop
import org.junit.Assert.assertEquals
import org.junit.Test

class CropMathTest {
    @Test
    fun defaultCropIsTheLargestCenteredSquare() {
        assertEquals(CropWindow(500f, 0f, 3000f), CropMath.window(4000, 3000, PhotoCrop()))
        assertEquals(CropWindow(0f, 500f, 3000f), CropMath.window(3000, 4000, PhotoCrop()))
    }

    @Test
    fun zoomShrinksTheWindowAroundTheCenter() {
        assertEquals(CropWindow(1250f, 750f, 1500f), CropMath.window(4000, 3000, PhotoCrop(zoom = 2f)))
    }

    @Test
    fun windowNeverLeavesThePhoto() {
        val window = CropMath.window(4000, 3000, PhotoCrop(zoom = 2f, centerX = 0f, centerY = 1f))
        assertEquals(CropWindow(0f, 1500f, 1500f), window)
    }

    @Test
    fun clampMovesTheCenterBackInside() {
        val clamped = CropMath.clamp(PhotoCrop(zoom = 1f, centerX = 0f, centerY = 0f), 4000, 3000)
        assertEquals(0.375f, clamped.centerX, 1e-4f)
        assertEquals(0.5f, clamped.centerY, 1e-4f)
    }

    @Test
    fun draggingRightRevealsTheLeftOfThePhoto() {
        val moved = CropMath.transform(PhotoCrop(), 4000, 3000, viewport = 300f, dx = 30f, dy = 0f, zoomBy = 1f)
        assertEquals((2000f - 300f) / 4000f, moved.centerX, 1e-4f)
        assertEquals(0.5f, moved.centerY, 1e-4f)
    }

    @Test
    fun zoomIsLimited() {
        val zoomedIn = CropMath.transform(PhotoCrop(), 4000, 3000, 300f, 0f, 0f, zoomBy = 100f)
        val zoomedOut = CropMath.transform(PhotoCrop(zoom = 2f), 4000, 3000, 300f, 0f, 0f, zoomBy = 0.01f)
        assertEquals(CropMath.MAX_ZOOM, zoomedIn.zoom, 1e-4f)
        assertEquals(1f, zoomedOut.zoom, 1e-4f)
    }
}
