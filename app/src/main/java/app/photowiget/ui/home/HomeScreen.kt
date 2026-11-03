package app.photowiget.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.photowiget.R
import app.photowiget.data.HomeLayout
import app.photowiget.ui.components.DesignActions
import app.photowiget.ui.components.DesignCard
import app.photowiget.ui.components.DesignCardModel
import app.photowiget.ui.components.DesignRow
import app.photowiget.ui.components.EmptyState
import kotlinx.coroutines.launch

@Composable
fun HomeRoute(
    onCreate: () -> Unit,
    onOpen: (String) -> Unit,
    onSettings: () -> Unit,
    pinRequest: String?,
    savedNotice: Boolean,
    onResultsHandled: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pinTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteTarget by rememberSaveable { mutableStateOf<String?>(null) }

    LifecycleResumeEffect(viewModel) {
        viewModel.refreshPlacements()
        onPauseOrDispose { }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val (message, long) = when (event) {
                HomeEvent.PinUnsupported -> R.string.pin_unsupported to true
                HomeEvent.Duplicated -> R.string.duplicated to false
                HomeEvent.Deleted -> R.string.deleted to false
            }
            launch {
                snackbarHostState.showSnackbar(
                    message = context.getString(message),
                    withDismissAction = long,
                    duration = if (long) SnackbarDuration.Long else SnackbarDuration.Short,
                )
            }
        }
    }

    LaunchedEffect(pinRequest, savedNotice) {
        if (pinRequest == null && !savedNotice) return@LaunchedEffect
        if (pinRequest != null) pinTarget = pinRequest
        if (savedNotice) scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.saved)) }
        onResultsHandled()
    }

    HomeScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onCreate = onCreate,
        onOpen = onOpen,
        onSettings = onSettings,
        onToggleLayout = viewModel::toggleLayout,
        actions = DesignActions(
            onEdit = onOpen,
            onAddToHome = { pinTarget = it },
            onDuplicate = viewModel::duplicate,
            onDelete = { deleteTarget = it },
        ),
    )

    val pinDesign = state.designs.firstOrNull { it.id == pinTarget }
    if (pinDesign != null) {
        PinSheet(
            design = pinDesign,
            onDismiss = { pinTarget = null },
            onPin = { span ->
                pinTarget = null
                viewModel.pin(pinDesign.id, span)
            },
        )
    }

    val deleteDesign = state.designs.firstOrNull { it.id == deleteTarget }
    if (deleteDesign != null) {
        DeleteDialog(
            design = deleteDesign,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                viewModel.delete(deleteDesign.id)
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    snackbarHostState: SnackbarHostState,
    onCreate: () -> Unit,
    onOpen: (String) -> Unit,
    onSettings: () -> Unit,
    onToggleLayout: () -> Unit,
    actions: DesignActions,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val gridState = rememberLazyGridState()
    val fabExpanded by remember { derivedStateOf { gridState.firstVisibleItemIndex == 0 } }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                actions = {
                    val showingGrid = state.layout == HomeLayout.Grid
                    IconButton(onClick = onToggleLayout) {
                        Icon(
                            painter = painterResource(if (showingGrid) R.drawable.ic_view_list else R.drawable.ic_grid_view),
                            contentDescription = stringResource(
                                if (showingGrid) R.string.action_view_list else R.string.action_view_grid,
                            ),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = onSettings) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = stringResource(R.string.action_settings),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            if (state.designs.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onCreate,
                    expanded = fabExpanded,
                    icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                    text = { Text(stringResource(R.string.new_widget)) },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            else -> DesignGrid(
                state = state,
                gridState = gridState,
                padding = padding,
                onOpen = onOpen,
                actions = actions,
            )
        }
    }
}

@Composable
private fun DesignGrid(
    state: HomeUiState,
    gridState: LazyGridState,
    padding: PaddingValues,
    onOpen: (String) -> Unit,
    actions: DesignActions,
) {
    val grid = state.layout == HomeLayout.Grid
    LazyVerticalGrid(
        columns = if (grid) GridCells.Adaptive(minSize = 164.dp) else GridCells.Adaptive(minSize = 340.dp),
        state = gridState,
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
        item(key = "stats", span = { GridItemSpan(maxLineSpan) }) {
            StatsRow(active = state.activeCount, total = state.designs.size)
        }
        items(state.designs, key = { it.id }) { design ->
            if (grid) {
                DesignCard(
                    model = design,
                    onClick = { onOpen(design.id) },
                    actions = actions,
                    modifier = Modifier.animateItem(),
                )
            } else {
                DesignRow(
                    model = design,
                    onClick = { onOpen(design.id) },
                    actions = actions,
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

@Composable
private fun StatsRow(active: Int, total: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Text(
                text = pluralStringResource(R.plurals.home_active, active, active),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = stringResource(R.string.home_total, total),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DeleteDialog(design: DesignCardModel, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null) },
        title = { Text(stringResource(R.string.delete_title, design.name)) },
        text = {
            Text(stringResource(if (design.active) R.string.delete_body_active else R.string.delete_body))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete_confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
