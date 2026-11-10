package app.deference.embycl.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import app.deference.coreui.keyboard.KeyShortcut
import app.deference.coreui.keyboard.RegisterShortcuts
import app.deference.embycl.ui.player.components.PlayerSpeeds
import app.deference.embycl.ui.player.components.subtitleLabel
import kotlin.math.abs
import kotlin.math.roundToInt

/** Window-level actions the shortcuts can trigger; null where the platform or mode doesn't offer them. */
internal class PlayerShortcutActions(
    val onEscape: () -> Unit,
    val onOpenMenu: (PlayerMenu) -> Unit,
    val onTogglePanel: (() -> Unit)?,
    val onToggleFullscreen: (() -> Unit)?,
    val onPictureInPicture: (() -> Unit)?,
    val onRevealControls: () -> Unit,
)

/**
 * The player's default YouTube-style shortcuts, registered while the player shows.
 *
 * A registered shortcut always consumes its key, so keys a focused control needs (Space, arrows) are only
 * registered while [focusFree]; letters and Esc work everywhere except inside an open menu ([enabled] false).
 * Every action reports a [PlayerFeedback] through [onFeedback] so the user sees the key was heard.
 */
@Composable
internal fun RegisterPlayerShortcuts(
    player: EmbeddedPlayer?,
    enabled: Boolean,
    focusFree: Boolean,
    controlsHidden: Boolean,
    actions: PlayerShortcutActions,
    onTriggered: () -> Unit,
    onFeedback: (PlayerFeedback) -> Unit,
    dispatchAction: (() -> Unit) -> Unit,
) {
    val currentActions by rememberUpdatedState(newValue = actions)
    val currentTriggered by rememberUpdatedState(newValue = onTriggered)
    val currentFeedback by rememberUpdatedState(newValue = onFeedback)
    val currentDispatch by rememberUpdatedState(newValue = dispatchAction)
    // The track C turns back on; survives turning subtitles off and on again.
    var lastSubtitle by remember(key1 = player) { mutableStateOf<PlayerTrack?>(value = null) }
    val panelAvailable = actions.onTogglePanel != null
    val fullscreenAvailable = actions.onToggleFullscreen != null
    val pipAvailable = actions.onPictureInPicture != null

    val shortcuts = remember(player, enabled, focusFree, controlsHidden, panelAvailable, fullscreenAvailable, pipAvailable) {
        val list = mutableListOf<KeyShortcut>()
        fun add(key: Key, description: String, ctrl: Boolean = false, shift: Boolean = false, action: () -> Unit) {
            list += KeyShortcut(
                key = key,
                description = description,
                ctrl = ctrl,
                shift = shift,
                onAction = {
                    currentTriggered()
                    action()
                },
            )
        }
        if (enabled) {
            add(key = Key.Escape, description = "Back one level") { currentActions.onEscape() }
            if (controlsHidden) add(key = Key.Tab, description = "Show controls") { currentActions.onRevealControls() }
            if (fullscreenAvailable) add(key = Key.F, description = "Full screen") { currentActions.onToggleFullscreen?.invoke() }
            if (pipAvailable) add(key = Key.I, description = "Picture in picture") { currentActions.onPictureInPicture?.invoke() }
            if (panelAvailable) add(key = Key.P, description = "Chapters panel") { currentActions.onTogglePanel?.invoke() }
        }
        val active = player
        if (enabled && active != null) {
            val dispatch: (() -> Unit) -> Unit = { action -> currentDispatch(action) }
            fun seek(seconds: Int) = dispatch {
                active.skip(deltaMillis = seconds * 1_000L)
                currentFeedback(PlayerFeedback.Seeked(seconds = seconds))
            }
            val togglePlay: () -> Unit = {
                dispatch {
                    val playing = active.state.value.playWhenReady
                    active.togglePlayWhenReady()
                    currentFeedback(if (playing) PlayerFeedback.Paused else PlayerFeedback.Played)
                }
            }
            add(key = Key.K, description = "Play or pause", action = togglePlay)
            add(key = Key.MediaPlayPause, description = "Play or pause", action = togglePlay)
            add(key = Key.J, description = "Back 10 seconds") { seek(seconds = -10) }
            add(key = Key.L, description = "Forward 10 seconds") { seek(seconds = 10) }
            add(key = Key.M, description = "Mute") {
                val level = active.audioLevel
                if (level == null) currentFeedback(PlayerFeedback.Unavailable) else dispatch {
                    val mute = !level.muted.value
                    level.setMuted(muted = mute)
                    currentFeedback(if (mute) PlayerFeedback.Muted else PlayerFeedback.Volume(percent = (level.volume.value * 100).roundToInt()))
                }
            }
            add(key = Key.C, description = "Subtitles on or off") {
                val tracks = active.subtitleTracks
                if (tracks == null) currentFeedback(PlayerFeedback.Unavailable) else dispatch {
                    val current = tracks.selected.value
                    if (current != null) {
                        lastSubtitle = current
                        tracks.select(track = null)
                        currentFeedback(PlayerFeedback.SubtitlesOff)
                    } else {
                        val target = lastSubtitle
                        if (target == null) {
                            // Nothing to return to yet: open the menu so the user picks one.
                            currentActions.onOpenMenu(PlayerMenu.Subtitles)
                        } else {
                            tracks.select(track = target)
                            currentFeedback(PlayerFeedback.SubtitlesOn(label = subtitleLabel(track = target)))
                        }
                    }
                }
            }
            add(key = Key.S, description = "Subtitles menu") {
                if (active.subtitleTracks == null) currentFeedback(PlayerFeedback.Unavailable) else currentActions.onOpenMenu(PlayerMenu.Subtitles)
            }
            add(key = Key.A, description = "Audio menu") {
                if (active.audioTracks == null) currentFeedback(PlayerFeedback.Unavailable) else currentActions.onOpenMenu(PlayerMenu.Audio)
            }
            add(key = Key.Z, description = "Cycle aspect ratio") {
                val aspect = active.aspectRatio
                if (aspect == null) currentFeedback(PlayerFeedback.Unavailable) else dispatch {
                    val modes = PlayerAspectRatio.entries
                    val next = modes[(modes.indexOf(aspect.mode.value) + 1) % modes.size]
                    aspect.setMode(mode = next)
                    currentFeedback(PlayerFeedback.Aspect(mode = next))
                }
            }
            add(key = Key.Comma, description = "Slower", shift = true) { stepSpeed(player = active, step = -1, dispatch = dispatch, onFeedback = currentFeedback) }
            add(key = Key.Period, description = "Faster", shift = true) { stepSpeed(player = active, step = 1, dispatch = dispatch, onFeedback = currentFeedback) }
            add(key = Key.MoveHome, description = "Jump to start") { dispatch { active.seekTo(positionMillis = 0L); currentFeedback(PlayerFeedback.Jumped(percent = 0, positionMillis = 0L)) } }
            listOf(Key.Zero, Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine).forEachIndexed { digit, key ->
                add(key = key, description = "Jump to ${digit * 10} percent") {
                    val duration = active.mediaProperties.value?.durationMillis ?: 0L
                    if (duration <= 0L) currentFeedback(PlayerFeedback.Unavailable) else dispatch {
                        val target = duration * digit / 10
                        active.seekTo(positionMillis = target)
                        currentFeedback(PlayerFeedback.Jumped(percent = digit * 10, positionMillis = target))
                    }
                }
            }
            add(key = Key.DirectionLeft, description = "Previous chapter", ctrl = true) { jumpChapter(player = active, forward = false, dispatch = dispatch, onFeedback = currentFeedback) }
            add(key = Key.DirectionRight, description = "Next chapter", ctrl = true) { jumpChapter(player = active, forward = true, dispatch = dispatch, onFeedback = currentFeedback) }
            if (focusFree) {
                // A focused button or slider owns these keys.
                add(key = Key.Spacebar, description = "Play or pause", action = togglePlay)
                add(key = Key.DirectionLeft, description = "Back 5 seconds") { seek(seconds = -5) }
                add(key = Key.DirectionRight, description = "Forward 5 seconds") { seek(seconds = 5) }
                add(key = Key.DirectionUp, description = "Volume up") { stepVolume(player = active, step = 0.05f, dispatch = dispatch, onFeedback = currentFeedback) }
                add(key = Key.DirectionDown, description = "Volume down") { stepVolume(player = active, step = -0.05f, dispatch = dispatch, onFeedback = currentFeedback) }
            }
        }
        list.toList()
    }
    RegisterShortcuts(
        shortcuts = shortcuts,
        key = "Player Shortcuts",
    )
}

