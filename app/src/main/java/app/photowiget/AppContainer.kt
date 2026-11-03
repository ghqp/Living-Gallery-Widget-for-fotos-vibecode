package app.photowiget

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import app.photowiget.data.GalleryDataSerializer
import app.photowiget.data.GalleryRepository
import app.photowiget.data.LegacyWidgetsMigration
import app.photowiget.data.PhotoFiles
import app.photowiget.data.PhotoImporter
import app.photowiget.data.Rotation
import app.photowiget.data.SettingsRepository
import app.photowiget.data.model.GalleryData
import app.photowiget.widget.RotationScheduler
import app.photowiget.widget.WidgetController
import app.photowiget.widget.WidgetRenderer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext

    /** Outlives screens; used for widget work and writes that must finish after a screen closes. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val files = PhotoFiles(appContext.filesDir)

    private val galleryStore: DataStore<GalleryData> = DataStoreFactory.create(
        serializer = GalleryDataSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler { GalleryData() },
        migrations = listOf(
            LegacyWidgetsMigration(
                filesDir = appContext.filesDir,
                files = files,
                defaultName = { appContext.getString(R.string.default_widget_name) },
            ),
        ),
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile = { appContext.dataStoreFile("gallery.json") },
    )

    val repository = GalleryRepository(galleryStore, files)
    val settings = SettingsRepository(appContext.settingsStore)
    val importer = PhotoImporter(appContext.contentResolver)
    val renderer = WidgetRenderer(appContext, files)
    val widgets = WidgetController(appContext, repository, renderer)
    private val scheduler = RotationScheduler(appContext)

    fun start() {
        scope.launch(Dispatchers.IO) { files.clearDrafts() }
        scope.launch {
            repository.data
                .map(Rotation::workPeriodMinutes)
                .distinctUntilChanged()
                .collect(scheduler::sync)
        }
        scope.launch {
            val version = versionCode()
            if (settings.renderedVersion() != version) {
                widgets.reconcile()
                widgets.updateAll()
                settings.setRenderedVersion(version)
            }
        }
    }

    val versionName: String get() = packageInfo().versionName.orEmpty()

    private fun versionCode(): Int {
        val info = packageInfo()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            info.versionCode
        }
    }

    private fun packageInfo(): PackageInfo =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.packageManager.getPackageInfo(appContext.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            appContext.packageManager.getPackageInfo(appContext.packageName, 0)
        }
}

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

val Context.appContainer: AppContainer
    get() = (applicationContext as LivingGalleryApp).container
