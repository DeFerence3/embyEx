package app.deference.embycl.ui.screens.details.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.deference.coreui.DropDown
import app.deference.coreui.DropdownItem
import app.deference.embycl.ui.core.animateWithHover
import app.deference.embycl.ui.screens.details.model.TrackTag
import app.deference.embycl.ui.screens.details.model.TrackUi
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.details_audio
import embyex.shared.generated.resources.details_subtitles
import embyex.shared.generated.resources.details_subtitles_off
import embyex.shared.generated.resources.details_track_tag_forced
import embyex.shared.generated.resources.details_track_tag_sdh
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DetailsTrackChips(
    audioTracks: List<TrackUi>,
    subtitleTracks: List<TrackUi>,
    selectedAudioIndex: Int?,
    selectedSubtitleIndex: Int?,
    useSheet: Boolean,
    onSelectAudio: (Int) -> Unit,
    onSelectSubtitle: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (audioTracks.isEmpty() && subtitleTracks.isEmpty()) return
    val off = stringResource(resource = Res.string.details_subtitles_off)
    val audio = audioTracks.firstOrNull { it.index == selectedAudioIndex } ?: audioTracks.firstOrNull()
    val subtitle = subtitleTracks.firstOrNull { it.index == selectedSubtitleIndex }
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        if (audio != null) {
            TrackChip(
                kind = stringResource(resource = Res.string.details_audio),
                value = audio.label(),
                icon = {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = null,
                    )
                },
                options = audioTracks.map { track -> track.toOption() },
                selectedKey = audio.index,
                useSheet = useSheet,
                onSelect = { key -> key?.let(onSelectAudio) },
            )
        }
        if (subtitleTracks.isNotEmpty()) {
            TrackChip(
                kind = stringResource(resource = Res.string.details_subtitles),
                value = subtitle?.label() ?: off,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Subtitles,
                        contentDescription = null,
                    )
                },
                options = listOf(
                    TrackOption(
                        key = null,
                        title = off,
                        detail = null,
                        tag = null,
                    ),
                ) + subtitleTracks.map { track -> track.toOption() },
                selectedKey = subtitle?.index,
                useSheet = useSheet,
                onSelect = onSelectSubtitle,
            )
        }
    }
}

private data class TrackOption(
    val key: Int?,
    val title: String,
    val detail: String?,
    val tag: TrackTag?,
)

private fun TrackUi.toOption(): TrackOption = TrackOption(
    key = index,
    title = name,
    detail = detail,
    tag = tag,
)

@Composable
private fun TrackUi.label(): String = listOfNotNull(
    name,
    tag?.let { tagText(tag = it) },
).joinToString(separator = " · ")

@Composable
private fun tagText(tag: TrackTag): String = when (tag) {
    TrackTag.Forced -> stringResource(resource = Res.string.details_track_tag_forced)
    TrackTag.Sdh -> stringResource(resource = Res.string.details_track_tag_sdh)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackChip(
    kind: String,
    value: String,
    icon: @Composable () -> Unit,
    options: List<TrackOption>,
    selectedKey: Int?,
    useSheet: Boolean,
    onSelect: (Int?) -> Unit,
) {
    var open by remember { mutableStateOf(value = false) }
    val interactionSource = remember { MutableInteractionSource() }
    val interactive = options.size > 1
    Box {
        if (interactive) {
            AssistChip(
                onClick = { open = true },
                label = {
                    ChipLabel(
                        kind = kind,
                        value = value,
                    )
                },
                leadingIcon = icon,
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = null,
                    )
                },
                interactionSource = interactionSource,
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .animateWithHover(interactionSource = interactionSource),
            )
        } else {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Row(
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                ) {
                    icon()
                    ChipLabel(
                        kind = kind,
                        value = value,
                    )
                }
            }
        }
        if (!useSheet) {
            DropDown(
                expanded = open,
                onDismiss = { open = false },
            ) {
                options.forEach { option ->
                    DropdownItem(
                        text = option.menuText(),
                        selected = option.key == selectedKey,
                        onClick = {
                            open = false
                            onSelect(option.key)
                        },
                    )
                }
            }
        }
    }
    if (useSheet && open) {
        ModalBottomSheet(onDismissRequest = { open = false }) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
            ) {
                Text(
                    text = kind,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Column(
                    modifier = Modifier
                        .selectableGroup()
                        .verticalScroll(state = rememberScrollState()),
                ) {
                    options.forEach { option ->
                        val selected = option.key == selectedKey
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = {
                                        open = false
                                        onSelect(option.key)
                                    },
                                ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null,
                            )
                            Column(modifier = Modifier.weight(weight = 1f)) {
                                Text(
                                    text = option.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                option.detail?.let { detail ->
                                    Text(
                                        text = detail,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                            option.tag?.let { tag ->
                                Text(
                                    text = tagText(tag = tag),
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackOption.menuText(): String = listOfNotNull(
    title,
    detail,
    tag?.let { tagText(tag = it) },
).joinToString(separator = " · ")

@Composable
private fun ChipLabel(
    kind: String,
    value: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(space = 6.dp)) {
        Text(
            text = kind,
        )
        Text(text = value)
    }
}
