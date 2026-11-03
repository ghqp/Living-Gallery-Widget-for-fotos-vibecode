package app.photowiget.ui.editor

import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.photowiget.R
import app.photowiget.data.model.WidgetBehavior
import app.photowiget.data.model.WidgetLook
import app.photowiget.ui.components.rememberPhoto
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EditorRoute(
    onClose: () -> Unit,
    onSaved: (designId: String, created: Boolean) -> Unit,
    viewModel: EditorViewModel = viewModel(factory = EditorViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState { state.photos.size.coerceAtLeast(1) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var adjustKey by rememberSaveable { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(GalleryPick()) { viewModel.addPhotos(it) }

    val requestClose = {
        if (viewModel.hasUnsavedChanges()) confirmDiscard = true else onClose()
    }
    BackHandler(onBack = requestClose)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is EditorEvent.Saved -> onSaved(event.designId, event.created)
                is EditorEvent.PhotosAdded -> launch { pagerState.animateScrollToPage(event.firstIndex) }
                is EditorEvent.Message -> launch {
                    val text = if (event.arg != null) {
                        context.getString(event.text, event.arg)
                    } else {
                        context.getString(event.text)
                    }
                    snackbarHostState.showSnackbar(text)
                }
                EditorEvent.Missing -> onClose()
            }
        }
    }

    EditorScreen(
        state = state,
        name = viewModel.name,
        isNew = viewModel.isNew,
        pagerState = pagerState,
        snackbarHostState = snackbarHostState,
        onNameChange = viewModel::onNameChange,
        onClose = requestClose,
        onSave = viewModel::save,
        onAddPhotos = {
            try {
                picker.launch(Unit)
            } catch (_: ActivityNotFoundException) {
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.error_picker)) }
            }
        },
        onSelectPhoto = { index -> scope.launch { pagerState.animateScrollToPage(index) } },
        onRemovePhoto = viewModel::removePhoto,
        onMovePhoto = viewModel::movePhoto,
        onAdjust = { adjustKey = it.key },
        onLookChange = viewModel::updateLook,
        onBehaviorChange = viewModel::updateBehavior,
    )

    val adjusting = state.photos.firstOrNull { it.key == adjustKey }
    if (adjusting != null) {
        AdjustPhotoDialog(
            photo = adjusting,
            look = state.look,
            onDismiss = { adjustKey = null },
            onApply = { crop ->
                viewModel.setCrop(adjusting.key, crop)
                adjustKey = null
            },
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text(stringResource(R.string.discard_title)) },
            text = { Text(stringResource(R.string.discard_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDiscard = false
                        onClose()
                    },
                ) { Text(stringResource(R.string.discard_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDiscard = false }) { Text(stringResource(R.string.keep_editing)) }
            },
        )
    }
}

@Composable
fun EditorScreen(
    state: EditorState,
    name: String,
    isNew: Boolean,
    pagerState: PagerState,
    snackbarHostState: SnackbarHostState,
    onNameChange: (String) -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
    onAddPhotos: () -> Unit,
    onSelectPhoto: (Int) -> Unit,
    onRemovePhoto: (String) -> Unit,
    onMovePhoto: (Int, Int) -> Unit,
    onAdjust: (EditorPhoto) -> Unit,
    onLookChange: (WidgetLook) -> Unit,
    onBehaviorChange: (WidgetBehavior) -> Unit,
) {
    var live by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val photoCount = state.photos.size

    LaunchedEffect(live, photoCount) {
        if (!live || photoCount < 2) return@LaunchedEffect
        while (true) {
            delay(LIVE_PREVIEW_INTERVAL_MS)
            pagerState.animateScrollToPage(
                page = (pagerState.currentPage + 1) % photoCount,
                animationSpec = tween(durationMillis = 700),
            )
        }
    }

    Scaffold(
        topBar = {
            EditorTopBar(
                title = stringResource(if (isNew) R.string.editor_title_new else R.string.editor_title_edit),
                canSave = state.canSave,
                saving = state.saving,
                onClose = onClose,
                onSave = onSave,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState, Modifier.navigationBarsPadding()) },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0),
    ) { padding ->
        if (state.loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }
        CollapsingEditor(
            modifier = Modifier
                .padding(padding)
                .imePadding(),
            header = { previewSize, decodeSize, collapse ->
                PreviewHeader(
                    photos = state.photos,
                    look = state.look,
                    name = name,
                    pagerState = pagerState,
                    previewSize = previewSize,
                    decodeSize = decodeSize,
                    captionAlpha = (1f - collapse * 1.6f).coerceIn(0f, 1f),
                    live = live,
                    onToggleLive = {
                        if (photoCount < 2 && !live) {
                            scope.launch {
                                snackbarHostState.showSnackbar(context.getString(R.string.live_preview_need_photos))
                            }
                        } else {
                            live = !live
                        }
                    },
                    onAdjust = onAdjust,
                    onAddPhotos = onAddPhotos,
                )
            },
            name = name,
            onNameChange = onNameChange,
            tabContent = { tab ->
                when (tab) {
                    EditorTab.Photos -> PhotosTab(
                        photos = state.photos,
                        currentIndex = pagerState.currentPage,
                        importing = state.importing,
                        onAdd = onAddPhotos,
                        onSelect = onSelectPhoto,
                        onRemove = onRemovePhoto,
                        onMove = onMovePhoto,
                    )
                    EditorTab.Frame -> FrameTab(look = state.look, onLookChange = onLookChange)
                    EditorTab.Border -> {
                        val current = state.photos.getOrNull(pagerState.currentPage)
                        BorderTab(
                            look = state.look,
                            photo = rememberPhoto(current?.file, 192),
                            onLookChange = onLookChange,
                        )
                    }
                    EditorTab.Tones -> TonesTab(
                        look = state.look,
                        photo = state.photos.getOrNull(pagerState.currentPage),
                        onLookChange = onLookChange,
                    )
                    EditorTab.Schedule -> ScheduleTab(
                        behavior = state.behavior,
                        photoCount = photoCount,
                        onBehaviorChange = onBehaviorChange,
                    )
                }
            },
        )
    }
}

