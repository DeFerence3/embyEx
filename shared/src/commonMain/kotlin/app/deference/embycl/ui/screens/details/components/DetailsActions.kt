package app.deference.embycl.ui.screens.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.deference.coreui.DropDown
import app.deference.coreui.DropdownItem
import app.deference.embycl.ui.screens.details.model.DownloadStatusUi
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.details_download
import embyex.shared.generated.resources.details_download_paused
import embyex.shared.generated.resources.details_download_progress
import embyex.shared.generated.resources.details_download_queued
import embyex.shared.generated.resources.details_download_retry
import embyex.shared.generated.resources.details_downloaded
import embyex.shared.generated.resources.details_favorite
import embyex.shared.generated.resources.details_mark_watched
import embyex.shared.generated.resources.details_play_from_server
import embyex.shared.generated.resources.details_remove_download
import embyex.shared.generated.resources.details_watched
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
internal fun DetailsActions(
    isFavorite: Boolean,
    isPlayed: Boolean,
    download: DownloadStatusUi,
    togglesEnabled: Boolean,
    fillWidth: Boolean,
    onToggleFavorite: () -> Unit,
    onToggleWatched: () -> Unit,
    onDownload: () -> Unit,
    onPlayFromServer: () -> Unit,
    onRemoveDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tile = if (fillWidth) Modifier.fillMaxWidth() else Modifier.width(width = 128.dp)
    Row(
        modifier = if (fillWidth) modifier.fillMaxWidth() else modifier,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        Box(modifier = if (fillWidth) Modifier.weight(weight = 1f) else Modifier) {
            DetailsActionTile(
                label = stringResource(resource = Res.string.details_favorite),
                checked = isFavorite,
                enabled = togglesEnabled,
                onClick = onToggleFavorite,
                modifier = tile,
            ) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                )
            }
        }
        Box(modifier = if (fillWidth) Modifier.weight(weight = 1f) else Modifier) {
            DetailsActionTile(
                label = stringResource(resource = if (isPlayed) Res.string.details_watched else Res.string.details_mark_watched),
                checked = isPlayed,
                enabled = togglesEnabled,
                onClick = onToggleWatched,
                modifier = tile,
            ) {
                Icon(
                    imageVector = if (isPlayed) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                    contentDescription = null,
                )
            }
        }
        Box(modifier = if (fillWidth) Modifier.weight(weight = 1f) else Modifier) {
            DownloadTile(
                status = download,
                onDownload = onDownload,
                onPlayFromServer = onPlayFromServer,
                onRemoveDownload = onRemoveDownload,
                modifier = tile,
            )
        }
    }
}

@Composable
private fun DownloadTile(
    status: DownloadStatusUi,
    onDownload: () -> Unit,
    onPlayFromServer: () -> Unit,
    onRemoveDownload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(value = false) }
    val colors = MaterialTheme.colorScheme
    Box {
        when (status) {
            DownloadStatusUi.Unavailable,
            DownloadStatusUi.Idle,
            -> DetailsActionTile(
                label = stringResource(resource = Res.string.details_download),
                enabled = status == DownloadStatusUi.Idle,
                onClick = onDownload,
                modifier = modifier,
            ) {
                Icon(
                    imageVector = Icons.Filled.Download,
                    contentDescription = null,
                )
            }
            DownloadStatusUi.Checking -> DetailsActionTile(
                label = stringResource(resource = Res.string.details_download),
                enabled = false,
                onClick = {},
                modifier = modifier,
            ) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            }
            DownloadStatusUi.Queued -> DetailsActionTile(
                label = stringResource(resource = Res.string.details_download_queued),
                onClick = {},
                modifier = modifier,
            ) {
                Icon(
                    imageVector = Icons.Filled.Schedule,
                    contentDescription = null,
                )
            }
            is DownloadStatusUi.Downloading -> DetailsActionTile(
                label = stringResource(
                    resource = Res.string.details_download_progress,
                    (status.progress * 100).roundToInt(),
                ),
                onClick = {},
                modifier = modifier,
            ) {
                CircularProgressIndicator(
                    progress = { status.progress },
                    strokeWidth = 3.dp,
                )
            }
            is DownloadStatusUi.Paused -> DetailsActionTile(
                label = stringResource(
                    resource = Res.string.details_download_paused,
                    (status.progress * 100).roundToInt(),
                ),
                onClick = {},
                modifier = modifier,
            ) {
                CircularProgressIndicator(
                    progress = { status.progress },
                    strokeWidth = 3.dp,
                )
            }
            DownloadStatusUi.Downloaded -> DetailsActionTile(
                label = stringResource(resource = Res.string.details_downloaded),
                onClick = { menuOpen = true },
                containerColor = colors.secondaryContainer,
                modifier = modifier,
            ) {
                Icon(
                    imageVector = Icons.Filled.DownloadDone,
                    contentDescription = null,
                )
            }
            DownloadStatusUi.Failed -> DetailsActionTile(
                label = stringResource(resource = Res.string.details_download_retry),
                onClick = onDownload,
                containerColor = colors.errorContainer,
                modifier = modifier,
            ) {
                Icon(
                    imageVector = Icons.Filled.ErrorOutline,
                    contentDescription = null,
                )
            }
        }
        DropDown(
            expanded = menuOpen && status == DownloadStatusUi.Downloaded,
            onDismiss = { menuOpen = false },
        ) {
            DropdownItem(
                text = stringResource(resource = Res.string.details_play_from_server),
                icon = Icons.Filled.Cloud,
                onClick = {
                    menuOpen = false
                    onPlayFromServer()
                },
            )
            DropdownItem(
                text = stringResource(resource = Res.string.details_remove_download),
                icon = Icons.Filled.Delete,
                onClick = {
                    menuOpen = false
                    onRemoveDownload()
                },
            )
        }
    }
}
