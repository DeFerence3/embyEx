package app.deference.embcl.ui.screens

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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Storage
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import app.deference.embcl.domain.model.EmbyServerDiscovery
import app.deference.embcl.domain.model.EmbyUser
import app.deference.embcl.domain.repository.EmbyRepository
import app.deference.embcl.ui.core.LocalBackStack
import app.deference.embcl.ui.core.MainScreen
import app.deference.embcl.ui.components.PasswordField
import app.deference.embcl.ui.components.PublicUserCard
import app.deference.embcl.ui.components.SignInButton
import app.deference.embcl.ui.components.SignInError
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun EmbySignIn(
	repository: EmbyRepository = koinInject(),
	onLocalFilesClick: () -> Unit = {
		// Navigate to local files if backStack is provided
	},
) {
	val backStack = LocalBackStack.current
	val scope = rememberCoroutineScope()
	var server by rememberSaveable { mutableStateOf("") }
	var username by rememberSaveable { mutableStateOf("") }
	var password by rememberSaveable { mutableStateOf("") }
	var isPasswordVisible by rememberSaveable { mutableStateOf(false) }
	var discovery by remember { mutableStateOf<EmbyServerDiscovery?>(null) }
	var selectedUser by remember { mutableStateOf<EmbyUser?>(null) }
	var manualSignIn by rememberSaveable { mutableStateOf(false) }
	var busy by remember { mutableStateOf(false) }
	var error by remember { mutableStateOf<String?>(null) }
	
	fun discoverUsers() {
		if (busy) return
		error = null
		busy = true
		scope.launch {
			runCatching { repository.discoverServer(server) }
				.onSuccess {
					discovery = it
					selectedUser = null
					manualSignIn = false
					password = ""
				}
				.onFailure { error = it.message ?: "Could not connect to the Emby server." }
			busy = false
		}
	}
	
	fun signInManually() {
		val current = discovery ?: return
		if (busy) return
		error = null
		busy = true
		scope.launch {
			runCatching { repository.authenticate(current, username, password) }
				.onFailure { error = it.message ?: "Could not sign in to Emby." }
			busy = false
		}
	}
	
	fun signInAs(user: EmbyUser, selectedPassword: String = password) {
		val current = discovery ?: return
		if (busy) return
		selectedUser = user
		error = null
		busy = true
		scope.launch {
			runCatching { repository.authenticate(current, user, selectedPassword) }
				.onFailure { error = it.message ?: "Could not sign in as ${user.name}." }
			busy = false
		}
	}
	
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
				val currentDiscovery = discovery
				if (currentDiscovery == null) {
					Text("Welcome to mpvEx", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
					Text(
						"Connect to your Emby server to choose an account.",
						color = MaterialTheme.colorScheme.onSurfaceVariant,
					)
					OutlinedTextField(
						value = server,
						onValueChange = { server = it },
						modifier = Modifier.fillMaxWidth(),
						label = { Text("Server address") },
						placeholder = { Text("http://192.168.1.10:8096") },
						leadingIcon = { Icon(Icons.Filled.Storage, null) },
						singleLine = true,
						keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
						keyboardActions = KeyboardActions(onDone = { discoverUsers() }),
					)
					error?.let { SignInError(it) }
					SignInButton(
						text = "Continue",
						busy = busy,
						enabled = server.isNotBlank(),
						onClick = ::discoverUsers,
					)
				} else {
					Row(verticalAlignment = Alignment.CenterVertically) {
						IconButton(
							onClick = {
								discovery = null
								selectedUser = null
								manualSignIn = false
								error = null
								password = ""
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
						selectedUser != null -> {
							val user = selectedUser !!
							PublicUserCard(user, currentDiscovery, repository, enabled = false) {}
							PasswordField(
								password = password,
								onPasswordChange = { password = it },
								visible = isPasswordVisible,
								onVisibilityChange = { isPasswordVisible = ! isPasswordVisible },
								onDone = { signInAs(user) },
							)
							error?.let { SignInError(it) }
							SignInButton("Sign in", busy, enabled = true) { signInAs(user) }
							TextButton(
								onClick = { selectedUser = null; password = ""; error = null },
								modifier = Modifier.align(Alignment.CenterHorizontally),
							) { Text("Choose another user") }
						}
						
						manualSignIn -> {
							Text("Manual sign in", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
							OutlinedTextField(
								value = username,
								onValueChange = { username = it },
								modifier = Modifier.fillMaxWidth(),
								label = { Text("Username") },
								leadingIcon = { Icon(Icons.Filled.AccountCircle, null) },
								singleLine = true,
								keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
							)
							PasswordField(
								password = password,
								onPasswordChange = { password = it },
								visible = isPasswordVisible,
								onVisibilityChange = { isPasswordVisible = ! isPasswordVisible },
								onDone = ::signInManually,
							)
							error?.let { SignInError(it) }
							SignInButton("Sign in", busy, enabled = username.isNotBlank(), onClick = ::signInManually)
							TextButton(
								onClick = { manualSignIn = false; password = ""; error = null },
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
									PublicUserCard(user, currentDiscovery, repository, enabled = ! busy) {
										password = ""
										error = null
										if (user.hasPassword || user.hasConfiguredPassword) {
											selectedUser = user
										} else {
											signInAs(user, "")
										}
									}
								}
							}
							error?.let { SignInError(it) }
							FilledTonalButton(
								onClick = { manualSignIn = true; error = null; password = "" },
								modifier = Modifier
									.fillMaxWidth()
									.height(52.dp),
								enabled = ! busy,
							) {
								Icon(Icons.Filled.AccountCircle, null)
								Spacer(Modifier.width(8.dp))
								Text("Manual sign in")
							}
						}
					}
				}
				TextButton(
					onClick = {
						onLocalFilesClick()
						backStack.add(MainScreen)
					},
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
