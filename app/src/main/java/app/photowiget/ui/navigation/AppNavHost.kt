package app.photowiget.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import app.photowiget.ui.editor.EditorRoute
import app.photowiget.ui.home.HomeRoute
import app.photowiget.ui.settings.SettingsRoute

private const val PIN_REQUEST = "pin_request"
private const val SAVED_NOTICE = "saved_notice"

@Composable
fun AppNavHost(
    navController: NavHostController,
    openDesignId: String?,
    onOpenDesignHandled: () -> Unit,
) {
    LaunchedEffect(openDesignId) {
        if (openDesignId == null) return@LaunchedEffect
        navController.navigate(EditorDestination(openDesignId)) {
            popUpTo(HomeDestination)
            launchSingleTop = true
        }
        onOpenDesignHandled()
    }

    NavHost(
        navController = navController,
        startDestination = HomeDestination,
        enterTransition = { forwardEnter() },
        exitTransition = { forwardExit() },
        popEnterTransition = { backEnter() },
        popExitTransition = { backExit() },
    ) {
        composable<HomeDestination> { entry ->
            val pinRequest by entry.savedStateHandle.getStateFlow<String?>(PIN_REQUEST, null)
                .collectAsStateWithLifecycle()
            val savedNotice by entry.savedStateHandle.getStateFlow(SAVED_NOTICE, false)
                .collectAsStateWithLifecycle()
            HomeRoute(
                onCreate = { navController.navigate(EditorDestination()) },
                onOpen = { navController.navigate(EditorDestination(it)) },
                onSettings = { navController.navigate(SettingsDestination) },
                pinRequest = pinRequest,
                savedNotice = savedNotice,
                onResultsHandled = {
                    entry.savedStateHandle[PIN_REQUEST] = null
                    entry.savedStateHandle[SAVED_NOTICE] = false
                },
            )
        }
        composable<EditorDestination> {
            EditorRoute(
                onClose = { navController.popBackStack() },
                onSaved = { designId, created ->
                    navController.previousBackStackEntry?.savedStateHandle?.let { handle ->
                        if (created) handle[PIN_REQUEST] = designId else handle[SAVED_NOTICE] = true
                    }
                    navController.popBackStack()
                },
            )
        }
        composable<SettingsDestination> {
            SettingsRoute(onBack = { navController.popBackStack() })
        }
    }
}

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.forwardEnter(): EnterTransition =
    fadeIn(tween(220, delayMillis = 60)) + slideInHorizontally(tween(320)) { it / 10 }

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.forwardExit(): ExitTransition =
    fadeOut(tween(140))

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.backEnter(): EnterTransition =
    fadeIn(tween(220, delayMillis = 60))

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.backExit(): ExitTransition =
    fadeOut(tween(160)) + slideOutHorizontally(tween(300)) { it / 10 }
