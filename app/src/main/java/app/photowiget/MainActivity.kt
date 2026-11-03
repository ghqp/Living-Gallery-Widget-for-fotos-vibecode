package app.photowiget

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.compose.rememberNavController
import app.photowiget.ui.AppTheme
import app.photowiget.ui.navigation.AppNavHost

class MainActivity : ComponentActivity() {
    private val openDesignId = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) openDesignId.value = intent.designId()
        addOnNewIntentListener { openDesignId.value = it.designId() }
        setContent {
            AppTheme(this) {
                AppNavHost(
                    navController = rememberNavController(),
                    openDesignId = openDesignId.value,
                    onOpenDesignHandled = { openDesignId.value = null },
                )
            }
        }
    }

    private fun Intent.designId(): String? =
        if (action == ACTION_OPEN_DESIGN) getStringExtra(EXTRA_DESIGN_ID) else null

    companion object {
        const val ACTION_OPEN_DESIGN = "app.photowiget.action.OPEN_DESIGN"
        const val EXTRA_DESIGN_ID = "app.photowiget.extra.DESIGN_ID"
    }
}
