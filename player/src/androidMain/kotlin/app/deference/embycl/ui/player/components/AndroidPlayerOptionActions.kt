package app.deference.embycl.ui.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.deference.coreui.DropDown
import app.deference.coreui.DropDownItem
import embyex.player.generated.resources.Res
import embyex.player.generated.resources.player_external
import embyex.player.generated.resources.player_more
import org.jetbrains.compose.resources.stringResource

/**
 * More options uses a sheet in portrait and a menu anchored to its button in landscape.
 * Add entries to the items list below to expose the same labelled actions in both layouts.
 * The caller owns expansion so the player can keep its controls visible while this is open.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AndroidPlayerOptionActions(
    portrait: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onOpenExternalPlayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val title = stringResource(resource = Res.string.player_more)
    // Keep this list limited to actions that are not already on the player toolbar.
    val items = listOf(
        DropDownItem(
            text = stringResource(resource = Res.string.player_external),
            icon = Icons.AutoMirrored.Filled.OpenInNew,
            onClick = {
                onExpandedChange(false)
                onOpenExternalPlayer()
            },
        ),
    )
    Box(modifier = modifier) {
        AndroidPlayerButton(
            icon = Icons.Default.MoreVert,
            description = title,
            onClick = { onExpandedChange(true) },
        )
        if (portrait) {
            if (expanded) {
                ModalBottomSheet(
                    onDismissRequest = { onExpandedChange(false) },
                    sheetState = rememberBottomSheetState(
                        initialValue = SheetValue.Hidden,
                        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded),
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(state = rememberScrollState())
                            .padding(bottom = 24.dp),
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        )
                        items.forEach { item ->
                            ListItem(
                                headlineContent = { Text(text = item.text) },
                                leadingContent = {
                                    Icon(imageVector = item.icon, contentDescription = null)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(role = Role.Button, onClick = item.onClick),
                            )
                        }
                    }
                }
            }
        } else {
            DropDown(
                expanded = expanded,
                items = items,
                onDismiss = { onExpandedChange(false) },
            )
        }
    }
}
