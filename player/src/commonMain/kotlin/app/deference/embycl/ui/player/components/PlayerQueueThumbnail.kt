package app.deference.embycl.ui.player.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import app.deference.embycl.ui.player.LocalPlayerArtwork

/** 16:9 artwork tile for a queue entry; a plain tile when there is no image or no loader. Decorative. */
@Composable
internal fun PlayerQueueThumbnail(
    uri: String?,
    modifier: Modifier = Modifier,
) {
    val artwork = LocalPlayerArtwork.current
    Box(
        modifier = modifier
            .aspectRatio(ratio = 16f / 9f)
            .clip(shape = MaterialTheme.shapes.small)
            .background(color = MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        if (uri != null && artwork != null) artwork(uri, Modifier.fillMaxSize())
    }
}
