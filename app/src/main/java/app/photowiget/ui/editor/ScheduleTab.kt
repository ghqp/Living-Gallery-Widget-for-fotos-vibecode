package app.photowiget.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.photowiget.R
import app.photowiget.data.model.RotationInterval
import app.photowiget.data.model.TapAction
import app.photowiget.data.model.WidgetBehavior
import app.photowiget.ui.components.labelRes

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScheduleTab(
    behavior: WidgetBehavior,
    photoCount: Int,
    onBehaviorChange: (WidgetBehavior) -> Unit,
) {
    val rotates = photoCount > 1
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        EditorCard(title = stringResource(R.string.schedule_title)) {
            Text(
                text = stringResource(if (rotates) R.string.schedule_body else R.string.schedule_need_photos),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(14.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                RotationInterval.entries.forEach { interval ->
                    val selected = interval == behavior.interval
                    FilterChip(
                        selected = selected,
                        onClick = { onBehaviorChange(behavior.copy(interval = interval)) },
                        enabled = rotates || interval == RotationInterval.Off,
                        label = { Text(stringResource(interval.labelRes)) },
                        leadingIcon = if (selected) {
                            {
                                Icon(
                                    painter = painterResource(R.drawable.ic_check),
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }
        }
        EditorCard(title = stringResource(R.string.order_title)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !behavior.shuffle,
                    onClick = { onBehaviorChange(behavior.copy(shuffle = false)) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text(stringResource(R.string.order_sequential)) }
                SegmentedButton(
                    selected = behavior.shuffle,
                    onClick = { onBehaviorChange(behavior.copy(shuffle = true)) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text(stringResource(R.string.order_shuffle)) }
            }
        }
        EditorCard(title = stringResource(R.string.tap_title)) {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = behavior.tapAction == TapAction.NextPhoto,
                    onClick = { onBehaviorChange(behavior.copy(tapAction = TapAction.NextPhoto)) },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text(stringResource(R.string.tap_next)) }
                SegmentedButton(
                    selected = behavior.tapAction == TapAction.OpenApp,
                    onClick = { onBehaviorChange(behavior.copy(tapAction = TapAction.OpenApp)) },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text(stringResource(R.string.tap_open)) }
            }
            if (!rotates) {
                Text(
                    text = stringResource(R.string.tap_single_photo),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }
    }
}
