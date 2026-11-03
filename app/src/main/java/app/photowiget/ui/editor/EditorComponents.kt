package app.photowiget.ui.editor

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import app.photowiget.R
import kotlin.math.roundToInt

enum class EditorTab(@param:StringRes val label: Int, @param:DrawableRes val icon: Int) {
    Photos(R.string.tab_photos, R.drawable.ic_photo_library),
    Frame(R.string.tab_frame, R.drawable.ic_category),
    Border(R.string.tab_border, R.drawable.ic_palette),
    Tones(R.string.tab_tones, R.drawable.ic_filter_vintage),
    Schedule(R.string.tab_schedule, R.drawable.ic_timer),
}

@Composable
fun EditorCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(Modifier.padding(20.dp)) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                Spacer(Modifier.height(14.dp))
            }
            content()
        }
    }
}

/** A slider that snaps to multiples of [step] and ticks under the finger on every stop. */
@Composable
fun StepSlider(
    value: Int,
    max: Int,
    step: Int,
    valueDescription: String,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    Slider(
        value = value.toFloat(),
        onValueChange = { raw ->
            val snapped = ((raw / step).roundToInt() * step).coerceIn(0, max)
            if (snapped != value) {
                haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                onValueChange(snapped)
            }
        },
        valueRange = 0f..max.toFloat(),
        steps = max / step - 1,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .semantics { stateDescription = valueDescription },
    )
}
