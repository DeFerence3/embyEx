package app.deference.embycl.ui.core.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Keep introductions inside scrolling content on compact screens. */
@Composable
fun PageIntro(
    eyebrow: String,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 12.dp, topEnd = 32.dp, bottomEnd = 12.dp, bottomStart = 32.dp),
        color = if (accent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = if (accent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(eyebrow, style = MaterialTheme.typography.labelLarge)
            Text(title, style = if (LocalDensity.current.fontScale > 1.3f) MaterialTheme.typography.titleLarge else MaterialTheme.typography.headlineLarge, modifier = Modifier.semantics { heading() })
            Text(description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun SectionHeading(title: String, modifier: Modifier = Modifier, detail: String? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).semantics { heading() })
        if (detail != null) Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
            Text(detail, Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun ExpressiveEmblem(icon: ImageVector, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(72.dp),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 20.dp, bottomEnd = 28.dp, bottomStart = 12.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, null, Modifier.size(32.dp)) }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExpressiveLoading(modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LoadingIndicator()
        Text("Loading your library…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
