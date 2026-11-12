package app.deference.embycl.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.screens.home.model.HomeSourceUi
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.home_status_address
import embyex.shared.generated.resources.home_status_close
import embyex.shared.generated.resources.home_status_connected_title
import embyex.shared.generated.resources.home_status_device_body
import embyex.shared.generated.resources.home_status_device_title
import embyex.shared.generated.resources.home_status_not_responding_body
import embyex.shared.generated.resources.home_status_not_responding_title
import embyex.shared.generated.resources.home_status_offline_body
import embyex.shared.generated.resources.home_status_offline_title
import embyex.shared.generated.resources.home_status_open_downloads
import embyex.shared.generated.resources.home_status_reconnecting_body
import embyex.shared.generated.resources.home_status_reconnecting_title
import embyex.shared.generated.resources.home_status_refresh
import embyex.shared.generated.resources.home_status_server
import embyex.shared.generated.resources.home_status_sign_in
import embyex.shared.generated.resources.home_status_signed_out_body
import embyex.shared.generated.resources.home_status_signed_out_title
import embyex.shared.generated.resources.home_status_try_again
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeSourceStatus(
    source: HomeSourceUi,
    serverName: String,
    serverAddress: String,
    layout: HomeLayout,
    onRefresh: () -> Unit,
    onOpenDownloads: () -> Unit,
    onSignIn: () -> Unit,
    onDismiss: () -> Unit,
) {
    val refresh = {
        onRefresh()
        onDismiss()
    }
    val downloads = {
        onDismiss()
        onOpenDownloads()
    }
    val signIn = {
        onDismiss()
        onSignIn()
    }
    if (layout == HomeLayout.Phone) {
        ModalBottomSheet(onDismissRequest = onDismiss) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        start = 24.dp,
                        end = 24.dp,
                        bottom = 24.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(space = 16.dp),
            ) {
                StatusBody(
                    source = source,
                    serverName = serverName,
                    serverAddress = serverAddress,
                )
                StatusActions(
                    source = source,
                    onRefresh = refresh,
                    onOpenDownloads = downloads,
                    onSignIn = signIn,
                )
            }
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = null,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(space = 16.dp)) {
                    StatusBody(
                        source = source,
                        serverName = serverName,
                        serverAddress = serverAddress,
                    )
                    StatusActions(
                        source = source,
                        onRefresh = refresh,
                        onOpenDownloads = downloads,
                        onSignIn = signIn,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(resource = Res.string.home_status_close))
                }
            },
        )
    }
}

@Composable
private fun StatusBody(
    source: HomeSourceUi,
    serverName: String,
    serverAddress: String,
) {
    val title = when (source) {
        HomeSourceUi.Connected,
        HomeSourceUi.Connecting,
        -> stringResource(
            resource = Res.string.home_status_connected_title,
            serverName,
        )
        HomeSourceUi.Reconnecting -> stringResource(
            resource = Res.string.home_status_reconnecting_title,
            serverName,
        )
        HomeSourceUi.Offline -> stringResource(resource = Res.string.home_status_offline_title)
        HomeSourceUi.NotResponding -> stringResource(
            resource = Res.string.home_status_not_responding_title,
            serverName,
        )
        HomeSourceUi.SignedOut -> stringResource(
            resource = Res.string.home_status_signed_out_title,
            serverName,
        )
        HomeSourceUi.ThisDevice -> stringResource(resource = Res.string.home_status_device_title)
    }
    val body = when (source) {
        HomeSourceUi.Connected,
        HomeSourceUi.Connecting,
        -> null
        HomeSourceUi.Reconnecting -> stringResource(
            resource = Res.string.home_status_reconnecting_body,
            serverName,
        )
        HomeSourceUi.Offline -> stringResource(resource = Res.string.home_status_offline_body)
        HomeSourceUi.NotResponding -> stringResource(resource = Res.string.home_status_not_responding_body)
        HomeSourceUi.SignedOut -> stringResource(resource = Res.string.home_status_signed_out_body)
        HomeSourceUi.ThisDevice -> stringResource(resource = Res.string.home_status_device_body)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Icon(
                imageVector = source.icon(),
                contentDescription = null,
                modifier = Modifier
                    .padding(all = 12.dp)
                    .size(size = 24.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
    }
    if (source != HomeSourceUi.ThisDevice && source != HomeSourceUi.Offline) {
        StatusRow(
            label = stringResource(resource = Res.string.home_status_server),
            value = serverName,
        )
        if (serverAddress.isNotBlank()) {
            StatusRow(
                label = stringResource(resource = Res.string.home_status_address),
                value = serverAddress,
            )
        }
    }
    body?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusRow(
    label: String,
    value: String,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(space = 16.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun StatusActions(
    source: HomeSourceUi,
    onRefresh: () -> Unit,
    onOpenDownloads: () -> Unit,
    onSignIn: () -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        when (source) {
            HomeSourceUi.Connected -> Unit
            HomeSourceUi.Connecting,
            HomeSourceUi.Reconnecting -> Button(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                )
                Text(
                    text = stringResource(resource = Res.string.home_status_refresh),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            HomeSourceUi.Offline,
            HomeSourceUi.NotResponding -> {
                Button(onClick = onRefresh) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                    )
                    Text(
                        text = stringResource(resource = Res.string.home_status_try_again),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                OutlinedButton(onClick = onOpenDownloads) {
                    Text(text = stringResource(resource = Res.string.home_status_open_downloads))
                }
            }
            HomeSourceUi.SignedOut -> {
                Button(onClick = onSignIn) {
                    Text(text = stringResource(resource = Res.string.home_status_sign_in))
                }
                OutlinedButton(onClick = onOpenDownloads) {
                    Text(text = stringResource(resource = Res.string.home_status_open_downloads))
                }
            }
            HomeSourceUi.ThisDevice -> OutlinedButton(onClick = onOpenDownloads) {
                Text(text = stringResource(resource = Res.string.home_status_open_downloads))
            }
        }
    }
}