private fun stepVolume(
    player: EmbeddedPlayer,
    step: Float,
    dispatch: (() -> Unit) -> Unit,
    onFeedback: (PlayerFeedback) -> Unit,
) {
    val level = player.audioLevel
    if (level == null) {
        onFeedback(PlayerFeedback.Unavailable)
        return
    }
    dispatch {
        val target = (level.volume.value + step).coerceIn(minimumValue = 0f, maximumValue = 1f)
        if (level.muted.value && step > 0f) level.setMuted(muted = false)
        level.setVolume(volume = target)
        onFeedback(PlayerFeedback.Volume(percent = (target * 100).roundToInt()))
    }
}

private fun stepSpeed(
    player: EmbeddedPlayer,
    step: Int,
    dispatch: (() -> Unit) -> Unit,
    onFeedback: (PlayerFeedback) -> Unit,
) {
    val speed = player.playbackSpeed
    if (speed == null) {
        onFeedback(PlayerFeedback.Unavailable)
        return
    }
    dispatch {
        val nearest = PlayerSpeeds.indices.minBy { abs(PlayerSpeeds[it] - speed.value) }
        val target = PlayerSpeeds[(nearest + step).coerceIn(minimumValue = 0, maximumValue = PlayerSpeeds.lastIndex)]
        speed.set(speed = target)
        onFeedback(PlayerFeedback.Speed(speed = target))
    }
}

/** Backward goes to the start of the current chapter, or the previous one within its first 3 seconds. */
private fun jumpChapter(
    player: EmbeddedPlayer,
    forward: Boolean,
    dispatch: (() -> Unit) -> Unit,
    onFeedback: (PlayerFeedback) -> Unit,
) {
    val chapters = player.chapters?.value.orEmpty()
    if (chapters.isEmpty()) {
        onFeedback(PlayerFeedback.Unavailable)
        return
    }
    val position = player.currentPositionMillis.value
    val current = chapters.indexOfLast { it.startMillis <= position }.coerceAtLeast(minimumValue = 0)
    val target = when {
        forward -> current + 1
        position - chapters[current].startMillis > 3_000L -> current
        else -> current - 1
    }
    val chapter = chapters.getOrNull(index = target)
    if (chapter == null) {
        onFeedback(PlayerFeedback.Unavailable)
        return
    }
    dispatch {
        player.seekTo(positionMillis = chapter.startMillis)
        onFeedback(PlayerFeedback.Chapter(index = target + 1, name = chapter.name))
    }
}
