package app.photowiget.ui.editor

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.zIndex
import app.photowiget.R
import app.photowiget.data.model.WidgetDesign
import app.photowiget.ui.components.rememberPhoto
import kotlinx.coroutines.launch

@Composable
fun PhotosTab(
    photos: List<EditorPhoto>,
    currentIndex: Int,
    importing: Boolean,
    onAdd: () -> Unit,
    onSelect: (Int) -> Unit,
    onRemove: (String) -> Unit,
    onMove: (Int, Int) -> Unit,
) {
    EditorCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.photos_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.photos_count, photos.size, WidgetDesign.MAX_PHOTOS),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onAdd,
                enabled = !importing && photos.size < WidgetDesign.MAX_PHOTOS,
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                modifier = Modifier.height(48.dp),
            ) {
                if (importing) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = LocalContentColor.current,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                }
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text(stringResource(R.string.add_photos))
            }
        }
        Spacer(Modifier.height(18.dp))
        if (photos.isEmpty()) {
            EmptyPhotos(onAdd = onAdd, enabled = !importing)
        } else {
            ReorderablePhotoGrid(
                photos = photos,
                currentIndex = currentIndex,
                onSelect = onSelect,
                onRemove = onRemove,
                onMove = onMove,
            )
            if (photos.size > 1) {
                Text(
                    text = stringResource(R.string.photos_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
        }
    }
}

@Composable
private fun EmptyPhotos(onAdd: () -> Unit, enabled: Boolean) {
    val outline = MaterialTheme.colorScheme.outline
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onAdd)
            .drawBehind {
                drawRoundRect(
                    color = outline,
                    cornerRadius = CornerRadius(18.dp.toPx()),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
                    ),
                )
            }
            .padding(vertical = 32.dp, horizontal = 24.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_add_photo),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(36.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.photos_empty, WidgetDesign.MAX_PHOTOS),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Three-column photo grid; long-press a photo and drag it to change the order. */
@Composable
private fun ReorderablePhotoGrid(
    photos: List<EditorPhoto>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    onRemove: (String) -> Unit,
    onMove: (Int, Int) -> Unit,
    columns: Int = 3,
    spacing: Dp = 10.dp,
) {
    val latestPhotos by rememberUpdatedState(photos)
    val latestOnMove by rememberUpdatedState(onMove)
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var draggingKey by remember { mutableStateOf<String?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val cell = (maxWidth - spacing * (columns - 1)) / columns
        val cellPx = with(density) { cell.toPx() }
        val stepPx = with(density) { (cell + spacing).toPx() }
        val rows = (photos.size + columns - 1) / columns
        val thumbPx = cellPx.toInt()
        fun slot(index: Int) = Offset((index % columns) * stepPx, (index / columns) * stepPx)

        Box(
            Modifier
                .fillMaxWidth()
                .height(cell * rows + spacing * (rows - 1).coerceAtLeast(0)),
        ) {
            photos.forEachIndexed { index, photo ->
                key(photo.key) {
                    val dragging = draggingKey == photo.key
                    val settle = remember { Animatable(Offset.Zero, Offset.VectorConverter) }
                    val target = slot(index)
                    val animatedTarget by animateOffsetAsState(target, label = "photoSlot")
                    val lifted = dragging || settle.isRunning
                    val base = if (lifted) target else animatedTarget
                    val scale by animateFloatAsState(if (dragging) 1.06f else 1f, label = "photoScale")
                    val elevation by animateDpAsState(if (dragging) 12.dp else 0.dp, label = "photoElevation")
                    val drop: () -> Unit = {
                        val release = dragOffset
                        scope.launch {
                            settle.snapTo(release)
                            draggingKey = null
                            dragOffset = Offset.Zero
                            settle.animateTo(Offset.Zero, spring(stiffness = Spring.StiffnessMediumLow))
                        }
                    }
                    PhotoThumb(
                        photo = photo,
                        number = index + 1,
                        sizePx = thumbPx,
                        selected = index == currentIndex,
                        onClick = { onSelect(index) },
                        onRemove = { onRemove(photo.key) },
                        modifier = Modifier
                            .zIndex(if (lifted) 1f else 0f)
                            .offset { (base + if (dragging) dragOffset else settle.value).round() }
                            .size(cell)
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                                shadowElevation = elevation.toPx()
                                shape = RoundedCornerShape(14.dp)
                            }
                            .pointerInput(photo.key, stepPx) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggingKey = photo.key
                                        dragOffset = Offset.Zero
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                    },
                                    onDrag = { change, amount ->
                                        change.consume()
                                        dragOffset += amount
                                        val list = latestPhotos
                                        val from = list.indexOfFirst { it.key == photo.key }
                                        if (from < 0) return@detectDragGesturesAfterLongPress
                                        val center = slot(from) + dragOffset + Offset(cellPx / 2f, cellPx / 2f)
                                        val column = (center.x / stepPx).toInt().coerceIn(0, columns - 1)
                                        val row = (center.y / stepPx).toInt().coerceIn(0, list.lastIndex / columns)
                                        val to = (row * columns + column).coerceIn(0, list.lastIndex)
                                        if (to != from) {
                                            latestOnMove(from, to)
                                            dragOffset += slot(from) - slot(to)
                                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                        }
                                    },
                                    onDragEnd = drop,
                                    onDragCancel = drop,
                                )
                            },
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoThumb(
    photo: EditorPhoto,
    number: Int,
    sizePx: Int,
    selected: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    val bitmap = rememberPhoto(photo.file, sizePx)
    val image = remember(bitmap) { bitmap?.asImageBitmap() }
    Box(
        modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .clickable(onClickLabel = stringResource(R.string.photo_preview_action), onClick = onClick),
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = stringResource(R.string.photo_item, number),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (selected) {
            Box(
                Modifier
                    .fillMaxSize()
                    .border(3.dp, MaterialTheme.colorScheme.primary, shape),
            )
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(40.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_close),
                    contentDescription = stringResource(R.string.remove_photo, number),
                    tint = Color.White,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
