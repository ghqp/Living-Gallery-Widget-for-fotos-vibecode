package app.photowiget.ui.editor

import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.photowiget.R
import app.photowiget.data.model.PhotoCrop
import app.photowiget.data.model.WidgetLook
import app.photowiget.render.ArtworkPainter
import app.photowiget.render.CropMath
import app.photowiget.render.FrameGeometry
import app.photowiget.render.ToneFilters
import app.photowiget.ui.components.rememberPhoto
import kotlin.math.roundToInt

/** Full-screen pan and zoom of one photo inside the widget frame. */
@Composable
fun AdjustPhotoDialog(
    photo: EditorPhoto,
    look: WidgetLook,
    onDismiss: () -> Unit,
    onApply: (PhotoCrop) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val crop = remember { mutableStateOf(photo.crop) }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLowest) {
            Column(Modifier.systemBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.action_close))
                    }
                    Text(
                        text = stringResource(R.string.adjust_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp),
                    )
                    Button(onClick = { onApply(crop.value) }, modifier = Modifier.padding(end = 8.dp)) {
                        Text(stringResource(R.string.adjust_done))
                    }
                }
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    val viewport = minOf(maxWidth, maxHeight) - 48.dp
                    CropViewport(
                        photo = photo,
                        look = look,
                        crop = crop,
                        viewportSize = with(LocalDensity.current) { viewport.toPx() },
                    )
                }
                Text(
                    text = stringResource(R.string.adjust_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    OutlinedButton(onClick = { crop.value = PhotoCrop() }, enabled = crop.value != PhotoCrop()) {
                        Icon(
                            painter = painterResource(R.drawable.ic_restart),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.adjust_reset))
                    }
                }
            }
        }
    }
}

@Composable
private fun CropViewport(
    photo: EditorPhoto,
    look: WidgetLook,
    crop: MutableState<PhotoCrop>,
    viewportSize: Float,
) {
    val bitmap = rememberPhoto(photo.file, (viewportSize * 2).roundToInt())
    val scrim = MaterialTheme.colorScheme.scrim.copy(alpha = 0.62f).toArgb()
    val outline = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f).toArgb()
    val frame = remember { Path() }
    val mask = remember { Path() }
    val viewportRect = remember { RectF() }
    val imageRect = remember { RectF() }
    val photoPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG) }
    val scrimPaint = remember { Paint(Paint.ANTI_ALIAS_FLAG) }
    val strokePaint = remember { Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE } }
    Canvas(
        Modifier
            .fillMaxSize()
            .pointerInput(bitmap, viewportSize) {
                val source = bitmap ?: return@pointerInput
                detectTransformGestures { _, pan, zoom, _ ->
                    crop.value = CropMath.transform(
                        crop.value, source.width, source.height, viewportSize, pan.x, pan.y, zoom,
                    )
                }
            }
            .pointerInput(bitmap) {
                val source = bitmap ?: return@pointerInput
                detectTapGestures(
                    onDoubleTap = {
                        val zoom = if (crop.value.zoom > 1.05f) 1f else 2f
                        crop.value = CropMath.clamp(crop.value.copy(zoom = zoom), source.width, source.height)
                    },
                )
            },
    ) {
        val left = (size.width - viewportSize) / 2f
        val top = (size.height - viewportSize) / 2f
        viewportRect.set(left, top, left + viewportSize, top + viewportSize)
        drawIntoCanvas { canvas ->
            val native = canvas.nativeCanvas
            val radius = look.cornerRadius * viewportSize / ArtworkPainter.DESIGN_SIDE
            FrameGeometry.outline(look.frame, viewportRect, radius, frame)
            if (bitmap != null) {
                val window = CropMath.window(bitmap.width, bitmap.height, crop.value)
                val scale = viewportSize / window.size
                imageRect.set(
                    left - window.left * scale,
                    top - window.top * scale,
                    left + (bitmap.width - window.left) * scale,
                    top + (bitmap.height - window.top) * scale,
                )
                photoPaint.colorFilter = ToneFilters.filter(look.tone)
                native.drawBitmap(bitmap, null, imageRect, photoPaint)
                mask.rewind()
                mask.fillType = Path.FillType.EVEN_ODD
                mask.addRect(imageRect, Path.Direction.CW)
                mask.addPath(frame)
                scrimPaint.color = scrim
                native.drawPath(mask, scrimPaint)
            }
            strokePaint.color = outline
            strokePaint.strokeWidth = 1.5.dp.toPx()
            native.drawPath(frame, strokePaint)
        }
    }
}
