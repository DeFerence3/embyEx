package app.deference.embycl.ui.player.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material.icons.filled.Tune
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.deference.coreui.DropdownItem
import app.deference.embycl.ui.player.EmbeddedPlayer
import app.deference.embycl.ui.player.PlayerAspectRatio
import app.deference.embycl.ui.player.PlayerMenu
import app.deference.embycl.ui.player.PlayerSubtitleStyleControls
import app.deference.embycl.ui.player.PlayerTrack
import app.deference.embycl.ui.player.PlayerTrackGroup
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_aspect
import embyex.player.generated.resources.player_aspect_value
import embyex.player.generated.resources.player_audio
import embyex.player.generated.resources.player_crop
import embyex.player.generated.resources.player_fit
import embyex.player.generated.resources.player_more
import embyex.player.generated.resources.player_no_tracks
import embyex.player.generated.resources.player_pip
import embyex.player.generated.resources.player_speed
import embyex.player.generated.resources.player_speed_normal
import embyex.player.generated.resources.player_speed_value
import embyex.player.generated.resources.player_stretch
import embyex.player.generated.resources.player_subtitle_style
import embyex.player.generated.resources.player_subtitles
import embyex.player.generated.resources.player_subtitles_off
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** Speeds offered in the menu; `<` and `>` step through the same list. */
internal val PlayerSpeeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

internal fun PlayerAspectRatio.label(): StringResource = when (this) {
    PlayerAspectRatio.FIT -> Res.string.player_fit
    PlayerAspectRatio.CROP -> Res.string.player_crop
    PlayerAspectRatio.STRETCH -> Res.string.player_stretch
}

internal fun subtitleLabel(
    track: PlayerTrack,
): String = track.labels.firstOrNull()?.value ?: track.language ?: track.id

internal fun audioLabel(
    track: PlayerTrack,
): String = track.name ?: track.labels.firstOrNull()?.value ?: track.id

@Composable
internal fun PlayerSubtitlesMenu(
    tracks: PlayerTrackGroup,
    menu: PlayerMenu?,
    onMenuChange: (PlayerMenu?) -> Unit,
    enabled: Boolean,
    dispatchAction: (() -> Unit) -> Unit,
    subtitleStyle: PlayerSubtitleStyleControls?,
) {
    PlayerTrackMenu(
        icon = Icons.Default.Subtitles,
        title = stringResource(resource = Res.string.player_subtitles),
        shortcut = "C",
        tracks = tracks,
        expanded = menu == PlayerMenu.Subtitles || (subtitleStyle != null && menu == PlayerMenu.SubtitleStyle),
        onExpandedChange = { onMenuChange(if (it) PlayerMenu.Subtitles else null) },
        enabled = enabled,
        dispatchAction = dispatchAction,
        label = ::subtitleLabel,
        offLabel = stringResource(resource = Res.string.player_subtitles_off),
        page = if (subtitleStyle != null && menu == PlayerMenu.SubtitleStyle) {
            {
                SubtitleStyleMenuPage(
                    controls = subtitleStyle,
                    onBack = { onMenuChange(PlayerMenu.Subtitles) },
                )
            }
        } else null,
        footer = if (subtitleStyle != null) {
            {
                HorizontalDivider()
                DropdownItem(
                    text = stringResource(resource = Res.string.player_subtitle_style),
                    icon = Icons.Default.Tune,
                    onClick = { onMenuChange(PlayerMenu.SubtitleStyle) },
                )
            }
        } else null,
    )
}

@Composable
private fun ColumnScope.SubtitleStyleMenuPage(
    controls: PlayerSubtitleStyleControls,
    onBack: () -> Unit,
) {
    PlayerSubtitleStylePage(
        style = controls.style,
        onStyleChange = controls.onStyleChange,
        containerColor = MenuDefaults.containerColor,
        containerShape = MaterialTheme.shapes.large,
    ) {
        DropdownItem(
            text = stringResource(resource = Res.string.player_subtitle_style),
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            onClick = onBack,
        )
        HorizontalDivider()
    }
}

@Composable
internal fun PlayerAudioMenu(
    tracks: PlayerTrackGroup,
    menu: PlayerMenu?,
    onMenuChange: (PlayerMenu?) -> Unit,
    enabled: Boolean,
    dispatchAction: (() -> Unit) -> Unit,
) {
    PlayerTrackMenu(
        icon = Icons.Default.Audiotrack,
        title = stringResource(resource = Res.string.player_audio),
        shortcut = "A",
        tracks = tracks,
        expanded = menu == PlayerMenu.Audio,
        onExpandedChange = { onMenuChange(if (it) PlayerMenu.Audio else null) },
        enabled = enabled,
        dispatchAction = dispatchAction,
        label = ::audioLabel,
    )
}

@Composable
private fun PlayerTrackMenu(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    shortcut: String,
    tracks: PlayerTrackGroup,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    enabled: Boolean,
    dispatchAction: (() -> Unit) -> Unit,
    label: (PlayerTrack) -> String,
    offLabel: String? = null,
    /** Shown instead of the track list, for example the Subtitle style page. */
    page: (@Composable ColumnScope.() -> Unit)? = null,
    /** Rows under the track list. */
    footer: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val candidates by tracks.candidates.collectAsState(initial = emptyList())
    val current by tracks.selected.collectAsState()
    PlayerMenuButton(
        icon = icon,
        description = title,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        enabled = enabled,
        shortcut = shortcut,
        selfPainted = page != null,
    ) {
        if (page != null) {
            page()
            return@PlayerMenuButton
        }
        if (offLabel != null) DropdownItem(
            text = offLabel,
            selected = current == null,
            onClick = {
                dispatchAction { tracks.select(track = null) }
                onExpandedChange(false)
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
                    onExpandedChange(false)
                },
            )
        }
        footer?.invoke(this)
    }
}

