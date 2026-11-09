package app.deference.embycl.ui.screens.details.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.deference.coreui.DropDown
import app.deference.coreui.DropdownItem
import app.deference.embycl.ui.core.animateWithHover
import app.deference.embycl.ui.screens.details.model.PlaybackUi
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.details_download
import embyex.shared.generated.resources.details_minutes_left
import embyex.shared.generated.resources.details_play
import embyex.shared.generated.resources.details_play_again
import embyex.shared.generated.resources.details_play_from_server
import embyex.shared.generated.resources.details_playback_options
import embyex.shared.generated.resources.details_remove_download
import embyex.shared.generated.resources.details_resume_from
import embyex.shared.generated.resources.details_start_over
import embyex.shared.generated.resources.details_watched_percent
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

@Composable
internal fun DetailsPlayBlock(
    playback: PlaybackUi,
    fillWidth: Boolean,
    onPlay: () -> Unit,
    onStartOver: (() -> Unit)?,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        PlaySplitButton(
            playback = playback,
            fillWidth = fillWidth,
            onPlay = onPlay,
            focusRequester = focusRequester,
        )
        if (playback is PlaybackUi.Resume) {
            ResumeProgress(
                playback = playback,
                onStartOver = onStartOver,
            )
        }
    }
}

@Composable
private fun PlaySplitButton(
    playback: PlaybackUi,
    fillWidth: Boolean,
    onPlay: () -> Unit,
    focusRequester: FocusRequester,
) {
    val leadingSource = remember { MutableInteractionSource() }
    Button(
        modifier = Modifier
            .then(
                if (fillWidth) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier.widthIn(
                        min = 230.dp,
                        max = 310.dp,
                    )
                },
            )
            .heightIn(min = 64.dp)
            .animateWithHover(interactionSource = leadingSource)
            .focusRequester(focusRequester = focusRequester),
        interactionSource = leadingSource,
        onClick = onPlay
    ){
        Icon(
            imageVector = if (playback == PlaybackUi.Again) Icons.Filled.Replay else Icons.Filled.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(size = 24.dp),
        )
        Spacer(modifier = Modifier.width(width = 8.dp))
        Text(
            text = when (playback) {
                PlaybackUi.Start -> stringResource(resource = Res.string.details_play)
                is PlaybackUi.Resume -> stringResource(
                    resource = Res.string.details_resume_from,
                    playback.position,
                )
                PlaybackUi.Again -> stringResource(resource = Res.string.details_play_again)
            },
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun ResumeProgress(
    playback: PlaybackUi.Resume,
    onStartOver: (() -> Unit)?,
) {
    val watched = stringResource(
        resource = Res.string.details_watched_percent,
        (playback.progress * 100).roundToInt(),
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        itemVerticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.widthIn(max = 560.dp),
    ) {
        LinearProgressIndicator(
            progress = { playback.progress },
            modifier = Modifier
                .weight(weight = 1f)
                .widthIn(min = 120.dp)
                .semantics { contentDescription = watched },
        )
        Text(
            text = stringResource(
                resource = Res.string.details_minutes_left,
                playback.remainingMinutes.toInt(),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        onStartOver?.let { startOver ->
            TextButton(onClick = startOver) {
                Icon(
                    imageVector = Icons.Filled.Replay,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(width = 4.dp))
                Text(text = stringResource(resource = Res.string.details_start_over))
            }
        }
    }
}
