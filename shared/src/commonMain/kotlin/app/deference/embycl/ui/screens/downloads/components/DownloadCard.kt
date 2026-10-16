package app.deference.embycl.ui.screens.downloads.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.deference.embycl.domain.model.download.DownloadFailureReason
import app.deference.embycl.domain.model.download.DownloadStatus
import app.deference.embycl.ui.screens.downloads.DownloadListItem
import app.deference.embycl.ui.screens.downloads.DownloadsAction
import app.deference.embycl.ui.screens.downloads.formatDownloadBytes
import coil3.compose.AsyncImage
import kotlin.math.roundToInt

@Composable
internal fun DownloadCard(
    item: DownloadListItem,
    onAction: (DownloadsAction) -> Unit,
    onRemove: () -> Unit,
    playbackEnabled: Boolean = true,
    busy: Boolean = false,
    playing: Boolean = false,
) {
    Surface(
        modifier = Modifier
            .testTag("download:${item.id}"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (LocalDensity.current.fontScale <= 1.5f) {
                    Surface(
                        modifier = Modifier
                            .size(
                                width = 72.dp,
                                height = 104.dp,
                            ),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                            if (item.localArtwork != null) {
                                AsyncImage(
                                    model = item.localArtwork,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(MaterialTheme.shapes.small),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = item.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val statusText = if (playing) {
                        "Playing on this device"
                    } else if (busy) {
                        "Updating download…"
                    } else if (!playbackEnabled && item.isPlayable) {
                        "Downloaded"
                    } else {
                        downloadStatusLabel(item = item)
                    }
                    val isError = item.task.status == DownloadStatus.FAILED ||
                        (item.task.status == DownloadStatus.COMPLETED && !item.fileAvailable)
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isError) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                    if (item.task.status != DownloadStatus.CANCELED) {
                        Text(
                            text = downloadSizeLabel(item = item),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (item.task.status == DownloadStatus.DOWNLOADING) {
                val progress = item.task.progress.fraction
                if (progress == null) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth(),
                    )
                } else {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth(),
                    )
                }
            }
            if (item.task.status == DownloadStatus.VERIFYING) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth(),
                )
                Text(
                    text = "Checking the file before it's ready to watch.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (item.isPlayable && item.resumeLabel != null) {
                Text(
                    text = item.resumeLabel,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                when (item.task.status) {
                    DownloadStatus.COMPLETED -> if (item.fileAvailable) {
                        FilledTonalButton(
                            onClick = {
                                onAction(
                                    DownloadsAction.Play(
                                        id = item.id,
                                    ),
                                )
                            },
                            enabled = playbackEnabled && !busy,
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(20.dp),
                            )
                            Spacer(
                                modifier = Modifier
                                    .width(4.dp),
                            )
                            Text(
                                text = if (item.resumeLabel == null) "Play" else "Resume",
                            )
                        }
                    }
                    DownloadStatus.QUEUED,
                    DownloadStatus.DOWNLOADING -> {
                        OutlinedButton(
                            onClick = {
                                onAction(
                                    DownloadsAction.Pause(
                                        id = item.id,
                                    ),
                                )
                            },
                            enabled = !busy,
                        ) {
                            Text(
                                text = "Pause",
                            )
                        }
                    }
                    DownloadStatus.PAUSED -> {
                        OutlinedButton(
                            onClick = {
                                onAction(
                                    DownloadsAction.Resume(
                                        id = item.id,
                                    ),
                                )
                            },
                            enabled = !busy,
                        ) {
                            Text(
                                text = "Resume download",
                            )
                        }
                    }
                    DownloadStatus.FAILED -> {
                        OutlinedButton(
                            onClick = {
                                onAction(
                                    DownloadsAction.Retry(
                                        id = item.id,
                                    ),
                                )
                            },
                            enabled = !busy,
                        ) {
                            Text(
                                text = "Retry",
                            )
                        }
                    }
                    DownloadStatus.WAITING_FOR_NETWORK -> {
                        TextButton(
                            onClick = {
                                onAction(DownloadsAction.Reconnect)
                            },
                        ) {
                            Text(
                                text = "Connect to server",
                            )
                        }
                    }
                    DownloadStatus.WAITING_FOR_AUTHENTICATION -> {
                        TextButton(
                            onClick = {
                                onAction(DownloadsAction.SignIn)
                            },
                        ) {
                            Text(
                                text = "Sign in",
                            )
                        }
                    }
                    DownloadStatus.VERIFYING,
                    DownloadStatus.CANCELED -> Unit
                }
                when (item.task.status) {
                    DownloadStatus.COMPLETED,
                    DownloadStatus.CANCELED,
                    DownloadStatus.FAILED -> {
                        TextButton(
                            onClick = onRemove,
                            enabled = !busy,
                        ) {
                            Text(
                                text = "Remove",
                            )
                        }
                    }
                    else -> {
                        TextButton(
                            onClick = {
                                onAction(
                                    DownloadsAction.Cancel(
                                        id = item.id,
                                    ),
                                )
                            },
                            enabled = !busy,
                        ) {
                            Text(
                                text = "Cancel download",
                            )
                        }
                    }
                }
                TextButton(
                    onClick = {
                        onAction(
                            DownloadsAction.OpenDetails(
                                id = item.id,
                            ),
                        )
                    },
                ) {
                    Text(
                        text = "Details",
                    )
                }
            }
        }
    }
}

private fun downloadStatusLabel(
    item: DownloadListItem,
): String = when (item.task.status) {
    DownloadStatus.COMPLETED -> if (item.fileAvailable) {
        "Ready to watch"
    } else if (item.fileCheckPending) {
        "Checking saved file…"
    } else {
        "File unavailable · Remove and download again"
    }
    DownloadStatus.QUEUED -> "Queued"
    DownloadStatus.DOWNLOADING -> item.task.progress.fraction?.let {
        "Downloading · ${(it * 100).roundToInt()}%"
    } ?: "Downloading"
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

private fun downloadSizeLabel(
    item: DownloadListItem,
): String {
    val progress = item.task.progress
    if (item.task.status == DownloadStatus.COMPLETED) {
        return formatDownloadBytes(
            bytes = progress.downloadedBytes,
        )
    }
    val downloaded = formatDownloadBytes(
        bytes = progress.downloadedBytes,
    )
    val total = progress.totalBytes?.let {
        formatDownloadBytes(
            bytes = it,
        )
    } ?: "size unknown"
    return "$downloaded / $total"
}
