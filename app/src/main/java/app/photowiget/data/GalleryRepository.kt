package app.photowiget.data

import androidx.datastore.core.DataStore
import app.photowiget.data.model.GalleryData
import app.photowiget.data.model.PendingPin
import app.photowiget.data.model.PhotoCrop
import app.photowiget.data.model.PhotoItem
import app.photowiget.data.model.Placement
import app.photowiget.data.model.WidgetBehavior
import app.photowiget.data.model.WidgetDesign
import app.photowiget.data.model.WidgetLook
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import kotlin.random.Random

data class DraftPhoto(val file: File, val crop: PhotoCrop)

data class SaveRequest(
    val designId: String?,
    val name: String,
    val photos: List<DraftPhoto>,
    val look: WidgetLook,
    val behavior: WidgetBehavior,
)

class GalleryRepository(
    private val store: DataStore<GalleryData>,
    private val files: PhotoFiles,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    val data: Flow<GalleryData> = store.data

    suspend fun snapshot(): GalleryData = store.data.first()

    suspend fun design(id: String): WidgetDesign? = snapshot().designs.firstOrNull { it.id == id }

    fun photoFile(design: WidgetDesign, item: PhotoItem): File = files.photoFile(design.id, item)

    suspend fun saveDesign(request: SaveRequest): WidgetDesign = withContext(Dispatchers.IO) {
        val id = request.designId ?: UUID.randomUUID().toString()
        val photos = request.photos.map { PhotoItem(file = files.adopt(id, it.file), crop = it.crop) }
        val now = clock()
        var saved: WidgetDesign? = null
        store.updateData { data ->
            val existing = data.designs.firstOrNull { it.id == id }
            val design = WidgetDesign(
                id = id,
                name = request.name,
                photos = photos,
                look = request.look,
                behavior = request.behavior,
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            )
            saved = design
            val designs = if (existing == null) {
                data.designs + design
            } else {
                data.designs.map { if (it.id == id) design else it }
            }
            data.copy(designs = designs)
        }
        files.prune(id, photos.mapTo(HashSet()) { it.file })
        checkNotNull(saved)
    }

    suspend fun duplicate(id: String, name: String): WidgetDesign? = withContext(Dispatchers.IO) {
        val source = design(id) ?: return@withContext null
        val copyId = UUID.randomUUID().toString()
        files.copyDesign(source.id, copyId)
        val now = clock()
        val copy = source.copy(id = copyId, name = name, createdAt = now, updatedAt = now)
        store.updateData { it.copy(designs = it.designs + copy) }
        copy
    }

    /** Deletes the design and returns the widget ids that were showing it. */
    suspend fun delete(id: String): List<Int> = withContext(Dispatchers.IO) {
        var orphaned = emptyList<Int>()
        store.updateData { data ->
            orphaned = data.placements.filter { it.designId == id }.map { it.appWidgetId }
            data.copy(
                designs = data.designs.filterNot { it.id == id },
                placements = data.placements.filterNot { it.designId == id },
                pendingPin = data.pendingPin?.takeUnless { it.designId == id },
            )
        }
        files.deleteDesign(id)
        orphaned
    }

    suspend fun bind(appWidgetId: Int, designId: String) {
        val now = clock()
        store.updateData { data ->
            if (data.designs.none { it.id == designId }) return@updateData data
            data.copy(
                placements = data.placements.filterNot { it.appWidgetId == appWidgetId } +
                    Placement(appWidgetId = appWidgetId, designId = designId, shownAt = now),
                pendingPin = data.pendingPin?.takeUnless { it.designId == designId },
            )
        }
    }

    suspend fun markPendingPin(designId: String, provider: String) {
        val now = clock()
        store.updateData { it.copy(pendingPin = PendingPin(designId, provider, now)) }
    }

    /**
     * Some launchers never deliver the pin callback. A widget of the requested size that shows up
     * shortly after the request is taken to be the one that was pinned.
     */
    suspend fun claimPendingPin(appWidgetId: Int, provider: String): Boolean {
        val now = clock()
        var claimed = false
        store.updateData { data ->
            val pin = data.pendingPin ?: return@updateData data
            val fresh = now - pin.requestedAt in 0..PIN_CLAIM_WINDOW_MS
            val unbound = data.placements.none { it.appWidgetId == appWidgetId }
            val designExists = data.designs.any { it.id == pin.designId }
            if (!fresh || !unbound || !designExists || pin.provider != provider) return@updateData data
            claimed = true
            data.copy(
                placements = data.placements + Placement(appWidgetId, pin.designId, shownAt = now),
                pendingPin = null,
            )
        }
        return claimed
    }

    suspend fun unbind(appWidgetIds: Collection<Int>) {
        val ids = appWidgetIds.toSet()
        store.updateData { data ->
            if (data.placements.none { it.appWidgetId in ids }) return@updateData data
            data.copy(placements = data.placements.filterNot { it.appWidgetId in ids })
        }
    }

    suspend fun retainPlacements(liveIds: Set<Int>) {
        store.updateData { data ->
            if (data.placements.all { it.appWidgetId in liveIds }) return@updateData data
            data.copy(placements = data.placements.filter { it.appWidgetId in liveIds })
        }
    }

    suspend fun advance(appWidgetId: Int, random: Random = Random.Default) {
        val now = clock()
        store.updateData { data ->
            val placement = data.placements.firstOrNull { it.appWidgetId == appWidgetId }
                ?: return@updateData data
            val design = data.designs.firstOrNull { it.id == placement.designId }
                ?: return@updateData data
            val next = placement.copy(
                photoIndex = Rotation.nextIndex(
                    placement.photoIndex,
                    design.photos.size,
                    design.behavior.shuffle,
                    random,
                ),
                shownAt = now,
            )
            data.copy(placements = data.placements.map { if (it.appWidgetId == appWidgetId) next else it })
        }
    }

    /** Advances every widget whose rotation interval has elapsed and returns their ids. */
    suspend fun advanceDue(random: Random = Random.Default): List<Int> {
        val now = clock()
        val changed = mutableListOf<Int>()
        store.updateData { data ->
            changed.clear()
            val designs = data.designs.associateBy { it.id }
            val placements = data.placements.map { placement ->
                val design = designs[placement.designId] ?: return@map placement
                val due = design.photos.size > 1 &&
                    Rotation.isDue(design.behavior.interval, placement.shownAt, now)
                if (!due) return@map placement
                changed += placement.appWidgetId
                placement.copy(
                    photoIndex = Rotation.nextIndex(
                        placement.photoIndex,
                        design.photos.size,
                        design.behavior.shuffle,
                        random,
                    ),
                    shownAt = now,
                )
            }
            if (changed.isEmpty()) data else data.copy(placements = placements)
        }
        return changed.toList()
    }

    private companion object {
        const val PIN_CLAIM_WINDOW_MS = 5 * 60_000L
    }
}
