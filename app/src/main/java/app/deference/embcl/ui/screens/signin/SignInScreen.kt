package app.deference.embcl.ui.screens.signin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.deference.embcl.ui.Screen
import app.deference.embcl.ui.core.components.ObserveEvent
import app.deference.embcl.ui.core.components.PasswordField
import app.deference.embcl.ui.core.components.PublicUserCard
import app.deference.embcl.ui.core.components.SignInButton
import app.deference.embcl.ui.core.components.SignInError
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SignInContent(
	state: SignInState,
	onAction: (SignInAction) -> Unit
) {
	Column(
		modifier = Modifier
			.fillMaxSize()
			.background(
				Brush.verticalGradient(
					listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.surface),
				),
			)
			.verticalScroll(rememberScrollState())
			.padding(horizontal = 24.dp, vertical = 48.dp),
		horizontalAlignment = Alignment.CenterHorizontally,
		verticalArrangement = Arrangement.Center,
	) {
		ElevatedCard(
			modifier = Modifier.fillMaxWidth(),
			shape = RoundedCornerShape(28.dp),
		) {
			Column(
				modifier = Modifier.padding(24.dp),
				verticalArrangement = Arrangement.spacedBy(16.dp),
			) {
				Surface(
					modifier = Modifier.size(64.dp),
					shape = RoundedCornerShape(20.dp),
					color = MaterialTheme.colorScheme.primary,
				) {
					Icon(
						Icons.Filled.LiveTv,
						contentDescription = null,
						tint = MaterialTheme.colorScheme.onPrimary,
						modifier = Modifier.padding(16.dp),
					)
				}
				val currentDiscovery = state.discovery
				if (currentDiscovery == null) {
					Text("Welcome to mpvEx", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
					Text(
						"Connect to your Emby server to choose an account.",
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
							Text("Discovered Servers", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
							state.discoveredServers.forEach { srv ->
								ElevatedCard(
									onClick = { onAction(SignInAction.SelectServer(srv.address)) },
									modifier = Modifier.fillMaxWidth(),
									colors = CardDefaults.elevatedCardColors(
										containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
						keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
						keyboardActions = KeyboardActions(onDone = { onAction(SignInAction.DiscoverServer) }),
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
								onDone = { onAction(SignInAction.SignInSelectedUser) },
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
								onDone = { onAction(SignInAction.SignInManually) },
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
									.height(52.dp),
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
