package app.photowiget.data

import app.photowiget.data.model.GalleryData
import app.photowiget.data.model.RotationInterval
import kotlin.math.min
import kotlin.random.Random

object Rotation {
    private const val MINUTE_MS = 60_000L
    private const val MAX_SLACK_MS = 5 * MINUTE_MS

    /** WorkManager does not run periodic work more often than this. */
    const val MIN_PERIOD_MINUTES = 15L

    fun nextIndex(current: Int, count: Int, shuffle: Boolean, random: Random = Random.Default): Int {
        if (count < 2) return 0
        val from = current.mod(count)
        if (!shuffle) return (from + 1) % count
        val pick = random.nextInt(count - 1)
        return if (pick >= from) pick + 1 else pick
    }

    /** Periodic work drifts, so a photo is due slightly before its full interval has passed. */
    fun isDue(interval: RotationInterval, shownAt: Long, now: Long): Boolean {
        if (interval == RotationInterval.Off) return false
        val period = interval.minutes * MINUTE_MS
        val slack = min(MAX_SLACK_MS, period / 3)
        return now - shownAt >= period - slack
    }

    /** Shortest interval among placed widgets that have something to rotate, or null if none do. */
    fun workPeriodMinutes(data: GalleryData): Long? {
        val designs = data.designs.associateBy { it.id }
        return data.placements
            .mapNotNull { designs[it.designId] }
            .filter { it.photos.size > 1 && it.behavior.interval != RotationInterval.Off }
            .minOfOrNull { it.behavior.interval.minutes }
            ?.coerceAtLeast(MIN_PERIOD_MINUTES)
    }
}
