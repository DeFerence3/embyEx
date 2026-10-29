package app.deference.embycl.ui.screens.downloads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.io.File

@Composable
internal fun rememberExternalPlaybackUri(
    absolutePath: String,
): () -> String = remember(key1 = absolutePath) {
    { File(absolutePath).toURI().toASCIIString() }
}
