package app.deference.embcl.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.deference.embcl.BuildConfig
import app.deference.embcl.R
import app.deference.embcl.core.session.Session
import app.deference.embcl.domain.model.ServerInfo
import app.deference.embcl.ui.Screen
import app.deference.embcl.ui.core.LocalNavigator
import app.deference.embcl.ui.core.components.DetailTopBar
import app.deference.embcl.ui.core.components.SignOutConfirmationDialog
import app.deference.embcl.ui.screens.settings.components.InfoSection
import app.deference.embcl.ui.screens.settings.components.SettingsAccount
import app.deference.embcl.ui.screens.settings.components.UserCard
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object EmbySettingsScreen : Screen {
	
	@Composable
	override fun Content() {
		val navigator = LocalNavigator.current
		val viewModel = koinViewModel<SettingsViewModel>()
		val state by viewModel.state.collectAsState()
		val account = remember { SettingsAccount.getFromSession() }
		SettingsContent(
			state = state,
			account = account,
			onRefresh = viewModel::refreshServerInfo,
			onBack = navigator::goBack,
			onSignOut = Session::logout,
		)
	}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsContent(
	state: SettingsState,
	account: SettingsAccount,
	onRefresh: () -> Unit,
	onBack: () -> Unit,
	onSignOut: () -> Unit,
) {
	var showSignOutConfirmation by rememberSaveable { mutableStateOf(false) }
	if (showSignOutConfirmation) {
		SignOutConfirmationDialog(
			serverName = account.serverName,
			onConfirm = {
				showSignOutConfirmation = false
				onSignOut()
			},
			onDismiss = { showSignOutConfirmation = false },
		)
	}
	
	Scaffold(topBar = { DetailTopBar("Settings", onBack = onBack) }) { padding ->
		Box(
			Modifier
				.fillMaxSize()
				.padding(padding), contentAlignment = Alignment.TopCenter
		) {
			LazyColumn(
				modifier = Modifier
					.widthIn(max = 680.dp)
					.fillMaxSize(),
				contentPadding = PaddingValues(20.dp),
				verticalArrangement = Arrangement.spacedBy(20.dp),
			) {
				item {
					UserCard(account)
				}
				
				item {
					InfoSection(
						"Server Information",
						state.isLoading,
						onRefresh,
						serverDetails(state.serverDetails?.info),
						state.error,
					)
/*
					Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
						InfoHeader("Server information")
						if (state.isLoading && state.serverDetails == null) {
							Text("Loading server information…", style = MaterialTheme.typography.bodyMedium)
						}
						state.error?.let { error ->
							Text(error, color = MaterialTheme.colorScheme.error)
							TextButton(onClick = onRefresh, enabled = ! state.isLoading) { Text("Retry") }
						}
						if (state.serverDetails?.isLimited == true) {
							Text("This account can only view public server information.", color = MaterialTheme.colorScheme.onSurfaceVariant)
						}
						InfoCard()
					}
*/
				}
				item {
					InfoSection(
						"Connection",
						state.isLoading,
						onRefresh,
						connectionDetails(state.serverDetails?.info),
						state.error,
					)
				}
				item {
					OutlinedButton(
						onClick = { showSignOutConfirmation = true },
						modifier = Modifier
							.fillMaxWidth()
							.heightIn(min = 56.dp),
						border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
						colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
					) {
						Icon(Icons.AutoMirrored.Filled.Logout, null, Modifier.padding(end = 12.dp))
						Text("Sign out", fontWeight = FontWeight.SemiBold)
					}
				}
				item {
					Text("${stringResource(R.string.app_name)} ${BuildConfig.VERSION_NAME}", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
				}
			}
		}
	}
}

private fun serverDetails(info: ServerInfo?) = buildList {
	add("Server name" to (info?.serverName?.takeIf { it.isNotBlank() } ?: Session.serverName))
	addDetail("Version", info?.version)
	addDetail("Operating system", info?.operatingSystemDisplayName?.takeIf { it.isNotBlank() } ?: info?.operatingSystem)
	addDetail("Package", info?.packageName)
	addDetail("Library monitoring supported", info?.supportsLibraryMonitor?.yesNo())
}

private fun connectionDetails(info: ServerInfo?) = buildList {
	add("Server address" to Session.serverUrl)
	add("Protocol" to if (Session.serverUrl.startsWith("https://", ignoreCase = true)) "HTTPS" else "HTTP")
	addDetail("Local addresses", (info?.localAddresses.orEmpty() + listOfNotNull(info?.localAddress)).filter { it.isNotBlank() }.distinct().joinToString("\n"))
	addDetail("Remote addresses", (info?.remoteAddresses.orEmpty() + listOfNotNull(info?.wanAddress)).filter { it.isNotBlank() }.distinct().joinToString("\n"))
	addDetail("HTTP port", info?.httpPort?.takeIf { it > 0 }?.toString())
}


private fun MutableList<Pair<String, String>>.addDetail(label: String, value: String?) {
	if (!value.isNullOrBlank()) add(label to value)
}

private fun Boolean.yesNo() = if (this) "Yes" else "No"

private fun ServerInfo.storageDetails(): List<Pair<String, String>> = buildList {
	addDetail("Program data", programDataPath)
	addDetail("Cache", cachePath)
	addDetail("Logs", logPath)
	addDetail("Metadata", metadataPath)
	addDetail("Transcoding temporary files", transcodingTempPath)
}
