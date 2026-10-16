package app.deference.embycl.ui.screens.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.Screen
import app.deference.embycl.ui.core.LocalNavigator
import app.deference.embycl.ui.core.components.DetailTopBar
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun DownloadsScreenContent(
	state: OfflineDownloadsUiState,
	onAction: (DownloadsAction) -> Unit,
	onOpenSync: () -> Unit,
	onSetUnmeteredOnly: (Boolean) -> Unit,
	modifier: Modifier = Modifier,
	searchOnly: Boolean = false,
) {
	val permission = rememberDownloadPermissionRequest()
	Column(
		modifier = modifier
			.fillMaxSize(),
	) {
		if (!searchOnly) {
			FlowRow(
				modifier = Modifier
					.fillMaxWidth()
					.padding(
						horizontal = 16.dp,
					),
				horizontalArrangement = Arrangement.spacedBy(8.dp),
			) {
				if (state.pendingSync.isNotEmpty()) {
					TextButton(
						onClick = onOpenSync,
					) {
						Text(
							text = "Watch progress (${state.pendingSync.size})",
						)
					}
				}
			}
			if (downloadMeteringSupported || state.unmeteredOnly) {
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(
							horizontal = 20.dp,
						),
				) {
					Column(
						modifier = Modifier
							.weight(1f),
					) {
						Text(
							text = "Download on unmetered networks only",
						)
						if (!downloadMeteringSupported) {
							Text(
								text = "Network metering is unavailable on this device. Turn this off to download.",
								style = MaterialTheme.typography.bodySmall,
							)
						}
					}
					Switch(
						checked = state.unmeteredOnly,
						onCheckedChange = onSetUnmeteredOnly,
					)
				}
			}
		}
		Box(
			modifier = Modifier
				.weight(1f),
		) {
			DownloadsContent(
				state = state.downloads.copy(
					busyIds = state.busyIds,
					playingId = state.playingId,
				),
				onAction = { action ->
					if (action is DownloadsAction.Resume || action is DownloadsAction.Retry) {
						permission {
							onAction(action)
						}
					} else {
						onAction(action)
					}
				},
				showOfflineBanner = false,
			)
		}
	}
}


@Serializable
data object EmbyDownloadsScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        val viewModel: OfflineDownloadsViewModel = koinViewModel()
        val state by viewModel.state.collectAsState()
        Scaffold(
            topBar = {
                DetailTopBar(
                    title = "Downloads",
                    onBack = navigator::goBack,
                )
            },
        ) { padding ->
            DownloadsScreenContent(
                modifier = Modifier
                    .padding(padding),
                state = state,
                onAction = viewModel::onAction,
                onOpenSync = viewModel::openSync,
                onSetUnmeteredOnly = viewModel::setUnmeteredOnly,
            )
        }
    }
}
