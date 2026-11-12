package app.deference.embycl.ui.screens.settings.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import app.deference.embycl.domain.model.home.HomeCardSize
import app.deference.embycl.domain.model.home.HomeLayoutSync
import app.deference.embycl.domain.model.home.HomeTop
import app.deference.embycl.theme.EmbympvTheme
import app.deference.embycl.ui.core.adaptive.DeviceConfiguration
import app.deference.embycl.ui.core.adaptive.PreviewDeviceConfiguration
import app.deference.embycl.ui.screens.home.model.HomeSectionKind
import app.deference.embycl.ui.screens.settings.home.components.HomeLayoutSettingsContent
import app.deference.embycl.ui.screens.settings.home.model.HomeLayoutRowUi

private val loadedState = HomeLayoutSettingsState(
    isLoading = false,
    rows = listOf(
        HomeLayoutRowUi(key = "continue", kind = HomeSectionKind.ContinueWatching, visible = true, pinned = true, canMoveUp = false, canMoveDown = false),
        HomeLayoutRowUi(key = "latest:movies", kind = HomeSectionKind.LatestIn(libraryName = "Movies"), visible = true, pinned = false, canMoveUp = false, canMoveDown = true),
        HomeLayoutRowUi(key = "latest:shows", kind = HomeSectionKind.LatestIn(libraryName = "TV Shows"), visible = true, pinned = false, canMoveUp = true, canMoveDown = true),
        HomeLayoutRowUi(key = "latest:music", kind = HomeSectionKind.LatestIn(libraryName = "Music"), visible = false, pinned = false, canMoveUp = true, canMoveDown = true),
        HomeLayoutRowUi(key = "favorites", kind = HomeSectionKind.Favorites, visible = true, pinned = false, canMoveUp = true, canMoveDown = true),
        HomeLayoutRowUi(key = "downloaded", kind = HomeSectionKind.Downloaded, visible = false, pinned = false, canMoveUp = true, canMoveDown = false),
    ),
)

@Composable
private fun SettingsPreview(
    state: HomeLayoutSettingsState,
    dark: Boolean = false,
    configuration: DeviceConfiguration = DeviceConfiguration.MOBILE_PORTRAIT,
) {
    EmbympvTheme(
        darkTheme = dark,
        dynamicColor = false,
    ) {
        PreviewDeviceConfiguration(configuration = configuration) {
            HomeLayoutSettingsContent(
                state = state,
                onAction = {},
                onBack = {},
            )
        }
    }
}

@Preview(name = "Edit Home · phone · light", widthDp = 393, heightDp = 873)
@Composable
private fun PhonePreview() = SettingsPreview(state = loadedState)

@Preview(name = "Edit Home · phone · dark", widthDp = 393, heightDp = 873)
@Composable
private fun PhoneDarkPreview() = SettingsPreview(
    state = loadedState,
    dark = true,
)

@Preview(name = "Edit Home · wide", widthDp = 1280, heightDp = 800)
@Composable
private fun WidePreview() = SettingsPreview(
    state = loadedState,
    configuration = DeviceConfiguration.DESKTOP,
)

@Preview(name = "Edit Home · top off, large cards", widthDp = 393, heightDp = 873)
@Composable
private fun TopOffPreview() = SettingsPreview(
    state = loadedState.copy(
        top = HomeTop.Off,
        cardSize = HomeCardSize.Large,
        rows = loadedState.rows.map { it.copy(pinned = false, canMoveUp = it.key != "continue") },
    ),
)

@Preview(name = "Edit Home · everything hidden", widthDp = 393, heightDp = 873)
@Composable
private fun NothingShownPreview() = SettingsPreview(
    state = loadedState.copy(
        top = HomeTop.Off,
        rows = loadedState.rows.map { it.copy(visible = false, pinned = false) },
    ),
)

@Preview(name = "Edit Home · many sections", widthDp = 393, heightDp = 873)
@Composable
private fun ManySectionsPreview() = SettingsPreview(
    state = loadedState.copy(rows = loadedState.rows.map { it.copy(visible = true) }),
)

@Preview(name = "Edit Home · offline", widthDp = 393, heightDp = 873)
@Composable
private fun OfflinePreview() = SettingsPreview(state = loadedState.copy(sync = HomeLayoutSync.LocalOnly))

@Preview(name = "Edit Home · saving", widthDp = 393, heightDp = 873)
@Composable
private fun SavingPreview() = SettingsPreview(state = loadedState.copy(sync = HomeLayoutSync.Saving))

@Preview(name = "Edit Home · loading", widthDp = 393, heightDp = 873)
@Composable
private fun LoadingPreview() = SettingsPreview(state = HomeLayoutSettingsState())

@Preview(name = "Edit Home · font scale 2x", widthDp = 393, heightDp = 873)
@Composable
private fun LargeFontPreview() {
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density,
            fontScale = 2f,
        ),
    ) {
        SettingsPreview(state = loadedState)
    }
}
