package app.deference.embycl.ui.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewSidebar
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.player.EmbeddedPlayer
import app.deference.embycl.ui.player.PlayerIconButton
import app.deference.embycl.ui.player.PlayerMenu
import app.deference.embycl.ui.player.PlayerSubtitleStyleControls
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_back
import embyex.player.generated.resources.player_exit_fullscreen
import embyex.player.generated.resources.player_forward
import embyex.player.generated.resources.player_fullscreen
import embyex.player.generated.resources.player_hide_chapters
import embyex.player.generated.resources.player_next_episode
import embyex.player.generated.resources.player_previous_episode
import embyex.player.generated.resources.player_pip
import embyex.player.generated.resources.player_show_chapters
import org.jetbrains.compose.resources.stringResource

/**
 * The three pills under the timeline. Left: sound and language. Centre: transport. Right: playback and window.
 * Both side pills get the same width so the transport stays centred, also beside the chapters panel.
 *
 * @param panelOpen null hides the panel button (nothing to list).
 * @param compact folds the volume slider, Speed and PiP away for narrow windows; Speed and PiP move into More.
 */
@Composable
internal fun PlayerDock(
    player: EmbeddedPlayer,
    enabled: Boolean,
    menu: PlayerMenu?,
    onMenuChange: (PlayerMenu?) -> Unit,
    panelOpen: Boolean?,
    onTogglePanel: () -> Unit,
    fullscreen: Boolean,
    onToggleFullscreen: (() -> Unit)?,
    onPictureInPicture: (() -> Unit)?,
    dispatchAction: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    /** Both null hides the episode buttons (films, downloads); one null disables that button. */
    onPreviousEpisode: (() -> Unit)? = null,
    onNextEpisode: (() -> Unit)? = null,
    episodes: Boolean = false,
    subtitleStyle: PlayerSubtitleStyleControls? = null,
) {
    val state by player.state.collectAsState()
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
    ) {
        Box(modifier = Modifier.weight(weight = 1f), contentAlignment = Alignment.CenterStart) {
            PlayerPill {
                player.audioLevel?.let { level ->
                    PlayerVolume(level = level, dispatchAction = dispatchAction, compact = compact)
                    DockDivider()
                }
                player.audioTracks?.let { tracks ->
                    PlayerAudioMenu(tracks = tracks, menu = menu, onMenuChange = onMenuChange, enabled = enabled, dispatchAction = dispatchAction)
                }
                player.subtitleTracks?.let { tracks ->
                    PlayerSubtitlesMenu(
                        tracks = tracks,
                        menu = menu,
                        onMenuChange = onMenuChange,
                        enabled = enabled,
                        dispatchAction = dispatchAction,
                        subtitleStyle = subtitleStyle,
                    )
                }
            }
        }
        PlayerPill(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
            if (episodes) PlayerIconButton(
                icon = Icons.Default.SkipPrevious,
                description = stringResource(resource = Res.string.player_previous_episode),
                onClick = { onPreviousEpisode?.invoke() },
                enabled = enabled && onPreviousEpisode != null,
                shortcut = "Shift+P",
                tonal = false,
            )
            PlayerIconButton(
                icon = Icons.Default.Replay10,
                description = stringResource(resource = Res.string.player_back),
                onClick = { dispatchAction { player.skip(deltaMillis = -10_000L) } },
                enabled = enabled,
                shortcut = "J",
                tonal = false,
            )
            PlayerPlayButton(
                paused = !state.playWhenReady,
                buffering = state.isLoadingOrBuffering,
                enabled = enabled,
                onClick = { dispatchAction { player.togglePlayWhenReady() } },
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            PlayerIconButton(
                icon = Icons.Default.Forward10,
                description = stringResource(resource = Res.string.player_forward),
                onClick = { dispatchAction { player.skip(deltaMillis = 10_000L) } },
                enabled = enabled,
                shortcut = "L",
                tonal = false,
            )
            if (episodes) PlayerIconButton(
                icon = Icons.Default.SkipNext,
                description = stringResource(resource = Res.string.player_next_episode),
                onClick = { onNextEpisode?.invoke() },
                enabled = enabled && onNextEpisode != null,
                shortcut = "Shift+N",
                tonal = false,
            )
        }
        Box(modifier = Modifier.weight(weight = 1f), contentAlignment = Alignment.CenterEnd) {
            PlayerPill {
                if (!compact) PlayerSpeedMenu(player = player, menu = menu, onMenuChange = onMenuChange, enabled = enabled, dispatchAction = dispatchAction)
                PlayerMoreMenu(
                    player = player,
                    menu = menu,
                    onMenuChange = onMenuChange,
                    enabled = enabled,
                    dispatchAction = dispatchAction,
                    onPictureInPicture = if (compact) onPictureInPicture else null,
                    subtitleStyle = subtitleStyle,
                )
                DockDivider()
                if (panelOpen != null) PlayerIconButton(
                    icon = Icons.AutoMirrored.Filled.ViewSidebar,
                    description = stringResource(resource = if (panelOpen) Res.string.player_hide_chapters else Res.string.player_show_chapters),
                    onClick = onTogglePanel,
                    shortcut = "P",
                    tonal = false,
                    selected = panelOpen,
                )
                if (!compact && onPictureInPicture != null) PlayerIconButton(
                    icon = Icons.Default.PictureInPictureAlt,
                    description = stringResource(resource = Res.string.player_pip),
                    onClick = onPictureInPicture,
                    shortcut = "I",
                    tonal = false,
                )
                if (onToggleFullscreen != null) PlayerIconButton(
                    icon = if (fullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    description = stringResource(resource = if (fullscreen) Res.string.player_exit_fullscreen else Res.string.player_fullscreen),
                    onClick = onToggleFullscreen,
                    shortcut = "F",
                    tonal = false,
                )
            }
        }
    }
}

@Composable
private fun DockDivider() {
    VerticalDivider(
        modifier = Modifier
            .height(height = 24.dp)
            .padding(horizontal = 4.dp)
            .width(width = 1.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}
