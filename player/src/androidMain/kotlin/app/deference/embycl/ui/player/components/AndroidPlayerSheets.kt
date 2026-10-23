package app.deference.embycl.ui.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.player.AndroidPlayerSheet
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_audio
import embyex.player.generated.resources.player_more
import embyex.player.generated.resources.player_no_tracks
import embyex.player.generated.resources.player_speed
import embyex.player.generated.resources.player_subtitles
import embyex.player.generated.resources.player_subtitles_off
import org.jetbrains.compose.resources.stringResource
import org.openani.mediamp.MediampPlayer
import org.openani.mediamp.features.PlaybackSpeed
import org.openani.mediamp.features.audioTracks
import org.openani.mediamp.features.subtitleTracks
import org.openani.mediamp.metadata.TrackGroup

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AndroidPlayerSheets(
    sheet: AndroidPlayerSheet,
    player: MediampPlayer,
    onDismiss: () -> Unit,
    onSheet: (AndroidPlayerSheet) -> Unit,
    dispatchAction: (() -> Unit) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(state = rememberScrollState())
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(resource = when (sheet) {
                    AndroidPlayerSheet.Audio -> Res.string.player_audio
                    AndroidPlayerSheet.Subtitles -> Res.string.player_subtitles
                    AndroidPlayerSheet.Speed -> Res.string.player_speed
                    AndroidPlayerSheet.More -> Res.string.player_more
                }),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
            when (sheet) {
                AndroidPlayerSheet.Audio -> player.audioTracks?.let { tracks ->
                    AndroidTrackChoices(
                        tracks = tracks,
                        label = { it.name ?: it.labels.firstOrNull()?.value ?: it.id },
                        onDismiss = onDismiss,
                        dispatchAction = dispatchAction,
                    )
                }
                AndroidPlayerSheet.Subtitles -> player.subtitleTracks?.let { tracks ->
                    AndroidTrackChoices(
                        tracks = tracks,
                        label = { it.labels.firstOrNull()?.value ?: it.language ?: it.id },
                        offLabel = stringResource(resource = Res.string.player_subtitles_off),
                        onDismiss = onDismiss,
                        dispatchAction = dispatchAction,
                    )
                }
                AndroidPlayerSheet.Speed -> player.features[PlaybackSpeed]?.let { speed ->
                    val current by speed.valueFlow.collectAsState(initial = speed.value)
                    listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f).forEach { value ->
                        AndroidPlayerChoice(
                            label = "${value}×",
                            selected = current == value,
                            onClick = {
                                dispatchAction { speed.set(speed = value) }
                                onDismiss()
                            },
                        )
                    }
                }
                AndroidPlayerSheet.More -> {
                    if (player.audioTracks != null) AndroidPlayerChoice(
                        label = stringResource(resource = Res.string.player_audio),
                        onClick = { onSheet(AndroidPlayerSheet.Audio) },
                    )
                    if (player.subtitleTracks != null) AndroidPlayerChoice(
                        label = stringResource(resource = Res.string.player_subtitles),
                        onClick = { onSheet(AndroidPlayerSheet.Subtitles) },
                    )
                    if (player.features[PlaybackSpeed] != null) AndroidPlayerChoice(
                        label = stringResource(resource = Res.string.player_speed),
                        onClick = { onSheet(AndroidPlayerSheet.Speed) },
                    )
                }
            }
        }
    }
}

@Composable
private fun <T> AndroidTrackChoices(
    tracks: TrackGroup<T>,
    label: (T) -> String,
    onDismiss: () -> Unit,
    dispatchAction: (() -> Unit) -> Unit,
    offLabel: String? = null,
) {
    val candidates by tracks.candidates.collectAsState(initial = emptyList())
    val current by tracks.selected.collectAsState()
    if (offLabel != null) AndroidPlayerChoice(
        label = offLabel,
        selected = current == null,
        onClick = {
            dispatchAction { tracks.select(track = null) }
            onDismiss()
        },
    )
    if (candidates.isEmpty()) Text(
        text = stringResource(resource = Res.string.player_no_tracks),
        modifier = Modifier.padding(all = 24.dp),
    )
    candidates.forEach { track ->
        AndroidPlayerChoice(
            label = label(track),
            selected = current == track,
            onClick = {
                dispatchAction { tracks.select(track = track) }
                onDismiss()
            },
        )
    }
}

@Composable
private fun AndroidPlayerChoice(
    label: String,
    onClick: () -> Unit,
    selected: Boolean? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = if (selected == null) Role.Button else Role.RadioButton, onClick = onClick)
            .semantics { if (selected != null) this.selected = selected }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, modifier = Modifier.weight(weight = 1f))
        if (selected != null) RadioButton(selected = selected, onClick = null)
    }
}
