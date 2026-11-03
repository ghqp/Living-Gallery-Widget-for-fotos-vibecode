package app.photowiget.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onSizeChanged
import app.photowiget.data.model.PhotoCrop
import app.photowiget.data.model.WidgetLook
import app.photowiget.render.ArtworkCanvasCache
import app.photowiget.render.ArtworkColors
import app.photowiget.render.ArtworkPainter
import app.photowiget.render.ArtworkSpec
import app.photowiget.render.CropMath
import java.io.File
import kotlin.math.min
import kotlin.math.roundToInt

/** The widget exactly as the home screen draws it, centered in the available space. */
@Composable
fun WidgetArtwork(
    photo: Bitmap?,
    crop: PhotoCrop,
    look: WidgetLook,
    modifier: Modifier = Modifier,
    cornerRadius: Float = look.cornerRadius.toFloat(),
    borderWidth: Float = look.borderWidth.toFloat(),
) {
    val placeholder = MaterialTheme.colorScheme.surfaceContainerHighest.toArgb()
    val borderColor = remember(look.borderTint, look.tone, photo) {
        ArtworkColors.border(look.borderTint, look.tone, photo)
    }
    val window = remember(photo, crop) { photo?.let { CropMath.window(it.width, it.height, crop) } }
    val cache = remember { ArtworkCanvasCache() }
    val spec = ArtworkSpec(look.frame, cornerRadius, borderWidth, borderColor, look.tone)
    Spacer(
        modifier.drawBehind {
            drawIntoCanvas { canvas ->
                ArtworkPainter.draw(
                    canvas.nativeCanvas, 0f, 0f, size.width, size.height,
                    photo, window, spec, placeholder, cache,
                )
            }
        },
    )
}

/** [WidgetArtwork] that loads its photo at the resolution it is laid out at. */
@Composable
fun DesignArtwork(
    file: File?,
    crop: PhotoCrop,
    look: WidgetLook,
    modifier: Modifier = Modifier,
) {
    var sidePx by remember { mutableIntStateOf(0) }
    val photo = rememberPhoto(file, (sidePx * crop.zoom).roundToInt())
    WidgetArtwork(
        photo = photo,
        crop = crop,
        look = look,
        modifier = modifier.onSizeChanged { sidePx = min(it.width, it.height) },
    )
}
