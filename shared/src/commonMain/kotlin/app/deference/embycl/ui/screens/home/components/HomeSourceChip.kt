package app.deference.embycl.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.core.focusRing
import app.deference.embycl.ui.screens.home.model.HomeSourceUi
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.home_source_connected
import embyex.shared.generated.resources.home_source_connecting
import embyex.shared.generated.resources.home_source_device
import embyex.shared.generated.resources.home_source_label
import embyex.shared.generated.resources.home_source_not_responding
import embyex.shared.generated.resources.home_source_offline
import embyex.shared.generated.resources.home_source_offline_name
import embyex.shared.generated.resources.home_source_reconnecting
import embyex.shared.generated.resources.home_source_signed_out
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HomeSourceChip(
    source: HomeSourceUi,
    serverName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = when (source) {
        HomeSourceUi.Offline -> stringResource(resource = Res.string.home_source_offline_name)
        HomeSourceUi.ThisDevice -> stringResource(resource = Res.string.home_source_device)
        else -> serverName
    }
    val status = when (source) {
        HomeSourceUi.Connected -> stringResource(resource = Res.string.home_source_connected)
        HomeSourceUi.Connecting -> stringResource(resource = Res.string.home_source_connecting)
        HomeSourceUi.Reconnecting -> stringResource(resource = Res.string.home_source_reconnecting)
        HomeSourceUi.Offline -> stringResource(resource = Res.string.home_source_offline)
        HomeSourceUi.NotResponding -> stringResource(resource = Res.string.home_source_not_responding)
        HomeSourceUi.SignedOut -> stringResource(resource = Res.string.home_source_signed_out)
        HomeSourceUi.ThisDevice -> null
    }
    val colors = source.chipColors()
    val label = stringResource(
        resource = Res.string.home_source_label,
        name,
        status ?: name,
    )
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.small,
        color = colors.first,
        contentColor = colors.second,
        modifier = modifier
            .focusRing(shape = MaterialTheme.shapes.small)
            .heightIn(min = 32.dp)
            .semantics { contentDescription = label },
    ) {
        Row(
            modifier = Modifier.padding(
                start = 8.dp,
                end = 12.dp,
                top = 6.dp,
                bottom = 6.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(space = 6.dp),
        ) {
            Icon(
                imageVector = source.icon(),
                contentDescription = null,
                modifier = Modifier.size(size = 18.dp),
            )
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            status?.let {
                Text(
                    text = "· $it",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                )
            }
        }
    }
}

internal fun HomeSourceUi.icon(): ImageVector = when (this) {
    HomeSourceUi.Connected -> Icons.Outlined.CloudDone
    HomeSourceUi.Connecting -> Icons.Outlined.Cloud
    HomeSourceUi.Reconnecting -> Icons.Outlined.Sync
    HomeSourceUi.Offline,
    HomeSourceUi.NotResponding -> Icons.Outlined.CloudOff
    HomeSourceUi.SignedOut -> Icons.AutoMirrored.Outlined.Login
    HomeSourceUi.ThisDevice -> Icons.Outlined.Smartphone
}

@Composable
private fun HomeSourceUi.chipColors(): Pair<Color, Color> = when (this) {
    HomeSourceUi.Connected,
    HomeSourceUi.Connecting -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    HomeSourceUi.Reconnecting -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    HomeSourceUi.Offline,
    HomeSourceUi.NotResponding -> MaterialTheme.colorScheme.surfaceContainerHighest to MaterialTheme.colorScheme.onSurface
    HomeSourceUi.SignedOut,
    HomeSourceUi.ThisDevice -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
}
