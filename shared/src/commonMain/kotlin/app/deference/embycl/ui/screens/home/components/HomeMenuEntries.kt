package app.deference.embycl.ui.screens.home.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.RemoveDone
import androidx.compose.material.icons.outlined.Tv
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.runtime.Composable
import app.deference.embycl.ui.screens.home.model.HomeCardKind
import app.deference.embycl.ui.screens.home.model.HomeCardUi
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.home_menu_add_favorite
import embyex.shared.generated.resources.home_menu_download
import embyex.shared.generated.resources.home_menu_go_to_series
import embyex.shared.generated.resources.home_menu_mark_unwatched
import embyex.shared.generated.resources.home_menu_mark_watched
import embyex.shared.generated.resources.home_menu_needs_server
import embyex.shared.generated.resources.home_menu_remove_continue
import embyex.shared.generated.resources.home_menu_remove_favorite
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun homeMenuEntries(
    card: HomeCardUi,
    canChangeOnServer: Boolean,
    serverName: String,
    onToggleWatched: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenSeries: () -> Unit,
    onRemoveFromContinue: () -> Unit,
    onDownload: () -> Unit,
): List<HomeMenuEntry> {
    val needsServer = if (canChangeOnServer) {
        null
    } else {
        stringResource(
            resource = Res.string.home_menu_needs_server,
            serverName,
        )
    }
    return buildList {
        add(
            HomeMenuEntry(
                icon = if (card.isWatched) Icons.Outlined.RemoveDone else Icons.Outlined.CheckCircle,
                label = stringResource(resource = if (card.isWatched) Res.string.home_menu_mark_unwatched else Res.string.home_menu_mark_watched),
                disabledReason = if (canChangeOnServer || card.isDownloaded) null else needsServer,
                onClick = onToggleWatched,
            ),
        )
        if (card.kind != HomeCardKind.Unstarted) {
            add(
                HomeMenuEntry(
                    icon = Icons.Outlined.VisibilityOff,
                    label = stringResource(resource = Res.string.home_menu_remove_continue),
                    disabledReason = needsServer,
                    onClick = onRemoveFromContinue,
                ),
            )
        }
        val series = card.seriesName
        if (card.seriesId != null && series != null) {
            add(
                HomeMenuEntry(
                    icon = Icons.Outlined.Tv,
                    label = stringResource(
                        resource = Res.string.home_menu_go_to_series,
                        series,
                    ),
                    disabledReason = needsServer,
                    onClick = onOpenSeries,
                ),
            )
        }
        add(
            HomeMenuEntry(
                icon = if (card.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                label = stringResource(resource = if (card.isFavorite) Res.string.home_menu_remove_favorite else Res.string.home_menu_add_favorite),
                disabledReason = needsServer,
                onClick = onToggleFavorite,
            ),
        )
        if (!card.isDownloaded && !card.isFolder) {
            add(
                HomeMenuEntry(
                    icon = Icons.Outlined.Download,
                    label = stringResource(resource = Res.string.home_menu_download),
                    disabledReason = needsServer,
                    onClick = onDownload,
                ),
            )
        }
    }
}
