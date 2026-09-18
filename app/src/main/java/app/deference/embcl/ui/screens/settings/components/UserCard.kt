package app.deference.embcl.ui.screens.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.deference.embcl.core.session.Session
import coil3.compose.AsyncImage

@Composable
fun UserCard(
	account: SettingsAccount,
	modifier: Modifier = Modifier,
) {
	OutlinedCard(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.extraLarge) {
		Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
			AsyncImage(
				model = Session.user.primaryImageUrl,
				contentDescription = "Account",
				modifier = Modifier
					.size(72.dp)
					.clip(CircleShape)
					.background(MaterialTheme.colorScheme.secondaryContainer),
				contentScale = ContentScale.Crop,
				error = rememberVectorPainter(Icons.Outlined.Person),
				placeholder = rememberVectorPainter(Icons.Outlined.Person),
			)
			Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
				Text(account.username, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
				Text(account.serverName, color = MaterialTheme.colorScheme.onSurfaceVariant)
				Text("Signed in", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
			}
		}
	}
}