package app.deference.embycl.ui.core

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun ExitConfirmationDialog(
	onConfirm: () -> Unit,
	onDismiss: () -> Unit,
) {
	AlertDialog(
		onDismissRequest = onDismiss,
		icon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) },
		title = { Text("Close EmbyEx?") },
		text = { Text("You’ll stay signed in and can pick up where you left off.") },
		confirmButton = {
			TextButton(onClick = onConfirm) {
				Text("Exit", color = MaterialTheme.colorScheme.error)
			}
		},
		dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
	)
}
