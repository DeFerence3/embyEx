package app.deference.embycl.ui.player.components

import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.ui.semantics.Role
import app.deference.embycl.ui.player.components.PlayerSpeeds
import app.deference.embycl.ui.player.components.audioLabel
import app.deference.embycl.ui.player.components.label
import app.deference.embycl.ui.player.components.speedLabel
import app.deference.embycl.ui.player.components.subtitleLabel
import embyex.player.generated.resources.player_external
import embyex.player.generated.resources.player_more
import embyex.player.generated.resources.player_subtitles_off
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.player.AndroidPlayerSheet
import app.deference.embycl.ui.player.EmbeddedPlayer
import app.deference.embycl.ui.player.PlayerAspectRatio
import app.deference.embycl.ui.player.PlayerTrack
import app.deference.embycl.ui.player.PlayerTrackGroup
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_aspect
import embyex.player.generated.resources.player_audio
import embyex.player.generated.resources.player_crop
import embyex.player.generated.resources.player_fit
import embyex.player.generated.resources.player_no_tracks
import embyex.player.generated.resources.player_speed
import embyex.player.generated.resources.player_stretch
import embyex.player.generated.resources.player_subtitles
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AndroidPlayerSheets(
    sheet: AndroidPlayerSheet,
    player: EmbeddedPlayer,
    onDismiss: () -> Unit,
    onSheet: (AndroidPlayerSheet) -> Unit,
    onOpenExternalPlayer: () -> Unit,
    showAspect: Boolean,
    dispatchAction: (() -> Unit) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden, enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)),
    ) {
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
                    AndroidPlayerSheet.Aspect -> Res.string.player_aspect
                    AndroidPlayerSheet.More -> Res.string.player_more
                }),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
            )
            when (sheet) {
                AndroidPlayerSheet.More -> {
                    if (showAspect && player.aspectRatio != null) ListItem(
                        headlineContent = { Text(text = stringResource(resource = Res.string.player_aspect)) },
                        leadingContent = { Icon(imageVector = Icons.Default.AspectRatio, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) { onSheet(AndroidPlayerSheet.Aspect) },
                    )
                    ListItem(
                        headlineContent = { Text(text = stringResource(resource = Res.string.player_external)) },
                        leadingContent = { Icon(imageVector = Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) {
                                onDismiss()
                                onOpenExternalPlayer()
                            },
                    )
                }
                AndroidPlayerSheet.Audio -> player.audioTracks?.let { tracks ->
                    AndroidTrackChoices(
                        tracks = tracks,
                        label = ::audioLabel,
                        onDismiss = onDismiss,
                        dispatchAction = dispatchAction,
                    )
                }
                AndroidPlayerSheet.Subtitles -> player.subtitleTracks?.let { tracks ->
                    AndroidTrackChoices(
                        tracks = tracks,
                        label = ::subtitleLabel,
                        offLabel = stringResource(resource = Res.string.player_subtitles_off),
                        onDismiss = onDismiss,
                        dispatchAction = dispatchAction,
                    )
                }
                AndroidPlayerSheet.Speed -> player.playbackSpeed?.let { speed ->
                    val current by speed.valueFlow.collectAsState(initial = speed.value)
                    PlayerSpeeds.forEach { value ->
                        AndroidPlayerChoice(
                            label = speedLabel(speed = value),
                            selected = current == value,
                            onClick = {
                                dispatchAction { speed.set(speed = value) }
                                onDismiss()
                            },
                        )
                    }
                }
                AndroidPlayerSheet.Aspect -> {
                    player.aspectRatio?.let { aspect ->
                        val mode by aspect.mode.collectAsState()
                        PlayerAspectRatio.entries.forEach { option ->
                            AndroidPlayerChoice(
                                label = stringResource(resource = option.label()),
                                selected = mode == option,
                                onClick = {
                                    dispatchAction { aspect.setMode(mode = option) }
                                    onDismiss()
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AndroidTrackChoices(
    tracks: PlayerTrackGroup,
    label: (PlayerTrack) -> String,
    offLabel: String? = null,
    onDismiss: () -> Unit,
    dispatchAction: (() -> Unit) -> Unit,
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
            .heightIn(min = 56.dp)
            .semantics { if (selected != null) this.selected = selected }
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selected != null) RadioButton(selected = selected, onClick = null)
		Spacer(Modifier.width(12.dp))
		if (selected == true) {
			val weight = FontWeight.Bold
			Text(text = label, fontWeight = weight, modifier = Modifier.weight(weight = 1f))
		}else{
			Text(text = label, modifier = Modifier.weight(weight = 1f))
		}
    }
}
