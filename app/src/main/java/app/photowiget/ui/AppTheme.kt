package app.photowiget.ui

import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.photowiget.appContainer
import app.photowiget.data.AppSettings
import app.photowiget.data.ThemeMode
import app.photowiget.ui.theme.LivingGalleryTheme

/** Applies the user's theme settings and matching system bar icons to [activity]. */
@Composable
fun AppTheme(activity: ComponentActivity, content: @Composable () -> Unit) {
    val settings by activity.appContainer.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    val current: AppSettings = settings ?: return
    val dark = when (current.theme) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    DisposableEffect(dark) {
        activity.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
            navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
        )
        onDispose { }
    }
    LivingGalleryTheme(darkTheme = dark, dynamicColor = current.dynamicColor, content = content)
}

private val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
private val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
