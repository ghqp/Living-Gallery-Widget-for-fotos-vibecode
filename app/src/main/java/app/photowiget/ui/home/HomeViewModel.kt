package app.photowiget.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.photowiget.AppContainer
import app.photowiget.LivingGalleryApp
import app.photowiget.R
import app.photowiget.data.HomeLayout
import app.photowiget.data.WidgetSpan
import app.photowiget.data.model.GalleryData
import app.photowiget.data.model.PhotoCrop
import app.photowiget.data.model.WidgetDesign
import app.photowiget.ui.components.DesignCardModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val loading: Boolean = true,
    val designs: List<DesignCardModel> = emptyList(),
    val layout: HomeLayout = HomeLayout.Grid,
) {
    val activeCount: Int get() = designs.count { it.active }
}

sealed interface HomeEvent {
    data object PinUnsupported : HomeEvent
    data object Duplicated : HomeEvent
    data object Deleted : HomeEvent
}

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(
        container.repository.data,
        container.settings.settings,
    ) { data, settings ->
        HomeUiState(
            loading = false,
            designs = data.toCards(),
            layout = settings.homeLayout,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = _events.receiveAsFlow()

    fun refreshPlacements() {
        viewModelScope.launch { container.widgets.reconcile() }
    }

    fun toggleLayout() {
        val next = if (state.value.layout == HomeLayout.Grid) HomeLayout.List else HomeLayout.Grid
        viewModelScope.launch { container.settings.setHomeLayout(next) }
    }

    fun duplicate(id: String) {
        viewModelScope.launch {
            val source = container.repository.design(id) ?: return@launch
            val name = container.appContext.getString(R.string.copy_name, source.name)
                .take(WidgetDesign.MAX_NAME_LENGTH)
            container.repository.duplicate(id, name)
            _events.send(HomeEvent.Duplicated)
        }
    }

    fun delete(id: String) {
        container.scope.launch {
            val orphaned = container.repository.delete(id)
            container.widgets.update(orphaned)
        }
        viewModelScope.launch { _events.send(HomeEvent.Deleted) }
    }

    fun pin(id: String, span: WidgetSpan) {
        viewModelScope.launch {
            val design = container.repository.design(id) ?: return@launch
            if (!container.widgets.requestPin(design, span)) {
                _events.send(HomeEvent.PinUnsupported)
            }
        }
    }

    private fun GalleryData.toCards(): List<DesignCardModel> {
        val active = placements.mapTo(HashSet()) { it.designId }
        return designs.sortedByDescending { it.createdAt }.map { design ->
            val cover = design.photos.firstOrNull()
            DesignCardModel(
                id = design.id,
                name = design.name,
                look = design.look,
                cover = cover?.let { container.repository.photoFile(design, it) },
                coverCrop = cover?.crop ?: PhotoCrop(),
                photoCount = design.photos.size,
                active = design.id in active,
            )
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as LivingGalleryApp
                HomeViewModel(app.container)
            }
        }
    }
}
