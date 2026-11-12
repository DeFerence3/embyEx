package app.deference.embycl.ui.screens.details.components

import app.deference.embycl.ui.core.components.ShelfHeader
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.deference.embycl.ui.screens.details.model.PersonKind
import app.deference.embycl.ui.screens.details.model.PersonUi
import coil3.compose.AsyncImage
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.details_cast_and_crew
import embyex.shared.generated.resources.details_role_director
import embyex.shared.generated.resources.details_role_writer
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun DetailsPeopleRail(
    people: List<PersonUi>,
    horizontalPadding: Dp,
    showScrollButtons: Boolean,
    modifier: Modifier = Modifier,
) {
    if (people.isEmpty()) return
    val listState = rememberLazyListState()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(space = 12.dp),
    ) {
        ShelfHeader(
            title = stringResource(resource = Res.string.details_cast_and_crew),
            listState = listState,
            showScrollButtons = showScrollButtons,
            horizontalPadding = horizontalPadding,
        )
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(space = 12.dp),
        ) {
            itemsIndexed(
                items = people,
                key = { index, person -> person.id ?: "${person.name}-$index" },
            ) { index, person ->
                PersonTile(
                    person = person,
                    index = index,
                )
            }
        }
    }
}

@Composable
private fun PersonTile(
    person: PersonUi,
    index: Int,
) {
    val role = when (person.kind) {
        PersonKind.Cast -> person.role
        PersonKind.Director -> stringResource(resource = Res.string.details_role_director)
        PersonKind.Writer -> stringResource(resource = Res.string.details_role_writer)
    }
    val description = listOfNotNull(
        person.name,
        role,
    ).joinToString(separator = ", ")
    Column(
        modifier = Modifier
            .width(width = 104.dp)
            .semantics(mergeDescendants = true) { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        val colors = MaterialTheme.colorScheme
        val (container, content) = when (index % 3) {
            0 -> colors.primaryContainer to colors.onPrimaryContainer
            1 -> colors.tertiaryContainer to colors.onTertiaryContainer
            else -> colors.secondaryContainer to colors.onSecondaryContainer
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio = 3f / 4f)
                .clip(shape = MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                color = container,
                modifier = Modifier.matchParentSize(),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = person.initials,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            }
            if (!person.photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = person.photoUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }
        Text(
            text = person.name,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        role?.let { text ->
            if (person.kind == PersonKind.Cast) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Surface(
                    shape = MaterialTheme.shapes.extraSmall,
                    color = colors.surfaceContainerHigh,
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            }
        }
    }
}
