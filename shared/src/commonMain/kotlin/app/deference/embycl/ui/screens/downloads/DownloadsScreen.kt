package app.deference.embycl.ui.screens.downloads

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.deference.embycl.domain.model.download.DownloadFailureReason
import app.deference.embycl.domain.model.download.DownloadStatus
import coil3.compose.AsyncImage
import kotlin.math.roundToInt

@Composable
fun DownloadsContent(
    state: DownloadsState,
    onAction: (DownloadsAction) -> Unit,
    modifier: Modifier = Modifier,
    bottomSpacing: Dp = 0.dp,
) {
    // Store only identity: confirmation uses the latest row, never a stale captured task.
    var removalId by rememberSaveable { mutableStateOf<String?>(null) }
    val removal = state.items.firstOrNull { it.id == removalId }
    if (removal != null) {
        AlertDialog(
            onDismissRequest = { removalId = null },
            icon = { Icon(Icons.Default.DeleteOutline, null) },
            title = { Text("Remove download?") },
            text = { Text("Remove “${removal.title}” from this device? Your server library and saved watch progress will be kept.") },
            confirmButton = {
                TextButton(onClick = {
                    removalId = null
                    onAction(DownloadsAction.Remove(removal.id))
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { removalId = null }) { Text("Keep download") } },
        )
    }
    val visible = remember(state.items, state.query, state.filter) {
        val query = state.query.trim()
        state.items.filter {
            state.filter.includes(it) && (it.title.contains(query, ignoreCase = true) || it.subtitle.contains(query, ignoreCase = true))
        }
    }
    Box(modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier.widthIn(max = 960.dp).fillMaxSize().testTag("downloads-list"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = bottomSpacing + 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "heading") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Downloads", style = if (LocalDensity.current.fontScale > 1.3f) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics { heading() })
                    Text("Your collection, ready when you are.", style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Saved in EmbyEx · Hidden from Gallery", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (state.isOffline) item(key = "offline") {
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudOff, null)
                            Text("You're offline", style = MaterialTheme.typography.titleSmall)
                        }
                        Text("Watch completed downloads while your server is unavailable.", style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { onAction(DownloadsAction.Reconnect) }) { Text("Connect to server") }
                    }
                }
            }
            if (state.items.isNotEmpty()) {
                item(key = "search") {
                    OutlinedTextField(
                        value = state.query, onValueChange = { onAction(DownloadsAction.QueryChanged(it)) },
                        label = { Text("Search downloads", maxLines = 1, overflow = TextOverflow.Ellipsis) }, leadingIcon = { Icon(Icons.Default.Search, null) },
                        trailingIcon = {
                            if (state.query.isNotEmpty()) IconButton(onClick = { onAction(DownloadsAction.QueryChanged("")) }) {
                                Icon(Icons.Default.Close, "Clear download search")
                            }
                        },
                        singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
                    )
                }
                item(key = "filters") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DownloadFilter.entries.forEach { filter ->
                            FilterChip(selected = state.filter == filter,
                                onClick = { onAction(DownloadsAction.FilterChanged(filter)) },
                                label = { Text("${filter.label} (${state.items.count(filter::includes)})") })
                        }
                    }
                }
            }
            if (state.isLoading) item(key = "loading") {
                Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text("Loading downloads…")
                }
            }
            if (state.loadFailed) item(key = "error") {
                DownloadMessage("Couldn't load downloads", "Try opening your saved collection again.", "Try again") {
                    onAction(DownloadsAction.Reload)
                }
            }
            if (!state.isLoading && !state.loadFailed && state.items.isEmpty()) item(key = "empty") {
                DownloadMessage("Take your favorites with you", "Download a movie or episode while connected. Watch it here, even when your server is unavailable.",
                    if (state.isOffline) "Connect to server" else "Browse library") {
                    onAction(if (state.isOffline) DownloadsAction.Reconnect else DownloadsAction.BrowseLibrary)
                }
            } else if (state.items.isNotEmpty() && visible.isEmpty()) item(key = "no-matches") {
                DownloadMessage("No matching downloads", "Try another title or show all downloads.", "Clear filters") {
                    onAction(DownloadsAction.QueryChanged(""))
                    onAction(DownloadsAction.FilterChanged(DownloadFilter.ALL))
                }
            }
            items(visible, key = { "download:${it.id}" }) { item ->
                DownloadCard(item, onAction, onRemove = { removalId = item.id })
            }
        }
    }
}

@Composable
private fun DownloadMessage(title: String, description: String, action: String, onClick: () -> Unit) {
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.DownloadForOffline, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FilledTonalButton(onClick = onClick) { Text(action) }
        }
    }
}

