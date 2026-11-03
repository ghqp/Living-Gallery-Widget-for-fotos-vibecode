package app.photowiget.ui.editor

import android.net.Uri
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.toRoute
import app.photowiget.AppContainer
import app.photowiget.LivingGalleryApp
import app.photowiget.R
import app.photowiget.data.DraftPhoto
import app.photowiget.data.SaveRequest
import app.photowiget.data.model.PhotoCrop
import app.photowiget.data.model.WidgetBehavior
import app.photowiget.data.model.WidgetDesign
import app.photowiget.data.model.WidgetLook
import app.photowiget.ui.navigation.EditorDestination
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class EditorPhoto(val key: String, val file: File, val crop: PhotoCrop)

data class EditorState(
    val loading: Boolean = true,
    val photos: List<EditorPhoto> = emptyList(),
    val look: WidgetLook = WidgetLook(),
    val behavior: WidgetBehavior = WidgetBehavior(),
    val importing: Boolean = false,
    val saving: Boolean = false,
) {
    val canSave: Boolean get() = !loading && !importing && !saving && photos.isNotEmpty()
}

sealed interface EditorEvent {
    data class Saved(val designId: String, val created: Boolean) : EditorEvent
    data class PhotosAdded(val firstIndex: Int) : EditorEvent
    data class Message(@param:StringRes val text: Int, val arg: Int? = null) : EditorEvent
    data object Missing : EditorEvent
}

class EditorViewModel(
    savedStateHandle: SavedStateHandle,
    private val container: AppContainer,
) : ViewModel() {
    private val designId: String? = savedStateHandle.toRoute<EditorDestination>().designId
    val isNew: Boolean = designId == null

    var name by mutableStateOf("")
        private set

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    private val _events = Channel<EditorEvent>(Channel.BUFFERED)
    val events: Flow<EditorEvent> = _events.receiveAsFlow()

    private var draftDir: File? = null
    private var initial: Snapshot? = null

    init {
        viewModelScope.launch { load() }
    }

    fun onNameChange(value: String) {
        name = value.replace('\n', ' ').take(WidgetDesign.MAX_NAME_LENGTH)
    }

    fun updateLook(look: WidgetLook) {
        _state.update { it.copy(look = look) }
    }

    fun updateBehavior(behavior: WidgetBehavior) {
        _state.update { it.copy(behavior = behavior) }
    }

    fun setCrop(key: String, crop: PhotoCrop) {
        _state.update { state ->
            state.copy(photos = state.photos.map { if (it.key == key) it.copy(crop = crop) else it })
        }
    }

    fun removePhoto(key: String) {
        _state.update { state -> state.copy(photos = state.photos.filterNot { it.key == key }) }
    }

    fun movePhoto(from: Int, to: Int) {
        _state.update { state ->
            if (from !in state.photos.indices || to !in state.photos.indices) return@update state
            val photos = state.photos.toMutableList()
            photos.add(to, photos.removeAt(from))
            state.copy(photos = photos)
        }
    }

    fun addPhotos(uris: List<Uri>) {
        val current = _state.value
        if (uris.isEmpty() || current.importing || current.loading) return
        val room = WidgetDesign.MAX_PHOTOS - current.photos.size
        if (room <= 0) {
            _events.trySend(EditorEvent.Message(R.string.error_too_many, WidgetDesign.MAX_PHOTOS))
            return
        }
        val accepted = uris.distinct().take(room)
        _state.update { it.copy(importing = true) }
        viewModelScope.launch {
            val directory = draftDir ?: container.files.createDraftDir().also { draftDir = it }
            val imported = withContext(Dispatchers.IO) {
                val gate = Semaphore(IMPORT_PARALLELISM)
                accepted.map { uri ->
                    async { gate.withPermit { importOrNull(uri, directory) } }
                }.awaitAll()
            }
            val added = imported.filterNotNull().map { EditorPhoto(UUID.randomUUID().toString(), it, PhotoCrop()) }
            var firstIndex = 0
            _state.update { state ->
                firstIndex = state.photos.size
                state.copy(importing = false, photos = (state.photos + added).take(WidgetDesign.MAX_PHOTOS))
            }
            when {
                added.size < accepted.size -> _events.send(EditorEvent.Message(R.string.error_import))
                uris.size > room -> _events.send(EditorEvent.Message(R.string.error_too_many, WidgetDesign.MAX_PHOTOS))
            }
            if (added.isNotEmpty()) _events.send(EditorEvent.PhotosAdded(firstIndex))
        }
    }

    fun hasUnsavedChanges(): Boolean = initial?.let { it != snapshot() } ?: false

    fun save() {
        val current = _state.value
        if (!current.canSave) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val design = try {
                container.repository.saveDesign(
                    SaveRequest(
                        designId = designId,
                        name = name.trim().ifEmpty { defaultName() },
                        photos = current.photos.map { DraftPhoto(it.file, it.crop) },
                        look = current.look,
                        behavior = current.behavior,
                    ),
                )
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _state.update { it.copy(saving = false) }
                _events.send(EditorEvent.Message(R.string.error_save))
                return@launch
            }
            container.scope.launch { container.widgets.updateDesign(design.id) }
            initial = snapshot()
            _events.send(EditorEvent.Saved(design.id, created = isNew))
        }
    }

    override fun onCleared() {
        val directory = draftDir ?: return
        container.scope.launch(Dispatchers.IO) { directory.deleteRecursively() }
    }

    private suspend fun load() {
        val design = designId?.let { container.repository.design(it) }
        if (designId != null && design == null) {
            _events.send(EditorEvent.Missing)
            return
        }
        name = design?.name ?: defaultName()
        val photos = design?.let { saved ->
            saved.photos.map { item ->
                EditorPhoto(key = item.file, file = container.repository.photoFile(saved, item), crop = item.crop)
            }
        }
        _state.value = EditorState(
            loading = false,
            photos = photos.orEmpty(),
            look = design?.look ?: WidgetLook(),
            behavior = design?.behavior ?: WidgetBehavior(),
        )
        initial = snapshot()
    }

    private fun importOrNull(uri: Uri, directory: File): File? = try {
        container.importer.import(uri, directory)
    } catch (_: Exception) {
        null
    } catch (_: OutOfMemoryError) {
        null
    }

    private fun defaultName(): String = container.appContext.getString(R.string.default_widget_name)

    private fun snapshot(): Snapshot {
        val state = _state.value
        return Snapshot(
            name = name.trim(),
            photos = state.photos.map { it.key to it.crop },
            look = state.look,
            behavior = state.behavior,
        )
    }

    private data class Snapshot(
        val name: String,
        val photos: List<Pair<String, PhotoCrop>>,
        val look: WidgetLook,
        val behavior: WidgetBehavior,
    )

    companion object {
        private const val IMPORT_PARALLELISM = 3

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as LivingGalleryApp
                EditorViewModel(createSavedStateHandle(), app.container)
            }
        }
    }
}
