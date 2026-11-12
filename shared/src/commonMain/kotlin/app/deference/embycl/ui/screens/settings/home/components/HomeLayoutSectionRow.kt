package app.deference.embycl.ui.screens.settings.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.core.focusRing
import app.deference.embycl.ui.screens.home.components.title
import app.deference.embycl.ui.screens.settings.home.model.HomeLayoutRowUi
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.home_edit_hidden
import embyex.shared.generated.resources.home_edit_move_down
import embyex.shared.generated.resources.home_edit_move_up
import embyex.shared.generated.resources.home_edit_pinned
import embyex.shared.generated.resources.home_edit_show
import embyex.shared.generated.resources.home_edit_shown
import org.jetbrains.compose.resources.stringResource

/** One section: name and state, Move up / Move down buttons (no drag-only reordering), and a Show switch. */
@Composable
internal fun HomeLayoutSectionRow(
    row: HomeLayoutRowUi,
    shape: Shape,
    onToggle: () -> Unit,
    onMove: (up: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = row.kind.title()
    val showLabel = stringResource(
        resource = Res.string.home_edit_show,
        title,
    )
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 72.dp)
                .padding(
                    start = 18.dp,
                    end = 10.dp,
                    top = 8.dp,
                    bottom = 8.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(weight = 1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = when {
                        row.pinned -> stringResource(resource = Res.string.home_edit_pinned)
                        row.visible -> stringResource(resource = Res.string.home_edit_shown)
                        else -> stringResource(resource = Res.string.home_edit_hidden)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = { onMove(true) },
                enabled = row.canMoveUp,
                modifier = Modifier.focusRing(shape = CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowUpward,
                    contentDescription = stringResource(
                        resource = Res.string.home_edit_move_up,
                        title,
                    ),
                )
            }
            IconButton(
                onClick = { onMove(false) },
                enabled = row.canMoveDown,
                modifier = Modifier.focusRing(shape = CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowDownward,
                    contentDescription = stringResource(
                        resource = Res.string.home_edit_move_down,
                        title,
                    ),
                )
            }
            Switch(
                checked = row.visible,
                onCheckedChange = { onToggle() },
                modifier = Modifier
                    .padding(start = 8.dp)
                    .focusRing(shape = CircleShape)
                    .semantics { contentDescription = showLabel },
            )
        }
    }
}
