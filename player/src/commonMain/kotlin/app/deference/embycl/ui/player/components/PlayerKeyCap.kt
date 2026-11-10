package app.deference.embycl.ui.player.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** A key cap such as "M" or "Shift". Decorative next to a label that already names the action. */
@Composable
internal fun PlayerKeyCap(
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0f),
        contentColor = LocalContentColor.current,
        border = BorderStroke(
            width = 1.dp,
            color = LocalContentColor.current,
        ),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .defaultMinSize(minWidth = 20.dp)
                .padding(horizontal = 5.dp, vertical = 1.dp),
        )
    }
}
