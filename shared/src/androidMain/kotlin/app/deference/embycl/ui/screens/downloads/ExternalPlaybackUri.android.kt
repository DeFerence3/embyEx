package app.deference.embycl.ui.screens.downloads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

/** Resolve only on a user-requested handoff; the provider grants access to this file alone. */
@Composable
internal fun rememberExternalPlaybackUri(
    absolutePath: String,
): () -> String {
    val context = LocalContext.current
    return remember(key1 = context, key2 = absolutePath) {
        {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.offline-media",
                File(absolutePath),
            ).toString()
        }
    }
}
