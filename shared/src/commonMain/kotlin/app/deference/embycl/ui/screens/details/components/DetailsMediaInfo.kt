package app.deference.embycl.ui.screens.details.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.screens.details.model.MediaFactUi
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.details_media_info
import embyex.shared.generated.resources.details_tracks_count
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DetailsMediaInfo(
    facts: List<MediaFactUi>,
    modifier: Modifier = Modifier,
    horizontal: Boolean = false,
) {
    if (facts.isEmpty()) return
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(space = if (horizontal) 12.dp else 8.dp),
    ) {
        SectionTitle(text = stringResource(resource = Res.string.details_media_info))
        if (horizontal) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(space = 40.dp),
                verticalArrangement = Arrangement.spacedBy(space = 12.dp),
            ) {
                facts.forEach { fact ->
                    Column(
                        modifier = Modifier.semantics(mergeDescendants = true) {},
                        verticalArrangement = Arrangement.spacedBy(space = 2.dp),
                    ) {
                        Text(
                            text = stringResource(resource = fact.label),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            text = fact.valueText(),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        } else {
            facts.forEach { fact ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(space = 16.dp),
                    modifier = Modifier.semantics(mergeDescendants = true) {},
                ) {
                    Text(
                        text = stringResource(resource = fact.label),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.widthIn(min = 80.dp),
                    )
                    Text(
                        text = fact.valueText(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun MediaFactUi.valueText(): String {
    val count = trackCount?.let { tracks ->
        pluralStringResource(
            resource = Res.plurals.details_tracks_count,
            quantity = tracks,
            tracks,
        )
    }
    return listOfNotNull(
        count,
        value,
    ).joinToString(separator = " · ")
}
