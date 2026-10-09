package app.deference.embycl.ui.screens.settings.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.deference.embycl.domain.model.update.CurrentBuildRelease
import app.deference.embycl.ui.core.markdown.MarkDownPage
import app.deference.embycl.ui.screens.settings.SettingsState

@Composable
fun CurrentBuildChangelogDialog(
    state: SettingsState,
    onRetry: () -> Unit,
    onOpenRelease: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Description, null) },
        title = { Text("Changelog · ${CurrentBuildRelease.tag}") },
        text = {
            val changelog = state.changelog
            when {
                state.isChangelogLoading || changelog == null -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    Text("Loading release notes…")
                }
                changelog.isFailure -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(changelog.exceptionOrNull()?.message ?: "Could not load release notes.")
                    TextButton(onClick = onRetry) { Text("Try again") }
                }
                changelog.getOrThrow().isBlank() -> Text("No changelog was provided for this build.")
                else -> MarkDownPage(changelog.getOrThrow(), Modifier.fillMaxWidth().heightIn(max = 360.dp))
            }
        },
        confirmButton = { TextButton(onClick = onOpenRelease) { Text("View release page") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
