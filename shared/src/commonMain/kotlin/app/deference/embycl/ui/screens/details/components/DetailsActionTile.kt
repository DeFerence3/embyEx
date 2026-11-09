package app.deference.embycl.ui.screens.details.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.core.animateWithHover

@Composable
internal fun DetailsActionTile(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    checked: Boolean? = null,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    icon: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val container = if (checked == true) MaterialTheme.colorScheme.primaryContainer else containerColor
    val tileModifier = modifier
        .heightIn(min = 64.dp)
        .animateWithHover(interactionSource = interactionSource)
        .alpha(alpha = if (enabled) 1f else 0.5f)
    val body: @Composable () -> Unit = {
        Box(contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier.padding(
                    horizontal = 4.dp,
                    vertical = 8.dp,
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    space = 4.dp,
                    alignment = Alignment.CenterVertically,
                ),
            ) {
                Box(
                    modifier = Modifier.size(size = 24.dp),
                    contentAlignment = Alignment.Center,
                ) { icon() }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
    if (checked != null) {
        Surface(
            checked = checked,
            onCheckedChange = { onClick() },
            enabled = enabled,
            shape = MaterialTheme.shapes.medium,
            color = container,
            interactionSource = interactionSource,
            modifier = tileModifier,
            content = body,
        )
    } else {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = MaterialTheme.shapes.medium,
            color = container,
            interactionSource = interactionSource,
            modifier = tileModifier,
            content = body,
        )
    }
}
