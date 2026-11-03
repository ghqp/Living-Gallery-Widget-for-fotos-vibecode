package app.photowiget.data

import androidx.datastore.core.DataMigration
import app.photowiget.data.model.GalleryData
import app.photowiget.data.model.PhotoItem
import app.photowiget.data.model.Placement
import app.photowiget.data.model.WidgetDesign
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

/** Moves widgets saved by version 0.x (`widgets.json` + `widgets/<id>/`) into the gallery store. */
class LegacyWidgetsMigration(
    private val filesDir: File,
    private val files: PhotoFiles,
    private val defaultName: () -> String,
    private val clock: () -> Long = System::currentTimeMillis,
) : DataMigration<GalleryData> {
    private val legacyIndex = File(filesDir, "widgets.json")

    override suspend fun shouldMigrate(currentData: GalleryData): Boolean = legacyIndex.exists()

    override suspend fun migrate(currentData: GalleryData): GalleryData {
        val legacy = decodeLegacy(legacyIndex.readText())
        val now = clock()
        val knownIds = currentData.designs.mapTo(HashSet()) { it.id }
        val designs = mutableListOf<WidgetDesign>()
        val placements = mutableListOf<Placement>()
        legacy.forEachIndexed { order, old ->
            if (old.id in knownIds) return@forEachIndexed
            val target = files.designDir(old.id)
            val source = File(filesDir, "widgets/${old.id}")
            if (source.isDirectory && !target.exists()) {
                target.parentFile?.mkdirs()
                if (!source.renameTo(target)) {
                    source.copyRecursively(target, overwrite = true)
                }
            }
            val photos = old.photoFileNames
                .filter { File(target, it).isFile }
                .map { PhotoItem(file = it) }
            if (photos.isEmpty()) return@forEachIndexed
            designs += WidgetDesign(
                id = old.id,
                name = defaultName(),
                photos = photos,
                createdAt = now - order,
                updatedAt = now,
            )
            old.appWidgetId?.let { appWidgetId ->
                placements += Placement(
                    appWidgetId = appWidgetId,
                    designId = old.id,
                    photoIndex = old.photoIndex.coerceIn(0, photos.lastIndex),
                    shownAt = now,
                )
            }
        }
        val claimed = placements.mapTo(HashSet()) { it.appWidgetId }
        return currentData.copy(
            designs = currentData.designs + designs,
            placements = currentData.placements.filterNot { it.appWidgetId in claimed } + placements,
        )
    }

    override suspend fun cleanUp() {
        legacyIndex.delete()
        File(filesDir, "widgets.json.tmp").delete()
        File(filesDir, "widgets").deleteRecursively()
        File(filesDir, "staging").deleteRecursively()
    }

    @Serializable
    internal data class LegacyWidget(
        val id: String,
        val photoFileNames: List<String> = emptyList(),
        val appWidgetId: Int? = null,
        val photoIndex: Int = 0,
    )

    internal companion object {
        fun decodeLegacy(raw: String): List<LegacyWidget> {
            if (raw.isBlank()) return emptyList()
            return try {
                GalleryJson.decodeFromString(ListSerializer(LegacyWidget.serializer()), raw)
            } catch (_: SerializationException) {
                emptyList()
            } catch (_: IllegalArgumentException) {
                emptyList()
            }
        }
    }
}
