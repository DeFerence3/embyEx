package app.deference.embycl.ui.screens.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.deference.embycl.BuildConfig
import app.deference.embycl.core.session.Session
import app.deference.embycl.domain.model.ServerInfo
import app.deference.embycl.domain.model.update.AppUpdate
import app.deference.embycl.domain.model.update.CurrentBuildRelease
import app.deference.embycl.ui.Screen
import app.deference.embycl.ui.core.LocalKoasty
import app.deference.embycl.ui.core.LocalNavigator
import app.deference.embycl.ui.core.SettingsPreferences
import app.deference.embycl.ui.core.components.DetailTopBar
import app.deference.embycl.ui.core.components.PageIntro
import app.deference.embycl.ui.core.components.SignOutConfirmationDialog
import app.deference.embycl.ui.screens.settings.components.CurrentBuildChangelogDialog
import app.deference.embycl.ui.screens.settings.components.InfoSection
import app.deference.embycl.ui.screens.settings.components.SettingsAccount
import app.deference.embycl.ui.screens.settings.components.SettingsAction
import app.deference.embycl.ui.screens.settings.components.SettingsSection
import app.deference.embycl.ui.screens.settings.components.UserCard
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.app_name
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Serializable
data object EmbySettingsScreen : Screen {

	@Composable
	override fun Content() {
		val navigator = LocalNavigator.current
		val viewModel = koinViewModel<SettingsViewModel>()
		val state by viewModel.state.collectAsState()
		val updateState by viewModel.updateState.collectAsState()
		val account = remember { SettingsAccount.getFromSession() }
		SettingsContent(
			state = state,
			account = account,
			onRefresh = viewModel::refreshServerInfo,
			onCheckUpdates = viewModel::checkForUpdates,
			onBack = navigator::goBack,
			onSignOut = Session::logout,
			update = updateState.update,
			onLoadChangelog = viewModel::loadCurrentBuildChangelog,
		)
	}
}

@Composable
fun SettingsContent(
	state: SettingsState,
	update: AppUpdate,
	account: SettingsAccount,
	onRefresh: () -> Unit,
	onCheckUpdates: () -> Unit,
	onBack: () -> Unit,
	onSignOut: () -> Unit,
	onLoadChangelog: () -> Unit = {},
) {
	var showChangelog by rememberSaveable { mutableStateOf(false) }
	val uriHandler = LocalUriHandler.current
	val koasty = LocalKoasty.current
	val openReleasePage: () -> Unit = {
		try {
			uriHandler.openUri(CurrentBuildRelease.pageUrl)
		} catch (_: Exception) {
			koasty.show("Could not open the release page. Check that a browser is available.")
		}
	}
	LaunchedEffect(showChangelog) {
		if (showChangelog) onLoadChangelog()
	}
	if (showChangelog) {
		CurrentBuildChangelogDialog(state, onLoadChangelog, openReleasePage) { showChangelog = false }
	}
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
					PageIntro(
						eyebrow = "PREFERENCES & CONNECTION",
						title = "Your space, connected.",
						description = "Manage your account, player, and server.",
						accent = true
					)
				}
				item {
					UserCard(account, onSignout = { showSignOutConfirmation = showSignOutConfirmation.not() })
				}

/*
				item {
					Row(
						modifier = Modifier.fillMaxWidth(),
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.SpaceBetween,
					) {
						Column(Modifier.weight(1f)) {
							Text("Show banners", style = MaterialTheme.typography.titleMedium)
							Text(
								"Intro banners on Home, Libraries and Settings.",
								style = MaterialTheme.typography.bodySmall,
								color = MaterialTheme.colorScheme.onSurfaceVariant,
							)
						}
						Switch(
							checked = SettingsPreferences.bannerEnabled,
							onCheckedChange = SettingsPreferences::setBanner,
						)
					}
				}
*/

				item {
					Row(
						modifier = Modifier.fillMaxWidth(),
						verticalAlignment = Alignment.CenterVertically,
						horizontalArrangement = Arrangement.spacedBy(SplitButtonDefaults.Spacing)
					) {
						var checked by remember { mutableStateOf(false) }
						val rotation by animateFloatAsState(
							targetValue = if (checked) 180f else 0f
						)
						
						SplitButtonDefaults.TonalLeadingButton(
							modifier = Modifier
								.weight(1f)
								.heightIn(min = 56.dp),
							onClick = onCheckUpdates,
							enabled = update != AppUpdate.Checking
						) {
							Icon(
								Icons.Default.Update,
								contentDescription = null,
								modifier = Modifier.padding(end = 12.dp)
							)
							
							Text(
								when (update) {
									AppUpdate.Checking -> "Checking for updates…"
									is AppUpdate.Downloading -> "View update download"
									is AppUpdate.Available -> "View available update"
									is AppUpdate.ReadyToInstall,
									is AppUpdate.AwaitingPermission,
									is AppUpdate.InstallerLaunched ->
										"Install downloaded update"
									else -> "Check for updates"
								},
								fontWeight = FontWeight.SemiBold
							)
						}
						Box(modifier = Modifier.heightIn(min = 56.dp)){
							SplitButtonDefaults.TonalTrailingButton(
								modifier = Modifier.heightIn(min = 56.dp),
								checked = checked,
								onCheckedChange = { checked = it }
							) {
								Icon(
									Icons.Default.ArrowDropDown,
									contentDescription = "More update options",
									modifier = Modifier.graphicsLayer {
										rotationZ = rotation
									}
								)
							}
							DropdownMenu(
								expanded = checked,
								onDismissRequest = { checked = false },
								shape = MaterialTheme.shapes.large
							) {
								DropdownMenuItem(
									text = { Text("Current build changelog") },
									leadingIcon = {
										Icon(Icons.Default.Description, null)
									},
									onClick = {
										checked = false
										showChangelog = true
									}
								)
								
								DropdownMenuItem(
									text = { Text("View release page") },
									leadingIcon = {
										Icon(Icons.Default.OpenInBrowser, null)
									},
									onClick = {
										checked = false
										openReleasePage()
									}
								)
							}
						}
					}
				}
				item {
					Text("${stringResource(Res.string.app_name)} · ${BuildConfig.VERSION_NAME}", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodySmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
				}

				if (hasPlayerSettings) {
					item { PlayerSettings() }
				}
				
				item {
					val settings = listOf(
						SettingsAction(
							header = "Show Banners",
							description = "Intro banners on Home, Libraries and Settings.",
							value = SettingsPreferences.bannerEnabled,
							onAction = SettingsPreferences::setBanner
						)
					)
					
					SettingsSection(
						title = "Ui Elements",
						settings = settings
					)
				}

				item {
					InfoSection(
						"Server information",
						state.isLoading,
						onRefresh,
						serverDetails(state.serverDetails?.info),
						state.error,
					)
				}
				item {
					InfoSection(
						"Server information",
						state.isLoading,
						onRefresh,
						serverDetails(state.serverDetails?.info),
						state.error,
					)
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
