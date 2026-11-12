package app.deference.embycl.ui.screens.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.deference.coreui.DropDown

@Composable
internal fun HomeCardMenuDropDown(
    expanded: Boolean,
    entries: List<HomeMenuEntry>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DropDown(
        expanded = expanded,
        onDismiss = onDismiss,
        modifier = modifier,
    ) {
        entries.forEach { entry ->
            DropdownMenuItem(
                text = {
                    Column {
                        Text(text = entry.label)
                        entry.disabledReason?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                leadingIcon = {
                    Icon(
                        imageVector = entry.icon,
                        contentDescription = null,
                    )
                },
                enabled = entry.disabledReason == null,
                onClick = {
                    onDismiss()
                    entry.onClick()
                },
            )
        }
    }
}
