package app.deference.embycl.ui.screens.signin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import app.deference.embycl.ui.core.components.ExpressiveEmblem
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.Screen
import app.deference.embycl.ui.core.components.ObserveEvent
import app.deference.embycl.ui.core.components.PasswordField
import app.deference.embycl.ui.core.components.PublicUserCard
import app.deference.embycl.ui.core.components.SignInButton
import app.deference.embycl.ui.core.components.SignInError
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SignInContent(
	state: SignInState,
	onAction: (SignInAction) -> Unit
) {
    val largeText = LocalDensity.current.fontScale > 1.3f
	Column(
		modifier = Modifier
			.fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .safeDrawingPadding()
            .imePadding()
			.verticalScroll(rememberScrollState())
			.padding(horizontal = 20.dp, vertical = 28.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center,
	) {
		ElevatedCard(
			modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth(),
			shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
		) {
			Column(
				modifier = Modifier.padding(24.dp),
				verticalArrangement = Arrangement.spacedBy(16.dp),
			) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ExpressiveEmblem(Icons.Default.LiveTv)
                    Column(Modifier.weight(1f)) {
                        Text("EmbyEx", style = if (largeText) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineSmall)
                        if (!largeText) Text("YOUR PERSONAL CINEMA", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Text(if (state.discovery == null) "01 / CONNECT YOUR SERVER" else "02 / CHOOSE YOUR ACCOUNT",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
				val currentDiscovery = state.discovery
				if (currentDiscovery == null) {
					Text(if (largeText) "Connect your server" else "Great stories.\nYour own space.", style = if (largeText) MaterialTheme.typography.titleLarge else MaterialTheme.typography.displaySmall)
					Text(
						"Connect your Emby server and make yourself at home.",
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
					if (state.isSearchingLocal) {
						Row(
							modifier = Modifier
								.fillMaxWidth()
								.padding(vertical = 4.dp),
							verticalAlignment = Alignment.CenterVertically,
							horizontalArrangement = Arrangement.spacedBy(8.dp),
						) {
							CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
							Text("Searching for local Emby servers...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
						}
					} else if (state.discoveredServers.isNotEmpty()) {
						Column(
							modifier = Modifier.fillMaxWidth(),
							verticalArrangement = Arrangement.spacedBy(8.dp),
						) {
							Text("Nearby servers", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
							state.discoveredServers.forEach { srv ->
								ElevatedCard(
									onClick = { onAction(SignInAction.SelectServer(srv.address)) },
									modifier = Modifier.fillMaxWidth(),
									colors = CardDefaults.elevatedCardColors(
										containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
									),
								) {
									Row(
										modifier = Modifier
											.fillMaxWidth()
											.padding(12.dp),
										verticalAlignment = Alignment.CenterVertically,
										horizontalArrangement = Arrangement.spacedBy(12.dp),
									) {
										Icon(Icons.Filled.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
										Column(modifier = Modifier.weight(1f)) {
											Text(srv.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
											Text(srv.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
										}
									}
								}
							}
						}
					}

					OutlinedTextField(
						value = state.server,
                        shape = MaterialTheme.shapes.medium,
                        enabled = !state.isBusy,
                        isError = state.error != null,
						onValueChange = { onAction(SignInAction.ServerChanged(it)) },
						modifier = Modifier.fillMaxWidth(),
						label = { Text("Server address") },
						placeholder = { Text("http://192.168.1.10:8096") },
						leadingIcon = { Icon(Icons.Filled.Storage, null) },
						trailingIcon = {
							IconButton(onClick = { onAction(SignInAction.ScanLocalServers) }, enabled = ! state.isSearchingLocal) {
								Icon(Icons.Filled.Refresh, contentDescription = "Scan network")
							}
						},
						singleLine = true,
						keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false, imeAction = ImeAction.Done),
						keyboardActions = KeyboardActions(onDone = { if (!state.isBusy && state.server.isNotBlank()) onAction(SignInAction.DiscoverServer) }),
					)
					state.error?.let { SignInError(it) }
					SignInButton(
						text = "Continue",
						busy = state.isBusy,
						enabled = state.server.isNotBlank(),
						onClick = { onAction(SignInAction.DiscoverServer) },
					)
				} else {
					Row(verticalAlignment = Alignment.CenterVertically) {
						IconButton(
							onClick = {
								onAction(SignInAction.ChangeServer)
							},
						) {
							Icon(Icons.AutoMirrored.Filled.ArrowBack, "Change server")
						}
						Column(Modifier.weight(1f)) {
							Text(currentDiscovery.serverInfo.serverName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
							Text("Choose an account", color = MaterialTheme.colorScheme.onSurfaceVariant)
						}
					}

					when {
						state.selectedUser != null -> {
							val user = state.selectedUser
							PublicUserCard(user, currentDiscovery, enabled = false, isSelected = true) {}
							PasswordField(
								password = state.password,
								onPasswordChange = { onAction(SignInAction.PasswordChanged(it)) },
								visible = state.isPasswordVisible,
								onVisibilityChange = { onAction(SignInAction.TogglePasswordVisibility) },
								enabled = !state.isBusy,
                                onDone = { if (!state.isBusy) onAction(SignInAction.SignInSelectedUser) },
							)
							state.error?.let { SignInError(it) }
							SignInButton("Sign in", state.isBusy, enabled = true) { onAction(SignInAction.SignInSelectedUser) }
							TextButton(
								onClick = { onAction(SignInAction.ChooseAnotherUser) },
								modifier = Modifier.align(Alignment.CenterHorizontally),
							) { Text("Choose another user") }
						}

						state.isManualSignIn -> {
							Text("Manual sign in", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
							OutlinedTextField(
								value = state.username,
                                shape = MaterialTheme.shapes.medium,
                                enabled = !state.isBusy,
								onValueChange = { onAction(SignInAction.UsernameChanged(it)) },
								modifier = Modifier
									.fillMaxWidth()
									.semantics { contentType = ContentType.Username },
								label = { Text("Username") },
								leadingIcon = { Icon(Icons.Filled.AccountCircle, null) },
								singleLine = true,
								keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Next),
							)
							PasswordField(
								password = state.password,
								onPasswordChange = { onAction(SignInAction.PasswordChanged(it)) },
								visible = state.isPasswordVisible,
								onVisibilityChange = { onAction(SignInAction.TogglePasswordVisibility) },
								enabled = !state.isBusy,
                                onDone = { if (!state.isBusy && state.username.isNotBlank()) onAction(SignInAction.SignInManually) },
							)
							state.error?.let { SignInError(it) }
							SignInButton("Sign in", state.isBusy, enabled = state.username.isNotBlank(), onClick = { onAction(SignInAction.SignInManually) })
							TextButton(
								onClick = { onAction(SignInAction.ShowPublicUsers) },
								modifier = Modifier.align(Alignment.CenterHorizontally),
							) { Text("Choose a listed user") }
						}

						else -> {
							if (currentDiscovery.users.isEmpty()) {
								Text(
									"This server has no public users. Sign in manually to continue.",
									color = MaterialTheme.colorScheme.onSurfaceVariant,
								)
							} else {
								currentDiscovery.users.forEach { user ->
									PublicUserCard(user, currentDiscovery, enabled = ! state.isBusy) {
										onAction(SignInAction.SelectUser(user))
									}
								}
							}
							state.error?.let { SignInError(it) }
							FilledTonalButton(
								onClick = { onAction(SignInAction.ShowManualSignIn) },
								modifier = Modifier
									.fillMaxWidth()
									.heightIn(min = 56.dp),
								enabled = ! state.isBusy,
							) {
								Icon(Icons.Filled.AccountCircle, null)
								Spacer(Modifier.width(8.dp))
								Text("Manual sign in")
							}
						}
					}
				}
				TextButton(
					onClick = { onAction(SignInAction.UseLocalFiles) },
					modifier = Modifier.align(Alignment.CenterHorizontally),
				) {
					Icon(Icons.Filled.Folder, null)
					Spacer(Modifier.width(8.dp))
					Text("Use local files")
				}
			}
		}
	}
}

@Serializable
data object EmbySignInScreen : Screen {

	@Composable
	override fun Content() {
		val viewModel = koinViewModel<SignInViewModel>()
		val state by viewModel.state.collectAsState()
		viewModel.events.ObserveEvent { event ->
			when (event) {
				is SignInEvent.Error -> Unit
			}
		}
		SignInContent(state, viewModel::onAction)
	}
}
