package app.deference.embycl.ui.player.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Segment
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.player.PlayerIconButton
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_chapter_now
import embyex.player.generated.resources.player_close
import org.jetbrains.compose.resources.stringResource

/**
 * Back button and title straight on the surface. [subtitle] is the line above the title (series · S · E).
 * [chapter] shows at the end when the chapters panel is closed; null hides it.
 */
@Composable
internal fun PlayerHeading(
    title: String,
    subtitle: String?,
    chapter: Pair<Int, String>?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    /** Shown last, for example the portrait "Show chapters" button. */
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
    ) {
        PlayerIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            description = stringResource(resource = Res.string.player_close),
            onClick = onClose,
            shortcut = "Esc",
        )
        Column(modifier = Modifier.weight(weight = 1f)) {
            if (subtitle != null) Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = LocalContentColor.current.copy(alpha = 0.82f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (chapter != null) Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Segment,
                contentDescription = null,
                modifier = Modifier.size(size = 20.dp),
            )
            Text(
                text = stringResource(Res.string.player_chapter_now, chapter.first, chapterTitle(name = chapter.second, number = chapter.first)),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        trailing?.invoke()
    }
}
