package app.deference.embycl.ui.screens.downloads.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.screens.downloads.DownloadFilter
import app.deference.embycl.ui.screens.downloads.DownloadListItem
import app.deference.embycl.ui.screens.downloads.DownloadsAction

@Composable
internal fun DownloadSearchFilter(
    query: String,
    filter: DownloadFilter,
    items: List<DownloadListItem>,
    localPlaybackEnabled: Boolean,
    onAction: (DownloadsAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = {
                onAction(
                    DownloadsAction.QueryChanged(
                        query = it,
                    ),
                )
            },
            label = {
                Text(
                    text = "Search downloads",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            onAction(
                                DownloadsAction.QueryChanged(
                                    query = "",
                                ),
                            )
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear download search",
                        )
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DownloadFilter.entries.forEach { itemFilter ->
                FilterChip(
                    selected = filter == itemFilter,
                    onClick = {
                        onAction(
                            DownloadsAction.FilterChanged(
                                filter = itemFilter,
                            ),
                        )
                    },
                    label = {
                        val baseLabel = if (!localPlaybackEnabled && itemFilter == DownloadFilter.READY) {
                            "Downloaded"
                        } else {
                            itemFilter.label
                        }
                        val count = items.count {
                            itemFilter.includes(
                                item = it,
                            )
                        }
                        Text(
                            text = "$baseLabel ($count)",
                        )
                    },
                )
            }
        }
    }
}
