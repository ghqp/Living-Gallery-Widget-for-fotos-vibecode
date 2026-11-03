package app.photowiget.ui.editor

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.photowiget.R
import app.photowiget.data.model.PhotoCrop
import app.photowiget.data.model.WidgetLook
import app.photowiget.ui.components.WidgetArtwork
import app.photowiget.ui.components.rememberPhoto
import kotlin.math.roundToInt

@Composable
fun PreviewHeader(
    photos: List<EditorPhoto>,
    look: WidgetLook,
    name: String,
    pagerState: PagerState,
    previewSize: Dp,
    decodeSize: Dp,
    captionAlpha: Float,
    live: Boolean,
    onToggleLive: () -> Unit,
    onAdjust: (EditorPhoto) -> Unit,
    onAddPhotos: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val radius by animateFloatAsState(look.cornerRadius.toFloat(), label = "cornerRadius")
    val border by animateFloatAsState(look.borderWidth.toFloat(), label = "borderWidth")
    val decodePx = with(LocalDensity.current) { decodeSize.roundToPx() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(top = 8.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(previewSize)) {
            if (photos.isEmpty()) {
                EmptyPreview(look, radius, border, onAddPhotos)
            } else {
                HorizontalPager(
                    state = pagerState,
                    key = { page -> photos.getOrNull(page)?.key ?: page },
                    beyondViewportPageCount = 1,
                    pageSpacing = 24.dp,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    val photo = photos.getOrNull(page)
                    val zoom = photo?.crop?.zoom ?: 1f
                    WidgetArtwork(
                        photo = rememberPhoto(photo?.file, (decodePx * zoom).roundToInt()),
                        crop = photo?.crop ?: PhotoCrop(),
                        look = look,
                        cornerRadius = radius,
                        borderWidth = border,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                val current = pagerState.currentPage.coerceIn(0, photos.lastIndex)
                if (captionAlpha > 0f) {
                    PreviewCaption(
                        name = name,
                        photoNumber = current + 1,
                        live = live,
                        onAdjust = { onAdjust(photos[current]) },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .graphicsLayer { alpha = captionAlpha },
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        LivePreviewChip(live = live, onToggle = onToggleLive)
    }
}

@Composable
private fun EmptyPreview(look: WidgetLook, radius: Float, border: Float, onAddPhotos: () -> Unit) {
    Box(contentAlignment = Alignment.Center) {
        WidgetArtwork(
            photo = null,
            crop = PhotoCrop(),
            look = look,
            cornerRadius = radius,
            borderWidth = border,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable(role = Role.Button, onClick = onAddPhotos)
                .padding(20.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_add_photo),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.preview_empty),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PreviewCaption(
    name: String,
    photoNumber: Int,
    live: Boolean,
    onAdjust: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0x47000000), Color(0x75000000))))
            .border(1.dp, Color.White.copy(alpha = 0.1f), shape)
            .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = name.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.4.sp),
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = stringResource(R.string.preview_photo, photoNumber),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = 1,
            )
        }
        AnimatedVisibility(visible = live, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
            Icon(
                painter = painterResource(R.drawable.ic_auto_awesome),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .padding(end = 2.dp)
                    .size(22.dp),
            )
        }
        IconButton(
            onClick = onAdjust,
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = Color.Black.copy(alpha = 0.32f),
                contentColor = Color.White,
            ),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_tune),
                contentDescription = stringResource(R.string.adjust_photo),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun LivePreviewChip(live: Boolean, onToggle: () -> Unit) {
    val container by animateColorAsState(
        if (live) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        label = "liveContainer",
    )
    val content by animateColorAsState(
        if (live) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "liveContent",
    )
    Surface(
        checked = live,
        onCheckedChange = { onToggle() },
        shape = CircleShape,
        color = container,
        contentColor = content,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_visibility),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(text = stringResource(R.string.live_preview), style = MaterialTheme.typography.labelLarge)
        }
    }
}
