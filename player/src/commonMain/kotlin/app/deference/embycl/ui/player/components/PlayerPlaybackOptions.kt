package app.deference.embycl.ui.player.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.deference.coreui.DropDown
import app.deference.coreui.DropdownItem
import app.deference.embycl.ui.player.PlayerIconButton
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_aspect
import embyex.player.generated.resources.player_crop
import embyex.player.generated.resources.player_fit
import embyex.player.generated.resources.player_more
import embyex.player.generated.resources.player_speed
import embyex.player.generated.resources.player_stretch
import org.jetbrains.compose.resources.stringResource
import app.deference.embycl.ui.player.EmbeddedPlayer
import app.deference.embycl.ui.player.PlayerAspectRatio
import app.deference.embycl.ui.player.PlayerAspect

@Composable
internal fun PlayerPlaybackOptions(
    player: EmbeddedPlayer,
    enabled: Boolean,
    onMenuExpandedChange: (Boolean) -> Unit,
    dispatchAction: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    val speed = player.playbackSpeed
    val aspect = player.aspectRatio
    var expanded by remember(key1 = player) { mutableStateOf(value = false) }
    var page by remember(key1 = player) { mutableStateOf(value = PlayerOptionsPage.Options) }
    val close = { expanded = false; onMenuExpandedChange(false) }
    DisposableEffect(key1 = player) {
        onDispose { onMenuExpandedChange(false) }
    }
    LaunchedEffect(key1 = enabled) {
        if (!enabled) close()
    }
    Box(modifier = modifier) {
        PlayerIconButton(
            icon = Icons.Default.MoreVert,
            description = stringResource(resource = Res.string.player_more),
            enabled = enabled && (speed != null || aspect != null),
            onClick = {
                page = PlayerOptionsPage.Options
                expanded = true
                onMenuExpandedChange(true)
            },
        )
        DropDown(
            expanded = expanded,
            modifier = Modifier.widthIn(min = 240.dp, max = 360.dp),
            onDismiss = close,
        ) {
            if (page != PlayerOptionsPage.Options) {
                DropdownItem(
                    text = stringResource(resource = Res.string.player_more),
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    onClick = { page = PlayerOptionsPage.Options },
                )
                HorizontalDivider()
            }
            when (page) {
                PlayerOptionsPage.Options -> {
                    if (speed != null) DropdownItem(
                        text = stringResource(resource = Res.string.player_speed),
                        icon = Icons.Default.Speed,
                        onClick = { page = PlayerOptionsPage.Speed },
                    )
                    if (aspect != null) DropdownItem(
                        text = stringResource(resource = Res.string.player_aspect),
                        icon = Icons.Default.AspectRatio,
                        onClick = { page = PlayerOptionsPage.Aspect },
                    )
                }
                PlayerOptionsPage.Speed -> if (speed != null) {
                    val current by speed.valueFlow.collectAsState(initial = speed.value)
                    listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f).forEach { value ->
                        DropdownItem(
                            text = "${value}×",
                            selected = current == value,
                            onClick = {
                                dispatchAction { speed.set(speed = value) }
                                close()
                            },
                        )
                    }
                }
                PlayerOptionsPage.Aspect -> if (aspect != null) {
                    val current by aspect.mode.collectAsState()
                    PlayerAspectRatio.entries.forEach { mode ->
                        DropdownItem(
                            text = stringResource(
                                resource = when (mode) {
                                    PlayerAspectRatio.FIT -> Res.string.player_fit
                                    PlayerAspectRatio.CROP -> Res.string.player_crop
                                    PlayerAspectRatio.STRETCH -> Res.string.player_stretch
                                },
                            ),
                            selected = current == mode,
                            onClick = {
                                dispatchAction { aspect.setMode(mode = mode) }
                                close()
                            },
                        )
                    }
                }
            }
        }
    }
}
