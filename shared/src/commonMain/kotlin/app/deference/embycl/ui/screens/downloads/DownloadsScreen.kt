package app.deference.embycl.ui.screens.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.screens.downloads.components.DownloadCard
import app.deference.embycl.ui.screens.downloads.components.DownloadMessage
import app.deference.embycl.ui.screens.downloads.components.DownloadSearchFilter
import app.deference.embycl.ui.screens.downloads.components.DownloadsOfflineBanner
import app.deference.embycl.ui.screens.downloads.components.RemoveDownloadDialog

@Composable
fun DownloadsContent(
    state: DownloadsState,
    onAction: (DownloadsAction) -> Unit,
    modifier: Modifier = Modifier,
    bottomSpacing: Dp = 0.dp,
    showOfflineBanner: Boolean = true,
) {
    var removalId by rememberSaveable { mutableStateOf<String?>(null) }
    val removal = state.items.firstOrNull { it.id == removalId }
    if (removal != null) {
        RemoveDownloadDialog(
            item = removal,
            onConfirm = {
                removalId = null
                onAction(
                    DownloadsAction.Remove(
                        id = removal.id,
                    ),
                )
            },
            onDismiss = {
                removalId = null
            },
        )
    }

    val visible = remember(
        key1 = state.items,
        key2 = state.query,
        key3 = state.filter,
    ) {
        val query = state.query.trim()
        state.items.filter {
            state.filter.includes(
                item = it,
            ) && (it.title.contains(
                other = query,
                ignoreCase = true,
            ) || it.subtitle.contains(
                other = query,
                ignoreCase = true,
            ))
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .widthIn(max = 960.dp)
                .fillMaxSize()
                .testTag("downloads-list"),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 20.dp,
                bottom = bottomSpacing + 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(
                key = "heading",
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val headingStyle = if (LocalDensity.current.fontScale > 1.3f) {
                        MaterialTheme.typography.titleLarge
                    } else {
                        MaterialTheme.typography.headlineLarge
                    }
                    Text(
                        text = "Downloads",
                        style = headingStyle,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .semantics {
                                heading()
                            },
                    )
                    Text(
                        text = "Your collection, ready when you are.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "Saved in EmbyEx · Hidden from Gallery",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (state.isOffline && showOfflineBanner) {
                item(
                    key = "offline",
                ) {
                    DownloadsOfflineBanner(
                        onReconnect = {
                            onAction(DownloadsAction.Reconnect)
                        },
                    )
                }
            }

            if (state.items.isNotEmpty()) {
                item(
                    key = "search-and-filters",
                ) {
                    DownloadSearchFilter(
                        query = state.query,
                        filter = state.filter,
                        items = state.items,
                        localPlaybackEnabled = state.localPlaybackEnabled,
                        onAction = onAction,
                    )
                }
            }

            if (state.isLoading) {
                item(
                    key = "loading",
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Loading downloads…",
                        )
                    }
                }
            }

            if (state.loadFailed) {
                item(
                    key = "error",
                ) {
                    DownloadMessage(
                        title = "Couldn't load downloads",
                        description = "Try opening your saved collection again.",
                        action = "Try again",
                        onClick = {
                            onAction(DownloadsAction.Reload)
                        },
                    )
                }
            }

            if (!state.isLoading && !state.loadFailed && state.items.isEmpty()) {
                item(
                    key = "empty",
                ) {
                    val actionLabel = if (state.isOffline) {
                        "Connect to server"
                    } else {
                        "Browse library"
                    }
                    DownloadMessage(
                        title = "Take your favorites with you",
                        description = "Download a movie or episode while connected. Watch it here, even when your server is unavailable.",
                        action = actionLabel,
                        onClick = {
                            onAction(
                                if (state.isOffline) {
                                    DownloadsAction.Reconnect
                                } else {
                                    DownloadsAction.BrowseLibrary
                                },
                            )
                        },
                    )
                }
            } else if (state.items.isNotEmpty() && visible.isEmpty()) {
                item(
                    key = "no-matches",
                ) {
                    DownloadMessage(
                        title = "No matching downloads",
                        description = "Try another title or show all downloads.",
                        action = "Clear filters",
                        onClick = {
                            onAction(
                                DownloadsAction.QueryChanged(
                                    query = "",
                                ),
                            )
                            onAction(
                                DownloadsAction.FilterChanged(
                                    filter = DownloadFilter.ALL,
                                ),
                            )
                        },
                    )
                }
            }

            items(
                items = visible,
                key = {
                    "download:${it.id}"
                },
            ) { item ->
                DownloadCard(
                    item = item,
                    onAction = onAction,
                    onRemove = {
                        removalId = item.id
                    },
                    playbackEnabled = state.localPlaybackEnabled,
                    busy = item.id in state.busyIds || state.playingId == item.id,
                    playing = state.playingId == item.id,
                )
            }
        }
    }
}
