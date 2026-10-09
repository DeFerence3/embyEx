package app.deference.embycl.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.deference.embycl.core.session.Session
import app.deference.embycl.domain.model.EmbyServer
import app.deference.embycl.domain.repository.ServerFinderRepo
import app.deference.embycl.ui.core.LocalKoasty
import app.deference.embycl.ui.core.components.*
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun NotConnected(
	serverFinderRepo: ServerFinderRepo = koinInject()
) {
	val coroutineScope = rememberCoroutineScope()
	var serverAddress by remember { mutableStateOf(Session.serverUrl) }
	var isReconnecting by remember { mutableStateOf(false) }
	var isSearchingLocal by remember { mutableStateOf(false) }
	var discoveredServers by remember { mutableStateOf<List<EmbyServer>>(emptyList()) }
	var errorMessage by remember { mutableStateOf<String?>(null) }
	val koasty = LocalKoasty.current

	fun rediscoverAndReconnect(targetUrl: String) {
		if (isReconnecting || targetUrl.isBlank()) return
		isReconnecting = true
		errorMessage = null
		coroutineScope.launch {
			runCatching { serverFinderRepo.discoverServer(targetUrl) }
				.onSuccess { discovery ->
					val currentServerId = Session.serverId
					val discoveredServerId = discovery.serverInfo.id
					if (currentServerId.isNotEmpty() && discoveredServerId.isNotEmpty() && currentServerId != discoveredServerId) {
						Session.logout()
						Session.setConnected(true)
					} else {
						val newUrl = discovery.server.toUrl()
						Session.updateServerUrl(newUrl, discovery.serverInfo.serverName, discoveredServerId)
						Session.setConnected(true)
					}
					isReconnecting = false
				}
				.onFailure { error ->
					isReconnecting = false
					errorMessage = "Failed to connect to server: ${error.message ?: "Unknown error"}"
				}
		}
	}

	fun scanLocalServers() {
		if (isSearchingLocal) return
		koasty.show("Searching local servers...")
		isSearchingLocal = true
		coroutineScope.launch {
			val local = runCatching { serverFinderRepo.searchForLocallyRunningServers() }.getOrDefault(emptyList())
			discoveredServers = local
			if(local.isEmpty()){
				koasty.show("No local servers found.")
			}
			isSearchingLocal = false
		}
	}

	LaunchedEffect(Unit) {
		scanLocalServers()
	}

    var confirmSignOut by rememberSaveable { mutableStateOf(false) }
    if (confirmSignOut) SignOutConfirmationDialog(Session.serverName,
        onConfirm = { confirmSignOut = false; Session.logout(); Session.setConnected(true) },
        onDismiss = { confirmSignOut = false })
    ConnectionRecoveryContent(
        serverAddress, { serverAddress = it }, isReconnecting, isSearchingLocal,
        discoveredServers, errorMessage,
        onReconnect = { rediscoverAndReconnect(serverAddress) },
        onScan = ::scanLocalServers,
        onSelectServer = { serverAddress = it.address; rediscoverAndReconnect(it.address) },
        onSignOut = { confirmSignOut = true },
    )
}

@Composable
fun ConnectionRecoveryContent(
    serverAddress: String,
    onAddressChange: (String) -> Unit,
    isReconnecting: Boolean,
    isSearchingLocal: Boolean,
    discoveredServers: List<EmbyServer>,
    errorMessage: String?,
    onReconnect: () -> Unit,
    onScan: () -> Unit,
    onSelectServer: (EmbyServer) -> Unit,
    onSignOut: () -> Unit,
) {
    Surface(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().safeDrawingPadding().imePadding(), contentAlignment = Alignment.Center) {
            LazyColumn(
                modifier = Modifier.widthIn(max = 560.dp).fillMaxWidth(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item { ExpressiveEmblem(Icons.Default.CloudOff) }
                item { PageIntro("LET’S GET YOU BACK", "Reconnect to your stories.", "Your server is taking a moment. Check its address or find it on your network.") }
                item {
                    OutlinedTextField(
                        value = serverAddress, onValueChange = onAddressChange,
                        label = { Text("Server address") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                        enabled = !isReconnecting, isError = errorMessage != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { if (!isReconnecting && serverAddress.isNotBlank()) onReconnect() }),
                    )
                }
                errorMessage?.let { item { SignInError(it) } }
                item { SignInButton("Reconnect", isReconnecting, serverAddress.isNotBlank(), onReconnect) }
                item {
                    FilledTonalButton(onClick = onScan, enabled = !isSearchingLocal && !isReconnecting,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                        if (isSearchingLocal) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Refresh, null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isSearchingLocal) "Searching your network…" else "Scan for servers")
                    }
                }
                if (discoveredServers.isNotEmpty()) {
                    item { SectionHeading("Nearby servers") }
                    items(discoveredServers, key = { it.address }) { server ->
                        Card(onClick = { onSelectServer(server) }, enabled = !isReconnecting, modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text(server.name, style = MaterialTheme.typography.titleMedium)
                                    Text(server.address, style = MaterialTheme.typography.bodySmall)
                                }
                                Icon(Icons.AutoMirrored.Default.ArrowForward, null)
                            }
                        }
                    }
                }
                item {
                    TextButton(onClick = onSignOut, enabled = !isReconnecting, modifier = Modifier.fillMaxWidth()) { Text("Sign out of this server") }
                }
            }
        }
    }
}
