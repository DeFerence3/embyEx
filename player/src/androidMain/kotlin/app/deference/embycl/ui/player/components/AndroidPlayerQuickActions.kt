package app.deference.embycl.ui.player.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.player.AndroidPlayerSheet
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_aspect
import embyex.player.generated.resources.player_audio
import embyex.player.generated.resources.player_lock
import embyex.player.generated.resources.player_pip
import embyex.player.generated.resources.player_rotate
import embyex.player.generated.resources.player_speed
import embyex.player.generated.resources.player_subtitles
import org.jetbrains.compose.resources.stringResource
import app.deference.embycl.ui.player.EmbeddedPlayer
import app.deference.embycl.ui.player.PlayerAspect

@Composable
internal fun AndroidPlayerQuickActions(
    player: EmbeddedPlayer?,
    enabled: Boolean,
    onSheet: (AndroidPlayerSheet) -> Unit,
    onRotate: () -> Unit,
    onLock: () -> Unit,
    onPictureInPicture: (() -> Unit)?,
    moreOptions: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .horizontalScroll(state = rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp, alignment = Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AndroidPlayerButton(
            icon = Icons.Default.ScreenRotation,
            description = stringResource(resource = Res.string.player_rotate),
            onClick = onRotate,
            enabled = enabled,
        )
        AndroidPlayerTrackButtons(
            player = player,
            enabled = enabled,
            onSheet = onSheet,
        )
        AndroidPlayerSpeedButton(
            player = player,
            enabled = enabled,
            onSheet = onSheet,
        )
        AndroidPlayerButton(
            icon = Icons.Default.Lock,
            description = stringResource(resource = Res.string.player_lock),
            onClick = onLock,
            enabled = enabled && player != null,
        )
        AndroidPlayerDisplayButtons(
            player = player,
            enabled = enabled,
            onSheet = onSheet,
            onPictureInPicture = onPictureInPicture,
        )
        moreOptions()
    }
}

@Composable
internal fun AndroidPlayerTrackActions(
    player: EmbeddedPlayer?,
    enabled: Boolean,
    onSheet: (AndroidPlayerSheet) -> Unit,
    moreOptions: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AndroidPlayerTrackButtons(
            player = player,
            enabled = enabled,
            onSheet = onSheet,
        )
        moreOptions()
    }
}

@Composable
internal fun AndroidPlayerUtilityActions(
    player: EmbeddedPlayer?,
    enabled: Boolean,
    onSheet: (AndroidPlayerSheet) -> Unit,
    onRotate: () -> Unit,
    onLock: () -> Unit,
    onPictureInPicture: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(weight = 1f)
                .horizontalScroll(state = rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AndroidPlayerButton(
                icon = Icons.Default.Lock,
                description = stringResource(resource = Res.string.player_lock),
                onClick = onLock,
                enabled = enabled && player != null,
            )
            AndroidPlayerButton(
                icon = Icons.Default.ScreenRotation,
                description = stringResource(resource = Res.string.player_rotate),
                onClick = onRotate,
                enabled = enabled,
            )
            AndroidPlayerSpeedButton(
                player = player,
                enabled = enabled,
                onSheet = onSheet,
            )
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AndroidPlayerDisplayButtons(
                player = player,
                enabled = enabled,
                onSheet = onSheet,
                onPictureInPicture = onPictureInPicture,
            )
        }
    }
}

@Composable
private fun AndroidPlayerTrackButtons(
    player: EmbeddedPlayer?,
    enabled: Boolean,
    onSheet: (AndroidPlayerSheet) -> Unit,
) {
    if (player?.audioTracks != null) AndroidPlayerButton(
        icon = Icons.Default.Audiotrack,
        description = stringResource(resource = Res.string.player_audio),
        onClick = { onSheet(AndroidPlayerSheet.Audio) },
        enabled = enabled,
    )
    if (player?.subtitleTracks != null) AndroidPlayerButton(
        icon = Icons.Default.Subtitles,
        description = stringResource(resource = Res.string.player_subtitles),
        onClick = { onSheet(AndroidPlayerSheet.Subtitles) },
        enabled = enabled,
    )
}

@Composable
private fun AndroidPlayerSpeedButton(
    player: EmbeddedPlayer?,
    enabled: Boolean,
    onSheet: (AndroidPlayerSheet) -> Unit,
) {
    if (player?.playbackSpeed != null) AndroidPlayerButton(
        icon = Icons.Default.Speed,
        description = stringResource(resource = Res.string.player_speed),
        onClick = { onSheet(AndroidPlayerSheet.Speed) },
        enabled = enabled,
    )
}

@Composable
private fun AndroidPlayerDisplayButtons(
    player: EmbeddedPlayer?,
    enabled: Boolean,
    onSheet: (AndroidPlayerSheet) -> Unit,
    onPictureInPicture: (() -> Unit)?,
) {
    if (onPictureInPicture != null) AndroidPlayerButton(
        icon = Icons.Default.PictureInPictureAlt,
        description = stringResource(resource = Res.string.player_pip),
        onClick = onPictureInPicture,
        enabled = enabled && player != null,
    )
    if (player?.aspectRatio != null) AndroidPlayerButton(
        icon = Icons.Default.AspectRatio,
        description = stringResource(resource = Res.string.player_aspect),
        onClick = { onSheet(AndroidPlayerSheet.Aspect) },
        enabled = enabled,
    )
}

