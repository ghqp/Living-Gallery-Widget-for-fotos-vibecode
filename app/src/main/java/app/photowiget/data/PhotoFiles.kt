package app.photowiget.data

import app.photowiget.data.model.PhotoItem
import java.io.File
import java.util.UUID

/** Photo files owned by the app: `designs/<id>/` for saved widgets, `drafts/` for unsaved edits. */
class PhotoFiles(private val root: File) {
    private val draftsRoot = File(root, "drafts")

    fun designDir(designId: String): File = File(root, "designs/$designId")

    fun photoFile(designId: String, item: PhotoItem): File = File(designDir(designId), item.file)

    fun createDraftDir(): File = File(draftsRoot, UUID.randomUUID().toString()).apply { mkdirs() }

    fun clearDrafts() {
        draftsRoot.deleteRecursively()
    }

    /** Moves [file] into the design folder unless it is already there and returns its stored name. */
    fun adopt(designId: String, file: File): String {
        val directory = designDir(designId)
        if (file.parentFile == directory) return file.name
        directory.mkdirs()
        val extension = file.extension.lowercase().takeIf { it in STORED_EXTENSIONS } ?: "jpg"
        val target = File(directory, "${UUID.randomUUID()}.$extension")
        if (!file.renameTo(target)) {
            file.copyTo(target, overwrite = true)
            file.delete()
        }
        return target.name
    }

    fun copyDesign(fromId: String, toId: String) {
        val source = designDir(fromId)
        if (source.isDirectory) source.copyRecursively(designDir(toId), overwrite = true)
    }

    fun deleteDesign(designId: String) {
        designDir(designId).deleteRecursively()
    }

    /** Deletes files of [designId] that are no longer referenced. */
    fun prune(designId: String, keep: Set<String>) {
        designDir(designId).listFiles()?.forEach { file ->
            if (file.name !in keep) file.delete()
        }
    }

    private companion object {
        val STORED_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp")
    }
}
