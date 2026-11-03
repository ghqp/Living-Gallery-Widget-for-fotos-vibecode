package app.photowiget.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import kotlin.math.max

/** Copies picked photos into app storage, upright and at most [STORED_MAX_SIDE] pixels long. */
class PhotoImporter(private val resolver: ContentResolver) {

    fun import(uri: Uri, directory: File): File {
        directory.mkdirs()
        val baseName = UUID.randomUUID().toString()
        val bounds = readBounds(uri) ?: throw IOException("Unreadable photo")
        val orientation = readOrientation(uri)
        val upright = orientation == ExifInterface.ORIENTATION_NORMAL ||
            orientation == ExifInterface.ORIENTATION_UNDEFINED
        val extension = passthroughExtension(uri)
        if (upright && extension != null && max(bounds.first, bounds.second) <= STORED_MAX_SIDE) {
            val destination = File(directory, "$baseName.$extension")
            val copied = resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destination).use { output -> input.copyTo(output) }
            }
            if (copied == null || destination.length() <= 0L) {
                destination.delete()
                throw IOException("Unreadable photo")
            }
            return destination
        }
        val bitmap = decodeUpright(uri, bounds, orientation) ?: throw IOException("Unreadable photo")
        val destination = File(directory, "$baseName.jpg")
        try {
            FileOutputStream(destination).use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)) {
                    throw IOException("Could not encode photo")
                }
            }
        } catch (error: IOException) {
            destination.delete()
            throw error
        } finally {
            bitmap.recycle()
        }
        return destination
    }

    private fun readBounds(uri: Uri): Pair<Int, Int>? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        if (options.outWidth <= 0 || options.outHeight <= 0) return null
        return options.outWidth to options.outHeight
    }

    private fun decodeUpright(uri: Uri, bounds: Pair<Int, Int>, orientation: Int): Bitmap? {
        val decoded = try {
            decode(uri, bounds, STORED_MAX_SIDE)
        } catch (_: OutOfMemoryError) {
            decode(uri, bounds, FALLBACK_MAX_SIDE)
        } ?: return null
        return applyOrientation(decoded, orientation)
    }

    private fun decode(uri: Uri, bounds: Pair<Int, Int>, maxSide: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleFitting(bounds.first, bounds.second, maxSide)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return null
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxSide) return bitmap
        val scale = maxSide.toFloat() / longest
        val scaled = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt().coerceAtLeast(1),
            (bitmap.height * scale).toInt().coerceAtLeast(1),
            true,
        )
        if (scaled != bitmap) bitmap.recycle()
        return scaled
    }

    private fun passthroughExtension(uri: Uri): String? = when (resolver.getType(uri)?.lowercase()) {
        "image/jpeg", "image/jpg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        else -> null
    }

    private fun readOrientation(uri: Uri): Int = try {
        resolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
    } catch (_: IOException) {
        ExifInterface.ORIENTATION_NORMAL
    }

    private fun applyOrientation(bitmap: Bitmap, orientation: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }

    private companion object {
        const val STORED_MAX_SIDE = 4096
        const val FALLBACK_MAX_SIDE = 2048
        const val JPEG_QUALITY = 97
    }
}

object PhotoDecoder {
    const val MAX_PIXELS = 8_388_608L

    /** Decodes [file] so that its shorter side is at least [minSide] pixels where the source allows. */
    fun decode(file: File, minSide: Int, maxPixels: Long = MAX_PIXELS): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleForMinSide(bounds.outWidth, bounds.outHeight, minSide, maxPixels)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return try {
            BitmapFactory.decodeFile(file.path, options)
        } catch (_: OutOfMemoryError) {
            options.inSampleSize *= 2
            BitmapFactory.decodeFile(file.path, options)
        }
    }
}
