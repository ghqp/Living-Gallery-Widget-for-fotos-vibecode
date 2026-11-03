package app.photowiget.ui.editor

import android.graphics.Bitmap
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.photowiget.R
import app.photowiget.data.model.BorderTint
import app.photowiget.data.model.ColorTone
import app.photowiget.data.model.FrameShape
import app.photowiget.data.model.PhotoCrop
import app.photowiget.data.model.WidgetLook
import app.photowiget.render.ArtworkColors
import app.photowiget.render.ArtworkPainter
import app.photowiget.render.FrameGeometry
import app.photowiget.ui.components.WidgetArtwork
import app.photowiget.ui.components.labelRes
import app.photowiget.ui.components.rememberPhoto

@Composable
fun FrameTab(look: WidgetLook, onLookChange: (WidgetLook) -> Unit) {
    EditorCard(title = stringResource(R.string.frame_title)) {
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FrameShape.entries.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { frame ->
                        FrameTile(
                            frame = frame,
                            cornerRadius = look.cornerRadius,
                            selected = frame == look.frame,
                            onClick = { onLookChange(look.copy(frame = frame)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FrameTile(
    frame: FrameShape,
    cornerRadius: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    val glyph by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
        label = "frameGlyph",
    )
    val label by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "frameLabel",
    )
    Column(
        modifier = modifier.selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            FrameGlyph(
                frame = frame,
                cornerRadius = cornerRadius,
                color = glyph,
                modifier = Modifier.fillMaxSize(0.46f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(frame.labelRes),
            style = MaterialTheme.typography.labelLarge,
            color = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun FrameGlyph(frame: FrameShape, cornerRadius: Int, color: Color, modifier: Modifier = Modifier) {
    val path = remember { Path() }
    val rect = remember { RectF() }
    val paint = remember { android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG) }
    Canvas(modifier) {
        rect.set(0f, 0f, size.width, size.height)
        val unit = size.minDimension / ArtworkPainter.DESIGN_SIDE
        FrameGeometry.outline(frame, rect, cornerRadius * unit, path)
        paint.color = color.toArgb()
        drawIntoCanvas { it.nativeCanvas.drawPath(path, paint) }
    }
}

@Composable
fun BorderTab(look: WidgetLook, photo: Bitmap?, onLookChange: (WidgetLook) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        EditorCard(title = stringResource(R.string.border_title)) {
            val width = stringResource(R.string.border_width, look.borderWidth)
            Text(
                text = width,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))
            StepSlider(
                value = look.borderWidth,
                max = WidgetLook.MAX_BORDER_WIDTH,
                step = 1,
                valueDescription = width,
                onValueChange = { onLookChange(look.copy(borderWidth = it)) },
            )
            AnimatedVisibility(
                visible = look.borderWidth > 0,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.border_color),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    TintSwatches(
                        selected = look.borderTint,
                        autoColor = remember(photo, look.tone) { photo?.let { ArtworkColors.fromPhoto(it, look.tone) } },
                        onSelect = { onLookChange(look.copy(borderTint = it)) },
                    )
                }
            }
        }
        EditorCard(title = stringResource(R.string.corner_title)) {
            val radius = stringResource(R.string.corner_value, look.cornerRadius)
            Text(
                text = radius,
                style = MaterialTheme.typography.labelLarge,
                color = if (look.frame.hasCorners) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Spacer(Modifier.height(8.dp))
            StepSlider(
                value = look.cornerRadius,
                max = WidgetLook.MAX_CORNER_RADIUS,
                step = WidgetLook.CORNER_RADIUS_STEP,
                valueDescription = radius,
                enabled = look.frame.hasCorners,
                onValueChange = { onLookChange(look.copy(cornerRadius = it)) },
            )
            if (!look.frame.hasCorners) {
                Text(
                    text = stringResource(R.string.corner_unavailable, stringResource(look.frame.labelRes)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun TintSwatches(selected: BorderTint, autoColor: Int?, onSelect: (BorderTint) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.selectableGroup(),
    ) {
        items(BorderTint.entries) { tint ->
            val color = Color(tint.argb ?: autoColor ?: 0xFFE1E2E8.toInt())
            val isSelected = tint == selected
            val description = stringResource(tint.labelRes)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .then(
                        if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier,
                    )
                    .selectable(selected = isSelected, onClick = { onSelect(tint) }, role = Role.RadioButton)
                    .semantics { contentDescription = description },
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                ) {
                    if (tint == BorderTint.Auto) {
                        Icon(
                            painter = painterResource(R.drawable.ic_auto_awesome),
                            contentDescription = null,
                            tint = if (color.luminance() > 0.5f) Color.Black.copy(alpha = 0.7f) else Color.White,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TonesTab(look: WidgetLook, photo: EditorPhoto?, onLookChange: (WidgetLook) -> Unit) {
    val tileSize = 76.dp
    val tilePx = with(LocalDensity.current) { tileSize.roundToPx() }
    val bitmap = rememberPhoto(photo?.file, tilePx)
    EditorCard(title = stringResource(R.string.tones_title)) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(end = 4.dp),
            modifier = Modifier.selectableGroup(),
        ) {
            items(ColorTone.entries) { tone ->
                ToneTile(
                    tone = tone,
                    selected = tone == look.tone,
                    photo = bitmap,
                    crop = photo?.crop ?: PhotoCrop(),
                    size = tileSize,
                    onClick = { onLookChange(look.copy(tone = tone)) },
                )
            }
        }
    }
}

@Composable
private fun ToneTile(
    tone: ColorTone,
    selected: Boolean,
    photo: Bitmap?,
    crop: PhotoCrop,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(size + 8.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
    ) {
        Box(
            modifier = Modifier
                .size(size + 8.dp)
                .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
                .padding(4.dp),
        ) {
            WidgetArtwork(
                photo = photo,
                crop = crop,
                look = WidgetLook(frame = FrameShape.Square, cornerRadius = 24, tone = tone),
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(tone.labelRes),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
