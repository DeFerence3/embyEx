package app.deference.embycl.ui.screens.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.core.focusRing
import app.deference.embycl.ui.screens.home.model.HomeFullState
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.home_device_empty_body
import embyex.shared.generated.resources.home_device_empty_title
import embyex.shared.generated.resources.home_offline_empty_body
import embyex.shared.generated.resources.home_offline_empty_title
import embyex.shared.generated.resources.home_server_empty_body
import embyex.shared.generated.resources.home_server_empty_title
import embyex.shared.generated.resources.home_signed_out_body
import embyex.shared.generated.resources.home_signed_out_title
import embyex.shared.generated.resources.home_status_sign_in
import embyex.shared.generated.resources.home_try_again
import embyex.shared.generated.resources.home_unreachable_body
import embyex.shared.generated.resources.home_unreachable_title
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HomeFullStateView(
    state: HomeFullState,
    serverName: String,
    onTryAgain: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val error = state == HomeFullState.OfflineEmpty || state == HomeFullState.Unreachable
    val icon = when (state) {
        HomeFullState.OfflineEmpty -> Icons.Outlined.CloudOff
        HomeFullState.Unreachable -> Icons.Outlined.Dns
        HomeFullState.SignedOut -> Icons.AutoMirrored.Outlined.Login
        HomeFullState.DeviceEmpty,
        HomeFullState.ServerEmpty,
        -> Icons.Outlined.VideoLibrary
    }
    val title = when (state) {
        HomeFullState.OfflineEmpty -> stringResource(resource = Res.string.home_offline_empty_title)
        HomeFullState.Unreachable -> stringResource(
            resource = Res.string.home_unreachable_title,
            serverName,
        )
        HomeFullState.SignedOut -> stringResource(
            resource = Res.string.home_signed_out_title,
            serverName,
        )
        HomeFullState.DeviceEmpty -> stringResource(resource = Res.string.home_device_empty_title)
        HomeFullState.ServerEmpty -> stringResource(
            resource = Res.string.home_server_empty_title,
            serverName,
        )
    }
    val body = when (state) {
        HomeFullState.OfflineEmpty -> stringResource(
            resource = Res.string.home_offline_empty_body,
            serverName,
        )
        HomeFullState.Unreachable -> stringResource(resource = Res.string.home_unreachable_body)
        HomeFullState.SignedOut -> stringResource(resource = Res.string.home_signed_out_body)
        HomeFullState.DeviceEmpty -> stringResource(resource = Res.string.home_device_empty_body)
        HomeFullState.ServerEmpty -> stringResource(resource = Res.string.home_server_empty_body)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 24.dp,
                vertical = 48.dp,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(
                    topStart = 36.dp,
                    topEnd = 24.dp,
                    bottomEnd = 36.dp,
                    bottomStart = 12.dp,
                ),
                color = if (error) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = if (error) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onTertiaryContainer,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(all = 24.dp)
                        .size(size = 40.dp),
                )
            }
            Spacer(modifier = Modifier.height(height = 4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            if (state == HomeFullState.SignedOut) {
                Button(
                    onClick = onSignIn,
                    modifier = Modifier.focusRing(shape = CircleShape),
                ) {
                    Text(text = stringResource(resource = Res.string.home_status_sign_in))
                }
            } else if (state != HomeFullState.DeviceEmpty) {
                Button(
                    onClick = onTryAgain,
                    modifier = Modifier.focusRing(shape = CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                    )
                    Text(
                        text = stringResource(resource = Res.string.home_try_again),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}
