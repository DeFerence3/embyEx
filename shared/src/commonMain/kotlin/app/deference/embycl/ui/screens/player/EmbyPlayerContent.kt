package app.deference.embycl.ui.screens.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.layout.ContentScale
import app.deference.embycl.domain.model.EmbyPlaybackItem
import app.deference.embycl.domain.model.EmbyPlaybackRequest
import app.deference.embycl.ui.core.SettingsPreferences
import app.deference.embycl.ui.player.LocalPlayerArtwork
import app.deference.embycl.ui.player.PlayerQueue
import app.deference.embycl.ui.player.PlayerQueueEntry
import coil3.compose.AsyncImage
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.deference.embycl.domain.model.download.LocalPlaybackRequest
import app.deference.embycl.domain.model.download.LocalPlaybackResult
import app.deference.embycl.ui.core.components.DetailTopBar
import app.deference.embycl.ui.player.EmbeddedPlayerScreen
import app.deference.embycl.ui.player.PlayerSource
import app.deference.embycl.ui.screens.downloads.LocalPlaybackEffect
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.player_dismiss
import embyex.shared.generated.resources.player_error
import embyex.shared.generated.resources.player_loading
import embyex.shared.generated.resources.player_retry
import embyex.shared.generated.resources.player_save_error
import embyex.shared.generated.resources.player_save_title
import embyex.shared.generated.resources.player_saving
import embyex.shared.generated.resources.player_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun EmbyPlayerContent(
    state: EmbyPlayerState,
    localRequest: LocalPlaybackRequest?,
    onAction: (EmbyPlayerAction) -> Unit,
    onLocalProgress: (String, LocalPlaybackResult) -> Unit,
    onLocalResult: (String, LocalPlaybackResult) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        val request = state.request
        if (localRequest != null) {
            LocalPlaybackEffect(request = localRequest, onProgress = onLocalProgress, onResult = onLocalResult)
        } else if (request != null) {
            key(request.sessionId) {
                CompositionLocalProvider(
                    LocalPlayerArtwork provides { uri, artworkModifier ->
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = artworkModifier,
                        )
                    },
                ) {
                    EmbeddedPlayerScreen(
                        source = PlayerSource(
                            key = request.sessionId,
                            title = request.item.title,
                            subtitle = request.item.subtitle(),
                            uri = request.item.url,
                            positionMs = request.item.positionMs,
                            headers = mapOf("X-Emby-Token" to request.accessToken),
                            artworkUri = request.item.artworkUrl,
                            queue = request.playerQueue(
                                autoplay = SettingsPreferences.autoplayNextEnabled,
                                onAutoplayChange = SettingsPreferences::setAutoplayNext,
                            ),
                        ),
                        onProgress = { onAction(EmbyPlayerAction.Update(request = request, position = it)) },
                        onClose = { onAction(EmbyPlayerAction.Update(request = request, position = it, finished = true)) },
                        onNavigate = { position, itemId ->
                            onAction(EmbyPlayerAction.Update(request = request, position = position, finished = true, nextItemId = itemId))
                        },
                        subtitleStyle = SettingsPreferences.subtitleStyle,
                        onSubtitleStyleChange = SettingsPreferences::updateSubtitleStyle,
                    )
                }
            }
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                topBar = { DetailTopBar(title = stringResource(resource = Res.string.player_title), onBack = onBack) },
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues = padding)
                        .padding(all = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(space = 16.dp, alignment = Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (state.failed) {
                        Text(text = stringResource(resource = Res.string.player_error))
                        Button(onClick = { onAction(EmbyPlayerAction.Retry) }) {
                            Text(text = stringResource(resource = Res.string.player_retry))
                        }
                    } else if (state.preparing) {
                        CircularProgressIndicator()
                        Text(text = stringResource(resource = Res.string.player_loading))
                    }
                }
            }
        }
        if (state.saving) Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(all = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(space = 16.dp, alignment = Alignment.CenterVertically),
            ) {
                CircularProgressIndicator()
                Text(text = stringResource(resource = Res.string.player_saving))
            }
        }
        if (state.saveFailed) AlertDialog(
            onDismissRequest = { onAction(EmbyPlayerAction.DismissSaveError) },
            title = { Text(text = stringResource(resource = Res.string.player_save_title)) },
            text = { Text(text = stringResource(resource = Res.string.player_save_error)) },
            confirmButton = {
                TextButton(onClick = { onAction(EmbyPlayerAction.RetrySave) }) {
                    Text(text = stringResource(resource = Res.string.player_retry))
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(EmbyPlayerAction.DismissSaveError) }) {
                    Text(text = stringResource(resource = Res.string.player_dismiss))
                }
            },
        )
    }
}

/** "Northbound · S2 · E5" above the title for episodes; null for films. */
private fun EmbyPlaybackItem.subtitle(): String? = episodeLabel?.let { label ->
    listOfNotNull(seriesName, label).joinToString(separator = " · ")
}

private fun EmbyPlaybackItem.queueEntry(): PlayerQueueEntry = PlayerQueueEntry(
    id = id,
    label = episodeLabel.orEmpty(),
    title = title,
    durationMs = durationMs,
    artworkUri = artworkUrl,
)

/** Episodes get a queue (possibly empty after the finale); films and other videos get none. */
private fun EmbyPlaybackRequest.playerQueue(
    autoplay: Boolean,
    onAutoplayChange: (Boolean) -> Unit,
): PlayerQueue? {
    if (item.episodeLabel == null) return null
    return PlayerQueue(
        previous = items.getOrNull(index = selectedIndex - 1)?.queueEntry(),
        upcoming = items.drop(n = selectedIndex + 1).map { it.queueEntry() },
        autoplay = autoplay,
        onAutoplayChange = onAutoplayChange,
    )
}
