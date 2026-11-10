package app.deference.embycl.ui.player.components

import androidx.compose.runtime.Composable
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_chapter_untitled
import org.jetbrains.compose.resources.stringResource

/** A chapter's name, or "Chapter [number]" when the file leaves it blank. [number] is 1-based. */
@Composable
internal fun chapterTitle(
    name: String,
    number: Int,
): String = name.ifBlank { stringResource(Res.string.player_chapter_untitled, number) }
