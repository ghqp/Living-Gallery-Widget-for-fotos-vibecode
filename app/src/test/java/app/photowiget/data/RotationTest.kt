package app.photowiget.data

import app.photowiget.data.model.GalleryData
import app.photowiget.data.model.PhotoItem
import app.photowiget.data.model.Placement
import app.photowiget.data.model.RotationInterval
import app.photowiget.data.model.WidgetBehavior
import app.photowiget.data.model.WidgetDesign
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class RotationTest {
    @Test
    fun inOrderWrapsAround() {
        assertEquals(1, Rotation.nextIndex(0, 3, shuffle = false))
        assertEquals(0, Rotation.nextIndex(2, 3, shuffle = false))
        assertEquals(2, Rotation.nextIndex(7, 3, shuffle = false))
        assertEquals(0, Rotation.nextIndex(0, 1, shuffle = false))
    }

    @Test
    fun shuffleNeverRepeatsTheCurrentPhoto() {
        val random = Random(42)
        repeat(500) {
            val current = it % 5
            val next = Rotation.nextIndex(current, 5, shuffle = true, random = random)
            assertNotEquals(current, next)
            assertTrue(next in 0 until 5)
        }
    }

    @Test
    fun photosBecomeDueShortlyBeforeTheirInterval() {
        val minute = 60_000L
        assertFalse(Rotation.isDue(RotationInterval.Off, shownAt = 0, now = 100 * minute))
        assertFalse(Rotation.isDue(RotationInterval.Hour1, shownAt = 0, now = 50 * minute))
        assertTrue(Rotation.isDue(RotationInterval.Hour1, shownAt = 0, now = 56 * minute))
        assertFalse(Rotation.isDue(RotationInterval.Minutes15, shownAt = 0, now = 9 * minute))
        assertTrue(Rotation.isDue(RotationInterval.Minutes15, shownAt = 0, now = 10 * minute))
    }

    @Test
    fun workRunsAtTheShortestIntervalOfPlacedWidgets() {
        val hourly = design("a", RotationInterval.Hour1, photos = 3)
        val quarter = design("b", RotationInterval.Minutes15, photos = 2)
        val single = design("c", RotationInterval.Minutes15, photos = 1)
        val unplaced = design("d", RotationInterval.Minutes15, photos = 4)
        val data = GalleryData(
            designs = listOf(hourly, quarter, single, unplaced),
            placements = listOf(Placement(1, "a"), Placement(2, "c")),
        )
        assertEquals(60L, Rotation.workPeriodMinutes(data))
        assertEquals(15L, Rotation.workPeriodMinutes(data.copy(placements = data.placements + Placement(3, "b"))))
        assertNull(Rotation.workPeriodMinutes(data.copy(placements = listOf(Placement(2, "c")))))
    }

    private fun design(id: String, interval: RotationInterval, photos: Int) = WidgetDesign(
        id = id,
        name = id,
        photos = List(photos) { PhotoItem("$it.jpg") },
        behavior = WidgetBehavior(interval = interval),
    )
}
