package app.deference.embcl.ui.core.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun SignOutConfirmationDialog(
	serverName: String,
	onConfirm: () -> Unit,
	onDismiss: () -> Unit,
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
		title = { Text("Sign out?") },
		text = { Text("Sign out of $serverName? You’ll need to sign in again to access your library.") },
		confirmButton = {
			TextButton(onClick = onConfirm) {
				Text("Sign out", color = MaterialTheme.colorScheme.error)
			}
		},
		dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
	)
}
