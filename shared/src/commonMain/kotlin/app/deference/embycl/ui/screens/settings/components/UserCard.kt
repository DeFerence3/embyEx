package app.deference.embycl.ui.screens.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun UserCard(
	account: SettingsAccount,
	onSignout: () -> Unit,
	modifier: Modifier = Modifier,
) {
	Card(modifier = modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
		Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(56.dp).clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.secondaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Person, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                AsyncImage(
                    model = "${account.serverUrl}/Users/${account.userId}/Images/Primary?MaxWidth=160&Quality=90",
                    contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                )
            }
			Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
				Text(account.username, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
				Text(account.serverName, color = MaterialTheme.colorScheme.onSurfaceVariant)
				Text("Signed in", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
			}

			IconButton(
				modifier = Modifier
					.align(Alignment.CenterVertically),
				onClick = onSignout
			){
				Icon(
					imageVector = Icons.AutoMirrored.Filled.Logout,
					contentDescription = "Sign out",
					tint = MaterialTheme.colorScheme.error
				)
			}
		}
	}
}
