package app.deference.embycl.ui.screens.downloads.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.deference.embycl.domain.model.download.DownloadKey
import app.deference.embycl.domain.model.download.DownloadSource
import app.deference.embycl.domain.model.download.SubtitleAvailability
import app.deference.embycl.ui.screens.downloads.formatDownloadBytes
import kotlinx.serialization.json.Json

/** Selection uses the complete source key, never the display name or list position. */
@Composable
fun DownloadSourceDialog(
    title: String,
    sources: List<DownloadSource>,
    selectedKey: DownloadKey?,
    serverAvailable: Boolean,
    onSelect: (DownloadKey) -> Unit,
    onConfirm: (DownloadSource) -> Unit,
    onDismiss: () -> Unit,
    isSubmitting: Boolean = false,
) {
    val selected = sources.singleOrNull {
        it.key == selectedKey
    }
    Dialog(
        onDismissRequest = {
            if (!isSubmitting) onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
        ),
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.large,
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .selectableGroup()
                        .testTag("download-source-list"),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Text(
                            text = "Choose a version",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            text = "Original files · Saved in EmbyEx, hidden from Gallery",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (!serverAvailable) {
                        item {
                            Text(
                                text = "Your server is unavailable. Reconnect before starting a download.",
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    if (sources.isEmpty()) {
                        item {
                            Text(
                                text = "No downloadable versions are available.",
                            )
                        }
                    }
                    items(
                        items = sources,
                        key = {
                            Json.encodeToString(
                                serializer = DownloadKey.serializer(),
                                value = it.key,
                            )
                        },
                    ) { source ->
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = if (source.key == selectedKey) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            },
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("source:${source.key.mediaSourceId}")
                                    .selectable(
                                        selected = source.key == selectedKey,
                                        enabled = !isSubmitting,
                                        role = Role.RadioButton,
                                        onClick = {
                                            onSelect(source.key)
                                        },
                                    )
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                RadioButton(
                                    selected = source.key == selectedKey,
                                    onClick = null,
                                    enabled = !isSubmitting,
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = source.name ?: "Original version",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    val sizeText = source.sizeBytes?.let {
                                        formatDownloadBytes(
                                            bytes = it,
                                        )
                                    } ?: "Size unknown"
                                    Text(
                                        text = "${source.container.uppercase()} · $sizeText",
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    source.streams.filter {
                                        it.type == "Video" || it.type == "Audio"
                                    }.forEach { stream ->
                                        Text(
                                            text = stream.displayTitle
                                                ?: listOfNotNull(
                                                    stream.type,
                                                    stream.codec,
                                                    stream.language,
                                                ).joinToString(" · "),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                    Text(
                                        text = if (source.subtitles.isEmpty()) {
                                            "No subtitles listed"
                                        } else {
                                            "Subtitles"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                    source.subtitles.forEach { subtitle ->
                                        val availability = when (subtitle.availability) {
                                            SubtitleAvailability.EMBEDDED -> "Included in video"
                                            SubtitleAvailability.SEPARATE_FILE -> "Downloaded separately"
                                            SubtitleAvailability.UNSUPPORTED -> "Unavailable offline"
                                        }
                                        Text(
                                            text = "${subtitle.stream.displayTitle ?: subtitle.stream.language ?: "Subtitle"} · $availability",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item {
                        Text(
                            text = "Downloads stay on this device when your server reconnects. Removing a download keeps the server copy.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                FlowRow(
                    modifier = Modifier
                        .align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isSubmitting,
                    ) {
                        Text(
                            text = "Not now",
                        )
                    }
                    Button(
                        onClick = {
                            selected?.let(onConfirm)
                        },
                        enabled = selected != null && serverAvailable && !isSubmitting,
                    ) {
                        Text(
                            text = if (isSubmitting) "Adding…" else "Start download",
                        )
                    }
                }
            }
        }
    }
}
