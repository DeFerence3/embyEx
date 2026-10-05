package app.deference.embycl.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.deference.embycl.core.session.Session
import app.deference.embycl.domain.model.EmbyServer
import app.deference.embycl.domain.repository.ServerFinderRepo
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
		isSearchingLocal = true
		coroutineScope.launch {
			val local = runCatching { serverFinderRepo.searchForLocallyRunningServers() }.getOrDefault(emptyList())
			discoveredServers = local
			isSearchingLocal = false
		}
	}

	LaunchedEffect(Unit) {
		scanLocalServers()
	}

	Surface {
		Box(
			modifier = Modifier.fillMaxSize().padding(24.dp),
			contentAlignment = Alignment.Center
		) {
			Column(
				modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
				horizontalAlignment = Alignment.CenterHorizontally,
				verticalArrangement = Arrangement.spacedBy(16.dp)
			) {
				Text(
					text = "Server Connection Timed Out",
					style = MaterialTheme.typography.headlineMedium,
					fontWeight = FontWeight.Bold,
					textAlign = TextAlign.Center
				)
				
				Text(
					text = "Unable to connect to Emby server. Reconnect or update server address.",
					style = MaterialTheme.typography.bodyMedium,
					color = MaterialTheme.colorScheme.onSurfaceVariant,
					textAlign = TextAlign.Center
				)
				
				if (errorMessage != null) {
					Text(
						text = errorMessage!!,
						color = MaterialTheme.colorScheme.error,
						style = MaterialTheme.typography.bodySmall,
						textAlign = TextAlign.Center
					)
				}
				
				OutlinedTextField(
					value = serverAddress,
					onValueChange = { serverAddress = it },
					label = { Text("Server URL") },
					singleLine = true,
					modifier = Modifier.fillMaxWidth(),
					enabled = !isReconnecting
				)
				
				if (discoveredServers.isNotEmpty()) {
					Text(
						text = "Discovered Local Servers",
						style = MaterialTheme.typography.labelLarge,
						color = MaterialTheme.colorScheme.primary,
						modifier = Modifier.align(Alignment.Start)
					)
					LazyColumn(
						modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp),
						verticalArrangement = Arrangement.spacedBy(8.dp)
					) {
						items(discoveredServers) { server ->
							Card(
								modifier = Modifier.fillMaxWidth().clickable(enabled = !isReconnecting) {
									serverAddress = server.address
									rediscoverAndReconnect(server.address)
								}
							) {
								Row(
									modifier = Modifier.fillMaxWidth().padding(12.dp),
									horizontalArrangement = Arrangement.SpaceBetween,
									verticalAlignment = Alignment.CenterVertically
								) {
									Column {
										Text(server.name, fontWeight = FontWeight.Bold)
										Text(server.address, style = MaterialTheme.typography.bodySmall)
									}
									Text("Select", color = MaterialTheme.colorScheme.primary)
								}
							}
						}
					}
				}
				
				Row(
					modifier = Modifier.fillMaxWidth(),
					horizontalArrangement = Arrangement.spacedBy(12.dp)
				) {
					OutlinedButton(
						onClick = {
							Session.logout()
							Session.setConnected(true)
						},
						modifier = Modifier.weight(1f),
						enabled = !isReconnecting
					) {
						Text("Sign Out")
					}
					
					Button(
						onClick = { rediscoverAndReconnect(serverAddress) },
						modifier = Modifier.weight(1f),
						enabled = !isReconnecting && serverAddress.isNotBlank()
					) {
						if (isReconnecting) {
							CircularProgressIndicator(
								modifier = Modifier.size(18.dp),
								strokeWidth = 2.dp,
								color = MaterialTheme.colorScheme.onPrimary
							)
						} else {
							Text("Reconnect")
						}
					}
				}
			}
		}
	}
}