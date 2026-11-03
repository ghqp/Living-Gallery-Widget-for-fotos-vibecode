package app.photowiget.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class ThemeMode { System, Light, Dark }

enum class HomeLayout { Grid, List }

data class AppSettings(
    val theme: ThemeMode = ThemeMode.Dark,
    val dynamicColor: Boolean = false,
    val homeLayout: HomeLayout = HomeLayout.Grid,
)

class SettingsRepository(private val store: DataStore<Preferences>) {

    val settings: Flow<AppSettings> = store.data.map { prefs ->
        AppSettings(
            theme = enumValueOrNull<ThemeMode>(prefs[THEME]) ?: ThemeMode.Dark,
            dynamicColor = prefs[DYNAMIC_COLOR] ?: false,
            homeLayout = enumValueOrNull<HomeLayout>(prefs[HOME_LAYOUT]) ?: HomeLayout.Grid,
        )
    }

    suspend fun setTheme(theme: ThemeMode) {
        store.edit { it[THEME] = theme.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        store.edit { it[DYNAMIC_COLOR] = enabled }
    }

    suspend fun setHomeLayout(layout: HomeLayout) {
        store.edit { it[HOME_LAYOUT] = layout.name }
    }

    suspend fun renderedVersion(): Int = store.data.first()[RENDERED_VERSION] ?: 0

    suspend fun setRenderedVersion(version: Int) {
        store.edit { it[RENDERED_VERSION] = version }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val HOME_LAYOUT = stringPreferencesKey("home_layout")
        val RENDERED_VERSION = intPreferencesKey("rendered_version")
    }
}

private inline fun <reified T : Enum<T>> enumValueOrNull(name: String?): T? =
    enumValues<T>().firstOrNull { it.name == name }
