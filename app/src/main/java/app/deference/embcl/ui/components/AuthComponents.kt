package app.deference.embcl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import app.deference.embcl.domain.model.EmbyServerDiscovery
import app.deference.embcl.domain.model.EmbyUser
import app.deference.embcl.domain.repository.EmbyRepository
import coil3.compose.AsyncImage

@Composable
fun PublicUserCard(
	user: EmbyUser,
	discovery: EmbyServerDiscovery,
	repository: EmbyRepository,
	enabled: Boolean,
	onClick: () -> Unit,
) {
	Card(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(enabled = enabled, onClick = onClick),
		shape = RoundedCornerShape(18.dp),
		colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
	) {
		Row(
			modifier = Modifier
				.fillMaxWidth()
				.padding(12.dp),
			verticalAlignment = Alignment.CenterVertically,
		) {
			Box(
				modifier = Modifier
					.size(52.dp)
					.clip(CircleShape)
					.background(MaterialTheme.colorScheme.primaryContainer),
				contentAlignment = Alignment.Center,
			) {
				Icon(Icons.Filled.AccountCircle, null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
				repository.publicUserImageUrl(discovery, user)?.let { imageUrl ->
					AsyncImage(
						model = imageUrl,
						contentDescription = null,
						modifier = Modifier.fillMaxSize(),
						contentScale = ContentScale.Crop,
					)
				}
			}
			Spacer(Modifier.width(14.dp))
			Column(Modifier.weight(1f)) {
				Text(user.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
				if (user.hasPassword || user.hasConfiguredPassword) {
					Text("Password required", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
				}
			}
		}
	}
}

@Composable
fun PasswordField(
	password: String,
	onPasswordChange: (String) -> Unit,
	visible: Boolean,
	onVisibilityChange: () -> Unit,
	onDone: () -> Unit,
) {
	OutlinedTextField(
		value = password,
		onValueChange = onPasswordChange,
		modifier = Modifier
			.fillMaxWidth()
			.semantics {
				contentType = ContentType.Password
			},
		label = { Text("Password") },
		leadingIcon = { Icon(Icons.Filled.Lock, null) },
		trailingIcon = { IconButton(onClick = onVisibilityChange) { Icon(Icons.Default.RemoveRedEye, null) } },
		singleLine = true,
		visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
		keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
		keyboardActions = KeyboardActions(onDone = { onDone() }),
	)
}

@Composable
fun SignInButton(
	text: String,
	busy: Boolean,
	enabled: Boolean,
	onClick: () -> Unit,
) {
	Button(
		onClick = onClick,
		modifier = Modifier
			.fillMaxWidth()
			.height(52.dp),
		enabled = ! busy && enabled,
	) {
		if (busy) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
		else Text(text)
	}
}

@Composable
fun SignInError(message: String) {
	Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
}
