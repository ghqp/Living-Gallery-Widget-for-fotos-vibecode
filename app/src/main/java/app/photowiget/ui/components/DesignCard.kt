package app.photowiget.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.photowiget.R
import app.photowiget.data.model.PhotoCrop
import app.photowiget.data.model.WidgetLook
import java.io.File

@Immutable
data class DesignCardModel(
    val id: String,
    val name: String,
    val look: WidgetLook,
    val cover: File?,
    val coverCrop: PhotoCrop,
    val photoCount: Int,
    val active: Boolean,
)

class DesignActions(
    val onEdit: (String) -> Unit,
    val onAddToHome: (String) -> Unit,
    val onDuplicate: (String) -> Unit,
    val onDelete: (String) -> Unit,
)

@Composable
fun DesignCard(
    model: DesignCardModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: DesignActions? = null,
    selected: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(Modifier.padding(8.dp)) {
            DesignArtwork(
                file = model.cover,
                crop = model.coverCrop,
                look = model.look,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            )
            Row(
                modifier = Modifier.padding(start = 8.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CardTitles(model, Modifier.weight(1f).padding(bottom = 6.dp))
                if (actions != null) {
                    DesignMenu(model, actions)
                } else {
                    Spacer(Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
fun DesignRow(
    model: DesignCardModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    actions: DesignActions? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            DesignArtwork(
                file = model.cover,
                crop = model.coverCrop,
                look = model.look,
                modifier = Modifier.size(76.dp),
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 16.dp),
            ) {
                CardTitles(model)
                Text(
                    text = pluralStringResource(R.plurals.photo_count, model.photoCount, model.photoCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (actions != null) DesignMenu(model, actions)
        }
    }
}

@Composable
private fun CardTitles(model: DesignCardModel, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            text = model.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = if (model.active) {
                stringResource(model.look.frame.labelRes)
            } else {
                stringResource(R.string.card_not_added)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DesignMenu(model: DesignCardModel, actions: DesignActions) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                painter = painterResource(R.drawable.ic_more_vert),
                contentDescription = stringResource(R.string.menu_more, model.name),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.medium,
        ) {
            MenuItem(R.string.menu_edit, R.drawable.ic_edit) {
                expanded = false
                actions.onEdit(model.id)
            }
            MenuItem(R.string.menu_add_to_home, R.drawable.ic_add_to_home_screen) {
                expanded = false
                actions.onAddToHome(model.id)
            }
            MenuItem(R.string.menu_duplicate, R.drawable.ic_content_copy) {
                expanded = false
                actions.onDuplicate(model.id)
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            DropdownMenuItem(
                text = { Text(stringResource(R.string.menu_delete)) },
                onClick = {
                    expanded = false
                    actions.onDelete(model.id)
                },
                leadingIcon = { Icon(painterResource(R.drawable.ic_delete), contentDescription = null) },
                colors = MenuDefaults.itemColors(
                    textColor = MaterialTheme.colorScheme.error,
                    leadingIconColor = MaterialTheme.colorScheme.error,
                ),
            )
        }
    }
}

@Composable
private fun MenuItem(label: Int, icon: Int, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(stringResource(label)) },
        onClick = onClick,
        leadingIcon = { Icon(painterResource(icon), contentDescription = null) },
    )
}
