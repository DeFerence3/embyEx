package app.deference.embycl.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.deference.coreui.DropDown
import app.deference.coreui.DropdownItem
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_audio
import embyex.player.generated.resources.player_no_tracks
import embyex.player.generated.resources.player_subtitles
import embyex.player.generated.resources.player_subtitles_off
import org.jetbrains.compose.resources.stringResource
import app.deference.embycl.ui.player.EmbeddedPlayer
import app.deference.embycl.ui.player.PlayerTrackGroup
import app.deference.embycl.ui.player.PlayerTrack

@Composable
internal fun PlayerTrackControls(
    player: EmbeddedPlayer,
    enabled: Boolean,
    dispatchAction: (() -> Unit) -> Unit,
    onMenuExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        player.subtitleTracks?.let { tracks ->
            TrackMenu(
                title = stringResource(resource = Res.string.player_subtitles),
                icon = Icons.Default.Subtitles,
                onMenuExpandedChange = onMenuExpandedChange,
                tracks = tracks,
                enabled = enabled,
                dispatchAction = dispatchAction,
                label = { track -> track.labels.firstOrNull()?.value ?: track.language ?: track.id },
                offLabel = stringResource(resource = Res.string.player_subtitles_off),
            )
        }
        player.audioTracks?.let { tracks ->
            TrackMenu(
                title = stringResource(resource = Res.string.player_audio),
                icon = Icons.Default.Audiotrack,
                onMenuExpandedChange = onMenuExpandedChange,
                tracks = tracks,
                enabled = enabled,
                dispatchAction = dispatchAction,
                label = { track -> track.name ?: track.labels.firstOrNull()?.value ?: track.id },
            )
        }
    }
}

@Composable
private fun TrackMenu(
    title: String,
    icon: ImageVector,
    onMenuExpandedChange: (Boolean) -> Unit,
    tracks: PlayerTrackGroup,
    enabled: Boolean,
    dispatchAction: (() -> Unit) -> Unit,
    label: (PlayerTrack) -> String,
    offLabel: String? = null,
) {
    val candidates by tracks.candidates.collectAsState(initial = emptyList())
    val current by tracks.selected.collectAsState()
    var expanded by remember(key1 = tracks) { mutableStateOf(value = false) }
    val close = { expanded = false; onMenuExpandedChange(false) }
    DisposableEffect(key1 = tracks) {
        onDispose { onMenuExpandedChange(false) }
    }
    LaunchedEffect(key1 = enabled) {
        if (!enabled) close()
    }
    Box {
        PlayerIconButton(
            icon = icon,
            description = title,
            onClick = { expanded = true; onMenuExpandedChange(true) },
            enabled = enabled,
        )
        DropDown(
            expanded = expanded,
            modifier = Modifier.widthIn(min = 240.dp, max = 360.dp),
            onDismiss = close,
        ) {
            if (offLabel != null) DropdownItem(
                text = offLabel,
                selected = current == null,
                onClick = {
                    dispatchAction { tracks.select(track = null) }
                    close()
                },
            )
            if (candidates.isEmpty()) DropdownItem(
                text = stringResource(resource = Res.string.player_no_tracks),
                enabled = false,
                onClick = null,
            )
            candidates.forEach { track ->
                DropdownItem(
                    text = label(track),
                    selected = current == track,
                    onClick = {
                        dispatchAction { tracks.select(track = track) }
                        close()
                    },
                )
            }
        }
    }
}
