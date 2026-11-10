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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Segment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_chapter_row
import embyex.player.generated.resources.player_chapter_row_playing
import embyex.player.generated.resources.player_chapters
import org.jetbrains.compose.resources.stringResource

/** Chapters of the open media; the playing one is marked by fill, weight and icon, not colour alone. */
@Composable
internal fun PlayerChaptersPanel(
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
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Column(modifier = Modifier.padding(all = 12.dp)) {
            Row(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
            ) {
                Icon(imageVector = Icons.AutoMirrored.Filled.Segment, contentDescription = null)
                Text(text = stringResource(resource = Res.string.player_chapters), style = MaterialTheme.typography.titleSmall)
            }
            LazyColumn(
                state = list,
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
