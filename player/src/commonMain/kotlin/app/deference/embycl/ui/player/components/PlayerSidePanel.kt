package app.deference.embycl.ui.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.automirrored.filled.Segment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.player.PlayerChapter
import app.deference.embycl.ui.player.PlayerPanelTab
import app.deference.embycl.ui.player.PlayerQueueEntry
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_autoplay
import embyex.player.generated.resources.player_chapter_row
import embyex.player.generated.resources.player_chapter_row_playing
import embyex.player.generated.resources.player_chapters
import embyex.player.generated.resources.player_minutes
import embyex.player.generated.resources.player_next_tag
import embyex.player.generated.resources.player_queue_empty
import embyex.player.generated.resources.player_queue_entry
import embyex.player.generated.resources.player_queue_entry_next
import embyex.player.generated.resources.player_up_next
import org.jetbrains.compose.resources.stringResource

/**
 * Chapters of the open media and the episodes after it. Tabs show only when both exist.
 *
 * @param upcoming null for films and downloads (no Up next tab); empty for the last episode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PlayerSidePanel(
    tab: PlayerPanelTab,
    onTabChange: (PlayerPanelTab) -> Unit,
    chapters: List<PlayerChapter>,
    currentChapter: Int,
    onChapterClick: (PlayerChapter) -> Unit,
    upcoming: List<PlayerQueueEntry>?,
    autoplay: Boolean,
    onAutoplayChange: (Boolean) -> Unit,
    onEntryClick: (PlayerQueueEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    val showChapters = chapters.isNotEmpty()
    val showQueue = upcoming != null
    val shown = when {
        tab == PlayerPanelTab.UpNext && showQueue -> PlayerPanelTab.UpNext
        showChapters -> PlayerPanelTab.Chapters
        else -> PlayerPanelTab.UpNext
    }
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(
            modifier = Modifier.padding(all = 12.dp),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            if (showChapters && showQueue) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    PlayerPanelTab.entries.forEachIndexed { index, entry ->
                        SegmentedButton(
                            selected = shown == entry,
                            onClick = { onTabChange(entry) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = PlayerPanelTab.entries.size),
                            icon = {
                                Icon(
                                    imageVector = if (entry == PlayerPanelTab.Chapters) Icons.AutoMirrored.Filled.Segment else Icons.AutoMirrored.Filled.PlaylistPlay,
                                    contentDescription = null,
                                    modifier = Modifier.size(size = 18.dp),
                                )
                            },
                        ) {
                            Text(text = stringResource(resource = if (entry == PlayerPanelTab.Chapters) Res.string.player_chapters else Res.string.player_up_next))
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                ) {
                    Icon(
                        imageVector = if (shown == PlayerPanelTab.Chapters) Icons.AutoMirrored.Filled.Segment else Icons.AutoMirrored.Filled.PlaylistPlay,
                        contentDescription = null,
                    )
                    Text(
                        text = stringResource(resource = if (shown == PlayerPanelTab.Chapters) Res.string.player_chapters else Res.string.player_up_next),
                        style = MaterialTheme.typography.titleSmall,
                    )
                }
            }
            when (shown) {
                PlayerPanelTab.Chapters -> ChapterList(
                    chapters = chapters,
                    currentIndex = currentChapter,
                    onChapterClick = onChapterClick,
                    modifier = Modifier.weight(weight = 1f, fill = false),
                )
                PlayerPanelTab.UpNext -> {
                    val entries = upcoming.orEmpty()
                    if (entries.isEmpty()) {
                        Text(
                            text = stringResource(resource = Res.string.player_queue_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(all = 12.dp),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(weight = 1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(space = 4.dp),
                        ) {
                            itemsIndexed(items = entries, key = { _, entry -> entry.id }) { index, entry ->
                                QueueRow(entry = entry, next = index == 0, onClick = { onEntryClick(entry) })
                            }
                        }
                        AutoplaySwitch(checked = autoplay, onCheckedChange = onAutoplayChange)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterList(
    chapters: List<PlayerChapter>,
    currentIndex: Int,
    onChapterClick: (PlayerChapter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val list = rememberLazyListState()
    LaunchedEffect(key1 = currentIndex) {
        // Keep the playing chapter in view as playback moves on.
        if (currentIndex >= 0) list.animateScrollToItem(index = (currentIndex - 2).coerceAtLeast(minimumValue = 0))
    }
    LazyColumn(
        state = list,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(space = 2.dp),
    ) {
        itemsIndexed(items = chapters, key = { index, chapter -> "$index:${chapter.startMillis}" }) { index, chapter ->
            ChapterRow(
                number = index + 1,
                chapter = chapter,
                current = index == currentIndex,
                onClick = { onChapterClick(chapter) },
            )
        }
    }
}

@Composable
private fun ChapterRow(
    number: Int,
    chapter: PlayerChapter,
    current: Boolean,
    onClick: () -> Unit,
) {
    val start = playerTime(milliseconds = chapter.startMillis)
    val description = stringResource(
        if (current) Res.string.player_chapter_row_playing else Res.string.player_chapter_row,
        number,
        chapter.name,
        start,
    )
    Surface(
        shape = CircleShape,
        color = if (current) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        contentColor = if (current) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
                selected = current
            },
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = 48.dp)
                .padding(start = 10.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = if (current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = if (current) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(size = 28.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (current) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(size = 18.dp))
                    } else {
                        Text(text = number.toString(), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Text(
                text = chapter.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (current) FontWeight.SemiBold else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(weight = 1f),
            )
            Text(text = start, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun QueueRow(
    entry: PlayerQueueEntry,
    next: Boolean,
    onClick: () -> Unit,
) {
    val description = stringResource(if (next) Res.string.player_queue_entry_next else Res.string.player_queue_entry, entry.label, entry.title)
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (next) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = description
                role = Role.Button
            },
    ) {
        Row(
            modifier = Modifier.padding(all = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            PlayerQueueThumbnail(uri = entry.artworkUri, modifier = Modifier.width(width = 96.dp))
            Column(modifier = Modifier.weight(weight = 1f)) {
                Text(
                    text = entry.durationMs?.let { "${entry.label} · " + stringResource(Res.string.player_minutes, (it / 60_000L).toInt()) } ?: entry.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (next) Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(top = 2.dp),
                ) {
                    Text(
                        text = stringResource(resource = Res.string.player_next_tag),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AutoplaySwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(resource = Res.string.player_autoplay),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(weight = 1f),
        )
        Switch(checked = checked, onCheckedChange = null)
    }
}
