package app.deference.embycl.ui.screens.downloads.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import app.deference.embycl.ui.screens.downloads.DownloadListItem

@Composable
internal fun RemoveDownloadDialog(
    item: DownloadListItem,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = null,
            )
        },
        title = {
            Text(
                text = "Remove download?",
            )
        },
        text = {
            Text(
                text = "Remove “${item.title}” from this device? Your server library and saved watch progress will be kept.",
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
            ) {
                Text(
                    text = "Remove",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(
                    text = "Keep download",
                )
            }
        },
    )
}
