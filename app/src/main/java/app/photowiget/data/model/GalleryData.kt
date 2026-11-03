package app.photowiget.data.model

import kotlinx.serialization.Serializable

@Serializable
data class GalleryData(
    val designs: List<WidgetDesign> = emptyList(),
    val placements: List<Placement> = emptyList(),
    val pendingPin: PendingPin? = null,
)

/** A widget instance on the home screen showing [designId]. */
@Serializable
data class Placement(
    val appWidgetId: Int,
    val designId: String,
    val photoIndex: Int = 0,
    val shownAt: Long = 0L,
)

/** A pin request the launcher has not confirmed yet. */
@Serializable
data class PendingPin(
    val designId: String,
    val provider: String,
    val requestedAt: Long,
)
