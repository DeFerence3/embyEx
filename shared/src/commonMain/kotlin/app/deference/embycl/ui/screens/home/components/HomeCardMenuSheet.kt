package app.deference.embycl.ui.screens.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.screens.home.model.HomeCardUi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeCardMenuSheet(
    card: HomeCardUi,
    entries: List<HomeMenuEntry>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 24.dp,
                    vertical = 8.dp,
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
            ) {
                HomeArtwork(
                    url = card.stillUrl,
                    modifier = Modifier
                        .width(width = 96.dp)
                        .aspectRatio(ratio = 16f / 9f)
                        .clip(shape = MaterialTheme.shapes.small),
                )
                Column(modifier = Modifier.weight(weight = 1f)) {
                    Text(
                        text = card.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    card.kicker()?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            entries.forEach { entry ->
                val enabled = entry.disabledReason == null
                ListItem(
                    headlineContent = { Text(text = entry.label) },
                    supportingContent = entry.disabledReason?.let { reason -> { Text(text = reason) } },
                    leadingContent = {
                        Icon(
                            imageVector = entry.icon,
                            contentDescription = null,
                        )
                    },
                    colors = if (enabled) {
                        ListItemDefaults.colors()
                    } else {
                        ListItemDefaults.colors(
                            headlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            leadingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            supportingColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    modifier = Modifier
                        .clickable(
                            enabled = enabled,
                            onClick = {
                                onDismiss()
                                entry.onClick()
                            },
                        )
                        .semantics { role = Role.Button },
                )
            }
        }
    }
}
