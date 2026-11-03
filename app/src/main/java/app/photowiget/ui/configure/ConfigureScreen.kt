package app.photowiget.ui.configure

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.photowiget.AppContainer
import app.photowiget.LivingGalleryApp
import app.photowiget.R
import app.photowiget.data.model.PhotoCrop
import app.photowiget.ui.components.DesignCard
import app.photowiget.ui.components.DesignCardModel
import app.photowiget.ui.components.EmptyState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class PickerState(
    val loading: Boolean = true,
    val designs: List<DesignCardModel> = emptyList(),
    val currentDesignId: String? = null,
)

class ConfigureViewModel(container: AppContainer, appWidgetId: Int) : ViewModel() {
    val state: StateFlow<PickerState> = container.repository.data.map { data ->
        val placed = data.placements.mapTo(HashSet()) { it.designId }
        PickerState(
            loading = false,
            designs = data.designs.sortedByDescending { it.createdAt }.map { design ->
                val cover = design.photos.firstOrNull()
                DesignCardModel(
                    id = design.id,
                    name = design.name,
                    look = design.look,
                    cover = cover?.let { container.repository.photoFile(design, it) },
                    coverCrop = cover?.crop ?: PhotoCrop(),
                    photoCount = design.photos.size,
                    active = design.id in placed,
                )
            },
            currentDesignId = data.placements.firstOrNull { it.appWidgetId == appWidgetId }?.designId,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PickerState())

    companion object {
        fun factory(appWidgetId: Int): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as LivingGalleryApp
                ConfigureViewModel(app.container, appWidgetId)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickerRoute(
    appWidgetId: Int,
    onPick: (String) -> Unit,
    onCreate: () -> Unit,
    onClose: () -> Unit,
    viewModel: ConfigureViewModel = viewModel(factory = ConfigureViewModel.factory(appWidgetId)),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.configure_title)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.action_close))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        floatingActionButton = {
            if (state.designs.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onCreate,
                    icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                    text = { Text(stringResource(R.string.new_widget)) },
                )
            }
        },
    ) { padding ->
        when {
            state.loading -> Box(Modifier.fillMaxSize())
            state.designs.isEmpty() -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                EmptyState(
                    title = stringResource(R.string.empty_title),
                    body = stringResource(R.string.empty_body),
                    action = stringResource(R.string.empty_action),
                    onAction = onCreate,
                )
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 164.dp),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 4.dp,
                    bottom = padding.calculateBottomPadding() + 96.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(state.designs, key = { it.id }) { design ->
                    DesignCard(
                        model = design,
                        onClick = { onPick(design.id) },
                        selected = design.id == state.currentDesignId,
                    )
                }
            }
        }
    }
}
