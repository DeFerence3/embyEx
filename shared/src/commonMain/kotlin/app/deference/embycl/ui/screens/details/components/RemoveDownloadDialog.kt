package app.deference.embycl.ui.screens.details.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.details_keep_download
import embyex.shared.generated.resources.details_remove_body
import embyex.shared.generated.resources.details_remove_download
import embyex.shared.generated.resources.details_remove_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun RemoveDownloadDialog(
    title: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(resource = Res.string.details_remove_title)) },
        text = {
            Text(
                text = stringResource(
                    resource = Res.string.details_remove_body,
                    title,
                ),
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(resource = Res.string.details_keep_download))
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(resource = Res.string.details_remove_download),
                )
            }
        },
    )
}
