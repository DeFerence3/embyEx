package app.deference.embycl.ui.screens.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.deference.embycl.domain.model.EmbyServer
import app.deference.embycl.ui.core.focusRing
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.home_server_picker_cancel
import embyex.shared.generated.resources.home_server_picker_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HomeServerPickerDialog(
    servers: List<EmbyServer>,
    modifier: Modifier = Modifier,
    onSelect: (EmbyServer) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Dns,
                contentDescription = null,
            )
        },
        title = { Text(text = stringResource(resource = Res.string.home_server_picker_title)) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(state = rememberScrollState())
            ) {
                servers.forEach { server ->
                    ListItem(
                        headlineContent = { Text(text = server.name) },
                        supportingContent = { Text(text = server.address) },
                        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        modifier = Modifier
                            .focusRing(shape = MaterialTheme.shapes.medium)
                            .clickable { onSelect(server) },
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(resource = Res.string.home_server_picker_cancel))
            }
        },
    )
}