@Composable
private fun EditorTopBar(
    title: String,
    canSave: Boolean,
    saving: Boolean,
    onClose: () -> Unit,
    onSave: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .height(72.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledIconButton(
            onClick = onClose,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier.size(44.dp),
        ) {
            Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.action_close))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Button(
            onClick = onSave,
            enabled = canSave,
            contentPadding = PaddingValues(horizontal = 26.dp),
            modifier = Modifier
                .height(48.dp)
                .widthIn(min = 96.dp),
        ) {
            if (saving) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = LocalContentColor.current,
                    modifier = Modifier.size(20.dp),
                )
            } else {
                Text(stringResource(R.string.action_save))
            }
        }
    }
}

/**
 * The preview shrinks while the settings below scroll up, and only grows back once the settings
 * are scrolled to the top again. The tab row sticks under the preview.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CollapsingEditor(
    header: @Composable (previewSize: Dp, decodeSize: Dp, collapse: Float) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    tabContent: @Composable (EditorTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by rememberSaveable { mutableStateOf(EditorTab.Photos) }
    val focusManager = LocalFocusManager.current
    val navigationBars = WindowInsets.navigationBars.asPaddingValues()
    BoxWithConstraints(modifier.fillMaxSize()) {
        val expanded = minOf(288.dp, maxWidth - 96.dp, maxHeight * 0.4f)
        val collapsed = expanded * 0.58f
        val rangePx = with(LocalDensity.current) { (expanded - collapsed).toPx() }
        var collapsedPx by rememberSaveable { mutableFloatStateOf(0f) }
        val connection = remember(rangePx) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (available.y >= 0f) return Offset.Zero
                    val next = (collapsedPx - available.y).coerceAtMost(rangePx)
                    val consumed = next - collapsedPx
                    collapsedPx = next
                    return Offset(0f, -consumed)
                }

                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    if (available.y <= 0f) return Offset.Zero
                    val next = (collapsedPx - available.y).coerceAtLeast(0f)
                    val used = collapsedPx - next
                    collapsedPx = next
                    return Offset(0f, used)
                }
            }
        }
        val fraction = if (rangePx > 0f) (collapsedPx / rangePx).coerceIn(0f, 1f) else 0f
        Column(
            Modifier
                .fillMaxSize()
                .nestedScroll(connection),
        ) {
            header(lerp(expanded, collapsed, fraction), expanded, fraction)
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
                contentPadding = PaddingValues(bottom = navigationBars.calculateBottomPadding() + 32.dp),
            ) {
                item(key = "name") {
                    OutlinedTextField(
                        value = name,
                        onValueChange = onNameChange,
                        label = { Text(stringResource(R.string.field_name)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 20.dp),
                    )
                }
                stickyHeader(key = "tabs") {
                    PrimaryScrollableTabRow(
                        selectedTabIndex = tab.ordinal,
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        edgePadding = 8.dp,
                        divider = {},
                        indicator = {
                            TabRowDefaults.PrimaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tab.ordinal, matchContentSize = false),
                                width = Dp.Unspecified,
                                shape = RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp),
                            )
                        },
                    ) {
                        EditorTab.entries.forEach { entry ->
                            Tab(
                                selected = entry == tab,
                                onClick = { tab = entry },
                                text = { Text(stringResource(entry.label), maxLines = 1) },
                                icon = { Icon(painterResource(entry.icon), contentDescription = null) },
                                selectedContentColor = MaterialTheme.colorScheme.primary,
                                unselectedContentColor = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }
                item(key = "content") {
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = {
                            fadeIn(tween(220, delayMillis = 60)) togetherWith fadeOut(tween(120)) using
                                SizeTransform(clip = false)
                        },
                        label = "editorTab",
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp),
                    ) { current ->
                        tabContent(current)
                    }
                }
            }
        }
    }
}

private const val LIVE_PREVIEW_INTERVAL_MS = 2_600L
