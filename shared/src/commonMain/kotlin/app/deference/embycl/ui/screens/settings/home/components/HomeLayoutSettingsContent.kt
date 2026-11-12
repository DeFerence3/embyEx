package app.deference.embycl.ui.screens.settings.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.deference.embycl.domain.model.home.HomeCardSize
import app.deference.embycl.domain.model.home.HomeLayoutSync
import app.deference.embycl.domain.model.home.HomeTop
import app.deference.embycl.ui.core.LocalKoasty
import app.deference.embycl.ui.core.components.DetailTopBar
import app.deference.embycl.ui.core.components.ExpressiveLoading
import app.deference.embycl.ui.core.focusRing
import app.deference.embycl.ui.screens.settings.components.InfoHeader
import app.deference.embycl.ui.screens.settings.components.calculateShape
import app.deference.embycl.ui.screens.settings.home.HomeLayoutSettingsAction
import app.deference.embycl.ui.screens.settings.home.HomeLayoutSettingsState
import embyex.shared.generated.resources.Res
import embyex.shared.generated.resources.home_edit
import embyex.shared.generated.resources.home_edit_card_comfortable
import embyex.shared.generated.resources.home_edit_card_large
import embyex.shared.generated.resources.home_edit_card_size
import embyex.shared.generated.resources.home_edit_hint
import embyex.shared.generated.resources.home_edit_local_only
import embyex.shared.generated.resources.home_edit_reset
import embyex.shared.generated.resources.home_edit_saved
import embyex.shared.generated.resources.home_edit_saving
import embyex.shared.generated.resources.home_edit_sections
import embyex.shared.generated.resources.home_edit_top
import embyex.shared.generated.resources.home_edit_top_continue
import embyex.shared.generated.resources.home_edit_top_off
import embyex.shared.generated.resources.home_notice_layout_reset
import embyex.shared.generated.resources.home_notice_undo_hint
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * Edit Home as a Settings page: one centred column (max 680 dp) at every window size, like the rest of Settings.
 * A small preview of Home sits on top and follows every change, which applies at once; "Reset to default" offers Undo
 * in a toast.
 */
@Composable
internal fun HomeLayoutSettingsContent(
    state: HomeLayoutSettingsState,
    onAction: (HomeLayoutSettingsAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val koasty = LocalKoasty.current
    val currentOnAction by rememberUpdatedState(newValue = onAction)
    LaunchedEffect(key1 = state.resetNotice) {
        if (!state.resetNotice) return@LaunchedEffect
        val title = getString(resource = Res.string.home_notice_layout_reset)
        val hint = getString(resource = Res.string.home_notice_undo_hint)
        koasty.show(
            title = title,
            message = hint,
            onClick = {
                koasty.dismiss()
                currentOnAction(HomeLayoutSettingsAction.UndoReset)
            },
        )
        // Acknowledged last: it changes this effect's key, which cancels the effect.
        currentOnAction(HomeLayoutSettingsAction.ResetNoticeShown)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            DetailTopBar(
                title = stringResource(resource = Res.string.home_edit),
                onBack = onBack,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues = padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (state.isLoading) {
                ExpressiveLoading(modifier = Modifier.align(alignment = Alignment.Center))
            } else {
                LayoutList(
                    state = state,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
private fun LayoutList(
    state: HomeLayoutSettingsState,
    onAction: (HomeLayoutSettingsAction) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .widthIn(max = 680.dp)
            .fillMaxSize(),
        contentPadding = PaddingValues(all = 20.dp),
        verticalArrangement = Arrangement.spacedBy(space = 3.dp),
    ) {
        item(key = "preview") {
            HomeLayoutPreview(
                top = state.top,
                cardSize = state.cardSize,
                rows = state.rows,
                modifier = Modifier.padding(bottom = 20.dp),
            )
        }
        item(key = "intro") {
            Column(verticalArrangement = Arrangement.spacedBy(space = 12.dp)) {
                Text(
                    text = stringResource(resource = Res.string.home_edit_hint),
                    style = MaterialTheme.typography.bodyLarge,
                )
                SyncLine(sync = state.sync)
            }
        }
        item(key = "top") {
            ChoiceGroup(
                title = stringResource(resource = Res.string.home_edit_top),
                options = listOf(
                    HomeTop.ContinueWatching to stringResource(resource = Res.string.home_edit_top_continue),
                    HomeTop.Off to stringResource(resource = Res.string.home_edit_top_off),
                ),
                selected = state.top,
                onSelect = { onAction(HomeLayoutSettingsAction.SetTop(top = it)) },
            )
        }
        item(key = "card-size") {
            ChoiceGroup(
                title = stringResource(resource = Res.string.home_edit_card_size),
                options = listOf(
                    HomeCardSize.Comfortable to stringResource(resource = Res.string.home_edit_card_comfortable),
                    HomeCardSize.Large to stringResource(resource = Res.string.home_edit_card_large),
                ),
                selected = state.cardSize,
                onSelect = { onAction(HomeLayoutSettingsAction.SetCardSize(size = it)) },
            )
        }
        item(key = "sections-title") {
            InfoHeader(
                title = stringResource(resource = Res.string.home_edit_sections),
                onRefresh = null,
                modifier = Modifier.padding(
                    top = 28.dp,
                    bottom = 8.dp,
                ),
            )
        }
        itemsIndexed(
            items = state.rows,
            key = { _, row -> row.key },
        ) { index, row ->
            HomeLayoutSectionRow(
                row = row,
                shape = calculateShape(
                    index = index,
                    lastIndex = state.rows.lastIndex,
                ),
                onToggle = { onAction(HomeLayoutSettingsAction.ToggleSection(key = row.key)) },
                onMove = { up ->
                    onAction(
                        HomeLayoutSettingsAction.MoveSection(
                            key = row.key,
                            up = up,
                        ),
                    )
                },
                modifier = Modifier.animateItem(),
            )
        }
        item(key = "reset") {
            OutlinedButton(
                onClick = { onAction(HomeLayoutSettingsAction.Reset) },
                modifier = Modifier
                    .padding(top = 28.dp)
                    .heightIn(min = 48.dp)
                    .focusRing(shape = CircleShape),
            ) {
                Icon(
                    imageVector = Icons.Outlined.RestartAlt,
                    contentDescription = null,
                )
                Text(
                    text = stringResource(resource = Res.string.home_edit_reset),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun <T> ChoiceGroup(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Column(
        modifier = Modifier.padding(top = 28.dp),
        verticalArrangement = Arrangement.spacedBy(space = 12.dp),
    ) {
        InfoHeader(
            title = title,
            onRefresh = null,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (value, label) ->
                SegmentedButton(
                    selected = selected == value,
                    onClick = { onSelect(value) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size,
                    ),
                    modifier = Modifier.heightIn(min = 48.dp),
                ) { Text(text = label) }
            }
        }
    }
}

@Composable
private fun SyncLine(sync: HomeLayoutSync) {
    val (icon, text) = when (sync) {
        HomeLayoutSync.Saved -> Icons.Outlined.CloudDone to stringResource(resource = Res.string.home_edit_saved)
        HomeLayoutSync.Saving -> Icons.Outlined.Sync to stringResource(resource = Res.string.home_edit_saving)
        HomeLayoutSync.LocalOnly -> Icons.Outlined.Smartphone to stringResource(resource = Res.string.home_edit_local_only)
    }
    Row(
        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(size = 18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
