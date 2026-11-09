package app.deference.embycl.ui.screens.details.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun DetailsTopBar(
    title: String?,
    collapsed: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedContent(
        targetState = collapsed && title != null,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        modifier = modifier.fillMaxWidth(),
        label = "DetailsTopBar",
    ) { solid ->
        if (solid) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(
                            horizontal = 12.dp,
                            vertical = 8.dp,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
                ) {
                    DetailsBackButton(onClick = onBack)
                    Text(
                        text = title.orEmpty(),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(all = 12.dp),
            ) {
                DetailsBackButton(onClick = onBack)
            }
        }
    }
}
