package app.photowiget.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object HomeDestination

@Serializable
data class EditorDestination(val designId: String? = null)

@Serializable
data object SettingsDestination

@Serializable
data object PickerDestination
