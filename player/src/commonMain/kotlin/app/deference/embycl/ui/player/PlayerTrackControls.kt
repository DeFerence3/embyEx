package app.deference.embycl.ui.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_audio
import embyex.player.generated.resources.player_subtitles
import embyex.player.generated.resources.player_subtitles_off
import org.jetbrains.compose.resources.stringResource
import org.openani.mediamp.MediampPlayer
import org.openani.mediamp.features.audioTracks
import org.openani.mediamp.features.subtitleTracks
import org.openani.mediamp.metadata.TrackGroup

@Composable
internal fun PlayerTrackControls(
    player: MediampPlayer,
    enabled: Boolean,
    dispatchAction: (() -> Unit) -> Unit,
    onMenuExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(modifier = modifier) {
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
    }
}

@Composable
private fun <T> TrackMenu(
    title: String,
    icon: ImageVector,
    onMenuExpandedChange: (Boolean) -> Unit,
    tracks: TrackGroup<T>,
    enabled: Boolean,
    dispatchAction: (() -> Unit) -> Unit,
    label: (T) -> String,
    offLabel: String? = null,
) {
    val candidates by tracks.candidates.collectAsState(initial = emptyList())
    val current by tracks.selected.collectAsState()
    var expanded by remember { mutableStateOf(value = false) }
    Box {
        PlayerIconButton(
            icon = icon,
            description = title,
            onClick = { expanded = true; onMenuExpandedChange(true) },
            enabled = enabled && candidates.isNotEmpty(),
        )
        DropdownMenu(
			expanded = expanded,
			onDismissRequest = { expanded = false; onMenuExpandedChange(false) }
		) {
            if (offLabel != null) DropdownMenuItem(
                text = { Text(text = offLabel) },
                onClick = { dispatchAction { tracks.select(track = null) }; expanded = false; onMenuExpandedChange(false) },
                modifier = Modifier
					.semantics { selected = current == null },
            )
            candidates.forEach { track ->
                DropdownMenuItem(
                    text = { Text(text = label(track)) },
                    onClick = { dispatchAction { tracks.select(track = track) }; expanded = false; onMenuExpandedChange(false) },
                    modifier = Modifier.semantics { selected = current == track },
                )
            }
        }
    }
}
