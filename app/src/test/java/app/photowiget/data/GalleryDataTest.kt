package app.photowiget.data

import androidx.datastore.core.CorruptionException
import app.photowiget.data.model.BorderTint
import app.photowiget.data.model.ColorTone
import app.photowiget.data.model.FrameShape
import app.photowiget.data.model.GalleryData
import app.photowiget.data.model.PendingPin
import app.photowiget.data.model.PhotoCrop
import app.photowiget.data.model.PhotoItem
import app.photowiget.data.model.Placement
import app.photowiget.data.model.RotationInterval
import app.photowiget.data.model.TapAction
import app.photowiget.data.model.WidgetBehavior
import app.photowiget.data.model.WidgetDesign
import app.photowiget.data.model.WidgetLook
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File

class GalleryDataTest {
    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun galleryRoundTrips() = runBlocking {
        val data = GalleryData(
            designs = listOf(
                WidgetDesign(
                    id = "w1",
                    name = "Trip",
                    photos = listOf(PhotoItem("a.jpg", PhotoCrop(2f, 0.3f, 0.6f)), PhotoItem("b.png")),
                    look = WidgetLook(FrameShape.Clover, 20, 4, BorderTint.Auto, ColorTone.Sepia),
                    behavior = WidgetBehavior(RotationInterval.Hours6, shuffle = true, tapAction = TapAction.OpenApp),
                    createdAt = 1,
                    updatedAt = 2,
                ),
            ),
            placements = listOf(Placement(appWidgetId = 7, designId = "w1", photoIndex = 1, shownAt = 3)),
            pendingPin = PendingPin("w1", "app.photowiget.widget.PhotoWidget3x2", 4),
        )
        val output = ByteArrayOutputStream()
        GalleryDataSerializer.writeTo(data, output)
        assertEquals(data, GalleryDataSerializer.readFrom(ByteArrayInputStream(output.toByteArray())))
    }

    @Test
    fun unknownValuesFallBackToDefaults() = runBlocking {
        val raw = """{"designs":[{"id":"x","name":"X","look":{"frame":"heart","tone":"lomo"},"future":1}]}"""
        val design = GalleryDataSerializer.readFrom(ByteArrayInputStream(raw.encodeToByteArray())).designs.single()
        assertEquals(FrameShape.Square, design.look.frame)
        assertEquals(ColorTone.Original, design.look.tone)
    }

    @Test(expected = CorruptionException::class)
    fun brokenFilesAreReportedAsCorrupt() {
        runBlocking { GalleryDataSerializer.readFrom(ByteArrayInputStream("{".encodeToByteArray())) }
    }

    @Test
    fun legacyWidgetsMoveIntoTheGallery() = runBlocking {
        val root = folder.root
        File(root, "widgets/old").mkdirs()
        File(root, "widgets/old/0.jpg").writeText("jpeg")
        File(root, "widgets/old/1.jpg").writeText("jpeg")
        File(root, "widgets/pending").mkdirs()
        File(root, "widgets/pending/0.png").writeText("png")
        File(root, "widgets.json").writeText(
            """
            [
              {"id":"pending","shape":"Rectangle","photoFileNames":["0.png"],"span":"Cells3x2"},
              {"id":"old","shape":"Square","photoFileNames":["0.jpg","1.jpg","gone.jpg"],"appWidgetId":12,"photoIndex":5}
            ]
            """.trimIndent(),
        )
        val files = PhotoFiles(root)
        val migration = LegacyWidgetsMigration(root, files, defaultName = { "New Widget" }, clock = { 1_000L })

        assertTrue(migration.shouldMigrate(GalleryData()))
        val migrated = migration.migrate(GalleryData())
        migration.cleanUp()

        assertEquals(listOf("pending", "old"), migrated.designs.map { it.id })
        assertEquals(listOf("0.jpg", "1.jpg"), migrated.designs[1].photos.map { it.file })
        assertEquals(FrameShape.Square, migrated.designs[1].look.frame)
        assertEquals(listOf(Placement(appWidgetId = 12, designId = "old", photoIndex = 1, shownAt = 1_000L)), migrated.placements)
        assertTrue(File(files.designDir("old"), "1.jpg").isFile)
        assertTrue(!File(root, "widgets.json").exists())
        assertTrue(!File(root, "widgets").exists())
    }
}