@Composable
private fun DownloadCard(item: DownloadListItem, onAction: (DownloadsAction) -> Unit, onRemove: () -> Unit) {
    Surface(modifier = Modifier.testTag("download:${item.id}"), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                if (LocalDensity.current.fontScale <= 1.5f) {
                    Surface(Modifier.size(72.dp, 104.dp), shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Movie, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            if (item.localArtwork != null) AsyncImage(item.localArtwork, null, Modifier.fillMaxSize().clip(MaterialTheme.shapes.small), contentScale = ContentScale.Crop)
                        }
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(downloadStatusLabel(item), style = MaterialTheme.typography.labelLarge,
                        color = if (item.task.status == DownloadStatus.FAILED || (item.task.status == DownloadStatus.COMPLETED && !item.fileAvailable))
                            MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    if (item.task.status != DownloadStatus.CANCELED) {
                        Text(downloadSizeLabel(item), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (item.task.status == DownloadStatus.DOWNLOADING) {
                val progress = item.task.progress.fraction
                if (progress == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                else LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
            }
            if (item.task.status == DownloadStatus.VERIFYING) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("Checking the file before it's ready to watch.", style = MaterialTheme.typography.bodySmall)
            }
            if (item.isPlayable && item.resumeLabel != null) Text(item.resumeLabel, style = MaterialTheme.typography.bodySmall)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                when (item.task.status) {
                    DownloadStatus.COMPLETED -> if (item.fileAvailable) {
                        FilledTonalButton(onClick = { onAction(DownloadsAction.Play(item.id)) }) {
                            Icon(Icons.Default.PlayArrow, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (item.resumeLabel == null) "Play" else "Resume")
                        }
                    }
                    DownloadStatus.QUEUED, DownloadStatus.DOWNLOADING -> {
                        OutlinedButton(onClick = { onAction(DownloadsAction.Pause(item.id)) }) { Text("Pause") }
                    }
                    DownloadStatus.PAUSED -> OutlinedButton(onClick = { onAction(DownloadsAction.Resume(item.id)) }) { Text("Resume download") }
                    DownloadStatus.FAILED -> OutlinedButton(onClick = { onAction(DownloadsAction.Retry(item.id)) }) { Text("Retry") }
                    DownloadStatus.WAITING_FOR_NETWORK -> TextButton(onClick = { onAction(DownloadsAction.Reconnect) }) { Text("Connect to server") }
                    DownloadStatus.WAITING_FOR_AUTHENTICATION -> TextButton(onClick = { onAction(DownloadsAction.SignIn) }) { Text("Sign in") }
                    DownloadStatus.VERIFYING, DownloadStatus.CANCELED -> Unit
                }
                when (item.task.status) {
                    DownloadStatus.COMPLETED, DownloadStatus.CANCELED, DownloadStatus.FAILED -> TextButton(onClick = onRemove) { Text("Remove") }
                    else -> TextButton(onClick = { onAction(DownloadsAction.Cancel(item.id)) }) { Text("Cancel download") }
                }
            }
        }
    }
}

private fun downloadStatusLabel(item: DownloadListItem): String = when (item.task.status) {
    DownloadStatus.COMPLETED -> if (item.fileAvailable) "Ready to watch" else "File unavailable · Remove and download again"
    DownloadStatus.QUEUED -> "Queued"
    DownloadStatus.DOWNLOADING -> item.task.progress.fraction?.let { "Downloading · ${(it * 100).roundToInt()}%" } ?: "Downloading"
    DownloadStatus.PAUSED -> "Paused by you"
    DownloadStatus.WAITING_FOR_NETWORK -> "Waiting for your server"
    DownloadStatus.WAITING_FOR_AUTHENTICATION -> "Sign in to continue downloading"
    DownloadStatus.VERIFYING -> "Verifying download"
    DownloadStatus.CANCELED -> "Canceled"
    DownloadStatus.FAILED -> when (item.task.failure) {
        DownloadFailureReason.STORAGE_FULL -> "Not enough space · Free up storage and retry"
        DownloadFailureReason.STORAGE_UNAVAILABLE -> "Storage unavailable"
        DownloadFailureReason.ACCESS_DENIED -> "Download permission denied"
        DownloadFailureReason.SOURCE_UNAVAILABLE -> "Media unavailable on your server"
        DownloadFailureReason.INVALID_RESPONSE -> "Server couldn't provide this download"
        DownloadFailureReason.VERIFICATION_FAILED -> "Download couldn't be verified"
        else -> "Download failed · Try again"
    }
}

private fun downloadSizeLabel(item: DownloadListItem): String {
    val progress = item.task.progress
    if (item.task.status == DownloadStatus.COMPLETED) return formatDownloadBytes(progress.downloadedBytes)
    return "${formatDownloadBytes(progress.downloadedBytes)} / ${progress.totalBytes?.let(::formatDownloadBytes) ?: "size unknown"}"
}

internal fun formatDownloadBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KiB", "MiB", "GiB", "TiB", "PiB", "EiB")
    var size = bytes.toDouble() / 1024
    var index = 0
    while (size >= 1024 && index < units.lastIndex) { size /= 1024; index++ }
    val tenths = (size * 10).roundToInt()
    return "${tenths / 10}.${tenths % 10} ${units[index]}"
}
