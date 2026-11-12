package app.deference.embycl.ui.screens.home.components

import embyex.shared.generated.resources.home_status_sign_in
import embyex.shared.generated.resources.home_signed_out_body
import embyex.shared.generated.resources.home_signed_out_title
import androidx.compose.foundation.shape.CircleShape
import app.deference.embycl.ui.core.focusRing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.screens.home.model.HomeSourceUi
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.home_device_body
import embyex.shared.generated.resources.home_device_title
import embyex.shared.generated.resources.home_offline_body
import embyex.shared.generated.resources.home_offline_title
import embyex.shared.generated.resources.home_try_again
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HomeStatusCallout(
    source: HomeSourceUi,
    serverName: String,
    onTryAgain: () -> Unit,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val offline = source == HomeSourceUi.Offline
    val signedOut = source == HomeSourceUi.SignedOut
    if (!offline && !signedOut && source != HomeSourceUi.ThisDevice) return
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 8.dp,
                top = 12.dp,
                bottom = 8.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            Icon(
                imageVector = source.icon(),
                contentDescription = null,
            )
            FlowRow(
                modifier = Modifier.weight(weight = 1f),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.padding(bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(space = 2.dp),
                ) {
                    Text(
                        text = when {
                            offline -> stringResource(resource = Res.string.home_offline_title)
                            signedOut -> stringResource(resource = Res.string.home_signed_out_title, serverName)
                            else -> stringResource(resource = Res.string.home_device_title)
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                        text = when {
                            offline -> stringResource(
                                resource = Res.string.home_offline_body,
                                serverName,
                            )
                            signedOut -> stringResource(resource = Res.string.home_signed_out_body)
                            else -> stringResource(resource = Res.string.home_device_body)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (signedOut) {
                    TextButton(
                        onClick = onSignIn,
                        modifier = Modifier.focusRing(shape = CircleShape),
                    ) {
                        Text(text = stringResource(resource = Res.string.home_status_sign_in))
                    }
                }
                if (offline) {
                    TextButton(
                        onClick = onTryAgain,
                        modifier = Modifier.focusRing(shape = CircleShape),
                    ) {
                        Text(text = stringResource(resource = Res.string.home_try_again))
                    }
                }
            }
        }
    }
}
