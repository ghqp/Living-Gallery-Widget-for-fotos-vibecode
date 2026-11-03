package app.photowiget.data

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import app.photowiget.data.model.GalleryData
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

internal val GalleryJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
}

object GalleryDataSerializer : Serializer<GalleryData> {
    override val defaultValue: GalleryData = GalleryData()

    override suspend fun readFrom(input: InputStream): GalleryData {
        val raw = input.readBytes().decodeToString()
        return try {
            GalleryJson.decodeFromString(GalleryData.serializer(), raw)
        } catch (error: SerializationException) {
            throw CorruptionException("Gallery data is unreadable", error)
        } catch (error: IllegalArgumentException) {
            throw CorruptionException("Gallery data is unreadable", error)
        }
    }

    override suspend fun writeTo(t: GalleryData, output: OutputStream) {
        output.write(GalleryJson.encodeToString(GalleryData.serializer(), t).encodeToByteArray())
    }
}
