package app.photowiget.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.util.Log
import app.photowiget.AppContainer
import app.photowiget.appContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** Runs [block] off the main thread while keeping the broadcast alive until it finishes. */
internal fun BroadcastReceiver.runAsync(context: Context, block: suspend (AppContainer) -> Unit) {
    val pending = goAsync()
    val container = context.appContainer
    container.scope.launch {
        try {
            block(container)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e("LivingGallery", "Widget broadcast failed", error)
        } finally {
            pending.finish()
        }
    }
}
