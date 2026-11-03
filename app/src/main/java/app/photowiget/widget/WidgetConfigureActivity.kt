package app.photowiget.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.photowiget.appContainer
import app.photowiget.ui.AppTheme
import app.photowiget.ui.configure.PickerRoute
import app.photowiget.ui.editor.EditorRoute
import app.photowiget.ui.navigation.EditorDestination
import app.photowiget.ui.navigation.PickerDestination
import app.photowiget.ui.navigation.backEnter
import app.photowiget.ui.navigation.backExit
import app.photowiget.ui.navigation.forwardEnter
import app.photowiget.ui.navigation.forwardExit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Lets the user choose or create the design for a widget added from the launcher's widget list. */
class WidgetConfigureActivity : ComponentActivity() {
    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var binding = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        appWidgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        setResult(RESULT_CANCELED, resultIntent())
        setContent {
            AppTheme(this) {
                val navController = rememberNavController()
                NavHost(
                    navController = navController,
                    startDestination = PickerDestination,
                    enterTransition = { forwardEnter() },
                    exitTransition = { forwardExit() },
                    popEnterTransition = { backEnter() },
                    popExitTransition = { backExit() },
                ) {
                    composable<PickerDestination> {
                        PickerRoute(
                            appWidgetId = appWidgetId,
                            onPick = ::bindAndFinish,
                            onCreate = { navController.navigate(EditorDestination()) },
                            onClose = ::finish,
                        )
                    }
                    composable<EditorDestination> {
                        EditorRoute(
                            onClose = { navController.popBackStack() },
                            onSaved = { designId, _ -> bindAndFinish(designId) },
                        )
                    }
                }
            }
        }
    }

    private fun bindAndFinish(designId: String) {
        if (binding) return
        binding = true
        val container = appContainer
        container.scope.launch {
            container.repository.bind(appWidgetId, designId)
            container.widgets.update(listOf(appWidgetId))
            withContext(Dispatchers.Main) {
                setResult(RESULT_OK, resultIntent())
                finish()
            }
        }
    }

    private fun resultIntent() = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
}
