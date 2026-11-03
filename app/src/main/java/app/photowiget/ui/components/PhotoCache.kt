package app.photowiget.ui.components

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.photowiget.data.PhotoDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Decoded photos for the UI, keyed by file and size bucket. Stored photo files never change. */
object PhotoCache {
    private val buckets = intArrayOf(192, 384, 720, 1080, 1600)
    private const val MAX_PIXELS = 6_000_000L

    private val memory = object : LruCache<String, Bitmap>(cacheBytes()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }

    private val decoder = Dispatchers.IO.limitedParallelism(3)

    fun bucket(px: Int): Int = buckets.firstOrNull { px <= it } ?: buckets.last()

    fun peek(file: File, bucket: Int): Bitmap? = memory.get(key(file, bucket))

    suspend fun load(file: File, bucket: Int): Bitmap? = withContext(decoder) {
        memory.get(key(file, bucket)) ?: PhotoDecoder.decode(file, bucket, MAX_PIXELS)?.also { bitmap ->
            bitmap.setHasMipMap(true)
            bitmap.prepareToDraw()
            memory.put(key(file, bucket), bitmap)
        }
    }

    private fun key(file: File, bucket: Int) = "${file.path}@$bucket"

    private fun cacheBytes(): Int = (Runtime.getRuntime().maxMemory() / 6).coerceAtMost(96L * 1024 * 1024).toInt()
}

/**
 * The photo in [file] decoded for display at about [sizePx]. While a new file loads, the previous
 * photo stays on screen instead of flashing a placeholder.
 */
@Composable
fun rememberPhoto(file: File?, sizePx: Int): Bitmap? {
    if (file == null || sizePx <= 0) return null
    val bucket = PhotoCache.bucket(sizePx)
    var loaded by remember { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(file, bucket) {
        loaded = PhotoCache.load(file, bucket)
    }
    return PhotoCache.peek(file, bucket) ?: loaded
}