@Composable
internal fun PlayerSpeedMenu(
    player: EmbeddedPlayer,
    menu: PlayerMenu?,
    onMenuChange: (PlayerMenu?) -> Unit,
    enabled: Boolean,
    dispatchAction: (() -> Unit) -> Unit,
) {
    val speed = player.playbackSpeed ?: return
    val current by speed.valueFlow.collectAsState(initial = speed.value)
    PlayerMenuButton(
        icon = Icons.Default.Speed,
        description = stringResource(Res.string.player_speed_value, speedLabel(speed = current)),
        expanded = menu == PlayerMenu.Speed,
        onExpandedChange = { onMenuChange(if (it) PlayerMenu.Speed else null) },
        enabled = enabled,
        shortcut = "< >",
    ) {
        PlayerSpeeds.forEach { value ->
            DropdownItem(
                text = if (value == 1f) stringResource(Res.string.player_speed_normal, speedLabel(speed = value)) else speedLabel(speed = value),
                selected = current == value,
                onClick = {
                    dispatchAction { speed.set(speed = value) }
                    onMenuChange(null)
                },
            )
        }
    }
}

/**
 * Rarely used options. [onSpeed] and [onPictureInPicture] are set only when the dock is compact and those
 * buttons moved in here.
 */
@Composable
internal fun PlayerMoreMenu(
    player: EmbeddedPlayer,
    menu: PlayerMenu?,
    onMenuChange: (PlayerMenu?) -> Unit,
    enabled: Boolean,
    dispatchAction: (() -> Unit) -> Unit,
    onPictureInPicture: (() -> Unit)? = null,
    subtitleStyle: PlayerSubtitleStyleControls? = null,
) {
    val aspect = player.aspectRatio
    val speed = player.playbackSpeed
    PlayerMenuButton(
        icon = Icons.Default.MoreVert,
        description = stringResource(resource = Res.string.player_more),
        expanded = menu == PlayerMenu.More || menu == PlayerMenu.Aspect || menu == PlayerMenu.SubtitleStyleFromMore ||
            (onPictureInPicture != null && menu == PlayerMenu.Speed),
        onExpandedChange = { onMenuChange(if (it) PlayerMenu.More else null) },
        enabled = enabled && (aspect != null || onPictureInPicture != null || subtitleStyle != null),
        selfPainted = subtitleStyle != null && menu == PlayerMenu.SubtitleStyleFromMore,
    ) {
        when (menu) {
            PlayerMenu.SubtitleStyleFromMore -> if (subtitleStyle != null) SubtitleStyleMenuPage(
                controls = subtitleStyle,
                onBack = { onMenuChange(PlayerMenu.More) },
            )
            PlayerMenu.Aspect -> if (aspect != null) {
                val current by aspect.mode.collectAsState()
                DropdownItem(
                    text = stringResource(resource = Res.string.player_aspect),
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = { onMenuChange(PlayerMenu.More) },
                )
                HorizontalDivider()
                PlayerAspectRatio.entries.forEach { mode ->
                    DropdownItem(
                        text = stringResource(resource = mode.label()),
                        selected = current == mode,
                        onClick = {
                            dispatchAction { aspect.setMode(mode = mode) }
                            onMenuChange(null)
                        },
                    )
                }
            }
            PlayerMenu.Speed -> if (speed != null) {
                val current by speed.valueFlow.collectAsState(initial = speed.value)
                DropdownItem(
                    text = stringResource(resource = Res.string.player_speed),
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = { onMenuChange(PlayerMenu.More) },
                )
                HorizontalDivider()
                PlayerSpeeds.forEach { value ->
                    DropdownItem(
                        text = speedLabel(speed = value),
                        selected = current == value,
                        onClick = {
                            dispatchAction { speed.set(speed = value) }
                            onMenuChange(null)
                        },
                    )
                }
            }
            else -> {
                if (onPictureInPicture != null && speed != null) {
                    val current by speed.valueFlow.collectAsState(initial = speed.value)
                    DropdownItem(
                        text = stringResource(Res.string.player_speed_value, speedLabel(speed = current)),
                        icon = Icons.Default.Speed,
                        onClick = { onMenuChange(PlayerMenu.Speed) },
                    )
                }
                if (aspect != null) {
                    val current by aspect.mode.collectAsState()
                    DropdownItem(
                        text = stringResource(Res.string.player_aspect_value, stringResource(resource = current.label())),
                        icon = Icons.Default.AspectRatio,
                        onClick = { onMenuChange(PlayerMenu.Aspect) },
                    )
                }
                if (subtitleStyle != null) DropdownItem(
                    text = stringResource(resource = Res.string.player_subtitle_style),
                    icon = Icons.Default.Tune,
                    // With a Subtitles button the page opens there, where the track list is.
                    onClick = {
                        onMenuChange(if (player.subtitleTracks != null) PlayerMenu.SubtitleStyle else PlayerMenu.SubtitleStyleFromMore)
                    },
                )
                if (onPictureInPicture != null) DropdownItem(
                    text = stringResource(resource = Res.string.player_pip),
                    icon = Icons.Default.PictureInPictureAlt,
                    onClick = {
                        onMenuChange(null)
                        onPictureInPicture()
                    },
                )
            }
        }
    }
}
