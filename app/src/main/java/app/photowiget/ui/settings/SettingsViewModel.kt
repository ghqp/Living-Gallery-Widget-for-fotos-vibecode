package app.photowiget.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.photowiget.AppContainer
import app.photowiget.LivingGalleryApp
import app.photowiget.data.AppSettings
import app.photowiget.data.ThemeMode
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val settings: StateFlow<AppSettings?> = container.settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val versionName: String = container.versionName

    private val _refreshed = Channel<Unit>(Channel.CONFLATED)
    val refreshed: Flow<Unit> = _refreshed.receiveAsFlow()

    fun setTheme(theme: ThemeMode) {
        viewModelScope.launch { container.settings.setTheme(theme) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { container.settings.setDynamicColor(enabled) }
    }

    fun refreshWidgets() {
        container.scope.launch {
            container.widgets.reconcile()
            container.widgets.updateAll()
            _refreshed.send(Unit)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as LivingGalleryApp
                SettingsViewModel(app.container)
            }
        }
    }
}
